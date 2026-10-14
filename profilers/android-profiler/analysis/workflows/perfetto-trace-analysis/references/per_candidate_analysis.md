Execute the protocol below using the candidate details (Trace Paths, Symptom
Window, UTID, System Vitals) provided in your initial prompt.

## Guiding Principles for Trace Analysis

Follow these principles to identify root causes, ensure data-driven analysis,
and avoid pitfalls while discovering the real bottleneck.

1. **Schema Validation via Intrinsic Discovery:** Read
   `$SKILL_ROOT/references/perfetto/sql.md` and query table schemas using
   `LIMIT 0` before drafting queries.

   - **Why:** Trace processor schemas evolve across versions; discovering schema directly prevents invalid assumptions and syntax failures.
2. **Empirical Data Grounding:** Support every claim with queried timestamps,
   slice durations or counter values (`[SQL]`).

   - **Why:** General Android heuristics cannot substitute for ground-truth trace metrics. When queries return empty results, broaden search constraints using fuzzy matching or wider time windows.
3. **Causation vs. Correlation:** Verify that the blocker's active execution
   overlaps with the victim's wait interval.

   - **Why:** Concurrent anomalies are only causally linked if their execution lifetimes intersect. For example, just because thread A was busy while thread B was waiting does not necessarily mean thread A caused the wait.
4. **Follow Evidence:** Follow dependency chains across thread, process and
   kernel boundaries as long as **each hop meaningfully explains the symptom**.

   - **Why:** Halting blocker traversal prematurely reports intermediate symptoms rather than the true origin.
5. **Explicit Uncertainty Reporting:** Categorize unverified execution paths as
   `[GAP]` or partial suspects.

   - **Why:** Transparent reporting of missing data enables engineers to evaluate probabilities without false certainty.
6. **Evidence-First Explanation:** Cite concrete retrieved metrics and slice
   timestamps before asserting platform behavioral context.

   - **Why:** Contextual explanations are only reliable when anchored in empirical trace observations.
7. **Systemic Confound Sweeps:** Before attributing a bottleneck to application
   software, verify that thermal throttling, CPU capping (`cpufreq`),
   scheduling (`sched_slice`), or LMKD pressure isn't uniformly degrading the
   system. Report such confounds as root cause modifiers.

   - **Why:** Software execution duration is not an absolute constant; it is modulated by the platform and hardware state. Hardware throttling can artificially inflate wall-clock time without increasing the instruction overhead, which can make healthy code look like an inefficient bottleneck.

   > To uncover short-lived anomalies that get mathematically missed by simple
   > averages or aggregate queries, isolate and look around the symptom window
   > and query for individual event spikes or percentiles.

## Investigation Steps

### Step 1: Setup and Baseline Verification

1. Follow `$SKILL_ROOT/references/perfetto/sql.md` for session-based query execution.
2. **Establish a Baseline:**
   - **A/B Baseline:** If a `Baseline Trace Path` was provided, query that trace first to establish expected slicing patterns, thread states, and execution budgets.
   - **In-Trace Baseline:** If no separate baseline was provided, locate a healthy, non-janky instance of the same operation earlier in the current trace.
   - *If no baseline is available anywhere, tag as `[GAP]` and proceed.*

### Step 2: Calculate Time Distribution and State Buckets

1. **Sum time spent across thread states:** Calculate the exact duration spent in `Running`, `R`, `S` and `D` states within the symptom window.
2. **Investigate significant buckets:** Target all significant state buckets (for example, accounting for \>= 20% of the window duration). Investigate the largest bucket first.
3. **Look for composite bottlenecks:** Identify if there are any composite bottlenecks (for example, "40% CPU-starved + 35% IO-blocked").
4. **Rule out false positives / red herrings:**
   - **Sleeping (`S`):** Benign unless the sleep overlaps with an active pending obligation (for example, a pending binder reply).
   - **Running/Runnable (`R`):** Inspect `cpu_frequency`, throttling counters, scheduling latency, and core migrations before attributing latency solely to code inefficiency (see Guiding Principle 7).
     - Query the trace and `slice` table for both the longest duration slices and repetitive micro-operations or gaps to prevent tunnel vision.
5. **Drill-down at transitions:** For each significant bucket, query what the thread was executing at state entry and exit points. Tag untraced or ambiguous segments as `[GAP]`. Do not guess.

