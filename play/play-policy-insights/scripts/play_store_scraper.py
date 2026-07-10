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

"""Scrapes and parses app metadata and safety info from Play Store."""

import html.parser
import json
import os
import ssl
import sys
import urllib.error
import urllib.request


def fetch_html(url, verify_ssl=True):
  """Fetches HTML content from a URL with a realistic User-Agent."""
  headers = {
      "User-Agent": (
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
          "AppleWebKit/537.36 (KHTML, like Gecko) "
          "Chrome/119.0.0.0 Safari/537.36"
      ),
      "Accept-Language": "en-US,en;q=0.9",
  }
  req = urllib.request.Request(url, headers=headers)
  if verify_ssl:
    context = ssl.create_default_context()
  else:
    context = ssl._create_unverified_context()
  try:
    with urllib.request.urlopen(req, timeout=10, context=context) as resp:
      return resp.read().decode("utf-8")
  except urllib.error.HTTPError as e:
    if e.code == 404:
      print(f"App not found on Play Store (404): {url}", file=sys.stderr)
    else:
      print(f"HTTP Error: {e.code} {e.reason}", file=sys.stderr)
    return None
  except Exception as e:
    print(f"Error fetching {url}: {e}", file=sys.stderr)
    return None


class PlayStoreMetadataParser(html.parser.HTMLParser):
  """Parses core app metadata from Play Store HTML."""

  def __init__(self):
    super().__init__()
    self.ld_json = None
    self.meta_category = None
    self.meta_content_rating = None
    self.meta_description = None
    self.title_text = None
    self.developer_name = None
    self.privacy_policy_url = None

    self.in_ld_json = False
    self.json_data = []
    self.in_title = False
    self.in_dev_link = False

    self.in_a_tag = False
    self.current_a_href = None

  def handle_starttag(self, tag, attrs):
    attrs_dict = dict(attrs)

    if tag == "script" and attrs_dict.get("type") == "application/ld+json":
      self.in_ld_json = True
      self.json_data = []

    if tag == "meta":
      itemprop = attrs_dict.get("itemprop")
      if itemprop == "applicationCategory":
        self.meta_category = attrs_dict.get("content")
      elif itemprop == "contentRating":
        self.meta_content_rating = attrs_dict.get("content")

      name = attrs_dict.get("name")
      prop = attrs_dict.get("property")
      if prop == "og:description" or name == "description":
        if not self.meta_description:
          self.meta_description = attrs_dict.get("content")

    if tag == "h1":
      self.in_title = True

    if tag == "a":
      href = attrs_dict.get("href", "")
      if "/store/apps/dev" in href:
        self.in_dev_link = True

      self.in_a_tag = True
      self.current_a_href = href

  def handle_endtag(self, tag):
    if tag == "script" and self.in_ld_json:
      self.in_ld_json = False
      try:
        self.ld_json = json.loads("".join(self.json_data))
        if isinstance(self.ld_json, list):
          self.ld_json = self.ld_json[0]
      except (json.JSONDecodeError, TypeError, IndexError):
        pass
    if tag == "h1":
      self.in_title = False
    if tag == "a":
      self.in_dev_link = False
      self.in_a_tag = False
      self.current_a_href = None

  def handle_data(self, data):
    if self.in_ld_json:
      self.json_data.append(data)
    elif self.in_title and not self.title_text:
      self.title_text = data.strip()
    elif self.in_dev_link and not self.developer_name:
      self.developer_name = data.strip()
    elif self.in_a_tag and self.current_a_href:
      if "privacy policy" in data.lower():
        if "google.com" not in self.current_a_href.lower():
          if not self.privacy_policy_url:
            self.privacy_policy_url = self.current_a_href


class PlayStoreDataSafetyParser(html.parser.HTMLParser):
  """Extracts visible text content from HTML structurally."""

  def __init__(self):
    super().__init__()
    self.text_parts = []
    self.ignore_tags = {"script", "style", "noscript", "svg", "path"}
    self.in_ignored_tag = 0

  def handle_starttag(self, tag, attrs):
    if tag.lower() in self.ignore_tags:
      self.in_ignored_tag += 1

  def handle_endtag(self, tag):
    if tag.lower() in self.ignore_tags:
      self.in_ignored_tag = max(0, self.in_ignored_tag - 1)

  def handle_data(self, data):
    if self.in_ignored_tag == 0:
      cleaned = data.strip()
      if cleaned:
        self.text_parts.append(cleaned)


def load_taxonomy():
  """Loads the valid data safety taxonomy from policies.json."""
  repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
  policy_path = os.path.join(repo_root, "resources", "policies.json")
  try:
    with open(policy_path, "r") as f:
      data = json.load(f)
      taxonomy = data.get("data_safety_section", {}).get("taxonomy", {})
      return {
          item["data_type"].lower()
          for item in taxonomy.values()
          if "data_type" in item
      }
  except Exception as e:
    print(
        f"Warning: Failed to load taxonomy from {policy_path}: {e}",
        file=sys.stderr,
    )
    return set()


# Official Play Store Data Safety Taxonomy for validation
VALID_TAXONOMY = load_taxonomy()


