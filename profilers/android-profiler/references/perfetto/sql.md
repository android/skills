# Querying Perfetto traces

This reference explains how to extract data from a Perfetto trace file
(`.pftrace`, `.perfetto-trace`, `.pb`) using `trace_processor` and
PerfettoSQL. Read it for ad-hoc querying outside a guided workflow; the
workflows under `$SKILL_ROOT/analysis/workflows` and
`$SKILL_ROOT/recording/workflows` carry their own queries.

The `trace_processor` binary is what every other Perfetto analysis tool
runs on top of, including the Perfetto UI. Reference docs:
<https://perfetto.dev/docs/analysis/trace-processor>.

> **Prerequisite - `trace_processor` must be invokable.** Before
> running any of the shell commands below, read
> `$SKILL_ROOT/references/perfetto/setup.md`. It defines how to make
> the bare `trace_processor` commands below work in this environment.

## Querying a trace: sessions

Querying goes through a **session**: load the trace once into a named
background session, then run every query against it with `--remote`.
Parsing a trace is the expensive part (tens of seconds for a multi-GB
trace); the session pays it once, and every real analysis runs more than
one query.

```sh
# 1. Load the trace into a background session - once per trace.
#    Pick a descriptive session name (e.g. derived from the trace file).
trace_processor server unix --name mysession --daemonize TRACE_FILE

# 2. Run queries against the warm session: instant, no reparse.
trace_processor query --remote mysession \
  "SELECT ts, dur, name FROM slice WHERE dur > 5e8 LIMIT 5"

# 3. When you are completely done with the trace:
trace_processor server kill mysession
```

Multiple statements separated by `;` are supported in one invocation.

Session rules:

- Session names are managed by trace_processor in a per-user session
  directory - there are no ports to choose and no collisions with other
  agents or the Perfetto UI.
- **Session state persists across `query --remote` calls.** A
  `CREATE PERFETTO TABLE` or `INCLUDE PERFETTO MODULE` run in one call is
  visible in the next, so materializing intermediate results pays off
  across invocations.
- Flags that configure trace loading (`--full-sort`, `--add-sql-package`, ...)
  belong on the `server unix` invocation, not on `query --remote` - the client
  rejects them with an explanatory error.
- `--remote` also accepts an absolute `*.sock` path or `host:port`;
  names are the common case.
- Forgotten sessions are reaped automatically after 30 minutes idle
  (`--idle-timeout`), but kill your session when the analysis is done.

`TRACE_FILE` can be a local path, an `http(s)://` URL, or a Perfetto UI
share link (`https://ui.perfetto.dev/#!/?s=<hash>`) - in the last two
cases trace_processor downloads the trace for you (cached under
`~/.cache/perfetto/` or the platform equivalent), resolving the share
link to its underlying trace first.

For a single throwaway query on a small trace you _can_ skip the session
(`trace_processor query TRACE_FILE "..."` parses, queries, and exits),
but treat that as the exception: it re-parses the trace on every
invocation and forgets created tables and included modules between
calls. Default to a session.

## Discovering what's in the trace

PerfettoSQL ships with **intrinsic table-functions** for browsing the
loaded standard library - modules, tables/views, functions, macros. Use
these to find what's available and to verify if a Standard Library module
already provides the needed abstraction before drafting custom logic.

**Mandatory Schema Check:** Do not guess column names or join keys. Always
use a plain `LIMIT 0` query to read the exact column schema of any specific
table, view, or query result before drafting your query.

> **Intrinsic surface - not stable API.** The `__intrinsic_*` names below
> are an implementation detail of trace processor. They're fair game for
> an agent to use during a session because this reference is loaded, but
> **don't bake `__intrinsic_*` names into committed scripts, dashboards,
> or stdlib modules** - they can change without notice.

