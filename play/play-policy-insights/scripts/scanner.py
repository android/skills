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

"""Scans Android codebase for data safety signals and sensitive API usage."""

import concurrent.futures
import functools
import json
import os
import re
import sys

# Pre-compiled regex patterns at module scope for thread-safe performance
URL_PATTERN = re.compile(r'"(https?://[^"]+)"')
XML_COMMENT_PATTERN = re.compile(r"<!--.*?-->", flags=re.DOTALL)
COMMENT_STRIP_PATTERN = re.compile(
    r'("(?:\\.|[^"\\])*")|//[^\n]*|/\*.*?\*/',
    flags=re.DOTALL,
)


@functools.lru_cache(maxsize=1)
def get_scanner_config():
  """Loads the scanner configuration from scanner_config.json with caching."""
  config_path = os.path.join(
      os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
      "resources",
      "scanner_config.json",
  )
  try:
    with open(config_path, "r", encoding="utf-8") as f:
      return json.load(f)
  except Exception as e:
    print(
        f"Warning: Failed to load scanner config from {config_path}: {e}",
        file=sys.stderr,
    )
    return {}


@functools.lru_cache(maxsize=1)
def get_supported_extensions():
  """Returns a tuple of supported file extensions from config."""
  return tuple(get_scanner_config().get("supported_extensions", []))


def collect_target_files(target_dir):
  """Performs a single master walk to categorize files."""
  categorized = {
      "manifests": [],
      "gradles": [],
      "source_code": [],
      "all_files": [],
  }

  normalized_target = os.path.abspath(target_dir)
  hybrid_root = normalized_target
  target_parent = os.path.dirname(normalized_target)

  for potential_root in [normalized_target, target_parent]:
    if os.path.exists(
        os.path.join(potential_root, "package.json")
    ) or os.path.exists(os.path.join(potential_root, "pubspec.yaml")):
      hybrid_root = potential_root
      print(
          f"Hybrid app detected. Expanding scan root to: {hybrid_root}",
          file=sys.stderr,
      )
      break

  config = get_scanner_config()
  ignored_dirs = set(config.get("ignored_directories", []))
  supported_exts = get_supported_extensions()

  for root, dirs, files in os.walk(hybrid_root):
    dirs[:] = sorted([d for d in dirs if d not in ignored_dirs])
    for file in sorted(files):
      file_path = os.path.join(root, file)
      categorized["all_files"].append((file, file_path, root))

      # Surgical filter for XMLs to avoid scanning thousands of low-signal
      # assets.
      if file.endswith(".xml"):
        # Exclude test-bleed XMLs
        if not any(
            x in file_path.lower()
            for x in ["/debug/", "/test/", "/androidtest/", "/testfixtures/"]
        ):
          if file == "AndroidManifest.xml":
            categorized["manifests"].append(file_path)
          else:
            path_parts = file_path.split(os.sep)
            is_valuable_xml = False
            for i, part in enumerate(path_parts):
              if part in ["res", "resources"] and i + 1 < len(path_parts):
                next_part = path_parts[i + 1]
                if any(
                    next_part.startswith(d)
                    for d in ["layout", "values", "xml", "navigation"]
                ):
                  is_valuable_xml = True
                  break
            if is_valuable_xml:
              categorized["source_code"].append(file_path)

      elif file.lower().endswith((".gradle", ".gradle.kts")):
        categorized["gradles"].append(file_path)

      elif file.lower().endswith(supported_exts) or (
          file.lower() == "pubspec.yaml" or file.lower() == "package.json"
      ):
        # Filter (Gap 3 - OS-Specific Path Separators and Test-Bleed based on
        # filename/path)
        file_lower = file.lower()
        is_test_or_debug = (
            any(
                x in file_path.lower()
                for x in [
                    "/debug/",
                    "/test/",
                    "/androidtest/",
                    "/testfixtures/",
                    ".spec.js",
                    ".spec.ts",
                    ".spec.jsx",
                    ".spec.tsx",
                ]
            )
            or file_lower.endswith("_test.dart")
        )
        if not is_test_or_debug:
          categorized["source_code"].append(file_path)

  return categorized


