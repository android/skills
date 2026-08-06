# Guiding Principles for Trace Analysis

Whether you are augmented with domain-specific knowledge or doing a standard
workflow analysis, follow these rules.

**Why:** Performance analysis is complex. It is entirely possible to end up
identifying a root cause while the reality is different (for example, a hardware
bottleneck causing a cascade of failures). The principles below keep your
analysis grounded in truth and ensure that you investigate the entire causal
chain to discover the true bottleneck.

1. **Do not guess schemas:** Read `$SKILL_ROOT/references/perfetto/sql.md` on
   how to query traces, discover the correct schemas, and learn DOs/DON'Ts
   (for example, using `GLOB` over `LIKE`, preferring fuzzy matching).
2. **Data is the only evidence:** Every claim needs tool output or queried data
   behind it (for example, SQL queries). Do not guess what "should" happen.
   General Android knowledge cannot substitute for environment-specific ground
   truth data.

> **When queries return nothing:** Do not conclude. Instead, broaden your
> search constraints (for example, fuzzy matching, wider time windows).

3. **Causation, not correlation:** Just because two anomalies occurred together
   does not mean one caused the other. For example, just because thread A was
   busy while thread B was waiting does not mean thread A caused the wait. Did
   the blocker's active period overlap with the victim's wait period?
4. **Follow evidence, not hop counts:** A causal chain can take multiple hops.
   Keep going as long as **each hop meaningfully explains the symptom**. Cut
   when evidence gets thin — not at an arbitrary depth limit. Real performance
   issues frequently cross subsystem boundaries; stopping too early risks
   reporting a symptom as a root cause.
5. **When you don't know, say so:** If you identify a gap in your investigation
   (for example, missing data, empty results), report the gap. Ranking partial
   suspects by evidence strength is more useful than fabricating a clean root
   cause.
6. **Explain causes only after you have evidence:** Platform context is useful
   (for example, "this is consistent with thermal throttling because ..."), but
   only after you have data to back it up, citing the data as proof (for
   example, specific timestamps or metrics you retrieved).
7. **Check for system-wide issues:** Before attributing a bottleneck to
   application software, verify that thermal throttling, CPU capping
   (`cpufreq`), scheduling (`sched_slice`), or LMKD pressure isn't uniformly
   degrading the system. Report such confounds as root cause modifiers.
   **Note:** Check `MIN`, `MAX`, `AVG` to ensure that anomalies are not missed
   because we just checked aggregates.
8. **Never assume, follow the methodology:** At every step of your
   investigation, strictly follow the defined investigation steps and
   guidelines to prevent blind spots, even when a cause seems obvious.