### Step 3: Domain and Hints Discovery

1. Perform a recursive search on the frontmatter (`category:`, `description:`, `keywords:`) of files in `$SKILL_ROOT/analysis/workflows/perfetto-trace-analysis/references/hints/` to identify hint files matching the collective investigation state (for example, observed victim, affected subsystems, and intermediate processes).
2. Read and apply all *matched* hint files during the Step 4 investigation - these contain expert-vetted diagnostics and guidelines essential for uncovering non-obvious bottlenecks. List every applied hint file under `Applied Hints` in your Step 6 output.

### Step 4: Exhaustive Root Cause Traversal

1. **Follow the dependency chain to the leaf:** If the victim thread was
   blocked or waiting on another thread or process, follow the waker and server
   chain across process boundaries until reaching a leaf. Do not stop at
   intermediate wakers (see Guiding Principle 4).

   - **Dynamic hint injection:** If traversal leads to a new subsystem or process, perform a targeted frontmatter search in `$SKILL_ROOT/analysis/workflows/perfetto-trace-analysis/references/hints/` for any new hints that may apply.
   - **State bucket exit states:** Every state bucket investigation should
     conclude in one of three states: *Terminal Root Cause* , *Blocked by
     Another Thread* or *Partial Suspect*.

     **Decision Matrix per State Bucket:**

     | Outcome / Finding | Action | Criteria \& Examples |
     |---|---|---|
     | **Blocked by Another Thread** | **Continue Traversal** | Follow waker/dependency chain across thread and process boundaries to find what the blocker was waiting for. |
     | **Terminal: Physical Bottleneck** | **Stop and Classify** | Thermal throttle, GPU saturation, storage or I/O bottleneck. |
     | **Terminal: Software Inefficiency** | **Stop and Classify** | Code doing disproportionate work (for example, synchronous disk I/O on main thread, allocation churn). |
     | **Terminal: Scheduling / Platform** | **Stop and Classify** | Background CPU quota, cgroup caps, foreground service restriction. |
     | **Terminal: External Service Stall** | **Stop and Classify** | Platform contention with no further hops (for example, `system_server` lock, `SurfaceFlinger` throttling). |
     | **Partial Suspect** | **Stop and Tag `[GAP]`** | Path unverified due to data gaps. |

2. **Perform a systemic sweep before concluding:** Discovering an application-
   layer code inefficiency does not terminate the investigation. Apply Guiding
   Principle 7 to verify whether platform-level confounds simultaneously
   degraded performance *around* the expanded symptom window. Report discovered
   confounds as co-root causes or duration modifiers.

### Step 5: Contextualize the Workload

Once the mechanical bottleneck is identified, query the trace to identify the
high-level user feature, UI operation, or exact workload that triggered it.
Explain the *why* , not just the *what*, to ensure recommendations are clear and
actionable for engineers.

### Step 6: Output Format

Output the investigation result adhering strictly to this schema:

    # Investigation Result

    - **Candidate:** [Package / Thread / Classification]
    - **Symptom Window:** [Start TS] - [End TS] ([Duration ms])
    - **Budget (Expected Duration):** [If known / applicable, else "N/A"]
    - **Applied Hints:** [List of matched hint files used]

    ## Primary Finding

    - **Classification:** [Hardware | Software/Code | System Exhaustion | Scheduling Policy | External Dependency]
    - **Details and Reasoning:** `___`
    - **Explained Duration:** `[X ms (Y% of window)]`
    - **Evidence SQL or Backing Data:** `___`
    - **Dependency Chain:** `[Root to Leaf Dependency Chain with UTIDs, UPIDs, and Timestamps]`
    - **Root Cause Conclusion:** `___`

    ## Other Findings / Partial Suspects

    - **Status:** [Partial Suspect | Low Confidence Suspicion | Terminal Root Cause]
    - *(same fields as Primary Finding)*

    ## Verification Checklist

    - [ ] All claims tagged (`[SQL]` / `[GAP]` / `[INFERRED]`)
    - [ ] Total explained duration accounts for the majority of the symptom window
    - [ ] Unexplored state buckets: [None / list with reasons]
    - [ ] Workload contextualized (specific UI actions/slices identified)