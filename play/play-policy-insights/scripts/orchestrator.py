#!/usr/bin/env python3

# Copyright 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Coordinates the end-to-end Play Policy audit flow.

This manages everything from triage to analysis aggregation.
"""

import argparse
import concurrent.futures
import glob
import json
import os
import re
import sys
import uuid
import xml.etree.ElementTree as ET

import scanner
from template_engine import render_template

# Constants for Gradle parsing
GRADLE_APP_ID_PATTERN = (
    r'applicationId\s*(?:[:=]|\.set\(?)\s*[\'"]?([a-zA-Z0-9._]+)[\'"]?\)?'
)
GRADLE_APP_ID_PROP_PATTERN = (
    r'applicationId\s*[:=]?\s*project\.property\(\s*[\'"]([^\'"]+)[\'"]\s*\)'
)
GRADLE_NAMESPACE_PATTERN = (
    r'namespace\s*(?:[:=]|\.set\(?)\s*[\'"]?([a-zA-Z0-9._]+)[\'"]?\)?'
)
GRADLE_NAMESPACE_PROP_PATTERN = (
    r'namespace\s*[:=]?\s*project\.property\(\s*[\'"]([^\'"]+)[\'"]\s*\)'
)
GRADLE_TARGET_SDK_PATTERN = (
    r"targetSdk(?:Version)?(?:\.set\(?)?\s*[:="
    r" (]*\s*([\'\"]?[\w\d_\.]+[\'\"]?)\)?"
)

# Well-known framework defaults for unresolved dynamic variables (e.g. Flutter)
FRAMEWORK_DEFAULTS = {
    "flutter.targetSdkVersion": 35,
    "flutter.compileSdkVersion": 35,
    "flutter.minSdkVersion": 21,
    "flutter.ndkVersion": "25.1.8937393",
}

# Patterns for diverse assignment styles (Groovy & Kotlin DSL)
# 1. Standard: targetSdkVersion = 35, ext.targetSdkVersion = 35, etc.
# 2. Delegated: val targetSdkVersion by extra(35)
# 3. Functional: set("targetSdkVersion", 35) or
#    extra.set("targetSdkVersion", 35)
# 4. Indexer: extra["targetSdkVersion"] = 35
GRADLE_ASSIGNMENT_PATTERNS = [
    re.compile(
        r"(?:\b(?:val|var|def|const|static|internal|private|"
        r"public|protected|ext|final)\s+)*"
        r"([\w\d_\.]+)(?:\s*:\s*[\w\d_\.]+)?\s*=\s*['\"]?"
        r"([^\x27\"\s\(\)<>\{\}]+)['\"]?"
    ),
    re.compile(
        r"val\s+([\w\d_\.]+)(?:\s*:\s*[\w\d_\.]+)?\s+by\s+"
        r"extra\s*\(\s*['\"]?([^\x27\"\s\(\)]+)['\"]?\s*\)"
    ),
    re.compile(
        r"(?:extra\.)?set\s*\(\s*['\"]([\w\d_\.]+)['\"]\s*,\s*"
        r"['\"]?([^\x27\"\s\(\)]+)['\"]?\s*\)"
    ),
    re.compile(
        r"extra\s*\[\s*['\"]([\w\d_\.]+)['\"]\s*\]\s*=\s*"
        r"['\"]?([^\x27\"\s]+)['\"]?"
    ),
]


def _strip_inline_comment(line, support_quotes=True):
  """Strips inline comments starting with '#' while preserving quotes."""
  in_double = False
  in_single = False
  for i, char in enumerate(line):
    if support_quotes:
      if char == '"' and not in_single:
        in_double = not in_double
      elif char == "'" and not in_double:
        in_single = not in_single

    if char == "#" and not in_double and not in_single:
      return line[:i]
  return line


def load_gradle_properties(target_dir):
  """Loads properties from gradle.properties and local.properties."""
  properties = {}
  for filename in ["gradle.properties", "local.properties"]:
    path = os.path.join(target_dir, filename)
    if os.path.exists(path):
      try:
        with open(path, "r", encoding="utf-8", errors="ignore") as f:
          for line in f:
            line = _strip_inline_comment(line, support_quotes=False).strip()
            if line and not line.startswith("#") and "=" in line:
              key, val = line.split("=", 1)
              properties[key.strip()] = val.strip()
      except Exception as e:  # pylint: disable=broad-exception-caught
        print(
            f"Warning: Failed to load properties from {path}: {e}",
            file=sys.stderr,
        )
  return properties


def load_version_catalog(target_dir):
  """Loads versions from gradle/libs.versions.toml if it exists."""
  catalog = {}
  toml_path = os.path.join(target_dir, "gradle", "libs.versions.toml")
  if os.path.exists(toml_path):
    try:
      with open(toml_path, "r", encoding="utf-8", errors="ignore") as f:
        current_section = None
        for line in f:
          line = _strip_inline_comment(line).strip()
          if not line or line.startswith("#"):
            continue
          if line.startswith("[") and line.endswith("]"):
            current_section = line[1:-1].strip()
            continue
          if current_section == "versions" and "=" in line:
            key, val = line.split("=", 1)
            val = val.strip().strip('"').strip("'")
            catalog[key.strip()] = val.strip()
    except Exception as e:  # pylint: disable=broad-exception-caught
      print(
          f"Warning: Failed to load version catalog from {toml_path}: {e}",
          file=sys.stderr,
      )
  return catalog


def load_custom_gradle_versions(target_dir, file_inventory=None):
  """Loads versions from shared Gradle files and project configuration."""
  versions = {}
  if not target_dir or not os.path.isdir(target_dir):
    return versions

  # 1. Identify Candidate Files (Root files are the primary targets)
  candidate_files = []

  # Leaking Strategy: If target_dir is a sub-module, leak up to find the root
  # e.g. /path/to/project/app -> check /path/to/project for root build files
  search_roots = [target_dir]
  parent = os.path.dirname(os.path.normpath(target_dir))
  if parent and parent != target_dir:
    search_roots.append(parent)

  for s_root in search_roots:
    for f in ["build.gradle", "build.gradle.kts"]:
      root_file = os.path.normpath(os.path.join(s_root, f))
      if os.path.exists(root_file) and root_file not in candidate_files:
        candidate_files.append(root_file)

  # Use inventory if provided to avoid redundant file walk
  if file_inventory:
    search_lists = [
        file_inventory.get("gradles", []),
        file_inventory.get("source_code", []),
    ]
    for file_list in search_lists:
      for path in file_list:
        norm_path = os.path.normpath(path)
        if norm_path not in candidate_files:
          file_lower = os.path.basename(norm_path).lower()
          if any(
              name in file_lower
              for name in [
                  "versions.gradle",
                  "dependencies.gradle",
                  "variables.gradle",
              ]
          ):
            candidate_files.append(norm_path)
          elif any(
              d in norm_path.lower() for d in ["build-logic", "buildsrc"]
          ) and file_lower.endswith((".gradle", ".gradle.kts", ".kt")):
            candidate_files.append(norm_path)
  else:
    # Fallback to walk if no inventory provided
    config = scanner.get_scanner_config()
    ignored_dirs = set(config.get("ignored_directories", []))
    for root, dirs, files in os.walk(target_dir):
      dirs[:] = [d for d in dirs if d not in ignored_dirs]
      for f in files:
        if f.lower().endswith((".gradle", ".gradle.kts", ".kt")):
          candidate_files.append(os.path.join(root, f))

  # 2. Parse candidate files for assignments
  for f_path in candidate_files:
    try:
      with open(f_path, "r", encoding="utf-8", errors="ignore") as f:
        content = f.read()
        for pattern in GRADLE_ASSIGNMENT_PATTERNS:
          for key, val in pattern.findall(content):
            # Clean up the key (remove 'ext.' or 'project.' prefixes)
            clean_key = key.split(".")[-1]
            if clean_key not in versions:
              versions[clean_key] = val.strip().strip('"').strip("'")
    except Exception as e:  # pylint: disable=broad-exception-caught
      print(
          f"Warning: Failed to parse Gradle file {f_path}: {e}", file=sys.stderr
      )

  return versions


def _load_json_file(path):
  """Safely loads a JSON file."""
  if os.path.exists(path):
    try:
      with open(path, "r", encoding="utf-8") as f:
        return json.load(f)
    except Exception as e:  # pylint: disable=broad-exception-caught
      print(f"Warning: Failed to load JSON from {path}: {e}", file=sys.stderr)
  return {}


def _read_text_file(path):
  """Safely reads a text file."""
  if os.path.exists(path):
    try:
      with open(path, "r", encoding="utf-8") as f:
        return f.read()
    except Exception as e:  # pylint: disable=broad-exception-caught
      print(f"Warning: Failed to read file {path}: {e}", file=sys.stderr)
  return ""


def _write_json_file(path, data):
  """Safely writes a JSON file."""
  try:
    with open(path, "w", encoding="utf-8") as f:
      json.dump(data, f, indent=4, sort_keys=True)
  except Exception as e:  # pylint: disable=broad-exception-caught
    print(f"Warning: Failed to write JSON to {path}: {e}", file=sys.stderr)


def _write_text_file(path, content):
  """Safely writes a text file."""
  try:
    with open(path, "w", encoding="utf-8") as f:
      f.write(content)
  except Exception as e:  # pylint: disable=broad-exception-caught
    print(f"Warning: Failed to write file {path}: {e}", file=sys.stderr)


def _filter_by_flavor(data, detected_flavors, prioritized_flavors):
  """Recursively filters lists and dicts based on flavor path segments."""
  if isinstance(data, dict):
    filtered_dict = {}
    for k, v in data.items():
      filtered_val = _filter_by_flavor(v, detected_flavors, prioritized_flavors)
      if isinstance(filtered_val, (dict, list)) and not filtered_val:
        continue
      filtered_dict[k] = filtered_val
    return filtered_dict

  elif isinstance(data, list):
    filtered_list = []
    for entry in data:
      # Handle tuple entries (like in codebase_map.all_files)
      entry_str = (
          entry[1]
          if isinstance(entry, tuple) and len(entry) > 1
          else str(entry)
      )

      is_excluded = any(
          f not in prioritized_flavors and f"src/{f}/" in entry_str
          for f in detected_flavors
      )
      if not is_excluded:
        filtered_list.append(entry)
    return filtered_list

  return data  # Fallback for primitive types


def parse_application_modules(
    gradle_files,
    manifest_files,
    gradle_properties,
    version_catalog,
    custom_gradle_versions=None,
):
  """Identifies distinct application modules in the project."""
  modules = []
  lookups = {
      "properties": gradle_properties,
      "catalog": version_catalog,
      "custom": custom_gradle_versions or {},
  }

  # First, check Gradle files for the application plugin
  for g_file in gradle_files:
    try:
      with open(g_file, "r", encoding="utf-8", errors="ignore") as f:
        content = f.read()
        if re.search(GRADLE_APP_PLUGIN_PATTERN, content):
          # Found an app module!
          mod_dir = os.path.dirname(g_file)
          mod_name = os.path.basename(mod_dir)

          # Try to extract details from this Gradle file
          app_id = None
          target_sdk = None
          app_label = None

          id_match = re.search(GRADLE_APP_ID_PATTERN, content)
          if id_match:
            app_id = id_match.group(1)
          else:
            prop_match = re.search(GRADLE_APP_ID_PROP_PATTERN, content)
            if prop_match:
              app_id = lookups["properties"].get(prop_match.group(1))

          sdk_match = re.search(GRADLE_TARGET_SDK_PATTERN, content)
          if sdk_match:
            target_sdk = _resolve_to_int(sdk_match.group(1), lookups)

          # Fallback to namespace if applicationId is missing
          if not app_id:
            ns_match = re.search(GRADLE_NAMESPACE_PATTERN, content)
            if ns_match:
              app_id = ns_match.group(1)
            else:
              ns_prop_match = re.search(GRADLE_NAMESPACE_PROP_PATTERN, content)
              if ns_prop_match:
                app_id = lookups["properties"].get(ns_prop_match.group(1))

          modules.append({
              "name": mod_name,
              "path": mod_dir,
              "application_id": app_id,
              "target_sdk": target_sdk,
              "app_label": app_label,
          })
    except Exception as e:  # pylint: disable=broad-exception-caught
      print(
          f"Warning: Failed to parse Gradle file {g_file}: {e}", file=sys.stderr
      )

  return modules


def determine_primary_identity(
    app_modules,
    gradle_files,
    manifest_files,
    gradle_properties,
    version_catalog,
    custom_gradle_versions,
    app_dir,
):
  """Heuristically determines the primary app ID and target SDK."""
  primary_id = None
  primary_sdk = None
  primary_label = None

  # 1. Prefer explicitly defined app modules
  for mod in app_modules:
    if mod.get("application_id"):
      primary_id = mod["application_id"]
      primary_sdk = mod["target_sdk"]
      primary_label = mod["app_label"]
      break

  # 2. Fallback to global Gradle properties
  if not primary_id:
    primary_id = gradle_properties.get("applicationId") or (
        gradle_properties.get("namespace")
    )
  if not primary_sdk:
    primary_sdk = _resolve_to_int(
        gradle_properties.get("targetSdkVersion")
        or gradle_properties.get("targetSdk"),
        {"properties": gradle_properties, "catalog": version_catalog},
    )

  # 3. Fallback to scanning manifests if still missing
  if not primary_id or not primary_sdk:
    for m_file in manifest_files:
      try:
        with open(m_file, "r", encoding="utf-8") as f:
          content = f.read()
          details = extract_manifest_details(content)
          if not primary_id:
            primary_id = details.get("package_name")
          if not primary_sdk:
            primary_sdk = details.get("target_sdk")
          if not primary_label:
            primary_label = details.get("app_label")
      except Exception:  # pylint: disable=broad-exception-caught
        pass

  # 4. Cross-Platform Framework Detection
  if not primary_id or not primary_sdk:
    # Check for Flutter (pubspec.yaml)
    pubspec_path = os.path.join(app_dir, "pubspec.yaml")
    if os.path.exists(pubspec_path):
      try:
        with open(pubspec_path, "r", encoding="utf-8") as f:
          for line in f:
            if line.startswith("name:"):
              primary_id = line.split(":")[1].strip()
              break
        primary_sdk = FRAMEWORK_DEFAULTS.get("flutter.targetSdkVersion")
      except Exception:  # pylint: disable=broad-exception-caught
        pass

    # Check for React Native (package.json)
    pkg_json_path = os.path.join(app_dir, "package.json")
    if os.path.exists(pkg_json_path):
      try:
        with open(pkg_json_path, "r", encoding="utf-8") as f:
          data = json.load(f)
          primary_id = data.get("name")
      except Exception:  # pylint: disable=broad-exception-caught
        pass

  return primary_id, primary_sdk, primary_label


def extract_manifest_details(xml_content):
  """Parses AndroidManifest.xml for key policy signals."""
  details = {
      "package_name": None,
      "target_sdk": None,
      "app_label": None,
      "permissions": [],
      "foreground_services": [],
  }

  try:
    # Handle XML with namespaces
    root = ET.fromstring(xml_content)
    ns = {"android": "http://schemas.android.com/apk/res/android"}

    details["package_name"] = root.get("package")

    # Target SDK
    uses_sdk = root.find("uses-sdk", ns)
    if uses_sdk is not None:
      details["target_sdk"] = uses_sdk.get(
          "{http://schemas.android.com/apk/res/android}targetSdkVersion"
      )

    # App Label
    application = root.find("application", ns)
    if application is not None:
      label = application.get(
          "{http://schemas.android.com/apk/res/android}label"
      )
      if label and label.startswith("@string/"):
        details["app_label"] = label[8:]
      else:
        details["app_label"] = label

    # Permissions
    for tag in ["uses-permission", "uses-permission-sdk-23"]:
      for perm in root.findall(tag, ns):
        name = perm.get("{http://schemas.android.com/apk/res/android}name")
        if name:
          details["permissions"].append(name)

    # Permissions from component declarations
    for tag in [
        "service",
        "receiver",
        "activity",
        "provider",
        "activity-alias",
    ]:
      for component in root.findall(f".//{tag}", ns):
        perm = component.get(
            "{http://schemas.android.com/apk/res/android}permission"
        )
        if perm:
          details["permissions"].append(perm)

        if tag == "provider":
          read_perm = component.get(
              "{http://schemas.android.com/apk/res/android}readPermission"
          )
          if read_perm:
            details["permissions"].append(read_perm)
          write_perm = component.get(
              "{http://schemas.android.com/apk/res/android}writePermission"
          )
          if write_perm:
            details["permissions"].append(write_perm)

    # Foreground Services
    for service in root.findall(".//service", ns):
      # Check for foregroundServiceType
      fgs_type = service.get(
          "{http://schemas.android.com/apk/res/android}foregroundServiceType"
      )
      if fgs_type:
        details["foreground_services"].append({
            "name": service.get(
                "{http://schemas.android.com/apk/res/android}name"
            ),
            "type": fgs_type,
        })
  except Exception as e:  # pylint: disable=broad-exception-caught
    print(f"Warning: Failed to parse XML: {e}", file=sys.stderr)

  return details


def generate_triage_summary(manifest_details, data_safety_scan):
  """Generates a high-level summary of the audit landscape."""
  summary = []
  
  package = manifest_details.get("package_name") or "Unknown"
  summary.append(f"Audit Target: {package}")
  
  sdk = manifest_details.get("target_sdk") or "Unknown"
  summary.append(f"Target SDK: {sdk}")

  perms_count = len(manifest_details.get("permissions", []))
  summary.append(f"Permissions Requested: {perms_count}")

  signals_count = 0
  if isinstance(data_safety_scan, dict):
    inner_scan = data_safety_scan.get("data_safety_scan", {})
    if "data_sources" in inner_scan:
      signals_count = len(inner_scan["data_sources"])
  summary.append(f"Sensitive Data Signals Found: {signals_count}")

  return "\n".join(summary)


def run_scanner_direct(target_dir, file_inventory, output_file):
  """Invokes the scanner directly using the provided inventory."""
  try:
    # This calls the internal scan logic from scanner.py
    results = scanner.perform_scan(target_dir, file_inventory)
    _write_json_file(output_file, results)
    return results
  except Exception as e:  # pylint: disable=broad-exception-caught
    print(f"Error during scan: {e}", file=sys.stderr)
    return {}


def write_agent_prompts(env_data, goal_map, output_data):
  """Generates surgical prompt files for worker agents and infers activation."""
  repo_root = env_data.get("repo_root")
  temp_dir = env_data.get("temp_dir")
  app_dir = env_data.get("app_dir")
  activated_goals = []

  # 1. Load Shared Resources
  policies_path = os.path.join(repo_root, "resources", "policies.json")
  policies_data = _load_json_file(policies_path)
  taxonomy = policies_data.get("data_safety_section", {}).get("taxonomy", {})
  taxonomy_keys = set(taxonomy.keys())

  common_mandates_path = os.path.join(
      repo_root, "resources", "common_mandates.md"
  )
  common_mandates = _read_text_file(common_mandates_path)

  config = scanner.get_scanner_config()
  permission_groups = config.get("permission_groups", {})

  # 2. Write Slices
  if output_data:
    for slice_name in ["manifest_details", "codebase_map"]:
      slice_path = os.path.join(temp_dir, f"{slice_name}.json")
      _write_json_file(slice_path, output_data.get(slice_name, {}))

  # 3. Build Base Context
  scan_results = output_data.get("data_safety_scan", {})
  base_context = scan_results.get("data_safety_scan", {}).copy()
  
  # Also ensure other metadata is included
  base_context["found_urls"] = scan_results.get("found_urls", [])
  base_context["semantic_files"] = scan_results.get("semantic_files", {})

  # Flatten anonymous categories (those with only a "-" key)
  for cat in [
      "exact_alarm",
      "accessibility",
      "foreground_service",
      "disclosure",
  ]:
    if (
        cat in base_context
        and isinstance(base_context[cat], dict)
        and "-" in base_context[cat]
    ):
      base_context[cat] = base_context[cat]["-"]

  manifest_details = output_data.get("manifest_details", {})
  base_context["TARGET_SDK"] = manifest_details.get("target_sdk")
  base_context["APP_NAME"] = output_data.get("app_name")
  base_context["PACKAGE_NAME"] = output_data.get("package_name")
  manifest_perms = manifest_details.get("permissions", [])

  # Populate requested_permissions dictionary based on groups
  requested_perms_dict = {}
  for group_name, perms_to_check in permission_groups.items():
    matched = [p for p in perms_to_check if p in manifest_perms]
    if matched:
      requested_perms_dict[group_name] = matched

  base_context["requested_permissions"] = requested_perms_dict
  base_context["TEMP_DIR"] = temp_dir
  base_context["APP_DIR"] = app_dir
  base_context["REPO_ROOT"] = repo_root

  # 4. Smart Filtering for data_sources (Noise Reduction)
  if "data_sources" in base_context:
    filtered_sources = {}
    for data_type, findings in base_context["data_sources"].items():
      # Deduplicate by File (max 2 per unique file)
      file_counts = {}
      diverse_findings = []
      for f in findings:
        file_path = f.split(":L")[0] if ":L" in f else f
        if file_counts.get(file_path, 0) < 2:
          diverse_findings.append(f)
          file_counts[file_path] = file_counts.get(file_path, 0) + 1

      # Global Cap (max 3 per data type)
      filtered_sources[data_type] = diverse_findings[:3]
    base_context["data_sources"] = filtered_sources

  max_evidence_per_worker = 6

  # Helper to evaluate and save a goal instance
  def _save_goal_instance(instance_name, context, template_content):
    full_template = template_content + "\n\n" + common_mandates
    final_prompt = render_template(full_template, context)

    if "{{ACTIVATE_GOAL}}" in final_prompt:
      final_prompt = final_prompt.replace("{{ACTIVATE_GOAL}}", "")

      goal_input_file = os.path.join(
          temp_dir, f"input_worker_{instance_name}.json"
      )
      _write_json_file(goal_input_file, context)

      output_path = os.path.join(temp_dir, f"prompt_worker_{instance_name}.md")
      _write_text_file(output_path, final_prompt)
      return True
    return False

  # 5. Process Goals Sequentially (Removes Concurrency GIL Overhead & State
  # Copies)
  for goal_name, goal_conf in goal_map.items():
    template_path = os.path.join(
        repo_root, "resources", goal_conf["prompt_file"]
    )
    template_content = _read_text_file(template_path)
    if not template_content:
      continue

    # Identify chunks for this goal
    chunks = []

    if goal_name == "data_safety" and "data_sources" in base_context:
      all_evidence = []
      for data_type, findings in base_context["data_sources"].items():
        if not taxonomy_keys or data_type in taxonomy_keys:
          for finding in findings:
            all_evidence.append((data_type, finding))

      for i in range(0, len(all_evidence), max_evidence_per_worker):
        chunk = all_evidence[i : i + max_evidence_per_worker]
        chunk_dict = {}
        for dt, finding in chunk:
          if dt not in chunk_dict:
            chunk_dict[dt] = {
                "description": taxonomy.get(dt, {}).get(
                    "description", "User data type."
                ),
                "findings": [],
            }
          chunk_dict[dt]["findings"].append(finding)
        chunks.append(chunk_dict)

    if not chunks:
      # Single pass for small goals
      if _save_goal_instance(goal_name, base_context, template_content):
        activated_goals.append(goal_name)
    else:
      # Multipass for large data safety sets
      for idx, chunk_data in enumerate(chunks, 1):
        instance_name = f"{goal_name}_part_{idx}"
        chunk_context = base_context.copy()
        chunk_context["data_sources"] = chunk_data
        chunk_context["GOAL_NAME"] = instance_name
        if _save_goal_instance(instance_name, chunk_context, template_content):
          activated_goals.append(instance_name)

  return activated_goals


def run_aggregation(temp_dir, repo_root):
  """Aggregates and chunks findings for the Critic."""
  worker_pattern = os.path.join(temp_dir, "worker_*.json")
  worker_files = sorted(glob.glob(worker_pattern))

  all_findings = []
  finding_id_counter = 1

  for w_file in worker_files:
    basename = os.path.basename(w_file)
    try:
      with open(w_file, "r") as f:
        w_data = json.load(f)
      findings_list = w_data.get("findings", [])
      if isinstance(findings_list, list):
        for finding in findings_list:
          # Store the source filename so generate_report can associate it
          # correctly
          finding["worker_file"] = basename
          # Assign sequential ID
          finding["finding_id"] = str(finding_id_counter)
          finding_id_counter += 1
          all_findings.append(finding)
    except Exception as e:  # pylint: disable=broad-exception-caught
      print(
          f"Warning: Failed to load worker file {w_file}: {e}", file=sys.stderr
      )

  # Save the master list of all findings
  aggregated_path = os.path.join(temp_dir, "aggregated_findings.json")
  with open(aggregated_path, "w") as f:
    json.dump({"findings": all_findings}, f, indent=4)

  # Filter findings that need Critic evaluation: skip only SUGGESTION findings
  critic_findings = []
  for f in all_findings:
    severity = str(f.get("severity", "SUGGESTION")).upper().strip()
    if severity == "SUGGESTION":
      continue
    critic_findings.append(f)

  # Chunk the findings for the Critic
  chunk_size = 3
  chunks = [
      critic_findings[i : i + chunk_size]
      for i in range(0, len(critic_findings), chunk_size)
  ]

  # Render Critic template for each chunk
  critic_template_path = os.path.join(repo_root, "resources", "critic.md")
  critic_template = ""
  if os.path.exists(critic_template_path):
    with open(critic_template_path, "r") as f:
      critic_template = f.read()

  common_mandates_path = os.path.join(
      repo_root, "resources", "common_mandates.md"
  )
  common_mandates = ""
  if os.path.exists(common_mandates_path):
    with open(common_mandates_path, "r") as f:
      common_mandates = f.read()

  full_template = critic_template + "\n\n" + common_mandates

  # Load contextual values from manifest_details.json
  manifest_path = os.path.join(temp_dir, "manifest_details.json")
  base_context = {}
  if os.path.exists(manifest_path):
    try:
      with open(manifest_path, "r") as f:
        m_details = json.load(f)
        base_context["APP_DIR"] = m_details.get("app_dir")
    except Exception:  # pylint: disable=broad-exception-caught
      pass

  if not repo_root:
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

  base_context["TEMP_DIR"] = temp_dir
  base_context["REPO_ROOT"] = repo_root

  for idx, chunk in enumerate(chunks, 1):
    # Create input_critic_<idx>.json
    chunk_dict = {f["finding_id"]: f for f in chunk}
    chunk_input_path = os.path.join(temp_dir, f"input_critic_{idx}.json")
    with open(chunk_input_path, "w") as f:
      json.dump(chunk_dict, f, indent=4)

    # Render prompt_critic_<idx>.md
    chunk_context = base_context.copy()
    chunk_context["CHUNK_INDEX"] = str(idx)

    final_prompt = render_template(full_template, chunk_context)
    chunk_prompt_path = os.path.join(temp_dir, f"prompt_critic_{idx}.md")
    with open(chunk_prompt_path, "w") as f:
      f.write(final_prompt)

  return len(chunks)


GRADLE_APP_PLUGIN_PATTERN = (
    r"(?:id\s*\(?\s*[\x27\x22]com\.android\.application[\x27\x22]\s*\)?|"
    r"apply\s*\(?\s*(?:plugin:\s*)?[\x27\x22]com\.android\.application"
    r"[\x27\x22]\s*\)?)(?!\s+apply\s+false)"
)


def _resolve_to_int(val, lookups, visited=None):
  """Recursively resolves a variable name to an integer value."""
  if visited is None:
    visited = set()

  if not val:
    return None

  # Clean and normalize the value
  val_str = str(val).strip().strip('"').strip("'")

  # Support project.property("key") syntax
  prop_match = re.search(
      r'project\.property\s*\(\s*[\'"](.*?)[\'"]\s*\)', val_str
  )
  if prop_match:
    val_str = prop_match.group(1).strip()

  # Robust resolution for Gradle Version Catalogs and dynamic getters
  # Strip common accessors and indirection prefixes
  val_str = re.sub(
      r"\.(?:get|toInt|getOrElse|provider)(?:\s*\([^)]*\))?", "", val_str
  )
  if val_str.startswith("libs.versions."):
    val_str = val_str[14:]
  elif val_str.startswith("libs."):
    val_str = val_str[5:]
  elif val_str.startswith("versions."):
    val_str = val_str[9:]

  if val_str.isdigit():
    return int(val_str)

  if val_str in visited:
    return None  # Circular reference protection
  visited.add(val_str)

  # 1. Direct and Namespaced Lookups
  search_keys = [val_str]
  if "." in val_str:
    # If A.B.C fails, try B.C, then C
    parts = val_str.split(".")
    for i in range(1, len(parts)):
      search_keys.append(".".join(parts[i:]))

  for key in search_keys:
    # Try custom versions first (usually has highest signal)
    if key in lookups.get("custom", {}):
      res = _resolve_to_int(lookups["custom"][key], lookups, visited)
      if res is not None:
        return res

    # Try gradle properties
    if key in lookups.get("properties", {}):
      res = _resolve_to_int(lookups["properties"][key], lookups, visited)
      if res is not None:
        return res

    # Try version catalog
    if key in lookups.get("catalog", {}):
      res = _resolve_to_int(lookups["catalog"][key], lookups, visited)
      if res is not None:
        return res

  return None


def main():
  """Deterministic context-gathering tool for the Reviewer skill."""
  parser = argparse.ArgumentParser(
      description="Deterministic context-gathering and state-query tool."
  )
  subparsers = parser.add_subparsers(dest="command", required=True)

  # Init subcommand
  init_parser = subparsers.add_parser(
      "init", help="Initialize scratch environment."
  )
  init_parser.add_argument("app_dir", help="Path to the Android application.")

  # Aggregate subcommand
  agg_parser = subparsers.add_parser("aggregate", help="Aggregate findings.")
  agg_parser.add_argument(
      "temp_dir", help="Path to the temporary scratch directory."
  )

  args = parser.parse_args()

  repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

  if args.command == "aggregate":
    temp_dir = os.path.abspath(args.temp_dir)
    if not os.path.isdir(temp_dir):
      print(
          f"Error: Specified temporary directory does not exist: {temp_dir}",
          file=sys.stderr,
      )
      sys.exit(1)

    chunk_count = run_aggregation(temp_dir, repo_root)
    print(json.dumps({"temp_dir": temp_dir, "critic_chunks": chunk_count}))
    sys.exit(0)

  # Initialization Mode
  app_dir = os.path.abspath(args.app_dir)
  if not os.path.isdir(app_dir):
    print(f"Error: {app_dir} is not a valid directory.", file=sys.stderr)
    sys.exit(1)

  scratch_id = str(uuid.uuid4())
  workspace_root = os.getcwd()
  temp_dir = os.path.join(
      workspace_root, ".scratch", f"play_policy_insights_{scratch_id}"
  )
  os.makedirs(temp_dir, exist_ok=True)

  # Perform a single master filesystem walk using the scanner
  file_inventory = scanner.collect_target_files(app_dir)

  gradle_properties = load_gradle_properties(app_dir)
  version_catalog = load_version_catalog(app_dir)
  # Pass inventory to avoid redundant walk
  custom_gradle_versions = load_custom_gradle_versions(app_dir, file_inventory)

  codebase_map = {
      "key_files": {
          "manifests": file_inventory["manifests"],
          "gradles": [
              g
              for g in file_inventory["gradles"]
              if g.endswith((".gradle", ".gradle.kts"))
          ],
      }
  }

  # 1. Detect and Filter Flavors (Play Store Prioritization)
  detected_flavors = set()
  src_dirs = []
  
  config = scanner.get_scanner_config()
  ignored_dirs = set(config.get("ignored_directories", []))
  
  # Find all 'src' directories
  for root, dirs, _ in os.walk(app_dir):
    dirs[:] = [d for d in dirs if d not in ignored_dirs]
    if "src" in dirs:
      src_dirs.append(os.path.join(root, "src"))
      dirs.remove("src")  # Don't recurse deeper for 'src' search

  for s_dir in src_dirs:
    for flavor in os.listdir(s_dir):
      if (
          os.path.isdir(os.path.join(s_dir, flavor))
          and flavor not in ignored_dirs
      ):
        detected_flavors.add(flavor)

  prioritized_flavors = []
  if "play" in detected_flavors:
    prioritized_flavors = ["main", "play"]

  if prioritized_flavors:
    # Filter codebase_map to only include prioritized flavors
    print(
        f"Prioritizing flavors: {', '.join(prioritized_flavors)}",
        file=sys.stderr,
    )

    filtered_files = {}
    for key, paths in codebase_map.get("key_files", {}).items():
      filtered_paths = []
      for p in paths:
        # Check if path contains any non-prioritized flavor
        is_excluded = False
        for f in detected_flavors:
          if f not in prioritized_flavors and f"src/{f}/" in p:
            is_excluded = True
            break
        if not is_excluded:
          filtered_paths.append(p)
      filtered_files[key] = filtered_paths
    codebase_map["key_files"] = filtered_files

  with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:
    # 1. Start the Data Safety Scanner immediately (independent task)
    data_safety_scan_file = os.path.join(temp_dir, "data_safety_scan.json")
    scanner_future = executor.submit(
        run_scanner_direct, app_dir, file_inventory, data_safety_scan_file
    )

    # 2. Extract Application Modules and Primary Identity
    app_modules = parse_application_modules(
        codebase_map.get("key_files", {}).get("gradles", []),
        codebase_map.get("key_files", {}).get("manifests", []),
        gradle_properties,
        version_catalog,
        custom_gradle_versions=custom_gradle_versions,
    )
    primary_app_id, primary_target_sdk, primary_app_label = (
        determine_primary_identity(
            app_modules,
            codebase_map.get("key_files", {}).get("gradles", []),
            codebase_map.get("key_files", {}).get("manifests", []),
            gradle_properties,
            version_catalog,
            custom_gradle_versions,
            app_dir,
        )
    )

    manifest_content = ""
    manifest_details = {
        "package_name": primary_app_id,
        "app_label": primary_app_label,
        "target_sdk": primary_target_sdk,
        "app_dir": app_dir,
        "permissions": [],
        "foreground_services": [],
    }

    if codebase_map.get("key_files", {}).get("manifests"):
      for _, manifest_path in enumerate(codebase_map["key_files"]["manifests"]):
        # Filter Test-Bleed based on relative path
        rel_path = os.path.relpath(manifest_path, app_dir)
        normalized_rel_path = "/" + rel_path.replace("\\", "/")
        if any(
            x in normalized_rel_path
            for x in [
                "/debug/",
                "/test/",
                "/androidTest/",
                "/testFixtures/",
            ]
        ):
          continue

        if os.path.exists(manifest_path):
          try:
            with open(manifest_path, "r") as f:
              content = f.read()
              if not manifest_content:
                manifest_content = content

              details = extract_manifest_details(content)

              # Identity Fallbacks
              if details.get("package_name") and not manifest_details.get(
                  "package_name"
              ):
                manifest_details["package_name"] = details["package_name"]

              if details.get("target_sdk") and not manifest_details.get(
                  "target_sdk"
              ):
                manifest_details["target_sdk"] = details["target_sdk"]

              if details.get("app_label") and not manifest_details.get(
                  "app_label"
              ):
                manifest_details["app_label"] = details["app_label"]

              manifest_details["permissions"].extend(
                  details.get("permissions", [])
              )
              manifest_details["foreground_services"].extend(
                  details.get("foreground_services", [])
              )
          except Exception as e:  # pylint: disable=broad-exception-caught
            print(f"Warning: Failed to read manifest: {e}", file=sys.stderr)

      manifest_details["permissions"] = sorted(
          list(set(manifest_details["permissions"]))
      )

      # Deduplicate and sort foreground services
      unique_services = []
      seen_services = set()
      for svc in manifest_details["foreground_services"]:
        svc_key = (svc.get("name"), svc.get("type"))
        if svc_key not in seen_services:
          seen_services.add(svc_key)
          unique_services.append(svc)
      
      # Final stable sort
      manifest_details["foreground_services"] = sorted(
          unique_services,
          key=lambda x: (x.get("name") or "", x.get("type") or ""),
      )

    # 3. Gather results
    data_safety_scan = scanner_future.result()

  # 5. Post-Scan Flavor Filtering
  if prioritized_flavors:
    data_safety_scan = _filter_by_flavor(
        data_safety_scan, detected_flavors, prioritized_flavors
    )

  # 6. Ingest local Play Store info
  local_info_path = os.path.join(
      app_dir, "play_store_assets", "play_store_info.json"
  )
  if os.path.exists(local_info_path):
    try:
      with open(local_info_path, "r", encoding="utf-8") as f:
        play_store_info = json.load(f)
        if "is_published" not in play_store_info:
          play_store_info["is_published"] = True
        if "store_url" not in play_store_info:
          play_store_info["store_url"] = f"file://{local_info_path}"

        with open(os.path.join(temp_dir, "play_store_info.json"), "w") as out_f:
          json.dump(play_store_info, out_f, indent=4)
      print(
          f"Ingested local Play Store info from {local_info_path}",
          file=sys.stderr,
      )
    except Exception as e:  # pylint: disable=broad-exception-caught
      print(
          f"Warning: Failed to ingest local Play Store info: {e}",
          file=sys.stderr,
      )

  reasoning = []
  goal_map = {}
  resources_dir = os.path.join(repo_root, "resources")
  if os.path.exists(resources_dir):
    for f in os.listdir(resources_dir):
      if f.startswith("goal_") and f.endswith(".md"):
        goal_name = f[5:-3]
        goal_map[goal_name] = {"prompt_file": f}

  if prioritized_flavors:
    reasoning.append(
        f"Prioritizing Play Store flavors: {', '.join(prioritized_flavors)}"
    )
  reasoning.append("Activated via template evaluation")

  output_data = {
      "app_name": (
          manifest_details.get("app_label")
          or os.path.basename(os.path.normpath(app_dir))
      ),
      "package_name": manifest_details.get("package_name"),
      "target_dir": app_dir,
      "codebase_map": codebase_map,
      "manifest_content": manifest_content,
      "manifest_details": manifest_details,
      "application_modules": app_modules,
      "data_safety_scan": data_safety_scan,
      "triage_summary": generate_triage_summary(
          manifest_details, data_safety_scan
      ),
      "triage_reasoning": "\n".join(reasoning),
      "detected_flavors": sorted(list(detected_flavors)),
      "prioritized_flavors": prioritized_flavors,
  }

  try:
    activated_goals = write_agent_prompts(
        {"repo_root": repo_root, "temp_dir": temp_dir, "app_dir": app_dir},
        goal_map,
        output_data,
    )
    print(f"Prompts generated in {temp_dir}", file=sys.stderr)
  except Exception as e:  # pylint: disable=broad-exception-caught
    print(f"Warning: Failed to generate prompts: {e}", file=sys.stderr)

  print(
      json.dumps(
          {"temp_dir": temp_dir, "activated_goals": activated_goals}, indent=4
      )
  )


if __name__ == "__main__":
  main()
