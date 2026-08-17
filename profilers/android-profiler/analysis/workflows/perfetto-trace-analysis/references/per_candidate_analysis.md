# Candidate Investigation Protocol

This is the execution protocol for a deep-dive subagent during trace analysis.

## Context

Execute the protocol below using the candidate details (Trace Paths, Symptom
Window, UTID, UPID, System Vitals) provided in your initial prompt.

## Investigation Protocol

**High-Level Algorithm:**

1. Prerequisites (Step 1)
2. Calculate time distribution and identify state buckets (Step 2)
3. Domain and System hints discovery and selection (Step 3)
4. Run exhaustive investigation for each significant state bucket sequentially
   (Steps 4 and 5), starting with the largest bucket. Output total time
   explained so far after investigating each.
5. Output the result (Step 6)

### Step 1: Prerequisites

- Follow `$SKILL_ROOT/references/perfetto/sql.md` for session-based query execution.
- Read
  `$SKILL_ROOT/analysis/workflows/perfetto-trace-analysis/references/guiding_principles.md`
  to identify best practices, ensure data-driven analysis, and avoid pitfalls.

### Step 2: Calculate the time distribution

1. **Establish a baseline if possible:** For example, if investigating a jank
   candidate, find a _non-janky_ instance of the **same** operation first to
   establish expected behavior and rule out red herrings. _If no healthy
   instance exists in the trace, note this as a `[GAP]` and proceed with the
   next steps_.
2. **Calculate time spent in each state:** Sum the time spent in `Running`,
   `R`, `S` and `D` states. Investigate all substantial buckets. Also look for
   composite bottlenecks (for example, "40% CPU-starved + 35% IO-blocked").
3. **Run checks to rule out red herrings:** For example:
   - A thread in `S` state could be normal behavior -> investigate only if
     the sleep actually overlaps with a pending obligation (for example, a
     pending binder reply).
   - A thread in `Running/Runnable`: When a thread spends high duration in
     `Running/Runnable`, inspect CPU frequency, throttling counters, and core
     migrations before attributing latency to code inefficiency, because
     hardware throttling inflates wall time without increasing instruction
     overhead:
     - Verify first that an identified code path is actually doing
       disproportionate work.
     - Query the trace to find what the thread was doing during this window.
       Query the `slice` table for the longest duration slices during this
       window.
     - Beyond blocking slices, query and identify repetitive micro-operations
       or gaps that collectively exhaust the budget to prevent tunnel vision.
     - Perform a system-wide check to identify if there was CPU throttling or
       core migrations around the symptom window (see Principle 7).
4. **Drill down:** For every significant bucket revealed from the time
   distribution analysis during the symptom window, identify what the thread
   was doing at the transition point. Investigate every significant bucket (for
   example, >= 20% of the time window) to avoid missing real bottlenecks or
   composite issues.
> **Action:** Tag missing data as `[GAP]`. Do not guess.

### Step 3: Domain and Hints Discovery

Even if you find a major bottleneck, continue searching for other bottlenecks
using expert-vetted domain and system hints and techniques. Follow these steps
in order:

- **Domain discovery:** Use your file search tools (for example, `grep_search`)
  to scan frontmatter (`domain:`, `description:`, `keywords:`) in
  `$SKILL_ROOT/analysis/workflows/perfetto-trace-analysis/references/hints/domains/`
  (if it exists) and identify matching domain hint files based on collective
  investigation state (victim, intermediate processes).
- **Subsystem hints:** Use your file search tools to scan frontmatter
  (`subsystem:`, `description:`, `keywords:`) in
  `$SKILL_ROOT/analysis/workflows/perfetto-trace-analysis/references/hints/subsystems/`
  for relevant tracks (for example, keywords for sleep states, memory, IPC).
- **Hint Selection and Application:** Apply all the matched subsystem and
  domain hint files during the Step 4 investigation, and list them under
  `Applied Hints` in your Step 6 output.

