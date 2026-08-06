---
name: perfetto-sql
description: >
  Translates natural language data intents into syntactically valid PerfettoSQL
  queries and executes them against a local trace file. Use this skill to
  extract slice, thread, or memory data from Android Perfetto traces using
  trace_processor.
keywords:
  - Android
  - Perfetto SQL
  - Query Guidelines
  - Performance Profiling
  - SQL Best Practices
  - SPAN_JOIN
  - Idempotency
---

# Ad-Hoc PerfettoSQL Querying

To write, debug, or execute ad-hoc PerfettoSQL queries or `trace_processor`
commands against a Perfetto trace, follow the guidelines and analytical
workflow in `$SKILL_ROOT/references/perfetto/sql.md`, and verify environment
prerequisites in `$SKILL_ROOT/references/perfetto/setup.md`.
