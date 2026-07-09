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

"""Generates a comprehensive compliance report from synthesized findings."""

import argparse
from datetime import datetime
import glob
import json
import os
import re
import sys
import play_store_scraper
from template_engine import render_template

# Precompiled regex patterns at module scope for performance optimizations
EMOJI_PATTERN = re.compile(
    r"[\U00010000-\U0010ffff\u2600-\u27bf\u2300-\u23ff\u2b50]+",
    flags=re.UNICODE,
)
CODE_LINK_PATTERN = re.compile(r"^(.*?)(?:[:\s]+L?(\d+))?$")
SEVERITY_CLEAN_PATTERN = re.compile(r"['\"`*🔴🟡🔵\s]+")


def run_scraper(package_name, output_dir):
  """Invokes the Play Store scraper directly."""
  try:
    print(f"Auditing {package_name} from Play Store...", file=sys.stderr)
    result = play_store_scraper.scrape_app_details(package_name)

    # Save to temp for consistency with existing report logic if needed
    os.makedirs(output_dir, exist_ok=True)
    out_path = os.path.join(output_dir, "play_store_declaration.json")
    with open(out_path, "w") as f:
      json.dump(result, f, indent=4, sort_keys=True)

    return result
  except Exception as e:
    print(f"Warning: Play Store scrape failed: {e}", file=sys.stderr)
  return {}


def strip_emojis(text):
  """Removes emoji characters from a string."""
  if not isinstance(text, str):
    return text
  return EMOJI_PATTERN.sub(r"", text)


def format_code_link(file_val):
  """Formats file paths and line numbers into Markdown links."""
  if not file_val or file_val == "N/A":
    return "N/A"
  files = []
  if isinstance(file_val, str):
    if "," in file_val:
      files = [f.strip() for f in file_val.split(",")]
    else:
      files = [file_val.strip()]
  elif isinstance(file_val, list):
    files = [str(f).strip() for f in file_val]
  formatted_links = []
  for f in files:
    f_str = str(f)
    if f_str.startswith("["):
      formatted_links.append(f_str)
      continue
    match = CODE_LINK_PATTERN.search(f_str)
    if match:
      path = match.group(1)
      line = match.group(2)
      basename = os.path.basename(path)
      if line:
        link = f"[{basename}:L{line}](file://{path}#L{line})"
      else:
        link = f"[{basename}](file://{path})"
      formatted_links.append(link)
    else:
      formatted_links.append(f_str)
  return ", ".join(formatted_links)


def render_table(
    items, columns, column_names, empty_message="No items identified."
):
  """Renders a Markdown table from a list of dictionaries."""
  if not items:
    return f"\n* {empty_message}\n"

  widths = [len(name) for name in column_names]
  rows = []
  for item in items:
    row = []
    for col in columns:
      val = str(item.get(col, "N/A")).replace("|", "\\|").replace("\n", "<br>")
      row.append(val)
    rows.append(row)
    for i, val in enumerate(row):
      widths[i] = max(widths[i], len(val))

  def format_row(vals):
    return (
        "| "
        + " | ".join(val.ljust(widths[i]) for i, val in enumerate(vals))
        + " |"
    )

  header = format_row(column_names)
  separator = (
      "| " + " | ".join("-" * widths[i] for i in range(len(widths))) + " |"
  )
  body = "\n".join(format_row(row) for row in rows)

  return f"\n{header}\n{separator}\n{body}\n"


def parse_boolean(val):
  """Safely converts boolean or sloppy string yes/no/true/false."""
  if isinstance(val, bool):
    return val
  if isinstance(val, str):
    v_clean = val.strip().lower()
    return v_clean in ("yes", "true", "1")
  return False


def parse_disclosure_status(val):
  """Safely parses disclosure enums or visual yes/no."""
  if not val:
    return "No"

  val_clean = str(val).strip().upper()
  if "EXEMPT" in val_clean:
    return "Exempt (Obvious)"
  if "DISCLOSED" in val_clean or "YES" in val_clean or "TRUE" in val_clean:
    return "Yes"

  return "No"


