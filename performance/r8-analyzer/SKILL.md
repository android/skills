---
name: r8-analyzer
description: Analyzes Android build files and R8 keep rules to identify redundancies,
  broad package-wide rules, and rules that subsume library consumer keep rules. Use
  this skill when developers want to optimize their Android app's size, remove redundant
  or overly broad keep rules, or troubleshoot ProGuard configurations. Don't use for
  general Gradle build errors, Compose UI performance, or runtime CPU profiling.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-24'
  keywords:
  - R8
  - proguard
  - keep rules
  - app size
  - optimization
  - shrinking
  - minification
  - obfuscation
---

## Step 1. Setup and configuration check

- Inspect `build.gradle`, `build.gradle.kts`, and `gradle.properties`.
- Use [references/CONFIGURATION.md](references/CONFIGURATION.md) to identify missing optimizations.
- **AGP** : If the version is lower than 9.3, suggest migration to 9.3 for the simplified [`optimization {}` DSL](references/android/topic/performance/app-optimization/enable-app-optimization.md) and the Configuration Analyzer task.
- **Full mode** : Verify `android.enableR8.fullMode=false` is removed from the `gradle.properties` file.

## Step 2. Analysis path selection

- Inspect `build.gradle`, `build.gradle.kts`, `settings.gradle`, `settings.gradle.kts`, `gradle.properties`, and `libs.versions.toml` to get the AGP and R8 versions. An R8 version pinned in `settings.gradle(.kts)` replaces the R8 bundled with AGP.
- Find the app module (usually `app`) and a build variant with R8 enabled (usually `release`). The following commands use them as `<module>` and `<Variant>`, for example `:app:minifyReleaseWithR8`.
- The helper scripts are in this skill's `scripts/` directory. In the following
  commands, `<skill_dir>` is this skill's directory. Run every command from the
  project root.

- **If AGP \>= 9.3.0** : Proceed to **Path A (Standalone Task)**.

- **If AGP \< 9.3.0** : Proceed to **Path B (Quantitative)**.

### Path A: Standalone Gradle task (AGP \>= 9.3.0)

- **Step 1: Check requirements** : Python and the `protobuf` package 6.33.4 or later are mandatory (used by [`scripts/keep_radius_pb2.py`](scripts/keep_radius_pb2.py)). Install it with `pip install "protobuf>=6.33.4"`; versions lower than 6.33.4 fail on import with a `VersionError` or `ImportError`.
- **Step 2: Run standalone task on R8-enabled build** : For the module and variant you found during path selection, run `./gradlew :<module>:analyze<Variant>R8Config` (for example, `./gradlew :app:analyzeReleaseR8Config`). It generates the report in `<module>/build/reports/r8/` (for example, `app/build/reports/r8/r8-config-analyzer-release.pb`). You must wait for this command to finish before proceeding.
- **Step 3: Convert to JSON** : You must explicitly run the conversion script [`scripts/convert_pb_to_json.py`](scripts/convert_pb_to_json.py) as `python3 <skill_dir>/scripts/convert_pb_to_json.py` with arguments `<module>/build/reports/r8` and `tmp/keepradius/keepruleradius.json`. Wait for this command to finish.
- **Step 4: Analyze** : You must explicitly run the analysis script [`scripts/analyze.py`](scripts/analyze.py) by executing `python3 <skill_dir>/scripts/analyze.py tmp/keepradius/keepruleradius.json`. This outputs `tmp/keepradius/analysis_result.txt`. Wait for this command to finish.

### Path B: Invoke R8 release build (AGP \< 9.3.0)

- **Step 1: Check requirements** : Python and the `protobuf` package 6.33.4 or later are mandatory: `pip install "protobuf>=6.33.4"`.
- **Step 2: Generate and analyze** : You must run the shell commands described in [references/FULL_BUILD_CONFIG_ANALYZER.md](references/FULL_BUILD_CONFIG_ANALYZER.md) to temporarily update the R8 version in `settings.gradle(.kts)` to 9.4.34 or later (so R8 generates the report), generate the proto report in `tmp/r8analysis/`, convert it to `tmp/r8analysis/keepruleradius.json`, analyze the result, and revert the temporary `settings.gradle(.kts)` change.
- **Step 3: Analyze** : Verify that the analysis outputs `tmp/r8analysis/analysis_result.txt` for scores and rule impact metrics.

## Step 3. Report generation

- **Format** : Follow [references/REPORT_FORMAT.md](references/REPORT_FORMAT.md) strictly.
- **Redundant rules** : Check keep rules against [references/INSPECT-RULES.md](references/INSPECT-RULES.md). If a keep rule matches a pattern in [references/INSPECT-RULES.md](references/INSPECT-RULES.md), include the guidance and remediation from that reference alongside the impact metrics.
- **Input** : Extract metrics (Scores, Impacts, Example Classes, rule origins) directly from the generated file `tmp/keepradius/analysis_result.txt` if using Path A, or `tmp/r8analysis/analysis_result.txt` if using Path B.
- **Output** : Short progress updates are fine while Gradle runs; the final message is the report only. Output only the raw Markdown report in the chat. Do not output conversational filler (for example, "Here is your report..."). Do not provide recommendations, next steps, or any other text outside of the sections defined in [references/REPORT_FORMAT.md](references/REPORT_FORMAT.md). Do not mention the path used for analysis of the configuration.

## Constraints

- **Strict output limit**: The final output must strictly be the Markdown report and nothing else.
- **No code changes** : Research and suggest only; do not modify project files. Create files only under `tmp/` (analysis output, the settings backup, and any scratch scripts), and never write the report to a file. The one exception is the temporary R8 `classpath` in `settings.gradle(.kts)` that Path B needs when the project doesn't already pin R8 9.4.34 or later: ask the user first when running interactively, and revert it after the analysis (see [references/FULL_BUILD_CONFIG_ANALYZER.md](references/FULL_BUILD_CONFIG_ANALYZER.md)). If that change is needed but the user says no, or asks you not to edit build files, report rule-level and build file findings without scores and say why.
- **No placeholder scores** : Never print `N/A%` as a score.
- **No redundancy**: Do not explain R8 benefits or reference skill internal files in the report.
- **Focus**: Omit sections (for example, Subsumed Rules, Configuration) if no issues or items are found.
