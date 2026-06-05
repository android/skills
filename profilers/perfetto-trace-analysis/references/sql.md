# Querying Perfetto Traces

This skill teaches you how to extract data from a Perfetto trace file
(`.pftrace`, `.perfetto-trace`, `.pb`) using `trace_processor` and PerfettoSQL.

The `trace_processor` binary is what every other Perfetto analysis tool runs on
top of, including the Perfetto UI. [Reference
Docs](https://perfetto.dev/docs/analysis/trace-processor).

> **Prerequisite — `trace_processor` must be invokable.** Before running any of
> the shell commands below, ensure you have set up the trace_processor by
> following guidelines mentioned here:
> [`getting-trace-processor.md`](references/getting-trace-processor.md) The
> guidelines tell you the exact invocation form for `trace_processor` in this
> environment — substitute it for every bare `trace_processor` reference below.
> Likewise, the long-running RPC mode needs the `perfetto` Python client, whose
> set up the guidelines also covers.

## Quickstart

To run a single query and exit:
```sh
./trace_processor query TRACE_FILE "SELECT ts, dur FROM slice LIMIT 10"
```

Multiple statements separated by `;` are supported in one invocation.

## Long-running mode (preferred for iteration)

Reparsing a trace on every query is slow — for a multi-GB trace it is tens of
seconds, every time. When you expect to run more than a couple of queries, start
the shell once as an HTTP RPC server and drive it from the Python client. (If
the Python client is not installed yet, the
[`getting-trace-processor.md`](references/getting-trace-processor.md) guidelines
in your environment covers it.)

```sh
# Terminal A: pick a random high port and start the server on it.
# Always pass --port explicitly: the default (9001) is also used by
# the Perfetto UI, and a fixed port collides with any other agent
# already running a trace_processor server.
PORT=$((9100 + RANDOM % 900))
./trace_processor server http --port $PORT TRACE_FILE
```

```python
# Terminal B (or any Python process): connect to the running server.
# PORT is the value chosen in Terminal A.
from perfetto.trace_processor import TraceProcessor

tp = TraceProcessor(addr='127.0.0.1:PORT')
for row in tp.query('SELECT ts, dur, name FROM slice WHERE dur > 5e8 LIMIT 5'):
    print(row.dur, row.name)

# Pandas is also supported if the dependency is installed:
df = tp.query('SELECT ts, dur, name FROM slice LIMIT 100').as_pandas_dataframe()

tp.close()
```

The server keeps the trace parsed in memory; each `tp.query()` call is just a
query against the existing session. This is the same RPC channel
`ui.perfetto.dev` uses when you load a trace there.

Notes:

-   Use `'127.0.0.1:PORT'`, not `'localhost:PORT'`. The server binds on
    IPv4/IPv6 explicitly and `localhost` resolution can pick an interface that
    isn't bound on macOS.
-   A quick liveness check from the shell: `curl http://127.0.0.1:PORT/status`
    returns plain JSON-ish status (loaded path, version) and is the fastest way
    to confirm the server is up.
-   The on-the-wire `/query`, `/parse`, `/rpc` endpoints take protobuf- encoded
    `QueryArgs`/`TraceProcessorRpc` payloads. **Do not hand-craft HTTP calls
    with `curl`** — use the Python client (or the WASM client embedded in the
    UI). Hand-crafted calls work for `/status` only.
-   If you want to stay in the C++ shell instead of Python, a one-shot
    `./trace_processor query TRACE_FILE "..."` is fine for a handful of queries;
    the server is the right answer the moment iteration starts.

## Discovering what's in the Trace

PerfettoSQL ships with **intrinsic table-functions** for browsing the loaded
standard library — modules, tables/views, functions, macros. Use these to find
what's available and to verify if a Standard Library module already provides the
needed abstraction before drafting custom logic.

**Mandatory Schema Check:** Do not guess column names or join keys. Always use a
plain `LIMIT 0` query to read the exact column schema of any specific table,
view, or query result before drafting your query.

> **Intrinsic surface — not stable API.** The `__intrinsic_*` names below are an
> implementation detail of trace_processor. They're fair game for an agent to
> use during a session because this skill is loaded, but **don't bake
> `__intrinsic_*` names into committed scripts, dashboards, or stdlib modules**
> — they can change without notice.

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

| View           | What's in it                                                      |
|----------------|-------------------------------------------------------------------|
| `slice`        | Atrace slices, async slices, anything with a duration on a track  |
| `thread`       | One row per thread                                                |
| `process`      | One row per process                                               |
| `thread_state` | Scheduling state transitions (Running, Runnable, Sleeping, …)     |
| `sched_slice`  | When threads were on-CPU                                          |
| `counter`      | Time-series counter samples                                       |
| `track`        | Every track in the trace; join on `track_id` to most other tables |

Static reference for the public surface (does not require a running
trace_processor): https://perfetto.dev/docs/analysis/sql-tables.

## Using the standard library

Most useful queries are *much* shorter when you build on stdlib modules instead
of joining raw tables yourself. Generated stdlib reference:
https://perfetto.dev/docs/analysis/stdlib-docs.

Include a module before referencing the views, tables or macros it defines:

```sql
INCLUDE PERFETTO MODULE slices.with_context;

SELECT name, dur, thread_name, process_name
FROM thread_or_process_slice
WHERE dur > 1e9                     -- slices longer than 1s
ORDER BY dur DESC
LIMIT 20;
```

A few commonly used modules to know:

-   `slices.with_context` — slice rows joined with their thread / process.
-   `sched.with_context` — `sched_slice` joined with thread / process.
-   `android.startup.startups` — one row per app startup.
-   `stacks.cpu_profiling` — flat samples and call-graph helpers.
-   `android.memory.heap_graph.dominator_tree` — retained-size analysis for Java
    heap dumps (see the `perfetto_workflow_android_heap_dump` skill for usage).

The module name maps directly to the file path under the stdlib root: `foo.bar`
lives at `foo/bar.sql`. Browse the full list at the stdlib reference linked
above.

## Guidelines and Hints

-   **Reach for stdlib first.** If you find yourself joining `slice` to
    `thread_track` to `thread` to `process`, there is almost certainly a stdlib
    module that already does it. Check the stdlib reference before writing the
    join.
-   **Filter on `dur > 0` and Trace Boundaries carefully.** Some slices have
    `dur = -1` (still open at trace end) and some have `dur = 0` (instant
    events). Be explicit about which you mean. When calculating a bounding box
    (for example, `ts + dur`) or summing durations (`SUM(dur)`), handle
    incomplete durations using: `IIF(dur = -1, trace_end() - ts, dur)`.
-   **Robust State Transitions.** Avoid manual timestamp arithmetic (for
    example, `ts + dur = next.ts`) to join adjacent events. Rely on standard
    library modules (for example, `sched.runnable`, `linux.perf.counters`,
    `intervals.overlap`) which safely handle trace gaps and preemptions.
-   **Working with Identifiers:**
  -   **Use Unique Identifiers for Joins:** When writing SQL queries in
      Perfetto, you must join tables using `utid` (unique thread ID) or `upid`
      (unique process ID) instead of the regular `tid` or `pid`. **Why it's
      useful**: The operating system recycles `TIDs` and `PIDs`, while `UTIDs`
      and `UPIDs` remain unique for the lifetime of the trace, which prevents
      incorrect joins.
  -   Columns like `id`, `utid`, `upid`, `track_id` are not stable across
      traces or even runs of trace_processor on the same trace. You can use
      them
      **inside** a query as join keys, but alongside the IDS, always join out to
      a stable name (`thread.name`, `process.name`, `slice.name`) when
      reporting results to the user.
  -   **Materialize expensive intermediate results.** `CREATE PERFETTO TABLE
      foo AS SELECT ...` caches the result so subsequent queries don't redo
      the work.
    -   *Note for `SPAN_JOIN`:* Intermediate tables fed into a `SPAN_JOIN`
        must be materialized using `CREATE PERFETTO TABLE`, not `CREATE
        VIEW`.
-   **Idempotency.** Ensure queries are idempotent to prevent "already exists"
    errors during multiple executions.
  -   For Perfetto objects, always use `CREATE OR REPLACE`: `CREATE OR REPLACE
      PERFETTO {TABLE|VIEW|MACRO|FUNCTION}`.
  -   For SQLite Virtual Tables (such as `SPAN_JOIN`), `CREATE OR REPLACE` is
      not supported. Explicitly drop them first: `DROP TABLE IF EXISTS my_table;
      CREATE VIRTUAL TABLE my_table USING SPAN_JOIN(...);`
  -   For standard SQLite indexes, prepend `DROP INDEX IF EXISTS index_name;`.
-   **`SPAN_JOIN` safety.** `SPAN_JOIN` will crash if intervals **within the
    same input table** overlap. Always use the `PARTITIONED {column}` (for
    example, `PARTITIONED track_id`) clause to isolate intervals.
-   **Avoid `SELECT *` in saved queries.** Trace processor table schemas can
    gain columns; pin the columns you actually use.
-   **Use `EXPLAIN QUERY PLAN` if a query is slow.** It shows whether SQLite is
    using indexes. Counter and slice tables have built-in indexes on `ts` and
    `track_id`; queries that don't filter on either will scan the whole table.
-   **Argument Extraction:** Use `EXTRACT_ARG(arg_set_id, 'key')` to fetch event
    properties instead of manually joining the `args` table.
-   **JSON Parsing:** When dealing with JSON text, use standard SQLite JSON
    functions (for example, `json_extract()`) to extract values.
-   **String Matching (Always use GLOB).** Use `GLOB` instead of `LIKE`. `LIKE`
    causes performance bottlenecks and treats underscores (`_`) as wildcards,
    leading to bugs.
  -   **Exact matches:** Use `=`.
  -   **Substring matches:** Use `GLOB` with `*` (for example, `name GLOB
      '*RenderThread*'`).
  -   **Case-insensitive matches:** Use `LOWER(name) GLOB` and make sure the
      search string is fully lowercase (for example, `LOWER(name) GLOB
      '*renderthread*'`). Use this when dealing with inconsistent trace
      capitalization (for example, `WakeLock` versus `wakelock`).
-   **Alias Precision.** Always prefix column names with table or view alias,
    that is: `{alias}.{column_name}`.
-   Query `android_thread_slices_for_all_startups` for app startup requests.
-   Join `counter_track` with `counter` to get values of counter with a specific
    name.
-   When querying for a CPU frequency counter, include the `linux.cpu.frequency`
    module and use the `cpu_frequency_counters` table.

## Common Analysis Patterns

-   **Calculating Time Overlaps & CPU Time:**
  1.  **Primary Method (MANDATORY):** Always search the standard library first
      before writing custom interval logic. For example, to find the exact CPU
      execution time of a slice, do not calculate it manually; instead, search
      the docs and use the `slices.cpu_time` module.
  2.  **Fallback Method (Use ONLY if you have verified no stdlib module or
      `SPAN_JOIN` applies):** If you must calculate custom overlap durations
      between two different sets of time intervals `[start1, end1]` and
      `[start2, end2]`:
  -   **Condition:** The intervals overlap if `start1 < end2` and `start2 <
      end1`.
  -   **Duration:** The overlap duration is calculated as `MIN(end1, end2)
      -   MAX(start1, start2)`.
  -   **Important:** Incomplete Perfetto slices have a duration of -1 (`dur =
      -1`). Always calculate the effective end time using `ts + IIF(dur = -1,
      trace_end() - ts, dur)` before applying this logic.
-   **Window Size:** When looking for events around a specific timestamp, start
    with 100ms as the window size.
-   **Total Duration:** To calculate the total time spent in slices matching a
    specific name pattern (for example, `*{name_pattern}*`), you must sum their
    durations. **Why it's useful**: This helps quantify the total impact of a
    specific function or feature on performance across multiple calls. Here is
    an example query (note the safe handling of incomplete slices):
  ```sql
  SELECT
  count(*) as total_count,
  sum(IIF(slice.dur = -1, trace_end() - slice.ts, slice.dur)) / 1000000.0 as total_dur_ms
  FROM slice
  WHERE slice.name GLOB '*{name_pattern}*';
  ```

## Execution Protocol

You must follow these steps sequentially, mirroring a multi-agent pipeline:

### Step 0: Trace Type Validation (Fail Fast)
Since this skill explicitly targets system traces, you must verify if the trace
is a system trace before proceeding. Run the following probe to confirm:
```sql
SELECT (
    EXISTS(SELECT 1 FROM sched LIMIT 1) OR
    EXISTS(SELECT 1 FROM cpu_counter_track LIMIT 1) OR
    EXISTS(SELECT 1 FROM raw LIMIT 1)
) AS is_system_trace
```

If this returns `0`, stop execution and inform the user in general
language without exposing the internal mechanics.

### Step 1: Dissection and Schema Research

1.  Identify the core question, required data points, and filtering conditions.
2.  **Precedence Rule:** If the user's request contains a SQL query, use it
    **without modification** and skip to Step 2 for validation.
3.  **Mandatory Schema and Module Search:**
  -   **Discovery and Search**: Do not guess schemas or rely on your
      internal knowledge. You must use the guidelines detailed in the
      [Discovering what's in the Trace](#discovering-whats-in-the-trace)
      section above to discover relevant views, tables or modules based on
      your problem domain and high-level intents (for example, 'CPU time',
      'running time', 'overlap', 'jank').
  -   **Why**: Searching solely for exact tables names misses comprehensive,
      pre-computed views built for these analyses.
  -   **Note**: You must verify if a Standard Library module already provides
      the needed abstraction before drafting manual arithmetic or custom
      functions.
  -   **Extract:** Extract only the schema, columns, and the exact `INCLUDE
      PERFETTO MODULE` statements for the required object from the intrinsic
      queries.
  -   **Verify:** Review the columns, types, and descriptions to ensure the
      table matches your needs.
4.  Print the research results before drafting the query:
    *Tables/Views:* `Schema for {name}:` listing columns and types.

### Step 2: Draft and Validate Loop (Max 3 Iterations)

Draft the SQL query in SQLite syntax using **only** the schemas retrieved in
Step 1. After drafting, you must validate against this checklist:

-   [ ] **SQLite Syntax:** Does the query parse successfully without syntax
    errors?
-   [ ] **Idempotency:** Are all object creations safe to re-run? (Did you use
    `CREATE OR REPLACE PERFETTO` and `DROP TABLE IF EXISTS` for virtual tables?)
-   [ ] **Existence:** Were all tables found in the intrinsic queries or the
    core tables exist?
-   [ ] **Intent Check:** Is there a pre-existing standard library table or view
    that will fulfill this intent before instead of writing manual arithmetic?
-   [ ] **Column Accuracy:** Do columns match the retrieved schemas?
-   [ ] **Alias Check:** Are ALL column names prefixed with their table or view
    alias (for example, `alias.column_name`)?
-   [ ] **Module Check:** Are `INCLUDE PERFETTO MODULE` statements included for
    all non-prelude modules? **You must use the exact module names discovered via
    the intrinsic queries (Path A) or your pre-trained knowledge (Path B).**
-   [ ] **Span Join Check:** If using `SPAN_JOIN`, are tables safely
    `PARTITIONED` to prevent overlapping interval crashes? Are intermediate
    tables materialized with `CREATE PERFETTO TABLE`?
-   [ ] **No LIKE Constraint:** Did you map string matches using `GLOB` or `=`
    instead of prohibited `LIKE`?

    **Execution Rules:**
  -   **File Usage** : If you must create a SQL file to execute queries (for
      example, due to query length or escaping issues), you must create them
      in the `/tmp/` directory.
  -   **State:** Database state does not persist across turns. You **cannot**
      share state (like views or tables) across queries in different turns.
      Every query must be standalone and fully self-contained.
  -   **Failure Resilience:** Debug and fix SQL syntax and logic errors when
      query fails. Don't simplify the analytical intent to pass validation.
      For example, if requested to calculate an overlap or intersection, you
      must fix the intersection math. Don't substitute with disjoint queries
      (for example, returning independent total durations) as a workaround.

### Step 3: Final Output

1.  Explicitly return and state the final validated SQL and explain the results
    to the user.
2.  Before finishing your response, delete all temporary SQL files you created
    in `/tmp/` directory.