def clean_severity(sev_str):
  """Robustly cleans severity strings by removing quotes, asterisks,
  emojis, and whitespace.
  """
  if not isinstance(sev_str, str):
    return "SUGGESTION"

  # Convert to uppercase and strip formatting characters
  cleaned = sev_str.upper()
  cleaned = SEVERITY_CLEAN_PATTERN.sub("", cleaned)

  if "CRITICAL" in cleaned:
    return "CRITICAL"
  if "IMPORTANT" in cleaned:
    return "IMPORTANT"
  if "SUGGESTION" in cleaned:
    return "SUGGESTION"

  return "SUGGESTION"


def render_finding_card(finding, policy_refs=None):
  """Renders a detailed finding card in Markdown."""
  severity = clean_severity(finding.get("severity"))
  severity_map = {
      "CRITICAL": "🔴 Critical",
      "IMPORTANT": "🟡 Important",
      "SUGGESTION": "🔵 Suggestion",
  }
  visual_severity = severity_map.get(severity, "🔵 Suggestion")

  title = strip_emojis(
      finding.get("issue") or finding.get("issue_summary") or "Policy Risk"
  )
  p_id = (
      finding.get("policy_id")
      or finding.get("policy_reference")
      or "Unknown Policy"
  )
  policy_link = f"**{p_id}**"

  policy_info = policy_refs.get(p_id) if policy_refs else None

  policy_link = f"**{p_id}**"
  policy_urls = []
  if p_id and policy_info:
    policy_link = f"**{policy_info.get('name', p_id)}**"
    policy_urls = policy_info.get("urls", [])

  files = finding.get("files_involved")
  has_local_override = False
  local_override_file = None

  if files and isinstance(files, list):
    for f in files:
      if "play_store_declaration.json" in f:
        has_local_override = True
        local_override_file = f
        break

  files_formatted = format_code_link(files)
  recommendation = finding.get("recommendation", "Review and remediate.")

  if has_local_override:
    recommendation = (
        "Update the local Play Store declaration file at "
        f"{format_code_link(local_override_file)}."
    )

  card = f"\n#### {title}\n"
  card += f"- **Policy**: {policy_link}\n"
  card += f"- **Severity**: {visual_severity}\n"
  card += f"- **Files**: {files_formatted}\n"
  card += f"- **Evidence**: {finding.get('evidence', 'N/A')}\n"
  card += f"- **Recommendation**: {recommendation}\n"

  if policy_urls:
    card += "- **References**:\n"
    for url in policy_urls:
      card += f"  - {url}\n"

  return card


def load_json(file_path):
  """Loads JSON from a file if it exists."""
  if os.path.exists(file_path):
    try:
      with open(file_path, "r") as f:
        return json.load(f)
    except Exception as e:
      print(f"Warning: Failed to load {file_path}: {e}", file=sys.stderr)
  return None


