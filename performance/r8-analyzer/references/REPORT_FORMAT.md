## 1. Configuration

*(Optional section for the report, omit if no relevant findings are present.)*

- **AGP version**: \[Current\] -\> Upgrade to 9.3.
- **Full mode** : Not enabled. Remove `android.enableR8.fullMode=false` from `gradle.properties`.
- **Optimized resource shrinking** : If AGP is 8.12 or 8.13, add `android.r8.optimizedResourceShrinking=true` to `gradle.properties`. From AGP 9.0 it's on by default.
- **Default ProGuard file** : Use `getDefaultProguardFile("proguard-android-optimize.txt")` instead of `proguard-android.txt`.

## 2. Global disable rules

*(Optional section for the report, omit if no relevant findings are present.)*

- \[Rule\]: Disables R8 globally. **Action** : Remove. If the rule has `in_app_files: false` in `globals` in `analysis_result.txt`, use the library action from Section 4 instead.

If there is `-dontobfuscate`, `-dontoptimize`, `-dontshrink`, or a negated keep
rule using the `!` inversion operator (for example,
`-keep class !com.example.** { *; }`, which matches every class except that
package across the app and libraries) in the codebase, mention in this section
or Section 4 and recommend removing the rule or replacing it with narrow rules
for specific classes.

## 3. Optimization summary

- **Optimization score**: \[X\]% code is available for R8 optimizations (for example, inlining, merging). \[100-X\]% of codebase can't be optimized by R8.
- **Shrinking score**: \[X\]% of code will be optimized by R8 by removing unused classes, fields, and methods. \[100-X\]% of codebase contains redundant classes, fields, and methods that can't be removed by R8.
- **Obfuscation score**: \[X\]% of the codebase is available for R8 to obfuscate.

Increasing these scores increases the codebase available to R8 for
optimizations.

If the scores couldn't be measured (for example, the user declined the
temporary `settings.gradle(.kts)` change), replace this section with one line
that says so and why. Never print `N/A%` as a score.

## 4. Keep rules evaluation

### \[Rule text\]

- **Source** : \[`file:line` for a rule in the app's own files; otherwise the `from` value in `analysis_result.txt` (library or AGP defaults)\]
- **Keeps**: \[X\] items or \[X\] % of the codebase from optimization. Classes: \[X\], Fields: \[X\], Methods: \[X\] are prevented from optimization due to this keep rule.
- **Kept items summary**: \[Provide a brief summary or description of the items being kept, for example, "Models and DTOs in the network layer" instead of just listing raw class names\]
- **Guidance** : *(Include if the rule matches a pattern in
  `references/INSPECT-RULES.md`)* \[Explanation and remediation from `references/INSPECT-RULES.md`\]
- **Action** : For a rule in the app's own files (`in_app_files: true` in `analysis_result.txt`): **Remove** (Library or default config bundles rules, or negated `!` rule) OR **Refine** (Too broad, replace with narrow rules for specific classes: \[Surgical Rule\]). For any other rule (`in_app_files:
  false`; `from` names the library, AGP defaults, or build-generated rules): **From \[library\] / AGP defaults.** Not editable in this project; to reduce this rule's impact, report it to the library owner with these numbers.

## 5. Subsumed keep rules

*(Optional section for the report, omit if no relevant findings are present.)*

### \[Redundant rules\]

- **Subsumed by**: \[Broader Rule\]
- **Action** : **Remove** if Section 4 keeps \[Broader Rule\] as is. If Section 4 recommends removing or refining \[Broader Rule\], write instead: **Covered
  only while \[Broader Rule\] stays**.

List only subsumed rules in the app's own files (`in_app_files: true`); leave
out library and AGP default rules. Suggest removing a subsumed rule only if the
rule that subsumes it stays as is.

Rules in `identical_rules` keep exactly the same items as the rules in their
`identical_to`. Label them **Identical to** \[rule\], not **Subsumed by**. If
one of the identical rules comes from a library or AGP defaults, it stays, so
recommend removing the app's copy; if all of them are in the app's own files,
keep one and remove the others.