---
subsystem: ipc
description: >
  Expert hints for IPC, binder transactions, and cross-process dependencies.
keywords:
  - ipc
  - binder
  - transaction
  - cross-process
  - bottleneck
  - service
  - system_server
---

# IPC & Binder Expert Hints

- Look for multiple outbound binder transactions from the same process
  (`system_server`) that carry similar data to different destinations in a
  short time frame. This "binder storm" indicates a lack of multiplexing.
- To trace data across processes, correlate slices using flow events by
  linking a slice's ID to `flow.source_slice_id` or `flow.dest_slice_id`.
- To detect binder spam, query the `binder_transaction` table and group by
  thread ID (`tid`), `service_name`, and `method_name` to find high numbers of
  identical calls.
- When high binder concurrency is found, identify the bottleneck server
  process by grouping transactions by `server_upid`.
- To analyze latency of a slow binder transaction, calculate time spent
  outside the server by subtracting `server_dur` from total `dur` in the
  `binder_transaction` table.
- When a thread is suspected of binder spam, correlate its `tid` with the
  `cpu_slice` table to check for high CPU consumption.
- To find code responsible for binder spam, get the `utid` of the problematic
  thread and use it to query `stack_profile_callsite`.
- To find callers of a problematic function, filter `stack_profile_callsite`
  for frames mapping to it, then trace upwards using `parent_id`.
- A long-running slice on one thread causally linked to a slice on another
  thread (for example, binder from `system_server` to SystemUI) indicates a
  scheduling dependency bottleneck.
- To find asynchronous operations that might cause UI jank, look for a binder
  transaction from a controlling process that returns quickly, followed by a
  long-running slice in the receiving process.