def aggregate_findings(temp_dir, taxonomy):
  """Consolidates audit findings from workers.

  Produces a unified report data object.
  """
  play_store_info = (
      load_json(os.path.join(temp_dir, "play_store_info.json")) or {}
  )

  aggregated_findings_path = os.path.join(temp_dir, "aggregated_findings.json")

  raw_findings = []
  critic_decisions = {}

  if os.path.exists(aggregated_findings_path):
    master_data = load_json(aggregated_findings_path) or {}
    raw_findings = master_data.get("findings", [])

    # Load all chunked critic outputs: critic_output_*.json
    critic_files = sorted(
        glob.glob(os.path.join(temp_dir, "critic_output_*.json"))
    )
    for c_file in critic_files:
      c_data = load_json(c_file) or {}
      for fid, dec in c_data.items():
        critic_decisions[fid] = dec

  # 2. Process Findings and Apply Decoupled Critic Verdicts
  identified_risks = []
  manual_review_needed = []
  data_safety_inventory = []

  # Sort raw findings by finding_id numerically for stable processing order
  def finding_sort_key(f):
    fid = f.get("finding_id", "0")
    try:
      return int(fid)
    except (ValueError, TypeError):
      return 0

  sorted_raw_findings = sorted(raw_findings, key=finding_sort_key)

  for finding in sorted_raw_findings:
    p_id = finding.get("policy_id", "Unknown")
    psl = finding.get("psl_constant")
    fid = finding.get("finding_id")
    wf = finding.get("worker_file")

    # Retrieve decision
    decision = critic_decisions.get(str(fid))
    if not decision:
      # Automatically approve/verify findings that bypassed the Critic
      # (like SUGGESTIONs)
      decision = {
          "action": "VERIFIED",
          "confidence": "High",
          "critic_justification": (
              "Automatically verified (bypassed critic review)."
          ),
      }

    action = str(decision.get("action", "VERIFIED")).upper().strip()
    confidence = decision.get("confidence", "Medium")
    justification = decision.get("critic_justification", "Verified by Critic.")

    # Apply Critic's editorial overrides if specified
    severity = decision.get("severity") or finding.get("severity", "SUGGESTION")
    severity = clean_severity(severity)

    issue_summary = decision.get("issue_summary") or finding.get(
        "issue_summary", "Unknown Issue"
    )
    recommendation = decision.get("recommendation") or finding.get(
        "recommendation", ""
    )

    files_involved = finding.get("files_involved", [])
    if isinstance(files_involved, list):
      files_involved = sorted([str(f) for f in files_involved])

    evidence = finding.get("evidence", "")

    reconstructed_finding = {
        "policy_id": p_id,
        "finding_id": fid or f"{wf}_{p_id}",
        "issue_summary": issue_summary,
        "severity": severity,
        "files_involved": files_involved,
        "evidence": (
            f"{evidence}\n\n**Critic Verification**: {justification}"
            f" (Confidence: {confidence})"
        ),
        "recommendation": recommendation,
    }

    # Programmatically evaluate if this is a compliant Data Safety finding
    # to avoid card clutter
    is_compliant_ds = False
    if "is_transferred" in finding:
      is_transferred = parse_boolean(finding.get("is_transferred"))
      user_initiated = parse_boolean(finding.get("user_initiated"))
      is_third_party = parse_boolean(finding.get("is_third_party"))
      disc_status = (
          str(finding.get("prominent_disclosure_status", "MISSING"))
          .upper()
          .strip()
      )

      # Compliant if either local-only OR transmitted but disclosed/exempt
      # OR user initiated
      if not is_transferred:
        is_compliant_ds = True
      elif user_initiated:
        is_compliant_ds = True
      elif "DISCLOSED" in disc_status or "EXEMPT" in disc_status:
        is_compliant_ds = True

    # Route the finding based on Critic's action
    if action == "VERIFIED":
      if not is_compliant_ds:
        identified_risks.append(reconstructed_finding)
    elif action == "MANUAL_REVIEW":
      if not is_compliant_ds:
        manual_review_needed.append(reconstructed_finding)

    # 3. Process Data Safety Inventory Programmatically (If it's a DS Finding)
    if "is_transferred" in finding:
      # Create a localized copy of the data safety keys (psl_constant is
      # natively preserved)
      inv_item = {
          "psl_constant": psl,
          "is_transferred": parse_boolean(finding.get("is_transferred")),
          "user_initiated": parse_boolean(finding.get("user_initiated")),
          "is_third_party": parse_boolean(finding.get("is_third_party")),
          "prominent_disclosure_status": (
              str(finding.get("prominent_disclosure_status", "MISSING"))
              .upper()
              .strip()
          ),
          "purpose": finding.get("purpose", "N/A"),
          "linked_to_user": parse_boolean(finding.get("linked_to_user")),
          "behavioral_proof": evidence,
          "disclosure_proof": recommendation,
      }

      # Programmatically align/vet inventory based on Critic's verdict
      if action == "PRUNED":
        inv_item["is_transferred"] = False
        inv_item["purpose"] = "Local functionality only"
        inv_item["behavioral_proof"] = f"Pruned by Critic: {justification}"

      data_safety_inventory.append(inv_item)

  # 4. Consolidate and Deduplicate Data Safety Inventory by psl_constant
  merged_inventory = {}
  for item in data_safety_inventory:
    psl_id = item.get("psl_constant")
    if not psl_id:
      continue

    if psl_id not in merged_inventory:
      merged_inventory[psl_id] = {
          "psl_constant": psl_id,
          "is_transferred": False,
          "user_initiated": True,  # Default to True for AND logic
          "is_third_party": False,
          "linked_to_user": False,
          "prominent_disclosure_status": "EXEMPT",
          "purposes": set(),
          "behavioral_proofs": set(),
          "disclosure_proofs": set(),
      }

    current = merged_inventory[psl_id]

    # Transmission (True if any is True)
    if item.get("is_transferred"):
      current["is_transferred"] = True

    # User Initiated (True only if ALL are True)
    if not item.get("user_initiated"):
      current["user_initiated"] = False

    # Third Party (True if any is True)
    if item.get("is_third_party"):
      current["is_third_party"] = True

    # Linked (True if any is True)
    if item.get("linked_to_user"):
      current["linked_to_user"] = True

    # Disclosure logic precedence: MISSING > DISCLOSED > EXEMPT
    new_disc = (
        str(item.get("prominent_disclosure_status", "EXEMPT")).upper().strip()
    )
    curr_disc = current["prominent_disclosure_status"]

    if "MISSING" in curr_disc or "MISSING" in new_disc:
      current["prominent_disclosure_status"] = "MISSING"
    elif "DISCLOSED" in curr_disc or "DISCLOSED" in new_disc:
      current["prominent_disclosure_status"] = "DISCLOSED"
    else:
      current["prominent_disclosure_status"] = "EXEMPT"

    if item.get("purpose") and item.get("purpose") != "N/A":
      current["purposes"].add(str(item["purpose"]).strip())
    if item.get("behavioral_proof") and item.get("behavioral_proof") != "N/A":
      current["behavioral_proofs"].add(str(item["behavioral_proof"]).strip())
    if item.get("disclosure_proof") and item.get("disclosure_proof") != "N/A":
      current["disclosure_proofs"].add(str(item["disclosure_proof"]).strip())

  # Reconstruct the list with joined sets
  normalized_inventory = []
  sorted_psl_ids = sorted(merged_inventory.keys())
  for psl_id in sorted_psl_ids:
    item = merged_inventory[psl_id]
    reconstructed = {
        "psl_constant": psl_id,
        "is_transferred": "Yes" if item["is_transferred"] else "No",
        "user_initiated": "Yes" if item.get("user_initiated") else "No",
        "is_third_party": "Yes" if item.get("is_third_party") else "No",
        "linked_to_user": "Yes" if item["linked_to_user"] else "No",
        "prominent_disclosure_status": parse_disclosure_status(
            item["prominent_disclosure_status"]
        ),
        "purpose": ", ".join(sorted(list(item["purposes"]))) or "N/A",
        "behavioral_proof": (
            ", ".join(sorted(list(item["behavioral_proofs"]))) or "N/A"
        ),
        "disclosure_proof": (
            ", ".join(sorted(list(item["disclosure_proofs"]))) or "N/A"
        ),
    }
    normalized_inventory.append(reconstructed)

  # 5. Decorate and Filter Data Safety Inventory
  decorated_inventory = []
  local_access_only = []

  for item in normalized_inventory:
    psl_id = item.get("psl_constant")
    tax_info = taxonomy.get(psl_id, {"category": "Other", "data_type": "Other"})
    item["category"] = tax_info["category"]
    item["data_type"] = tax_info["data_type"]

    is_local = (
        item.get("is_transferred") == "No"
        or item.get("purpose") == "Local functionality only"
    )
    if is_local:
      local_access_only.append(item)
    else:
      decorated_inventory.append(item)

  # Cross-reference with Play Store
  matches = []
  mismatches = []
  is_published = play_store_info.get("is_published", False)

  if is_published:
    play_declarations = play_store_info.get("data_safety", {}).get(
        "data_collected", []
    )
    play_data_types = set()
    for category_dict in play_declarations:
      for type_dict in category_dict.get("types", []):
        play_data_types.add(type_dict.get("type"))

    for item in decorated_inventory:
      dt = item["data_type"]
      if dt in play_data_types:
        matches.append({"data_type": dt, "status": "Declared and detected"})
      else:
        is_obvious = "Exempt" in item.get(
            "prominent_disclosure_status", ""
        ) or "Obvious" in item.get("prominent_disclosure_status", "")
        mismatches.append({
            "data_type": dt,
            "local_view": (
                f"Detected in code (Evidence: {item.get('behavioral_proof')})"
            ),
            "play_view": "Not declared in Play Store",
            "status": (
                "Exempt: Obvious core functionality"
                if is_obvious
                else "Discrepancy"
            ),
        })

    # Check for unjustified declarations (in Play Store but not in code)
    detected_data_types = {
        item["data_type"] for item in decorated_inventory + local_access_only
    }
    for category_dict in play_declarations:
      for type_dict in category_dict.get("types", []):
        dt = type_dict.get("type")
        if dt not in detected_data_types:
          mismatches.append({
              "data_type": dt,
              "local_view": "Not detected in code",
              "play_view": "Declared in Play Store",
              "status": "Discrepancy",
          })

  # 7. Determine Compliance
  overall_compliance = "Compliant"
  critical_risks = any(
      clean_severity(r.get("severity")) == "CRITICAL" for r in identified_risks
  )
  active_mismatches = any(m.get("status") == "Discrepancy" for m in mismatches)

  if critical_risks or active_mismatches:
    overall_compliance = "Non-compliant"
  elif manual_review_needed or any(
      clean_severity(r.get("severity")) == "IMPORTANT" for r in identified_risks
  ):
    overall_compliance = "Needs review"

  # 8. Sort result lists for deterministic output
  def report_finding_sort_key(f):
    pid = f.get("policy_id", "")
    fid = f.get("finding_id", "0")
    try:
      num_fid = int(fid)
    except (ValueError, TypeError):
      num_fid = 0
    return (pid, num_fid)

  identified_risks.sort(key=report_finding_sort_key)
  manual_review_needed.sort(key=lambda x: x.get("issue_summary", ""))
  matches.sort(key=lambda x: x.get("data_type", ""))
  mismatches.sort(key=lambda x: x.get("data_type", ""))

  # 9. Generate Summary

  risk_count = len(identified_risks)
  mismatch_count = len(
      [m for m in mismatches if m.get("status") == "Discrepancy"]
  )
  exempt_count = len([
      m
      for m in mismatches
      if m.get("status") == "Exempt: Obvious core functionality"
  ])

  summary_parts = []
  summary_parts.append(
      "The automated audit of"
      f" {play_store_info.get('title', 'the application')} is complete."
  )
  summary_parts.append(
      f"Identified {risk_count} potential policy risks and {mismatch_count}"
      " active Data Safety discrepancies."
  )
  if exempt_count > 0:
    summary_parts.append(
        f"{exempt_count} detections were flagged as 'Obvious' and exempt from"
        " prominent disclosure."
    )

  if overall_compliance == "Non-compliant":
    summary_parts.append(
        "Immediate remediation is required for critical findings and"
        " declaration mismatches."
    )
  elif overall_compliance == "Needs review":
    summary_parts.append(
        "Manual review is recommended for several ambiguous findings."
    )
  else:
    summary_parts.append(
        "The application appears broadly compliant with analyzed policies."
    )

  # 9. Final Report Construction
  report = {
      "overall_compliance": overall_compliance,
      "summary": " ".join(summary_parts),
      "package_name": play_store_info.get("package_name", "unknown"),
      "is_published": is_published,
      "identified_risks": identified_risks,
      "data_safety_comparison": {"matches": matches, "mismatches": mismatches},
      "local_data_access": [
          {
              "data_type": item["data_type"],
              "category": item["category"],
              "evidence": item.get("behavioral_proof"),
          }
          for item in local_access_only
      ],
      "manual_review_needed": [
          {"issue_summary": r.get("issue_summary")}
          for r in manual_review_needed
      ],
      "suggested_data_safety_declaration": {
          "collected_data": [
              {
                  "data_type": item["data_type"],
                  "purpose": item.get("purpose"),
                  "linked_to_user": item.get("linked_to_user") == "Yes",
              }
              for item in decorated_inventory
          ]
      },
  }

  return report


