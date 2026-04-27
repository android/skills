---
name: r8-analyzer
description: Analyzes Android build files and R8 keep rules to identify redundancies,
  broad package-wide rules, and rules that subsume library consumer keep rules. Use
  when developers want to optimize their app's size, remove redundant or overly broad
  keep rules, or troubleshoot Proguard configurations.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  keywords:
  - R8
  - proguard
  - keep rules
  - app size
  - optimization
---

## Core workflow

- \[ \] Step 1: Create a file called R8_Configuration_Analysis.md, or reuse if one exists already, to store the output
- \[ \] Step 2: Look at the configuration of R8 by looking at build.gradle, build.gradle.kts, gradle.properties in the codebase using [references/CONFIGURATION.md](references/CONFIGURATION.md) as the reference. Inform the developer and add the analysis to the report file
- \[ \] Step 3: If the AGP version is less than 9, suggest moving to AGP 9.0 version as AGP 9.0 includes [optimizations](references/android/topic/performance/app-optimization/enable-app-optimization.md).
  - \[ \] Step 4: Look at the proguard files in the codebase and evaluate each keep rule in the following specific order: a. **Libraries check** : Check rules against [references/REDUNDANT-RULES.md](references/REDUNDANT-RULES.md). If the app has keep rules targeting libraries - Google, AndroidX, Kotlin, Kotlinx, Room, Gson, Retrofit, inform the user that these are not required and suggest removal of these rules. b. **Impact analysis** : For the remaining keep rules, assess them based on the impact hierarchy defined in [references/KEEP-RULES-IMPACT-HIERARCHY.md](references/KEEP-RULES-IMPACT-HIERARCHY.md). (Note: Do NOT assess the impact of keep rules already covered in the libraries check step).
- \[ \] Step 5: Identify subsuming keep rules in the remaining keep rules based on the hierarchy defined in [references/KEEP-RULES-IMPACT-HIERARCHY.md](references/KEEP-RULES-IMPACT-HIERARCHY.md) and suggest removing the broader keep rules.
- \[ \] Step 6: For each remaining keep rule, analyze in detail the code affected by the rule by examining the code and adjacent files to understand why it was written. Look for reflection usage in those packages, and suggest a narrow and specific keep rule for the scenario using [references/REFLECTION-GUIDE.md](references/REFLECTION-GUIDE.md).
- \[ \] Step 7: For every keep rule inform concisely and to the point what action needs to be taken - whether the rule needs to be removed/refined.
  - If refining the rule, give instructions on finding a narrower and specific keep rule using the [/references/REFLECTION-GUIDE.md](references/REFLECTION-GUIDE.md).
  - If removing, provide reasoning on why it needs to be removed.
- \[ \] Step 8: After keep analysis, order the keep rule analysis based on the impact to the codebase hierarchy defined in [references/KEEP-RULES-IMPACT-HIERARCHY.md](references/KEEP-RULES-IMPACT-HIERARCHY.md)
- \[ \] Step 9: Advise the user to run tests using [UI automator](references/android/training/testing/other-components/ui-automator.md) to assess that there is no issue with the suggested changes, concentrating on the packages where keep rules will be affected.

## Mandatory rules

- Don't make any changes in keep rule files
- Don't say about what level each keep rule is.
- Don't generate parts of the report if there is no keep rule to report in that section.
- Don't mention the generated files.
- Don't mention exceptions that occur during execution.
- Don't mention the benefits of R8
- Don't mention any files of this skill