def _identify_semantic_files(all_files):
  """Maps files to semantic categories based on filename patterns."""
  semantic_files = {}
  file_patterns = get_scanner_config().get("file_patterns", {})
  supported_exts = get_supported_extensions()

  for file, file_path, root in all_files:
    file_lower = file.lower()
    # Check if it's a relevant source/config file
    if file_lower.endswith(supported_exts + (".xml",)):
      rel_path = os.path.relpath(file_path, root)
      for category, patterns in file_patterns.items():
        if any(p in file_lower for p in patterns):
          if category not in semantic_files:
            semantic_files[category] = []
          semantic_files[category].append(rel_path)

  return semantic_files


def _scan_single_file(file_path, all_signal_categories, target_dir):
  """Internal worker to scan a single file for all signal categories."""
  local_api_usage = {}
  found_urls = []
  supported_exts = get_supported_extensions()

  try:
    with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
      content = f.read()

      # URLs - with quick check for policy relatedness
      policy_url_keywords = ["privacy", "support", "delete", "terms", "policy"]
      for match in URL_PATTERN.finditer(content):
        url = match.group(1)
        if any(kw in url.lower() for kw in policy_url_keywords):
          found_urls.append(url)

      # Cleanup comments based on file type
      file_path_lower = file_path.lower()
      if file_path_lower.endswith(".xml"):
        content = XML_COMMENT_PATTERN.sub("", content)
      elif file_path_lower.endswith(supported_exts):
        content = COMMENT_STRIP_PATTERN.sub(
            lambda m: m.group(1) if m.group(1) else "", content
        )

      # Signal Patterns
      for cat_name, signals in all_signal_categories.items():
        for signal_id, patterns in signals.items():
          for p in patterns:
            if p in content:
              if cat_name not in local_api_usage:
                local_api_usage[cat_name] = {}
              if signal_id not in local_api_usage[cat_name]:
                local_api_usage[cat_name][signal_id] = []

              rel_path = os.path.relpath(file_path, target_dir)
              local_api_usage[cat_name][signal_id].append(
                  f"{rel_path} (Pattern: {p})"
              )
              break  # Move to next signal once pattern found in file

  except Exception:  # pylint: disable=broad-exception-caught
    pass

  return {"api_usage": local_api_usage, "urls": found_urls}


def perform_scan(target_dir, file_inventory):
  """Orchestrates the parallel scanning of identified files."""
  api_usage = {}
  all_urls = []
  all_signal_categories = get_scanner_config().get("signal_categories", {})

  with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
    futures = [
        executor.submit(
            _scan_single_file, f, all_signal_categories, target_dir
        )
        for f in file_inventory.get("source_code", [])
    ]

    for future in concurrent.futures.as_completed(futures):
      res = future.result()
      for cat_name, cat_findings in res["api_usage"].items():
        if cat_name not in api_usage:
          api_usage[cat_name] = {}
        for signal_id, findings in cat_findings.items():
          if signal_id not in api_usage[cat_name]:
            api_usage[cat_name][signal_id] = []
          api_usage[cat_name][signal_id].extend(findings)

      for u in res["urls"]:
        all_urls.append(u)

  # Final cleanup and formatting
  for cat_name in api_usage:
    for signal_id in api_usage[cat_name]:
      api_usage[cat_name][signal_id] = sorted(
          list(set(api_usage[cat_name][signal_id]))
      )

  return {
      "data_safety_scan": api_usage,
      "found_urls": sorted(list(set(all_urls))),
      "semantic_files": _identify_semantic_files(
          file_inventory.get("all_files", [])
      ),
      "codebase_map": file_inventory,
  }