def main():
  """Main entry point for report generation."""
  parser = argparse.ArgumentParser(description="Compliance report generator.")
  parser.add_argument(
      "temp_dir",
      help="Path to the temporary scratch directory containing audit findings.",
  )

  args = parser.parse_args()
  temp_dir = os.path.abspath(args.temp_dir)
  repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

  policy_refs = (
      load_json(os.path.join(repo_root, "resources", "policies.json"))
  )
  if not policy_refs:
    policy_refs = {}
  taxonomy = policy_refs.get("data_safety_section", {}).get("taxonomy", {})

  manifest_details = (
      load_json(os.path.join(temp_dir, "manifest_details.json")) or {}
  )
  package_name = manifest_details.get("package_name")

  # Load Play Store info (potentially ingested by orchestrator)
  play_store_info_path = os.path.join(temp_dir, "play_store_info.json")
  play_store_info = {}
  if os.path.exists(play_store_info_path):
    play_store_info = load_json(play_store_info_path) or {}

  if not play_store_info and package_name:
    play_store_info = run_scraper(package_name, temp_dir)
    # Cache it for report reuse
    with open(play_store_info_path, "w") as f:
      json.dump(play_store_info, f, indent=4, sort_keys=True)

  report_data = aggregate_findings(temp_dir, taxonomy)

  template_path = os.path.join(
      repo_root, "resources", "compliance_report_template.md"
  )
  if not os.path.exists(template_path):
    print(f"Error: Template not found at {template_path}", file=sys.stderr)
    sys.exit(1)

  try:
    with open(template_path, "r") as f:
      content = f.read()
  except Exception as e:
    print(f"Error reading template: {e}", file=sys.stderr)
    sys.exit(1)

  output_path = os.path.join(temp_dir, "compliance_report.md")

  violations = report_data.get("identified_risks", [])
  mismatches = report_data.get("data_safety_comparison", {}).get(
      "mismatches", []
  )
  manual = report_data.get("manual_review_needed", [])

  compliance_raw = report_data.get("overall_compliance") or "Needs review"
  status_map = {
      "Non-compliant": "🔴 Non-Compliant",
      "Needs review": "🟡 Needs Review",
      "Compliant": "🟢 Compliant",
  }
  compliance_ui = status_map.get(compliance_raw, "🟡 Needs Review")

  app_id = report_data.get("package_name") or "Unknown App"

  findings_content = ""
  if not violations:
    findings_content = "\n* No policy risks identified in this scan.\n"
  else:
    grouped_findings = {}
    for v in violations:
      p_id = v.get("policy_id")
      macro_cat = "Other Policies"
      if p_id and p_id in policy_refs:
        macro_cat = policy_refs[p_id].get("category", "Other Policies")

      if macro_cat not in grouped_findings:
        grouped_findings[macro_cat] = []
      grouped_findings[macro_cat].append(v)

    ordered_cats = [
        "Restricted Content",
        "Privacy, Deception and Device Abuse",
        "Monetization and Ads",
        "Store Listing and Promotion",
        "Developer Account Management",
    ]

    all_cats = ordered_cats + [
        cat for cat in grouped_findings if cat not in ordered_cats
    ]

    for cat in all_cats:
      if cat in grouped_findings:
        findings_content += f"\n### {cat}\n"
        for v in grouped_findings[cat]:
          findings_content += render_finding_card(v, policy_refs) + "\n"

  is_not_published = report_data.get("is_published") is False

  d_table_msg = "Code detection matches Play Store declarations."
  if is_not_published:
    d_table_msg = (
        "N/A - App is not yet published. No declarations found for comparison."
    )

  d_table = render_table(
      mismatches,
      ["data_type", "local_view", "play_view", "status"],
      ["Data Type", "Code Detection", "Play Store Declaration", "Status"],
      d_table_msg,
  )

  # Local Access Section
  local_access = report_data.get("local_data_access", [])
  la_content = ""
  if local_access:
    la_table = render_table(
        local_access,
        ["data_type", "category", "evidence"],
        ["Data Type", "Category", "Access Evidence"],
        "No local-only access detected.",
    )
    la_content = (
        "## Local Data Access (No Transmission)\nThe following data types are"
        " accessed by the code but no evidence of network transmission or"
        " exfiltration was detected. These typically do not require a"
        f" 'Collection' declaration in the Data Safety section.\n{la_table}"
    )

  # Suggested Declaration (Conditional Section)
  suggested_dec = report_data.get("suggested_data_safety_declaration", {})
  dec_content = ""
  if suggested_dec and any(suggested_dec.values()):
    dec_content = (
        "### Suggested Data Safety Declaration"
        f" Updates\n```json\n{json.dumps(suggested_dec, indent=4)}\n```"
    )

  checklist_items = []
  for m in manual:
    m_issue = strip_emojis(
        m.get("issue") or m.get("issue_summary") or "Review item"
    )
    checklist_items.append(f"- [ ] {m_issue}")

  risk_categories = set(
      [v.get("category") for v in violations if v.get("category")]
  )
  mapping = {
      "Undeclared Collection": "Update Data Safety section in Play Console.",
      "Permissions": "Review and minimize requested permissions.",
  }
  for cat in risk_categories:
    if cat in mapping:
      checklist_items.append(f"- [ ] {mapping[cat]}")
  if not checklist_items:
    checklist_items.append("- [ ] Review all identified policy risks.")

  template_context = {
      "overall_compliance": compliance_ui,
      "current_date": datetime.now().strftime("%Y-%m-%d"),
      "app_name_id": app_id,
      "findings_detail": findings_content,
      "data_safety_table": d_table,
      "local_access_section": la_content,
      "suggested_declaration_section": dec_content,
      "personalized_checklist": "\n".join(checklist_items),
  }

  content = render_template(content, template_context)

  with open(output_path, "w") as f:
    f.write(content)

  json_output_path = output_path.replace(".md", ".json")
  with open(json_output_path, "w") as f:
    json.dump(
        {
            "metadata": {
                "app_id": app_id,
                "scan_date": datetime.now().isoformat(),
                "overall_compliance": compliance_raw,
            },
            "findings": violations,
            "data_safety_mismatches": mismatches,
            "manual_review": manual,
            "suggested_declaration": suggested_dec,
        },
        f,
        indent=4,
        sort_keys=True,
    )
  print(f"Reports generated successfully at {output_path}")


if __name__ == "__main__":
  main()
