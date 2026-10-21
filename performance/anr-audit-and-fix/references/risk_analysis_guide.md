Goal: decide whether code **in the file under review** stalls the main thread
(or a Binder thread that a caller waits on synchronously) long enough to cause
an ANR, and point to the exact lines. Work through the steps in order. A
candidate becomes a finding only if it passes Steps 2, 3, and 5. The one
exception is Step 4 (View threading): its proof is off-main execution on an
attached View, followed by Step 5.

## Step 1: List candidates in the file

- **Blocking primitives and locks** : `runBlocking`, blocking futures, latches, monitors, `Thread.sleep`, RxJava `blocking*` operators, or locks contended with background work.
- **Synchronous I/O or heavy compute** : network, disk, file, or database reads and writes (`SharedPreferences.Editor.commit()`, `ContentResolver.query`, non-suspend DAO calls), bitmap decoding, JSON or XML payload parsing, `MediaPlayer.prepare()`, or unbounded loops.
- **Synchronous IPC or slow component lifecycle** : cross-process Binder or `ContentProvider` calls, or heavy work inside `Application`, `Activity`, `Service`, `BroadcastReceiver`, or `ContentProvider` lifecycle callbacks.
- **Off-main mutation of attached Views** (Step 4).

## Step 2: Prove the executing thread (mandatory)

Establish which thread actually executes each candidate rather than assuming:

- **Inspect the enclosing scope** : check coroutine dispatchers (`lifecycleScope` and `viewModelScope` default to Main), reactive schedulers, executors or handlers, and Android component callbacks (`ContentProvider` queries run on a Binder thread cross-process, or the caller's thread in-process).
- **Trace callers when the thread depends on the caller**: locate callers in the project (1 to 2 hops) to verify the dispatching thread, and state the verified call chain.
- **Listeners and callbacks**: verify which thread or executor dispatches the callback before assuming it runs on a worker thread.
- **Dismiss unproven candidates**: if you can't show that the candidate runs on the main thread or a synchronously awaited Binder thread (or off the main thread for Step 4 candidates), list it under dismissed as "thread not established".

## Step 3: Prove the call blocks

- **Seeded in-memory reactive reads return immediately** : `blockingFirst()` or `getValue()` on a seeded `BehaviorRelay` or `BehaviorSubject`, `StateFlow.value`, or `first()` on a `StateFlow` / populated `SharedFlow` (`replay >= 1`) return in-memory state immediately. Check how the source is initialized before flagging.
- **Suspension isn't blocking** : suspend functions, `delay`, and coroutine `Mutex`/`Semaphore` suspend the coroutine without blocking the underlying thread.
- **`runBlocking` blocks only its calling thread** : `runBlocking` called on a background thread (even with `Dispatchers.Main` inside) doesn't block the main thread unless the main thread is waiting on that worker.
- **Short bounded waits aren't findings**: a single sleep or wait of roughly a frame to a few frames (tens of milliseconds to about a hundred), not in a loop and not on a hot path, causes minor frame drops, not an ANR. Don't report it as a finding; mention it under dismissed candidates at most.
- **Latent hazard** : a blocking primitive on the main thread whose inner work completes in memory without I/O or suspension is a `LOW` finding. Explain why it doesn't block today and what change would make it block.

## Step 4: View threading violations (sync barrier leak)

Treat off-main-thread UI access on an **attached** View as a **Sync Barrier
Leak ANR** risk, not merely a `CalledFromWrongThreadException` crash risk.

- **Mechanism** : `ViewRootImpl.requestLayout()` calls `checkThread()`, but invalidation paths (`invalidate()` -\> `onDescendantInvalidated()` -\> `scheduleTraversals()`) don't, allowing a background thread to enter `scheduleTraversals()` concurrently with the main thread. Both call `mHandler.getLooper().getQueue().postSyncBarrier()` and overwrite the unsynchronized `mTraversalBarrier` token; `doTraversal()` removes only one token, leaking an orphaned synchronization barrier in `MessageQueue` (freezing synchronous main-thread messages between VSYNC frames or throwing `IllegalStateException` at `MessageQueue.removeSyncBarrier`). Android 17 fixed this platform race; Android 16 and older remain vulnerable, so apps shipping to those versions must still remove the violation. Creating or configuring a *detached* View off-main doesn't trigger this race.
- **What to check** : View or adapter mutations inside background coroutine scopes (`Dispatchers.Default`/`IO`), background Rx schedulers, executors or threads, or asynchronous callbacks invoked on worker threads (such as network, image-loading, media, sensor, or Binder listeners).
- **Detection** : Android 17's `View#registerCalledFromWrongThreadListener` (requires `compileSdk 37`; guard calls with `Build.VERSION.SDK_INT >= 37`), `adb shell am compat enable ENFORCE_THREAD_CHECKS_ON_VIEW_ROOT_IMPL_APIS
  <package>` (debuggable builds), or the Robolectric unit-test flag `-Drobolectric.enforceViewMethodsCalledOnMainThread=true`.
- **Remediation** : fix the threading context at the **root scope** :
  - *Coroutines* : start UI flows on `Dispatchers.Main` (`lifecycleScope` or `viewModelScope`) and wrap only slow I/O or compute work in `withContext(Dispatchers.IO)` or `Dispatchers.Default`.
  - *Threads, executors, and callbacks* : keep in-memory UI reads and mutations on the main thread before submitting background work, or configure the upstream stream, callback executor, or target API to deliver results on the main thread (`AndroidSchedulers.mainThread()`, main-thread UI targets, or main executor).

## Step 5: Attribute and grade

- **Attribute to the file that owns the defect**: report a finding only if the blocking statement or off-main View mutation originates in the file under review (or in a concrete dependency that unconditionally blocks when called on the main thread from this file). If this file invokes an abstract callback or listener interface and an external caller or host class chooses to block inside its callback implementation, attribute the defect to that external file and dismiss it here as "owned by caller or host file".
- **Severity** :
  - `HIGH`: main-thread I/O or long waits on primary paths (startup, lifecycle, input), or off-main mutation of attached Views.
  - `MEDIUM`: main-thread blocking on secondary paths, or bounded work stalling for hundreds of milliseconds.
  - `LOW`: latent hazards that complete in memory today, or Binder-thread blocking reached only from external processes.

## Output

1. One-line verdict first: `ANR risk: yes (<highest severity>)` or `ANR risk:
   none found in this file`.
2. For each finding: line range, executing thread and verified call chain, why it blocks (or, for Step 4, why it leaks the sync barrier), severity, and suggested fix.
3. **Dismissed candidates**: one line each with the reason (for example, "thread not established", "seeded relay, returns immediately", "runs on IO through caller X", "owned by caller or host file").