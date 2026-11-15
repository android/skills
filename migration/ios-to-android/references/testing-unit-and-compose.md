> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

**Official Framework:** [Jetpack Compose UI Testing](https://developer.android.com/develop/ui/compose/testing) (`ComposeTestRule` /
`AndroidComposeTestRule` Kotlin UI tests).
**Extension:** `.kt`

> [!IMPORTANT]
> **Important:** **Validation Automation Philosophy** When converting an iOS capability to Android, unit tests alone are insufficient for verifying visual state integration. The agent should generate automated UI and End-to-End tests corresponding to the exact user flows extracted from the source iOS repository (`XCUITest` / `.storyboard` / SwiftUI views).

## 1. Jetpack Compose UI Test Structure

When porting an iOS `XCUITest` user journey or view assertion, you **Must**
generate a native **Jetpack Compose UI test** using `createComposeRule()` or
`createAndroidComposeRule<T>()`.

### Core Elements:

- `@get:Rule val composeTestRule = createComposeRule()` : Initializes the testing environment.
- `composeTestRule.setContent { ... }` : Sets the composable hierarchy under test.
- `onNodeWithText("...")`, `onNodeWithTag("...")`, `onNodeWithContentDescription("...")` : Semantics tree finders.
- `.performClick()`, `.performTextInput("...")`, `.performScrollTo()` : User interaction actions.
- `.assertIsDisplayed()`, `.assertTextEquals("...")`, `.assertIsEnabled()` : State assertions.

## 2. Mapping iOS XCUITest to Jetpack Compose UI Tests

### A. Launch \& Visual Parity

**iOS Context (`XCUITest`):**
`swift
app.launch()
XCTAssertTrue(app.staticTexts["Welcome"].exists)`

**Jetpack Compose UI Test (`WelcomeScreenTest.kt`):**
`kotlin
@Test
fun validateWelcomeScreenParity() {
composeTestRule.setContent {
MyAppTheme { WelcomeScreen() }
}
composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
}`

### B. Functional Interactions \& User Flows

**iOS Context (`XCUITest`):**
`swift
app.buttons["Login"].tap()
app.textFields["Email"].typeText("user@example.com")
XCTAssertTrue(app.staticTexts["Home Dashboard"].exists)`

**Jetpack Compose UI Test (`LoginJourneyTest.kt`):**
`kotlin
@Test
fun validateLoginJourney() {
composeTestRule.setContent {
MyAppTheme { LoginScreen() }
}
composeTestRule.onNodeWithTag("email_field").performTextInput("user@example.com")
composeTestRule.onNodeWithText("Login").performClick()
composeTestRule.onNodeWithText("Home Dashboard").assertIsDisplayed()
}`

### C. Verifying Layout \& Vector Assets

**iOS Context (`XCUITest`):**
Verify `my_icon.imageset` rendering and visibility.

**Jetpack Compose UI Test (`IconCompatibilityTest.kt`):**
`kotlin
@Test
fun validateVectorCompatibility() {
composeTestRule.setContent {
MyAppTheme { HomeScreen() }
}
composeTestRule.onNodeWithContentDescription("my_icon").assertIsDisplayed()
}`

## 3. Storage Convention

All generated UI and journey validation tests **MUST** be written to the
standard Android Gradle instrumentation test directory:
`app/src/androidTest/java/`

Unit tests for ViewModels, repositories, and business logic must be placed in:
`app/src/test/java/`

*** ** * ** ***

## 4. Forced Build Verification \& Command Matrix

The agent **MUST** execute and observe standard Gradle CLI commands to verify
that the project builds and tests pass cleanly:

| Verification Gate | Command | Passing Criterion |
|---|---|---|
| **Debug Build Compilation** | `./gradlew :app:assembleDebug` | Exit code 0, 0 compiler errors |
| **Unit \& ViewModel Tests** | `./gradlew :app:testDebugUnitTest --continue` | Exit code 0, 0 failed test cases |
| **Compose UI Instrumented Tests** | `./gradlew :app:connectedDebugAndroidTest` | Exit code 0 on connected emulator/device |
| **Multi-Module Full Check** | `./gradlew check -x lint` | Exit code 0 across all modules |

### Compiler Error Recovery Loop

If `./gradlew assembleDebug` or tests fail, the agent **MUST** follow this
iterative recovery cycle:

1. **Parse Root Cause** : Inspect standard error output for compiler errors, KSP code-generation failures (Room `@Entity` missing primary key, mismatched column types), or Hilt `@Inject` graph errors.
2. **Locate Conflicting Declarations** : Check `gradle/libs.versions.toml` for version conflicts between Kotlin compiler and Compose compiler extension, or missing KSP plugins.
3. **Apply Targeted Fix**: Edit the offending files without rewriting unaffected code.
4. **Re-run Command** : Execute `./gradlew :app:assembleDebug`. Never advance while the build is failing.

*** ** * ** ***

## 5. Prioritized Quality Gate \& Refinement Triage

To prevent open-ended refinement loops and avoid token thrashing, apply this
**strict 3-tier triage hierarchy**:

    [Quality Gate Triage]
      │
      ├──► Priority #1: STABILITY (Crashes & Build Blockers)
      │      └── Unconditional loop: Fix all crashes and fatal exceptions until the app runs cleanly.
      │
      ├──► Priority #2: CORE FEATURE PARITY (Missing CUJs)
      │      └── Single (1x) refinement loop: Compare against `.migration/feature_specification.md`.
      │          If still failing after 1 loop, document as an open gap in `migration_walkthrough.md`.
      │
      └──► Priority #3: CRITICAL UI DEFECTS (Layout, Missing Assets, Edge-to-Edge)
             └── Single (1x) refinement loop: Fix broken margins/padding, jarring unstyled colors,
                 or missing Drawables.
             └── RULE: Ignore minor typography/aesthetic nitpicks unless specified in migration brief.

*** ** * ** ***

## 6. Migration Walkthrough Specification (`migration_walkthrough.md`)

Upon completing migration and validation, the agent **Must** generate
`.migration/migration_walkthrough.md` to provide full transparency:

```markdown
# Migration Walkthrough: [App Name]

## 1. Migration Executive Summary
- **Source Platform**: iOS (SwiftUI / UIKit, Xcode [version])
- **Target Platform**: Android (Jetpack Compose, Kotlin [version], AGP [version])
- **Final Build Status**: `./gradlew assembleDebug` EXIT 0
- **Test Suite Status**: [X] unit tests passed, [Y] Compose UI tests passed

## 2. Subsystem Transformation Log
| Subsystem | iOS Source | Android Target | Verification Method | Status |
| :--- | :--- | :--- | :--- | :--- |
| Local Storage | Core Data `Model.xcdatamodeld` | Room Database + KSP | Unit test (`DaoTest.kt`) | VERIFIED |
| Network Layer | `URLSession` + Combine | Retrofit + Coroutines Flow | MockWebServer test | VERIFIED |
| UI Hierarchy | SwiftUI `ContentView` | Compose `MainScreen.kt` | `@Preview` + ComposeTestRule | VERIFIED |
| In-App Billing | StoreKit 2 `Product.purchase` | Google Play Billing Library 8 | Integration test | STUBBED |

## 3. Known API Gaps & Manual Action Items
- **Apple Sign-In**: Stubbed with `AuthenticationProviderStub.kt`. Action: Replace with Google Credential Manager.
- **Push Notification Token**: FCM receiver configured in `AndroidManifest.xml`. Action: Download `google-services.json` from Firebase Console.
```