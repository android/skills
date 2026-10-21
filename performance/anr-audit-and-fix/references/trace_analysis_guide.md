Diagnose ANR traces using the following rules and format the output with
[references/anr_trace_report_template.md](https://developer.android.com/agents/skills/performance/anr-audit-and-fix/references/anr_trace_report_template).

*** ** * ** ***

## 1. Pre-flight context checks

- **Process match** : confirm that the dumped process (`--- pid N at ...` / `Cmd line:` header) matches the ANR `Subject`. `Input dispatching timed out` subjects often carry no package or PID; record `unknown` rather than aborting.
- **Capture delay (`Immediate` vs. `Delayed Snapshot`)** :
  - `Immediate`: dump captured at the ANR timestamp (the unresponsive thread's stack reflects the freeze).
  - `Delayed Snapshot (unresponsive thread stack bypassed)`: significant lag between the ANR and dump timestamps, OR `Debug Store` records a slow main-thread `LooperMsg` that finished right before the dump while the unresponsive thread is now idle in `nativePollOnce`. **Don't dismiss as
    bug-free or misclassify a completed slow transaction as a Sync Barrier
    Leak.**
  - `Debug Store` exists only on Android 15+ (absence on older devices is not a signal). Its `LooperMsg` entry logs slow (over 2 seconds) `ActivityThread$H` messages (such as Activity lifecycle transactions or `BIND_APPLICATION`), whereas receiver and service start or bind callbacks use separate `Debug
    Store` tags and generic `Handler.post` runnables are not logged. A completed `LooperMsg` therefore directly identifies a slow main-thread callback: report `Indeterminate (Delayed Snapshot)` and attribute it using Section 2 (`APP_DEFECT` unless external starvation holds).
- **User-perceived ANR** :
  - **`Yes` (`Foreground UI freeze / Foreground termination`)** : any `Input
    dispatching timed out` ANR, OR any component, service, job, or startup ANR when **`Foreground: Yes`** (killing a foreground process terminates the user's visible app).
  - **`No` (`Silent background termination`)** : background component or watchdog ANRs when **`Foreground: No`**.

*** ** * ** ***

## 2. Attribution verdict

Always commit to a single primary **`Verdict`** (never output `"Shared"` or
`"Mixed"`). Record any secondary environmental or app contention in
**`Contributing Factor`** (`None | EXTERNAL (<process or metric>) | APP
(<thread or workload>)`).

The verdict's job is to tell the developer whether there is something in the
app to fix. Evaluate the verdicts **in the following order** and stop at the
first one whose evidence is present; record the lower-ranked explanation, if
any, as a one-line `Contributing Factor`. Precedence never promotes a verdict
without real app-side evidence.

1. **`APP_DEFECT`** (application code, lock, lifecycle contract, or threading bug). Applies when the unresponsive thread (`tid=1`, the `binder:` thread for ContentProvider ANRs, or the custom-`Handler` or `goAsync()` worker thread for broadcast receivers) has a concrete app-side blocking dependency or contract violation, regardless of background system load:
   - Direct I/O or heavy computation on the unresponsive thread, a synchronous wait on in-process or cross-process work (such as `Future.get()`, a latch, synchronous SDK initialization, an avoidable synchronous Binder call, a lock, monitor, or futex whose holder is **actively executing** app work on the CPU, or a deadlock where the holder waits on a resource the unresponsive thread owns), or a View threading violation that leaked a sync barrier (Section 3). This is `APP_DEFECT` even when the waited-on work is slow because of app-driven CPU, GC, or memory pressure; the primary fix is removing the blocking dependency and the overload goes in `Contributing Factor: APP (<thread or workload>)`. *(Distinguish `Blocked`
     on `<0x...>` from normal `Native` idle polling in
     `MessageQueue.nativePollOnce`.)*
   - **Guard** : a lock or GC wait whose target thread (the lock holder or GC daemon, when the app's own heap is not exhausted) is itself starved by external load (waiting on the run queue or in kernel I/O `state=D`) is not app-side evidence **only if the device is actually saturated** ; fall through to the following verdicts. If total CPU has headroom and the holder still can't get scheduled because the app runs it at a much lower priority than the waiting unresponsive thread, that is a priority inversion the app created: stay with `APP_DEFECT`.
2. **`APP_RESOURCE_OVERLOAD`** (system resource contention driven by the target app). Applies when device CPU, memory, or I/O is saturated (or thrashing on major page faults) AND the target app is a dominant resource driver (for example, a top CPU consumer alongside heavy major page faults, GC thrashing, or excessive thread creation in the hundreds). If the target app has a low share of total CPU while `kswapd0` or other processes dominate---even if cold-start code paging produces several hundred major faults or standard framework/SDK thread pools---fall through to `EXTERNAL_BOTTLENECK`. Other processes being loaded when the target app is a dominant driver does not downgrade this verdict; note them in `Contributing Factor: EXTERNAL (<process or metric>)`.
3. **`EXTERNAL_BOTTLENECK`** (`External Starvation`; target app is a passive victim). Applies only when neither verdict 1 nor verdict 2 holds: other processes or the kernel saturate CPU, memory, or I/O while the target app consumes negligible or low CPU and has no blocking app-side dependency (including cold-start class, library, or provider initialization stalled in disk wait or starved under system I/O and memory pressure, or app threads suspended in kernel cgroup freezer channels).
   - **Strict no-fix rule** : when Verdict is `EXTERNAL_BOTTLENECK`, **never
     propose app code fixes or defensive workarounds** . Fill both Section 4 fields with `N/A -- No application code change required (External system
     bottleneck)`.

*** ** * ** ***

## 3. Root cause and special forensic patterns

- **`Sync Barrier Leak` vs. `Indeterminate` vs. `Async Lifecycle Timeout`** (when `tid=1` is in `nativePollOnce`, `dispatchVsync`, or rendering VSYNC frames):
  - *Exclusions first*:
  - All threads suspended in kernel cgroup freezer channels -\> **`EXTERNAL_BOTTLENECK`** (`External Starvation`).
  - `Debug Store` shows a completed slow `LooperMsg` covering the ANR window, or dump capture was significantly delayed -\> **`Indeterminate (Delayed
    Snapshot)`** (with the Verdict from Section 2 evidence, or `APP_DEFECT` when no external or resource-overload evidence is present).
  - Uncompleted async component contract (`BroadcastReceiver.goAsync()` without `PendingResult.finish()`, `JobService` timeout, or `SHORT_SERVICE` exceeding its deadline without `Service.stopSelf()`) -\> **`Async Lifecycle Timeout`** . Only claim this when the trace shows it (for example, `GoAsync` or job-stop records in `Debug Store`, or a `SHORT_SERVICE` timeout named in the `Subject:` line); **never infer an
    async contract leak from an empty `Debug Store`**.
  - *Mechanism* : `ViewRootImpl.requestLayout()` calls `checkThread()`, but invalidation paths (`invalidate()` -\> `onDescendantInvalidated()` -\> `scheduleTraversals()`) don't, allowing a background thread to enter `scheduleTraversals()` concurrently with the main thread. Both call `mHandler.getLooper().getQueue().postSyncBarrier()` and overwrite the unsynchronized `mTraversalBarrier` token; `doTraversal()` removes only the latest token, leaking an orphaned sync barrier (`Message.target == null`) in `MessageQueue`. Synchronous messages starve behind the barrier while asynchronous VSYNC frames continue to run. Android 17 fixed this platform race; Android 16 and older remain vulnerable, so check the trace's Android version when weighing this pattern.
  - *ANR subject signatures* : background component timeouts (`Broadcast of
    Intent`, `executing service`) and `Input dispatching timed out (Application
    does not have a focused window)`, because the window-focus request is also a synchronous main-thread message that starves behind the leaked barrier. A no-focused-window subject alone is not evidence of a sync barrier (for example, if companion code or window state explicitly sets `FLAG_NOT_FOCUSABLE`); apply the criteria that follow.
  - **`View Threading Violation (Sync Barrier Leak [Confirmed])`** : background thread caught mutating UI views/`ViewRootImpl` in the dump, OR `tid=1` is actively rendering a VSYNC frame (`doFrame -> doTraversal`) while a synchronous message (`SchRcv` in `Debug Store`) starves alongside a spinning background worker.
  - **`View Threading Violation (Sync Barrier Leak [probable])`** : when the offending background thread has already exited, **don't** diagnose by elimination alone. Require **corroborating VSYNC rendering evidence** (such as `tid=1` caught in `dispatchVsync`, or elevated context switches and CPU ticks on `main` and `RenderThread` showing asynchronous rendering continued while the synchronous message starved), immediate capture, `tid=1` in `nativePollOnce` (idle or dispatching VSYNC), an empty or absent `Debug
    Store`, and no lock, IPC, or `state=D` I/O wait on `tid=1`.
  - *Remediation and detection* : confine all View mutations to the main thread at the **root scope** (launching on `Dispatchers.Main` or `lifecycleScope` and wrapping only slow I/O in `withContext(Dispatchers.IO)`). When the offending call site isn't on the stack, audit background callbacks and pinpoint it using Android 17's compatibility check (`adb shell am compat
    enable ENFORCE_THREAD_CHECKS_ON_VIEW_ROOT_IMPL_APIS <package>`, debuggable builds only), Android 17's `View#registerCalledFromWrongThreadListener` (requires `compileSdk 37`; guard calls with `Build.VERSION.SDK_INT >= 37`) for production telemetry, the Robolectric unit-test flag `-Drobolectric.enforceViewMethodsCalledOnMainThread=true`, or crash logs for `IllegalStateException` at `MessageQueue.removeSyncBarrier`.
- **Blocked or missing unresponsive thread** :
  - If the unresponsive thread is `Blocked` on a lock, identify the holder thread and diagnose what it is doing (app work, GC, I/O, waiting on a lock the unresponsive thread owns, or a lower-priority thread being starved) rather than the waiting thread's own frames; the remediation follows from the holder's workload.
  - If `tid=1` is absent from `DALVIK THREADS`, locate the main thread among the `(not attached)` entries in `DALVIK THREADS` (`sysTid == PID`) before concluding anything; its frames (such as VM teardown waiting on other threads) reveal the actual stall.
- **Stackless traces (stack-dump timeout or missing thread stacks)** :
  - Distinguish using kernel `Waiting Channels` (`sysTid=<tid> <wchan>` lines) and `/proc` stats:
  - **External starvation (`EXTERNAL_BOTTLENECK`)** : severe system I/O or memory pressure with the main thread (`sysTid == PID`) in kernel disk or file system wait and negligible app CPU.
  - **Internal futex contention (`APP_DEFECT` / `Lock Contention`)** : the main thread and a convoy of app worker threads parked in `futex_*` wait channels while the app actively consumes CPU.
  - **App-driven overload (`APP_RESOURCE_OVERLOAD`)**: the app dominates CPU or major page faults without an internal futex convoy.
  - **ART `Signal Catcher` vs. custom `SIGQUIT` interception** : ART's built-in `Signal Catcher` thread normally waits in `sigtimedwait` / `do_sigtimedwait`. **Never** flag it alone; only flag custom `SIGQUIT` interception when an additional app thread also appears in a signal-wait channel.
- **Secondary or compounding threads (`Secondary / Compounding Threads`)** :
  - Scan worker threads and report concurrent aggravators (or `None`): kernel disk sync (`state=D` in `fsync`/`fdatasync`), third-party JNI or C/C++ hooks in ART/GC/signal paths, custom signal-wait threads, unbounded thread pools, or thread priority abuse.