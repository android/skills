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

## - bottleneck

Use this workflow to diagnose general Android performance issues: jank, app
startup delays, latency spikes, or thread stalls.

Follow these steps in order:

## Step 1: Identify Symptom and Target

Clarify the following with the user (ask questions, present options):

- *What is the symptom*: Do they want to investigate frame drops, jank, startup latency, ANR, app crash, system crash, or memory pressure?
- *Who is the victim:* Where did they observe the symptom? For example, "a frame drop in `com.example.sample`".
- *Baseline trace:* If multiple trace files are provided (A/B trace comparison), identify which is the baseline. You will pass both to the sub-agents in Step 3, which use the baseline trace to establish expected behavior.

> **Input Trace Required:** Confirm that the user has provided a trace file path
> or URL (for example, `.pftrace`, `.perfetto-trace`, or a Perfetto UI link). If
> none is provided, pause and ask the user to provide one before proceeding.
> **Rules:**
>
> - If the symptom is not known, pause and work with the user for clarification.
> - If the symptom is known but the victim/timestamp is unknown, proceed to Step 2 (Triage) to discover candidates.

## Step 2: System-Wide Triage

Launch a sub-agent to run triage using the following prompt template:

    Trace Path: [path of the trace with the issue]
    Goal: Identify all candidate bottlenecks for [symptom] in
    [package or entire system]
    Execution: Read
    `$SKILL_ROOT/analysis/workflows/perfetto-trace-analysis/references/triage.md`
    and follow its instructions.

From the triage output:

- If no candidates are found, ask the user for specific timestamps or symptoms.
- If multiple candidates exist, present the top 3 most severe instances to the user and confirm which one(s) to investigate. The user may select one or more.

## Step 3: Investigate Candidates in Parallel

For every confirmed candidate, launch dedicated sub-agents in parallel with this
prompt:

    Trace path: [path]
    Baseline Trace Path: [path, if provided, else "None"]
    Candidate Info: [package_name, `utid`, `upid`, render_thread_utid,
    process_name, thread_name]
    Symptom Window: [start_ts, end_ts, duration]
    Expanded Symptom Window: [expanded_start_ts to end_ts, if applicable]
    System Vitals: [payload from Step 2]
    Execution Protocol: Read
    `$SKILL_ROOT/analysis/workflows/perfetto-trace-analysis/references/per_candidate_analysis.md`
    and follow it end-to-end.

AWAIT completion of all agents before proceeding to Step 4.

## Step 4: Final Consolidation and Report

Consolidate the sub-agent findings into a final report containing:

1. **Summary and Classification:** Classify root cause (hardware, software/code, scheduling policy, system exhaustion, external dependency).
2. **Dependency Chain:** Full causal path from symptom to root cause with thread names, `UTID/UPID`s, blocking states, and timestamps.
3. **Evidence Tags:** Tag every statement with `[SQL]` (backed by trace data), `[INFERRED]` (logical deduction), or `[GAP]` (untraced or missing data).
4. **Platform Context:** Cite observed slice names, values and timestamps for explaining the platform behavior. **What's bad:** "SurfaceFlinger does X around vsync" **What's a good explanation:** "`SurfaceFlinger`'s composition slice at `ts=142.3ms` ran `4ms` after the vsync signal at `ts=138.1ms`, consistent with X".
5. **Partial Suspects:** Secondary branches or unverified paths that were
   investigated but did not reach a terminal root cause, ranked by evidence
   strength. Report partial suspects even if a terminal root cause was found.

   > If evidence is evenly split between multiple potential causes, report each
   > suspicion with its supporting data so that engineers can evaluate
   > probabilities without false certainty. Do not arbitrarily pick a winner.
6. **Recommended Next Steps:** Scan the frontmatter of workflows in
   `$SKILL_ROOT/analysis/workflows/` and recommend specialized matching
   workflows, if applicable, based on the findings.