### Step 4: Exhaustive Investigation (Do Not Give Up Early)

- **Follow the dependency chain:**
  - If the victim thread was waiting or blocked by another thread, follow the
    chain to the leaf. Find out what it is waiting _for_. Cross process
    boundaries if necessary. Do not conclude without following the entire
    blocker chain (See Principle 4 in `guiding_principles.md`).
  - **Dynamic hint injection:** If your dependency chain leads to a new
    subsystem or process you haven't researched yet, do a single search in
    `$SKILL_ROOT/analysis/workflows/perfetto-trace-analysis/references/hints/`
    for relevant hints before proceeding. Do not fall into infinite loops.
  - Every state bucket investigation should end in one of three states:
    **Terminal root cause**, **Blocked by another thread**, or
    **Partial suspect**.

    > A finding is a "terminal root cause" if:
    >
    > - You can trace it down to a physical bottleneck (thermal throttle, GPU,
    >   storage). **Require Specificity:** Do not conclude with generic labels.
    >   Specify the _what_ clearly.
    > - A specific function or code path is identified as doing disproportionate
    >   work relative to its purpose (for example, synchronous disk IO on main
    >   thread, unnecessary object allocation triggering GC).
    > - A scheduling policy or resource limit is identified as artificially
    >   constraining the thread (for example, background CPU cap, foreground
    >   service restriction).
    > - The bottleneck is identified in a different process/service that the
    >   investigated process cannot control (for example, `system_server` lock
    >   contention, `SurfaceFlinger` throttling).

- **Systemic sweep before concluding:** Discovering an application-layer
  bottleneck (software root cause) does not terminate the investigation of a
  state bucket. Before concluding any state bucket as a terminal root cause,
  check relevant system hints for that state (`CPU`, `IO`, `Memory`, `IPC`)
  and verify whether platform-level confounds - such as CPU scaling, memory
  pressure, thermal throttling, or I/O saturation - simultaneously degraded
  performance **around the symptom window.** Report discovered systemic
  confounds as **co-root causes** or duration modifiers (see Principle 7 in
  `guiding_principles.md`).

**Why:** Software execution duration is not an absolute constant; it is
modulated by platform state (frequency scaling, thermals, memory reclamation).
Always check system-wide confounds before reporting an inefficient code path as
the sole root cause - to avoid concluding inflated software duration as the
sole root cause while missing the underlying kernel or hardware anomaly that
magnified it. Back findings with empirical proof (`[SQL]`) or mark as `[GAP]`
if inconclusive.

### Step 5: Contextualize the Workload

Once the mechanical bottleneck is identified in Step 4, query the trace to
identify the high-level user feature, UI operation, or exact workload that
triggered it. Explain the _why_, not just the _what_. This workload context
ensures that we can provide actionable next steps for the user instead of
leaving them confused.

### Step 6: Output

Output the following investigation result:

```markdown
# Investigation Result

Candidate:
Symptom Window: [Start TS] and [End TS]
Symptom Duration: [Duration]
Budget (Expected Duration): [If applicable]
Applied Hints: [List of matched subsystem and domain hint files used]

## Primary Finding

- **Classification:** [Hardware | Software/Code | System Exhaustion | Scheduling Policy | External Dependency]
- **Details and Reasoning:** `___`
- **Explained Duration:** `___`
- **Evidence SQL or backing data:** `___`
- **Dependency chain:** [Root to Leaf Dependency Chain]
- **Root cause conclusion:** `___`

## Other Findings

- **Status:** [Partial Suspect | Low Confidence Suspicion | Terminal Root Cause]
- _(same fields)_

## Verification Checklist

- All claims tagged (`[SQL]`/`[GAP]`/`[INFERRED]`): [yes/no]
- Total explained time accounts for most of the symptom window: [yes/no]
- Unexplored state buckets: [none/list with reason why]
- Workload contextualized (for example, specific slices/layers involved): [yes/no]
```
