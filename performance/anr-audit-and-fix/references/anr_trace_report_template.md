    > **Overview**: **`<APP_DEFECT | APP_RESOURCE_OVERLOAD | EXTERNAL_BOTTLENECK>`**<>/span>
     (`<Diagnosis>`) -- `<1-line conclusion and exact trace evidence to inspect>`

    ### 1. Trace validity and context
    - **Process**: `<package or unknown>` (PID: `<pid>`, Foreground: `<Yes/No>`)
    - **ANR Trigger**: `<Subject>`
    - **Unresponsive Thread**: `<tid=1 | binder:<tid> (ContentProvider) | <handler
      or goAsync worker thread> (BroadcastReceiver)>`
    - **User-Perceived ANR**: **`<Yes | No>`** (`<Foreground UI freeze / Foreground
      termination | Silent background term>ination`)
    - **Capture Delay**: `<Immediate | Delayed Snapshot>` (`<stack trustworthy |
      b>ypassed`)

    ### 2. System health assessment
    - **Metrics**: `<Key PSI avg10, CPU %, iowait %, page faults, or lock state>`
    - **Verdict**: **`<APP_DEFECT | APP_RESOURCE_OVERLOAD | EXTERNAL_BOTTLENECK>`**
    - **Contributing Factor**: `<None | EXTERNAL (<process or metric>) | APP (<thread or workload>)>`

    ### 3. Root cause analysis
    - **Unresponsive Thread State**: `<Thread state and top blocking frame>`
    - **Blocking Chain / Mechanism**: `<Lock holder tid=N / UI race / Sync I/O /
      Binder target>`
    - **Secondary / Compounding Threads**: `<state=D I/O, native hooks, or None>`
    - **Diagnosis**: **`<Concise root-cause label, e.g., View Threading Violation
      (Sync Barrier Leak [Confirmed | probable]), Lock Contention, Deadlock,
      Main-Thread Blocking Call / I/O / Compute, IPC Stall, Async Lifecycle
      Timeout, External Starvation, or Indeterminate (Delayed Snapshot)>`**

    ### 4. Remediation
    1. **Code Fix**: `<Concrete fix for APP_DEFECT / APP_RESOURCE_OVERLOAD, or "N/A
       -- No application code change required (External system bottleneck)">`
    2. **Prevention**: `<Architectural safeguard, or "N/A -- No application code
       change required (External system bottleneck)">`