```sql
-- 1. List every stdlib module currently available.
SELECT package, module FROM __intrinsic_stdlib_modules ORDER BY 1, 2;

-- 2. List the tables/views a specific module exposes
--    (after INCLUDE PERFETTO MODULE).
INCLUDE PERFETTO MODULE slices.with_context;
SELECT name, type, exposed, description
FROM __intrinsic_stdlib_tables('slices.with_context');

-- 3. List functions / macros a module exposes.
SELECT name, return_type, args
FROM __intrinsic_stdlib_functions('slices.with_context');
SELECT name, return_type, args
FROM __intrinsic_stdlib_macros('android.memory.heap_graph.helpers');

-- 4. Read the column schema of any table, view, or query.
--    LIMIT 0 returns the result header with no row scan; trace_processor
--    prints "column N = <name>" lines for each column.
SELECT * FROM slice LIMIT 0;
SELECT * FROM thread_or_process_slice LIMIT 0;
SELECT * FROM (SELECT ts, dur, name FROM slice WHERE dur > 0) LIMIT 0;
```

Useful starting points for any trace:

| View           | What's in it                                                     |
| :------------- | :--------------------------------------------------------------- |
| `slice`        | Atrace slices, async slices, anything with a duration on a track |
| `thread`       | One row per thread                                               |
| `process`      | One row per process                                              |
| `thread_state` | State transitions (Running, Runnable, Sleeping, ...)             |
| `sched_slice`  | When threads were on-CPU                                         |
| `counter`      | Time-series counter samples                                      |
| `track`        | Every track in the trace; join on `track_id` to other tables     |

Static reference for the public surface (does not require a running
trace_processor): <https://perfetto.dev/docs/analysis/sql-tables>.

## Using the standard library

Most useful queries are _much_ shorter when you build on stdlib modules
instead of joining raw tables yourself. Generated stdlib reference:
<https://perfetto.dev/docs/analysis/stdlib-docs>.

Include a module before referencing the views, tables or macros it
defines:

```sql
INCLUDE PERFETTO MODULE slices.with_context;

SELECT name, dur, thread_name, process_name
FROM thread_or_process_slice
WHERE dur > 1e9                     -- slices longer than 1s
ORDER BY dur DESC
LIMIT 20;
```

A few commonly used modules to know:

- `slices.with_context` - slice rows joined with their thread / process.
- `sched.with_context` - `sched_slice` joined with thread / process.
- `android.startup.startups` - one row per app startup.
- `stacks.cpu_profiling` - flat samples and call-graph helpers.
- `android.memory.heap_graph.dominator_tree` - retained-size analysis for
  Java heap dumps.

The module name maps directly to the file path under the stdlib root:
`foo.bar` lives at `foo/bar.sql`. Browse the full list at the stdlib
reference linked above.

## Tips for writing good PerfettoSQL

- **Reach for stdlib first.** If you find yourself joining `slice` to
  `thread_track` to `thread` to `process`, there is almost certainly a
  stdlib module that already does it. Check the stdlib reference before
  writing the join.
- **Filter on `dur > 0` and Trace Boundaries carefully.** Some slices have
  `dur = -1` (still open at trace end) and some have `dur = 0` (instant
  events). Be explicit about which you mean. When calculating a bounding box
  (for example, `ts + dur`) or summing durations (`SUM(dur)`), handle
  incomplete durations using: `IIF(dur = -1, trace_end() - ts, dur)`.
- **Robust State Transitions.** Avoid manual timestamp arithmetic (for
  example, `ts + dur = next.ts`) to join adjacent events. Rely on standard
  library modules (for example, `sched.runnable`, `linux.perf.counters`,
  `intervals.overlap`) which safely handle trace gaps and preemptions.
