# System-Wide Triage

Follow these steps in order:

1. **Verify data integrity:** Query `stats` for CPU and data loss indicators.
   If found, warn the user that findings may be incomplete and proceed with
   `[GAP]` awareness.

2. **Run metrics:** Systemic issues (like thermal throttling, LMKs, binder
   contention) affect all threads.

   > **Prerequisite:** Verify that `trace_processor` is available in your
   > environment before executing commands (see
   > `$SKILL_ROOT/references/perfetto/setup.md`).

   Run the following query based on the user's intent to understand the
   high-level picture:

   ```sh
   trace_processor [trace_file] --run-metrics [comma_separated_metrics]
   ```

3. **Define target and symptom window:** Find the specific issue's start
   (`ts`), duration (`dur`) and end timestamp (`ts + dur`). Verify the trace
   actually covers the full window.

   > After identifying the specific instance to investigate and the symptom
   > window (for example, janky frame at `ts = 5.1s`), check whether the first
   > blocking event was caused by a stall that _started before_ the symptom
   > window. Expanding the window upstream is essential because stalls (for
   > example, in binder servers, memory reclamation or kernel locks) often
   > originate hundreds of milliseconds before the user-visible frame drop or
   > latency spike occurs.
   >
   > - Find the victim's first non-Running state transition within the symptom
   >   window.
   > - Follow the waker chain for that transition. Check waker timestamps -
   >   if the blocking event began significantly before the symptom window,
   >   expand the investigation window upstream to include that origin.
   > - If it cascades down to an origin in another process (for example, a
   >   binder server that stalled `500ms` before the jank), **note** the
   >   expanded window and include the upstream stall as a co-candidate to
   >   investigate further.

4. **Output** the triage summary using the format defined below.

## Quick reference for triaging

Key available metrics for `--run-metrics`:
`android_startup`, `android_cpu`, `android_mem`, `android_lmk`,
`android_binder`, `android_surfaceflinger`, `android_gpu`

Quick lookup table based on symptom:

| Symptom/Issue    | What to check                         | Useful Perfetto tables                          |
| :--------------- | :------------------------------------ | :---------------------------------------------- |
| App startup      | Main thread                           | `android.startup.startups` (`android_startups`) |
| App jank         | Main, render threads                  | `actual_frame_timeline_slice`                   |
| System jank      | SurfaceFlinger                        | `actual_frame_timeline_slice`                   |
| App/system crash | `Process crashed` or `tombstoned`     | `slice`                                         |
| ANR              | Main thread, `system_server` watchdog | `thread_state`, `slice`                         |
| Frame issues     | `DrawFrame` or `doFrame` slices       | `slice`                                         |

## Final Output Format

Output the triage summary in the following format:

```markdown
## Trace Metadata

- **Device Model:** `[String, e.g., Pixel 7 Pro]`
- **Android Build:** `[String, e.g., TQ2A.230505.002]`

## System Vitals Summary

- **Status:** `[Nominal | Degraded | Critical]`
- **Flags Raised:** _(Only list systemic issues that are actually detected)_
  - **Metric:** `[String, e.g., thermal_throttling, lmkd, binder_contention]`
  - **Description:** `[e.g., 'Thermal throttling during symptom window']`

## Candidate

- **Issue Classification:** `[String, e.g., Startup, Jank, ANR_Input, Crash]`
- **Package Name:** `[String]`
- **Process Name:** `[String]`
- **Thread Name:** `[String]`
- **UPID:** `[Integer]`
- **Target UTID:** `[Integer]` _(The primary thread)_
- **Render Thread UTID:** `[Integer or null]` _(If issue is jank)_
- **Start TS:** `[Integer Timestamp]`
- **End TS:** `[Integer Timestamp]`
- **Duration (ms):** `[Float]`
- **Severity Note:** `[String, e.g., '150ms missed frame - worst instance']`
```