def extract_metadata(html_content):
  """Extracts all metadata into a clean dictionary."""
  metadata = {
      "title": "Unknown App",
      "developer": "Unknown Developer",
      "category": None,
      "content_rating": None,
      "description": None,
      "privacy_policy_url": None,
  }

  if not html_content:
    return metadata

  parser = PlayStoreMetadataParser()
  parser.feed(html_content)

  # 1. Prioritize ld+json payload
  if parser.ld_json:
    metadata["title"] = parser.ld_json.get("name", metadata["title"])

    author = parser.ld_json.get("author", {})
    if isinstance(author, dict):
      metadata["developer"] = author.get("name", metadata["developer"])
    elif isinstance(author, str):
      metadata["developer"] = author

    metadata["category"] = parser.ld_json.get(
        "applicationCategory", metadata["category"]
    )
    metadata["content_rating"] = parser.ld_json.get(
        "contentRating", metadata["content_rating"]
    )
    metadata["description"] = parser.ld_json.get(
        "description", metadata["description"]
    )

  # 2. Fall back to HTML tags if json was missing elements
  if metadata["title"] == "Unknown App" and parser.title_text:
    metadata["title"] = parser.title_text
  if metadata["developer"] == "Unknown Developer" and parser.developer_name:
    metadata["developer"] = parser.developer_name
  if not metadata["category"] and parser.meta_category:
    metadata["category"] = parser.meta_category
  if not metadata["content_rating"] and parser.meta_content_rating:
    metadata["content_rating"] = parser.meta_content_rating
  if not metadata["description"] and parser.meta_description:
    metadata["description"] = parser.meta_description

  # Privacy Policy is only found in <a> tags
  metadata["privacy_policy_url"] = parser.privacy_policy_url

  return metadata


def extract_data_safety(html_content):
  """Extracts Data Safety information structurally."""
  if not html_content:
    return {"data_collected": [], "data_shared": []}

  parser = PlayStoreDataSafetyParser()
  parser.feed(html_content)
  parts = parser.text_parts

  result = {"data_collected": [], "data_shared": []}

  purposes_keywords = [
      "App functionality",
      "Analytics",
      "Fraud prevention, security, and compliance",
      "Personalization",
      "Account management",
      "Advertising or marketing",
      "Developer communications",
  ]

  known_categories = {
      "Location",
      "Personal info",
      "Financial info",
      "Health and fitness",
      "Messages",
      "Photos and videos",
      "Audio files",
      "Files and docs",
      "Calendar",
      "Contacts",
      "App activity",
      "Web browsing",
      "App info and performance",
      "Device or other IDs",
  }

  # Robust parsing ignoring UI icons
  cur_sec = None
  cur_cat = None
  cur_type = None

  for p in parts:
    p_lower = p.lower()

    # Section Transitions
    if (
        p_lower == "data shared"
        or p_lower
        == "here's more information the developer has provided about the kinds"
        " of data this app may share"
    ):
      cur_sec = "data_shared"
      cur_cat = None
      continue
    elif (
        p_lower == "data collected"
        or p_lower
        == "here's more information the developer has provided about the kinds"
        " of data this app may collect"
    ):
      cur_sec = "data_collected"
      cur_cat = None
      continue
    elif p_lower == "security practices" or p_lower == "data safety":
      if cur_sec:
        break

    if not cur_sec:
      continue

    # Ignore UI artifacts
    if p_lower in [
        "expand_more",
        "expand_less",
        "info",
        "data collected and for what purpose",
        "data shared and for what purpose",
    ]:
      continue

    # If it's a known purpose keyword, attach it to the current type
    matched_purposes = [kw for kw in purposes_keywords if kw.lower() in p_lower]
    if matched_purposes:
      if cur_type:
        cur_type["purposes"].extend(matched_purposes)
        cur_type["purposes"] = list(set(cur_type["purposes"]))
      continue

    if p_lower == "· optional":
      if cur_type:
        cur_type["optional"] = True
      continue

    # If it's not a purpose or an artifact, it's either a Category or a
    # Data Type
    if len(p) > 50:
      continue

    if p in known_categories:
      if not cur_cat or cur_cat["category"] != p:
        cur_cat = {"category": p, "types": []}
        result[cur_sec].append(cur_cat)
        cur_type = None
      continue

    if cur_cat:
      if p == cur_cat["category"]:
        continue

      cur_type = {"type": p, "purposes": [], "optional": False}
      cur_cat["types"].append(cur_type)

  # Post-process to remove summary headers (UI groupings)
  for cat in result["data_collected"] + result["data_shared"]:
    filtered_types = []
    cat_name = cat["category"].lower()

    for t in cat["types"]:
      t_name_lower = t["type"].lower()

      # Indicators that this is a summary header rather than a declaration:
      # 1. It has no associated purposes (headers are never assigned purposes)
      # 2. AND it either:
      #    a. Matches the category name exactly (redundant UI artifact)
      #    b. Is not a valid item in the official taxonomy (summary string)
      if not t["purposes"]:
        if (
            t_name_lower == cat_name
            or t_name_lower not in VALID_TAXONOMY
        ):
          continue
      
      filtered_types.append(t)
    
    cat["types"] = filtered_types

  return result


def scrape_app_details(pkg_name, verify_ssl=True):
  """Scrapes app metadata and Data Safety info for a given package."""
  details_url = (
      f"https://play.google.com/store/apps/details?id={pkg_name}&hl=en&gl=US"
  )
  ds_url = (
      f"https://play.google.com/store/apps/datasafety?id={pkg_name}&hl=en&gl=US"
  )

  details_html = fetch_html(details_url, verify_ssl=verify_ssl)
  metadata = extract_metadata(details_html)

  ds_html = fetch_html(ds_url, verify_ssl=verify_ssl)
  data_safety = extract_data_safety(ds_html)

  return {
      "package_name": pkg_name,
      "is_published": bool(details_html),
      "store_url": details_url,
      "title": metadata["title"],
      "developer": metadata["developer"],
      "description": metadata["description"],
      "category": metadata["category"],
      "app_info": {"content_rating": metadata["content_rating"]},
      "developer_links": {"privacy_policy_url": metadata["privacy_policy_url"]},
      "data_safety": data_safety,
  }
