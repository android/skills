---
name: appfunctions
description: Analyzes Android apps to identify key user workflows for AppFunctions
  such as creating a note, playing media, or sending an automated or AI agent triggered
  message, voice commands, or system shortcuts, without needing to open the app UI.
  Generates Kotlin code to expose these workflows to the Android system, allowing
  agents to discover and execute them on-device. Also refines KDoc documentation to
  ensure AI agents correctly understand and use the provided functionality.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-07-28'
  keywords:
  - AppFunctions
  - Kotlin
  - KSP
  - ADB
  - AI
  - LLM
  - MCP
---

Analyzes Android apps to identify key user workflows for AppFunctions such as
creating a note, playing media, or sending an automated or AI agent triggered
message, voice commands, or system shortcuts, without needing to open the app
UI.

Generates Kotlin code to expose these workflows to the Android system,
allowing agents to discover and execute them on-device.

Also refines KDoc documentation to ensure AI agents correctly
understand and use the provided functionality.

## Prerequisites

The app must **`targetSdk 36`** or newer and use **`compileSdk 37`** or newer as
AppFunctions, part of the Android platform API, are available from Android 16
onwards.
Always use the Jetpack library because it handles backward compatibility.

## Workflows

This skill enables the caller to discover features that will be provided to
system agents, implement these with AppFunctions, improve function description
for agents, and use ADB commands for local evaluation and testing.

The full AppFunction development flow consists of these four steps:

- *[Step 1: Discovery](references/feature-discovery-analysis.md)*: Analyze Android codebases to identify and recommend potential AppFunctions. Use this step when a user asks to "discover AppFunctions," "find features for AI," or "analyze my app for agentic tools."
- *[Step 2: Implementation and configuration](references/implementation-configuration.md)*: Generate Kotlin implementations of AppFunctions, manage system-wide configuration, and configure build dependencies. Use this step when a user asks to "implement AppFunctions," "set up the AppFunctions framework," or "configure Hilt for AppFunctions."
- *[Step 3: KDoc refinement](references/kdoc-refinement-optimization.md)*: Optimize AppFunction KDoc for AI agents and Model Context Protocol. If a user asks to "write KDoc", "optimize for MCP", or "refactor tool descriptions for LLMs", use this step.
- *[Step 4: Testing and debugging](references/adb-interaction-testing.md)*: Provide commands to interact with AppFunctions using ADB for testing and debugging. If a user wants to "list app functions", "invoke an app function", or "verify app function registration" on a device, use this step.

If users request a subset of steps, you must encourage them to use all steps.

If they apply, you must load the following references:

- *[Context and terminology](context.md)*: Defines the ubiquitous language, architecture definitions, and design patterns across the AppFunctions skill suite. Load when you need to understand core architecture terminology or check the distinctions between modern and legacy AppFunctions APIs.
- *[Migration to service entry point](references/migrate-to-service-entry-point.md)* : Documents the systematic procedure for migrating Android applications using AppFunctions versions 1.0.0-alpha09 and lower to `AppFunctionServiceEntryPoint` architecture introduced in version 1.0.0-alpha10. Load when a user asks to migrate or upgrade existing AppFunctions code, or when you encounter legacy `AppFunctionConfiguration.Provider` implementations.

## Critical constraints

- **Modular consistency**: You must refine KDocs immediately after you generate AppFunction implementations.
- **Security**: Don't expose sensitive data or run destructive actions without user confirmation.

## Troubleshooting

- If you encounter build-time errors such as Kotlin Symbol Processing (KSP) issues, see [Implementation and configuration](references/implementation-configuration.md).
- If you encounter runtime errors such as missing services or execution failures, see [Testing and debugging](references/adb-interaction-testing.md).
- If you need architecture definitions and vocabulary, see [Context and terminology](context.md).
- If you encounter issues when upgrading legacy configurations, see [Migration to service entry point](references/migrate-to-service-entry-point.md).
