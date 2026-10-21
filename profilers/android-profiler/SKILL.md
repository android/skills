---
name: android-profiler
description: 'Records and analyzes Android performance data and diagnoses performance
  bottlenecks across user apps, system apps, and system services, even if the user
  does not explicitly mention profiling or traces. Use when: recording system traces,
  CPU call stacks, Vulkan GPU/DRM memory, or Java/native heap profiles; analyzing
  Perfetto traces for UI jank, frame drops, ANRs, or app startup latency issues; investigating
  CPU bottlenecks, IPC/Binder storms, or thread scheduling stalls; diagnosing battery
  drain, modem power rails, and kernel wake locks; debugging memory pressure, Low
  Memory Kills (LMK), OOMs, leaks, and allocation churn; performing A/B baseline trace
  comparisons to find regressions; executing ad hoc PerfettoSQL queries on Android
  traces.'
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-17'
  keywords:
  - android performance
  - debugging
  - profiling
  - recording
  - trace analysis
  - memory leaks
  - bottleneck
  - jank
  - SQL
---

Your primary role is to figure out what the user wants to do (disambiguation),
find the right instructions for it (workflow discovery), and follow them
step-by-step (following the execution plan). If the request is unclear, work
with the user to finalize the execution plan before proceeding.

## Prerequisites and Setup

Before executing any workflows, read [`references/env_setup.md`](references/env_setup.md). It defines
what to set `$SKILL_ROOT` to - the anchor every other path in this skill is
written against.

## Intent Disambiguation

Do not guess the user's intent. If the user request is not clear, **ask the
user** what they want to do before proceeding.

## Recording

Route all recording requests through
[recording/recording_orchestrator](recording/recording_orchestrator.md). This defines guidelines and
pre-flight checks or dependency checks that apply to all recording workflows,
and ensures you have the necessary setup to proceed. Read the orchestrator and
execute the plan it describes based on what the user wants to record (for
example, a system trace or a heap dump).

## Analysis

Route all analysis requests through
[analysis/analysis_orchestrator](analysis/analysis_orchestrator.md).
