---
name: perfetto-sql
description: >
  Translates natural language data intents into syntactically valid PerfettoSQL
  queries and/or executes them against a trace file, if provided. Use this
  workflow to draft, debug, or execute queries extracting slice, thread, or
  memory data from Android Perfetto traces using trace_processor.
keywords:
  - Perfetto SQL
  - SQL Guidelines
  - SQL Best Practices
  - Ad-hoc Query
  - Trace Processor
  - SPAN_JOIN
  - Idempotency
---

# Ad-Hoc PerfettoSQL Querying

Use this workflow to write, debug, or execute PerfettoSQL queries:

- **Executing against a trace:** If a trace file path or URL is provided, verify
  environment prerequisites in `$SKILL_ROOT/references/perfetto/setup.md` and
  follow the session execution workflow in
  `$SKILL_ROOT/references/perfetto/sql.md`.
- **Static query authoring or debugging:** If no trace is attached, draft,
  optimize, or debug the queries using the syntax, schema guidelines, and
  standard library best practices in `$SKILL_ROOT/references/perfetto/sql.md`.