- **Working with Identifiers:**
  - **Use Unique Identifiers for Joins:** When writing SQL queries in
    Perfetto, you must join tables using `utid` (unique thread ID) or `upid`
    (unique process ID) instead of regular `tid` or `pid`. **Why it's
    useful**: The operating system recycles `TIDs` and `PIDs`, while `UTIDs`
    and `UPIDs` remain unique for the lifetime of the trace, which prevents
    incorrect joins.
  - Columns like `id`, `utid`, `upid`, `track_id` are not stable across traces
    or even runs of trace_processor on the same trace. You can use them
    **inside** a query as join keys, but alongside IDs, always join out to
    a stable name (`thread.name`, `process.name`, `slice.name`) when reporting
    results to the user.
  - **Materialize expensive intermediate results.** `CREATE PERFETTO TABLE foo
    AS SELECT ...` caches the result so subsequent queries don't redo the work.
    - _Note for `SPAN_JOIN`:_ Intermediate tables fed into a `SPAN_JOIN` must
      be materialized using `CREATE PERFETTO TABLE`, not `CREATE VIEW`.
- **Idempotency.** Ensure queries are idempotent to prevent "already exists"
  errors during multiple executions.
  - For Perfetto objects, always use `CREATE OR REPLACE`:
    `CREATE OR REPLACE PERFETTO {TABLE|VIEW|MACRO|FUNCTION}`.
  - For SQLite Virtual Tables (such as `SPAN_JOIN`), `CREATE OR REPLACE` is
    not supported. Explicitly drop them first:
    `DROP TABLE IF EXISTS my_table;`
    `CREATE VIRTUAL TABLE my_table USING SPAN_JOIN(...);`
  - For standard SQLite indexes, prepend `DROP INDEX IF EXISTS index_name;`.
- **`SPAN_JOIN` safety.** `SPAN_JOIN` will crash if intervals **within the
  same input table** overlap. Always use the `PARTITIONED {column}` (for
  example, `PARTITIONED track_id`) clause to isolate intervals.
- **Avoid `SELECT *` in saved queries.** Trace processor table schemas can
  gain columns; pin the columns you actually use.
- **Use `EXPLAIN QUERY PLAN` if a query is slow.** It shows whether SQLite is
  using indexes. Counter and slice tables have built-in indexes on `ts` and
  `track_id`; queries that don't filter on either will scan the whole table.
- **Argument Extraction:** Use `EXTRACT_ARG(arg_set_id, 'key')` to fetch event
  properties instead of manually joining the `args` table.
- **JSON Parsing:** When dealing with JSON text, use standard SQLite JSON
  functions (for example, `json_extract()`) to extract values.
- **String Matching (Always use GLOB).** Use `GLOB` instead of `LIKE`. `LIKE`
  causes performance bottlenecks and treats underscores (`_`) as wildcards,
  leading to bugs.
  - **Exact matches:** Use `=`.
  - **Substring matches:** Use `GLOB` with `*` (for example,
    `name GLOB '*RenderThread*'`).
  - **Case-insensitive matches:** Use `LOWER(name) GLOB` and make sure the
    search string is fully lowercase (for example,
    `LOWER(name) GLOB '*renderthread*'`). Use this when dealing with
    inconsistent trace capitalization (for example, `WakeLock` vs `wakelock`).
- **Alias Precision.** Always prefix column names with table or view alias,
  that is: `{alias}.{column_name}`.

## Common Analysis Patterns

- **Calculating Time Overlaps & CPU Time:**
  1. **Primary Method (MANDATORY):** Always search the standard library first
     before writing custom interval logic. For example, to find the exact CPU
     execution time of a slice, do not calculate it manually; instead, search
     the docs and use the `slices.cpu_time` module.
  2. **Fallback Method (Use ONLY if you have verified no stdlib module or
     `SPAN_JOIN` applies):** If you must calculate custom overlap durations
     between two different sets of time intervals `[start1, end1]` and
     `[start2, end2]`:
     - **Condition:** The intervals overlap if `start1 < end2` and
       `start2 < end1`.
     - **Duration:** The overlap duration is calculated as
       `MIN(end1, end2) - MAX(start1, start2)`.
     - **Important:** Incomplete Perfetto slices have a duration of -1
       (`dur = -1`). Always calculate the effective end time using
       `ts + IIF(dur = -1, trace_end() - ts, dur)` before applying this logic.
