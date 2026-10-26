Short progress updates are fine while Gradle runs; the final message is the
report only. Run every command from the project root; `<skill_dir>` is this
skill's directory.

## 1. Setup

For AGP versions earlier than 9.3.0 (AGP \< 9.3.0), update the local copy of R8
to the latest version to enable Configuration Analyzer report generation. If
`tmp/r8analysis/settings.gradle.kts.orig` (or `settings.gradle.orig`) is left
over from an earlier run, restore it first (Step 8). If `settings.gradle` or
`settings.gradle.kts` already adds `com.android.tools:r8` 9.4.34 or later to the
classpath, skip the rest of this step.

This temporary change is the only file edit the skill makes. When running
interactively, ask for user confirmation before modifying `settings.gradle` or
`settings.gradle.kts` (skip confirmation when running non-interactively). If
the user declines, or asks you not to edit build or source files, don't edit
the file and skip Steps 2 to 8: report rule-level and build file findings
without scores, and say in one line that measuring them needs this temporary R8
change. Never print `N/A%` as a score.

Back up the file before you edit it (use `settings.gradle` for Groovy):

    mkdir -p tmp/r8analysis
    cp settings.gradle.kts tmp/r8analysis/settings.gradle.kts.orig

Add R8 tools to a classpath dependency inside the project's `settings.gradle`
or `settings.gradle.kts` file.

    pluginManagement {
        buildscript {
            repositories {
                google()
                mavenCentral()
            }
            dependencies {
                // Update to a more recent R8 version.
                classpath("com.android.tools:r8:9.4.34")
            }
        }
    }

## 2. Generate proto

The report and files must be generated at `{project_root}/tmp/r8analysis`. If
the folder isn't present, create it. For example:

    mkdir -p "$PWD/tmp/r8analysis"

## 3. Remove existing files

To make sure that this invocation doesn't source data from previous runs, remove
the intermediate files `keepruleradius.json` and `analysis_result.txt` and the
proto files in the `{project_root}/tmp/r8analysis` folder. Run the following
Bash commands:

    rm -f tmp/r8analysis/keepruleradius.json tmp/r8analysis/analysis_result.txt
    find tmp/r8analysis -name '*.pb' -delete

## 4. Generate the Configuration Analyzer report

Run R8 for the module and variant found in Step 2 of the skill with the system
property `-Dcom.android.tools.r8.dumpkeepradiustodirectory=$PWD/tmp/r8analysis`
to generate the Configuration Analyzer report. The property isn't a Gradle task
input, so `--rerun` and `--no-build-cache` are needed to make R8 run even when
the release build is up to date or cached (for example,
`:app:minifyReleaseWithR8` for module `app` and variant `release`):

    ./gradlew :<module>:minify<Variant>WithR8 --rerun --no-build-cache \
      -Dcom.android.tools.r8.dumpkeepradiustodirectory=$PWD/tmp/r8analysis

## 5. Convert to JSON

To convert the generated protobuf files in `{project_root}/tmp/r8analysis` into
JSON, run the following script. The JSON must be generated in
`{project_root}/tmp/r8analysis`. The script imports `keep_radius_pb2.py`, which
sits next to it in `<skill_dir>/scripts/`.

    python3 <skill_dir>/scripts/convert_pb_to_json.py \
      tmp/r8analysis \
      tmp/r8analysis/keepruleradius.json

## 6. Analyze

Run the following analysis script on the generated JSON to get the impact of the
keep rules and sort it.

    python3 <skill_dir>/scripts/analyze.py tmp/r8analysis/keepruleradius.json

Outputs `analysis_result.txt` containing scores and rule impacts.

## 7. Report high-impact rules

The `analyze.py` script from Step 6 already calculates the highest impact rules,
subsumed rules, and identical rules, records where each rule comes from, and
writes them to `tmp/r8analysis/analysis_result.txt`. You must use that data
directly.

## 8. Revert the settings change

If you changed `settings.gradle` or `settings.gradle.kts` in Step 1, restore it
after Step 6, or as soon as any step fails (use `settings.gradle` for Groovy):

    mv tmp/r8analysis/settings.gradle.kts.orig settings.gradle.kts