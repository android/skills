# Guiding Principles for Trace Analysis

Whether you are augmented with domain-specific knowledge or doing a standard
workflow analysis, follow these rules.

**Why:** Performance analysis is complex. It is entirely possible to end up
identifying a root cause while the reality is different (for example, a hardware
bottleneck causing a cascade of failures). The principles below keep your
analysis grounded in truth and ensure that you investigate the entire causal
chain to discover the true bottleneck.

1. **Schema Validation via Intrinsic Discovery:**
   Read `$SKILL_ROOT/references/perfetto/sql.md` and query table schemas
   using `LIMIT 0` before drafting queries.
   - **Why:** Trace processor schemas evolve across versions; discovering
   schema directly prevents invalid assumptions and syntax failures.
2. **Empirical Data Grounding:** Support every claim with tool output or
   queried timestamps, slice durations or counter values (`[SQL]`).
   - **Why:** General Android heuristics cannot substitute for ground-truth
     trace metrics. When queries return empty results, broaden search
     constraints using fuzzy matching or wider time windows.
3. **Causation vs. Correlation:** Verify that the blocker's active execution
   overlaps with the victim's wait interval.
   - **Why:** Concurrent anomalies are only causally linked if their
     execution lifetimes intersect. For example, just because thread A was busy
     while thread B was waiting does not necessarily mean thread A caused the
     wait.
4. **Follow Evidence:** Follow dependency chains across thread, process and
   kernel boundaries to the terminal root cause. Keep going as long as
   **each hop meaningfully explains the symptom**.
   - **Why:** Halting blocker traversal prematurely reports intermediate
     symptoms rather than the true origin.
5. **Explicit Uncertainty Reporting:** Categorize unverified execution paths
   as `[GAP]` or partial suspects.
   - **Why:** Transparent reporting of missing data enables engineers to evaluate
     probabilities without false certainty.
6. **Evidence-First Explanation:** Cite concrete retrieved metrics and slice
   timestamps before asserting platform behavioral context.
   - **Why:** Contextual explanations are only reliable when anchored in
     empirical trace observations.
7. **Systemic Confound Sweeps:** Before attributing a bottleneck to
   application software, verify that thermal throttling, CPU capping
   (`cpufreq`), scheduling (`sched_slice`), or LMKD pressure isn't uniformly
   degrading the system. Report such confounds as root cause modifiers.
   > To uncover short-lived anomalies that get mathematically missed by simple
   > averages or aggregate queries, isolate and look around the symptom window
   > and query for individual event spikes or percentiles.
8. **Systematic Step Adherence:** At every step of your investigation, strictly
   follow the defined investigation steps.
   - **Why:** Structured verification prevents analytical blind spots and
     premature conclusions on obvious but non-critical anomalies.