- Include the `android.startup.startups` module and query
  `android_thread_slices_for_all_startups` (or `android_startups`) for
  app startup requests.
- Join `counter_track` with `counter` to get values of counter with a
  specific name.
- When querying for a CPU frequency counter, include the `linux.cpu.frequency`
  module and use the `cpu_frequency_counters` table.
- **Window Size:** When looking for events around a specific timestamp, start
  with 100ms as the window size.
- **Total Duration:** To calculate the total time spent in slices matching a
  specific name pattern (for example, `*{name_pattern}*`), you must sum their
  durations. **Why it's useful**: This helps quantify the total impact of a
  specific function or feature on performance across multiple calls. Here is
  an example query (note the safe handling of incomplete slices):
  ```sql
  SELECT
    count(*) as total_count,
    sum(IIF(slice.dur = -1, trace_end() - slice.ts, slice.dur)) / 1e6
      as total_dur_ms
  FROM slice
  WHERE slice.name GLOB '*{name_pattern}*';
  ```

## Analytical Workflow (Standard Operating Procedure)

To ensure accuracy and efficiency, follow these steps:

1. **Research & Dissection:** Identify the core question and required data
   points.
2. **Mandatory Schema Validation:** Locate relevant modules via
   `__intrinsic_stdlib_modules` and their tables via
   `__intrinsic_stdlib_tables('module_name')`. Verify column names and types.
   - **Intent Check:** You must verify if a stdlib module already provides
     the needed abstraction before drafting manual arithmetic or custom joins.

- **IMPORTANT:** If your query requires calculating overlaps, intersections,
  or boundaries between intervals, you MUST search `__intrinsic_stdlib_modules`
  globally (for example, `WHERE module GLOB '*overlap*'`) before writing
  `MIN()/MAX()` or `IIF(dur = -1...)` logic.

3. **Draft & Validate Loop (Max 3 Iterations):**

- [ ] **Draft:** Use only verified schemas. Ensure `INCLUDE PERFETTO MODULE`
      is present for non-prelude modules.
- [ ] **Verify Idempotency:** Use `CREATE OR REPLACE` or `DROP TABLE IF EXISTS`
      for virtual tables.
- [ ] **Check Precision:** Are ALL columns prefixed with aliases (e.g.,
      `s.name`)? Are you joining on `utid`/`upid`?
- [ ] **String Matching:** Did you use `GLOB` or `=` instead of `LIKE`?
- [ ] **Span Join Check:** If using `SPAN_JOIN`, are tables `PARTITIONED`
      and materialized?
- [ ] **Execute:** Run against the session:
      `trace_processor query --remote SESSION "QUERY"`.

**Execution Rules:**

- **File Usage:** If you must create a SQL file to execute queries (for
  example, due to query length or escaping issues), you must create them
  in the `/tmp/` directory.
- **Failure Resilience:** Debug and fix SQL syntax and logic errors when
  query fails. Don't simplify the analytical intent to pass validation.
  For example, if requested to calculate an overlap or intersection, you
  must fix the intersection math. Don't substitute with disjoint queries
  (for example, returning independent total durations) as a workaround.

4. **Cleanup & Finalize:**

- Explicitly return and state the final validated SQL and explain the
  results to the user.
- **Save an analysis report.** Write a markdown file in the working
  directory (default `perfetto_analysis_report.md`) containing: the
  question investigated, the trace file(s) analyzed, the findings with
  concrete numbers, the final validated queries (so the analysis can be
  re-run), and open questions / next steps. Point the user at it in
  your final message.
- Before finishing, delete any temporary SQL files created in `/tmp/`.

## Where to look for more

- Language tour:
  <https://perfetto.dev/docs/analysis/perfetto-sql-getting-started>
- Trace processor reference:
  <https://perfetto.dev/docs/analysis/trace-processor>
- Generated table reference:
  <https://perfetto.dev/docs/analysis/sql-tables>
- Generated stdlib reference:
  <https://perfetto.dev/docs/analysis/stdlib-docs>
