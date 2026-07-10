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

"""Small logic-less template engine for Markdown generation."""

import re

# Precompiled regex patterns at module scope for linear-time optimizations
TAG_PATTERN = re.compile(r"\{\{(#|/)?(IF_ANY|IF_ALL|IF|EACH)?\s*(.*?)\}\}")
NEWLINE_COLLAPSE_PATTERN = re.compile(r"\n{3,}")
VAR_VALID_PATTERN = re.compile(r"^[A-Za-z0-9_.]+$")


def get_nested_val(ctx, key):
  """Resolves nested dot-notation keys from context (e.g., app.info.package)."""
  parts = key.split(".")
  val = ctx
  for p in parts:
    if isinstance(val, dict):
      val = val.get(p)
    else:
      return None
  return val


class Node:
  """Base class for all Abstract Syntax Tree (AST) node elements."""

  def render(self, ctx, buffer):
    raise NotImplementedError


class TextNode(Node):
  """Represents raw static text blocks."""

  def __init__(self, text):
    self.text = text

  def render(self, ctx, buffer):
    buffer.append(self.text)


class VarNode(Node):
  """Represents dynamic variable interpolation with literal fallbacks."""

  def __init__(self, key, raw_text):
    self.key = key
    self.raw_text = raw_text

  def render(self, ctx, buffer):
    val = get_nested_val(ctx, self.key)
    if val is not None:
      buffer.append(str(val))
    else:
      # Preserve the unresolved tag exactly like the original engine
      buffer.append(self.raw_text)


class IfNode(Node):
  """Represents conditional block logic (IF, IF_ALL, IF_ANY)."""

  def __init__(self, keys_str, mode, children):
    self.keys = [k.strip() for k in keys_str.split(",")]
    self.mode = mode
    self.children = children

  def render(self, ctx, buffer):
    condition_met = False
    if self.mode == "IF":
      condition_met = bool(get_nested_val(ctx, self.keys[0]))
    elif self.mode == "IF_ANY":
      condition_met = any(get_nested_val(ctx, k) for k in self.keys)
    elif self.mode == "IF_ALL":
      condition_met = all(get_nested_val(ctx, k) for k in self.keys)

    if condition_met:
      for child in self.children:
        child.render(ctx, buffer)


def strip_nodes(nodes):
  """Helper to strip leading/trailing whitespace from first/last TextNodes."""
  if not nodes:
    return nodes

  # Strip leading whitespace from first TextNode
  if isinstance(nodes[0], TextNode):
    nodes[0].text = nodes[0].text.lstrip()
    if not nodes[0].text:
      nodes.pop(0)

  # Strip trailing whitespace from last TextNode
  if nodes and isinstance(nodes[-1], TextNode):
    nodes[-1].text = nodes[-1].text.rstrip()
    if not nodes[-1].text:
      nodes.pop()

  return nodes


class EachNode(Node):
  """Represents standard iteration logic {{#EACH}} over lists/dicts."""

  def __init__(self, key, children):
    self.key = key
    self.children = strip_nodes(children)

  def _render_children(self, local_ctx):
    """Helper to render nested children under a specific loop context."""
    sub_buffer = []
    for child in self.children:
      child.render(local_ctx, sub_buffer)
    return "".join(sub_buffer)

  def render(self, ctx, buffer):
    val = get_nested_val(ctx, self.key)
    if not val:
      return

    rendered_items = []
    if isinstance(val, dict):
      for k, v in val.items():
        if k == "-":
          continue
        new_ctx = ctx.copy()
        new_ctx["KEY"] = str(k)
        new_ctx["VALUE"] = v
        rendered_items.append(self._render_children(new_ctx))

    elif isinstance(val, list):
      for item in val:
        new_ctx = ctx.copy()
        new_ctx["ITEM"] = item
        rendered_items.append(self._render_children(new_ctx))

    # Join iterations together with single newline
    buffer.append("\n".join(rendered_items))


class _ParserFrame:
  """Helper class representing an active stack frame in the compiler."""

  def __init__(self, tag_type, children, match):
    self.tag_type = tag_type
    self.children = children
    self.match = match


def compile_template(template_str):
  """Parses template_str forward-only, building the AST Node tree."""
  root_children = []
  stack = [_ParserFrame(None, root_children, None)]

  pos = 0
  for match in TAG_PATTERN.finditer(template_str):
    start, end = match.span()

    # 1. Capture static text prior to the tag
    if start > pos:
      stack[-1].children.append(TextNode(template_str[pos:start]))

    prefix = match.group(1)
    tag_type = match.group(2)
    arg = match.group(3).strip() if match.group(3) else ""
    raw_tag = match.group(0)

    if prefix == "#":
      if tag_type:
        # Pushing a new nested frame block onto the stack
        new_children = []
        stack.append(_ParserFrame(tag_type, new_children, match))
      else:
        # Invalid tag starting with #, treat as normal text block
        stack[-1].children.append(TextNode(raw_tag))

    elif prefix == "/":
      if tag_type:
        # Block closer tag
        if len(stack) > 1 and stack[-1].tag_type == tag_type:
          frame = stack.pop()
          open_arg = frame.match.group(3).strip()

          if frame.tag_type == "EACH":
            node = EachNode(open_arg, frame.children)
          else:
            node = IfNode(open_arg, frame.tag_type, frame.children)

          stack[-1].children.append(node)
        else:
          # Mismatched closer, treat as static text
          stack[-1].children.append(TextNode(raw_tag))
      else:
        # Invalid closer, treat as static text
        stack[-1].children.append(TextNode(raw_tag))

    else:
      # Variable tag or unknown braces
      if VAR_VALID_PATTERN.match(arg):
        stack[-1].children.append(VarNode(arg, raw_tag))
      else:
        stack[-1].children.append(TextNode(raw_tag))

    pos = end

  # Capture any trailing text blocks
  if pos < len(template_str):
    stack[-1].children.append(TextNode(template_str[pos:]))

  # Graceful recovery for unclosed tags to prevent runtime crashes
  while len(stack) > 1:
    frame = stack.pop()
    # Re-inject the unclosed opening tag as plain text, followed by its children
    stack[-1].children.append(TextNode(frame.match.group(0)))
    stack[-1].children.extend(frame.children)

  return root_children


def render_template(template_str, context_dict):
  """Renders a markdown template with high performance and zero dependencies."""
  nodes = compile_template(template_str)

  buffer = []
  for node in nodes:
    node.render(context_dict, buffer)

  final_string = "".join(buffer)

  # Collapse 3 or more consecutive newlines into exactly 2 (preserves layout)
  return NEWLINE_COLLAPSE_PATTERN.sub("\n\n", final_string)
