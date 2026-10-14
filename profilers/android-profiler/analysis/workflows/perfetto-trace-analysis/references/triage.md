Follow these steps in order:

## 1. Verify Data Integrity

Query the `stats` table for CPU and data loss indicators. If dropped events
exist, warn the user that findings may be incomplete and proceed with `[GAP]`
awareness.

## 2. Run High-Level System Metrics

Run metrics to detect systemic issues (thermal throttling, LMKs, binder
contention) affecting system health.
> **Prerequisite:** Verify `trace_processor` is available (see
> `$SKILL_ROOT/references/perfetto/setup.md`) and start a warm background
> session per `$SKILL_ROOT/references/perfetto/sql.md`
> (`trace_processor server unix --name <session> --daemonize [trace_file]`) so
> all triage and candidate queries share one in-memory parse.

Query the standard library views below against `--remote <session>` (or run a
single metrics pass via
`trace_processor [trace_file] --run-metrics [comma_separated_metrics]`):

### Quick Reference for Triaging

Key available metrics for `--run-metrics`: `android_startup`, `android_cpu`,
`android_mem`, `android_lmk`, `android_binder`, `android_surfaceflinger`,
`android_gpu`.

| Symptom / Issue | What to check | Useful Perfetto tables |
|---|---|---|
| **App Startup** | Main thread, class loading, bindApplication | `android.startup.startups` (`android_startups`), `slice` |
| **App Jank** | Main thread, RenderThread | `actual_frame_timeline_slice`, `slice`, `thread_state` |
| **System Jank** | SurfaceFlinger, HWC composition | `actual_frame_timeline_slice`, `slice` |
| **App / System Crash** | `Process crashed` or `tombstoned` | `slice`, `process` |
| **ANR** | Main thread, `system_server` watchdog | `thread_state`, `slice` |
| **Frame Timing Issues** | `DrawFrame` or `doFrame` slices | `slice`, `actual_frame_timeline_slice` |
| **Memory Pressure / LMK** | LMKD kills, RSS growth, kswapd | `counter`, `sched_slice` |

## 3. Define Target and Symptom Window

1. Locate the start (`ts`), duration (`dur`) and end timestamp (`ts + dur`) of the specific issue.
2. **Expand upstream:** Check if the first blocking event was caused by a stall that *started before* the symptom window. Stalls (for example, in binder servers, memory reclamation or kernel locks) often originate hundreds of milliseconds before the user-visible frame drop or latency spike occurs.
   - Find the victim's first non-Running state transition within the symptom window.
   - Follow the waker chain. If the blocking event began significantly before the symptom window, expand the investigation window upstream to include that origin. If it cascades down to an origin in another process (for example, a binder server that stalled `500ms` before the jank), **note** the expanded window and track the upstream stall as a co-candidate in the output.
3. **Output** the triage summary using the format defined below.

## 4. Final Output Format

Output the triage summary in this format:

    ## Trace Metadata
    - **Device Model:** `[String, e.g., Pixel 7 Pro]`
    - **Android Build:** `[String, e.g., TQ2A.230505.002]`

    ## System Vitals Summary
    - **Status:** `[Nominal | Degraded | Critical]`
    - **Flags Raised:** *(Only list systemic issues that are actually detected)*
      - **Metric:** `[String, e.g., thermal_throttling, lmkd, binder_contention]`
      - **Description:** `[e.g., 'Thermal throttling during symptom window']`

    ## Candidate(s)
    *(Provide this block for each identified candidate)*
    - **Issue Classification:** `[String, e.g., Startup, Jank, ANR_Input, Crash]`
    - **Package Name:** `[String]`
    - **Process Name:** `[String]`
    - **Thread Name:** `[String]`
    - **UPID:** `[Integer]`
    - **Target UTID:** `[Integer]` *(The primary thread)*
    - **Render Thread UTID:** `[Integer or N/A]` *(If issue is jank)*
    - **Symptom Window:** `[Start TS] - [End TS] ([Duration ms])`
    - **Expanded Window:** `[Expanded Start TS] - [End TS] ([Expanded Duration ms] or "None")`
    - **Severity Note:** `[String, e.g., '150ms missed frame - worst instance']`