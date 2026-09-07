---
name: r8-analyzer
description: Analyzes Android build files and R8 keep rules to identify redundancies,
  broad package-wide rules, and rules that subsume library consumer keep rules. Use
  when developers want to optimize their app's size, remove redundant or overly broad
  keep rules, or troubleshoot Proguard configurations.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-08-28'
  keywords:
  - R8
  - proguard
  - keep rules
  - app size
  - optimization
---

## Step 1. Setup and configuration check

- Inspect `build.gradle`, `build.gradle.kts`, and `gradle.properties`.
- Use [references/CONFIGURATION.md](references/CONFIGURATION.md) to identify missing optimizations.
- **AGP** : If version is lower than 9.0, suggest migration to 9.0 for [build-time performance improvement](references/android/topic/performance/app-optimization/enable-app-optimization.md).
- **Full Mode** : Verify `android.enableR8.fullMode=false` is removed from gradle.properties.

## Step 2. Analysis path selection

- Inspect `build.gradle`, `build.gradle.kts`, and `gradle.properties` and
  `libs.versions.toml` to get the AGP and R8 versions.

- **If AGP \>= 9.3.0** : Proceed to **Path A (Standalone Task)**.

- **If AGP \< 9.3.0 and R8 \>= 9.3.7-dev** : Proceed to **Path B (Quantitative)**.

- If none of the conditions are met, proceed to **Path C (Heuristic)**.

### Path A: Standalone Gradle task (AGP \>= 9.3.0)

- **Step 1: Run standalone task** : Run `./gradlew :app:analyzeReleaseR8Config` to evaluate the R8 configuration. You MUST wait for this command to finish before proceeding.
- **Step 2: Convert to JSON** : The report is generated at `app/build/reports/r8/r8-config-analyzer-release.pb`. You MUST explicitly run the conversion script by executing: `python3 .agents/skills/r8-analyzer/scripts/convert_pb_to_json.py`. Wait for this command to finish.
- **Step 3: Analyze** : You MUST explicitly run the analysis script by executing: `python3 .agents/skills/r8-analyzer/scripts/analyze.py`. This outputs `tmp/keepradius/analysis_result.txt`. Wait for this command to finish.

### Path B: Quantitative data generation (R8 \>= 9.3.7-dev and AGP \< 9.3.0)

- **Step 1: Check requirements** : Python and `protobuf` package are mandatory.
- **Step 2: Generate and analyze** : You MUST run the shell commands described in [references/CONFIGURATION-ANALYZER.md](references/CONFIGURATION-ANALYZER.md) to generate the proto file using R8 configuration analyzer, convert it to JSON and analyze the result.
- **Step 3: Analyze** : You MUST ensure the analysis produces `tmp/keepradius/analysis_result.txt` for scores and rule impact metrics.

### Path C: Heuristic evaluation and recommendation (R8 \< 9.3.7-dev)

*(Use ONLY if quantitative data generation is not possible)*

- **Step 1: Manual evaluation** : Inspect `proguard-rules.pro`.
- **Step 2: Library check** : Compare rules against [references/REDUNDANT-RULES.md](references/REDUNDANT-RULES.md). Suggest **Remove** for bundled rules.
- **Step 3: Custom rule check** : Use [references/KEEP-RULES-IMPACT-HIERARCHY.md](references/KEEP-RULES-IMPACT-HIERARCHY.md) and [references/REFLECTION-GUIDE.md](references/REFLECTION-GUIDE.md) to prioritize and evaluate. Suggest **Refine** for broad rules (for example, package-wide).
- **Step 4: Validation** : Suggest Macrobenchmark tests using [UI Automator](references/android/training/testing/other-components/ui-automator.md) for any proposed changes. Proceed to Step 3.

## Step 3. Report generation

- **Format** : Follow [references/REPORT_FORMAT.md](references/REPORT_FORMAT.md) strictly.
- **Input**: Extract metrics (Scores, Impacts, Example Classes) directly from generated file analysis.txt if using Path A, or from manual findings if using Path B.
- **Output** : Output ONLY the raw Markdown report in the chat. Do NOT output conversational filler (for example, "Here is your report..."). Do NOT provide recommendations, next steps, or any other text outside of the sections defined in [references/REPORT_FORMAT.md](references/REPORT_FORMAT.md) Do NOT mention the path used for analysis of the configuration

## Constraints

- **Strict output limit**: The final output MUST strictly be the Markdown report and nothing else.
- **No code changes**: Research and suggest only; Do not modify files.
- **No redundancy**: Do not explain R8 benefits or reference skill internal files in the report.
- **Focus**: Omit sections (for example, Subsumed Rules, Configuration) if no issues or items are found.
