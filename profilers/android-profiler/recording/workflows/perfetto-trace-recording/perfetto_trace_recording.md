---
name: perfetto-trace-recording
description: >
  Records Java/native heap dumps, system traces, or custom configs via
  Perfetto helper scripts on Android.
keywords:
  - perfetto
  - record
  - system trace
  - heap dump
  - memory leak
  - custom config
---

# Recording Perfetto Traces on Android (Helper Scripts)

> [!IMPORTANT] **Scope:** This guide is **strictly for recording traces on
> Android devices**. For other platforms (Linux, macOS, or Chrome), refer to
> the platform documentation on [perfetto.dev/docs](https://perfetto.dev/docs/).

Rather than running raw `adb` commands, use the official Perfetto helper
scripts. They automatically handle pushing configurations, starting tracing
daemons, pulling the trace file, and optionally opening it in the browser.

Ensure **Developer options** and **USB debugging** are enabled, and your
device is connected via USB before starting.

---

## 0. Download the Helper Scripts

Download the helper scripts from the official Perfetto repository:

```bash
TOOLS_URL="https://raw.githubusercontent.com/google/perfetto/main/tools"

# Java Heap Dump (ART)
curl -O "$TOOLS_URL/java_heap_dump" && chmod +x java_heap_dump

# Native Heap Profiling (heapprofd)
curl -O "$TOOLS_URL/heap_profile" && chmod +x heap_profile

# CPU Stack Sampling (traced_perf)
curl -O "$TOOLS_URL/cpu_profile" && chmod +x cpu_profile

# General Tracing (Ftrace, ATrace, custom configs)
curl -O "$TOOLS_URL/record_android_trace" && chmod +x record_android_trace
```

---

## 1. Memory Tracing

Use these tools to analyze memory leaks, object retention, C/C++ allocations,
or system-wide memory counters.

### A. Java Heap Dump (ART)

Capture a snapshot of all Java objects in a process to investigate memory
leaks. Reference docs:
<https://perfetto.dev/docs/data-sources/java-heap-profiler>.

```bash
# Trigger a Java heap dump for a specific app
./java_heap_dump -n YOUR_APP_PACKAGE_NAME -o ./heap_dump.perfetto-trace
```

### B. Native C/C++ Heap Profiling (heapprofd)

Track C/C++ memory allocations (malloc/free) to find native leaks. Reference
docs: <https://perfetto.dev/docs/data-sources/native-heap-profiler>.

```bash
# Profile native allocations for a specific app
./heap_profile -n YOUR_APP_PACKAGE_NAME

# Profile with custom sampling interval (default is 4096 bytes)
./heap_profile -n YOUR_APP_PACKAGE_NAME -i 2048
```

### C. System-wide Memory Counters

Track RSS, Swap, and process stats over time. Reference docs:
<https://perfetto.dev/docs/data-sources/memory-counters-sys-stats>.

**Note:** To record memory counters, you must use a custom config via
`record_android_trace` (see Section 4).

---

## 2. Stack Sampling / Callstack Profiling (traced_perf)

Identify CPU hotspots in C/C++ or Rust code by periodically sampling
callstacks. Reference docs:
<https://perfetto.dev/docs/data-sources/cpu-profiler>.

```bash
# Profile CPU usage by sampling callstacks at 100Hz (default) for 10 seconds
./cpu_profile -n YOUR_APP_PACKAGE_NAME -d 10000

# Profile at a custom frequency (e.g., 200Hz)
./cpu_profile -n YOUR_APP_PACKAGE_NAME -f 200
```

---

## 3. System Tracing (CPU, Scheduling, & ATrace)

Investigate jank, slow transitions, CPU scheduling, and system calls.
Reference docs: <https://perfetto.dev/docs/data-sources/ftrace>.

You can specify duration, buffer size, and categories directly on the command
line:

```bash
# Record scheduling, frequency, and window manager events for 5 seconds
./record_android_trace -t 5s -b 32mb sched gfx wm -a YOUR_APP_PACKAGE_NAME
```

Common categories: `sched` (CPU scheduling), `freq` (CPU frequency), `gfx`
(Graphics), `am` (Activity Manager), `wm` (Window Manager), `view` (View
System).

---

## 4. Custom Configs (Mix & Match Data Sources)

If you need a custom mixture of data sources (e.g., combining Java heap dumps
with ftrace), or control that command-line flags do not offer (ring buffers,
long traces, per-counter polling), synthesize a config:

1. **Read the config reference:**
   `$SKILL_ROOT/recording/workflows/perfetto-trace-recording/references/trace_config_reference.md`
   explains config structure and data sources, pointing to exemplar configs in
   `$SKILL_ROOT/recording/workflows/perfetto-trace-recording/references/example-configs/`
   that you can start from and merge. For other sources, consult the
   official guide at <https://perfetto.dev/docs/data-sources/>.
2. **Save the Config:** Write the synthesized text configuration to a local
   file (e.g., `config.pftxt`).
3. **Execute the Trace:** Run the trace using the general recorder script
   `record_android_trace` (specialized scripts do not accept custom configs):

```bash
./record_android_trace -c config.pftxt -o ./my_trace.perfetto-trace
```

If the config has a typo, this fails fast with a parse error naming the bad
field - fix the config and retry.
