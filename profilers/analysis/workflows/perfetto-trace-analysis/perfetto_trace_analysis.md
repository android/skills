---
name: perfetto-trace-analysis
description: >
  Analyzes Perfetto traces to find the root cause of performance issues in
  user or system Android apps (for example, janks, app startup, memory, or
  latency stalls).
keywords:
  - perfetto
  - trace
  - jank
  - startup
  - latency
  - stall
  - thread
  - bottleneck
---

# Perfetto Trace Analysis

Use this workflow to diagnose general Android performance issues: jank, app
startup, latency, stall, or a thread-blocking problem.

Follow these steps in order:

## Step 1: Identify what to investigate first

Work with the user (for example, ask questions, present options) to
understand the following:

- _What is the symptom_: Do they want to investigate frame drops, jank,
  startup issues, app crash, system crash, ANR?
- _Who is the victim:_ Where did they observe the symptom? For example, "a
  frame drop in `com.example.sample`".

> **Input Trace Required:** Confirm that the user has provided a trace file
> path or URL (for example, `.pftrace`, `.perfetto-trace`, or a Perfetto UI
> link). If none is provided, pause and ask the user to provide one before
> proceeding.

> If you do not know the symptom, pause and ask the user for clarification.

> If you know the symptom but not the victim, proceed to Step 2 (Triage) to
> discover candidates. Present the 3-5 most severe instances to the user and
> proceed with the user selection.

> **A/B Trace Comparison:** If the user provides multiple traces (for
> example, a baseline and an issue trace), explicitly clarify which is the
> baseline. You will pass both paths to the subagents in Step 3 so they can
> use the baseline trace to establish expected behavior.

## Step 2: Run a system-wide triage

Spin off a subagent/task and instruct it to follow instructions from
`$SKILL_ROOT/analysis/workflows/perfetto-trace-analysis/references/triage.md`
to run a system-wide triage. (Do not read this file yourself. Pass this path
in the sub-agent prompt so it loads the file and performs the triage steps).

Note the output produced by the subagent to identify the filtered list of
**candidates** that we need to investigate.

> **Note:**
> - If no candidates are found, ask the user for symptom clarification and
>   specific timestamps.
> - If the triage reveals multiple candidates, select the top 2 or 3 most
>   severe, representative candidates and ask the user before proceeding
>   which ones to investigate. The user may choose one or more.

**Expected outcome:** Confirmation on which candidate(s) to investigate
before proceeding.

## Step 3: Investigate each branch in parallel

For **every** candidate (one or more) identified in the previous step, spin
off a task/subagent to investigate these tracks _in parallel_.

Construct a prompt for each candidate containing:
```markdown
Trace path: [path]
Baseline Trace Path: [path, if provided]
Candidate Info: [`utid`, `upid`, thread_name, process_name, `render_thread_utid` (if applicable)]
Symptom Window: [start_ts, end_ts, duration]
System Vitals: [payload from Step 2]
Budget: [expected duration, if applicable]
Execution Protocol: Read
`$SKILL_ROOT/analysis/workflows/perfetto-trace-analysis/references/per_candidate_analysis.md`
and follow its instructions end-to-end.
```

SPAWN the subagent(s) in parallel and await completion of all subagents before
proceeding to Step 4.

## Step 4: Final report and consolidation

Read the output from every spawned subagent and generate a consolidated "Trace
Analysis Report" adhering to these instructions:

1. **Summary and Root Cause:** State the findings in simple, clear language
   and classify root causes (hardware, software/code, scheduling policy,
   system exhaustion, or external dependency).
2. **Dependency Chain:** Map the full path from symptom to root cause.
   Include thread names, `UTID`s, blocking states, and exact timestamps at
   every hop.
3. **Evidence Tags:** Tag every single claim with `[SQL]` (backed by data),
   `[INFERRED]` (logical deduction), or `[GAP]` (partial or missing data),
   and briefly define these tags at the start.
4. **Platform Context:** Explain Android system behaviors _only_ by citing
   specific retrieved slice names, values, and timestamps.
   **What's bad:** "SurfaceFlinger does X around vsync"
   **What's a good explanation:** "SurfaceFlinger's composition slice at
   `ts=142.3ms` ran `4ms` after the vsync signal at `ts=138.1ms`, consistent
   with X".
5. **Partial Suspects:** List all branches that were investigated but did not
   reach a terminal root cause, ranked by evidence strength. Include what
   was found and where verification fell short. A partial suspect can be the
   real contributor - report it even if a terminal root cause was found
   elsewhere.

   > If evidence is evenly split between multiple potential causes, report each
   > suspicion with its supporting data and evidence so that engineers can
   > evaluate probabilities without false certainty. Do not arbitrarily pick
   > a winner.