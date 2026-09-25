---
name: wear-compose-m3
description: Guidance for Wear OS Compose Material3 (Wear Compose Material 3 / Wear
  Compose Material3) watch and wearable apps. Covers creating, updating, and migrating
  projects (Material 2.5 and Horologist migration) using androidx.wear.compose.material3,
  androidx.wear.compose.foundation, and androidx.wear.compose.navigation3, including
  AppScaffold, ScreenScaffold, TransformingLazyColumn, ambient mode, and previews.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-09-24'
  keywords:
  - Wear OS
  - Compose
  - Material3
  - Horologist
  - TransformingLazyColumn
  - ScalingLazyColumn
  - AppScaffold
  - ScreenScaffold
  - EdgeButton
  - SwipeDismissableSceneStrategy
  - Material 2.5
  - ambient mode
---

## Prerequisites and compatibility

1. **Current Wear OS Compose version in-use:** To find the installed library version, read `gradle/libs.versions.toml` or `build.gradle.kts` directly. Don't run `./gradlew dependencies` or other shell commands to resolve versions.
2. **Wear OS Compose Material3 version:** If an internal tool is available to establish the **latest stable version** `{VERSION}` of `androidx.wear.compose:compose-material3`, use that tool.
   - Otherwise, fetch the [official Maven metadata XML](https://dl.google.com/dl/android/maven2/androidx/wear/compose/compose-material3/maven-metadata.xml) to identify `{VERSION}` (highest number, ignoring `-alpha`, `-beta`, or `-rc`).
3. **Strict compliance:** If a version is listed as stable, you MUST use it, unless overridden by the user. Do not downgrade based on initial "Unresolved reference" errors in the editor or outdated web search results.
4. **Kotlin \& Compose compiler:** Use Kotlin **2.0.0+** with the `org.jetbrains.kotlin.plugin.compose` Gradle plugin.
5. **Min SDK:** Ensure `minSdk` is at least **25**.
6. **Compile SDK \& AGP:** Ensure `compileSdk` and AGP meet the library's AAR metadata floors (`compileSdk >= 35` and `AGP >= 8.6.0` for `1.6.x`; `compileSdk >= 37` and `AGP >= 9.1.0` for `1.7.x`).

## Gotchas

1. **Mandatory sync and validation:** After updating versions in `libs.versions.toml` or `build.gradle.kts`, perform a Gradle sync (or `./gradlew assembleDebug`) before refactoring, and run `./gradlew assembleDebug` after refactoring to verify the build.
2. **Prohibition of guessing (error protocol):** If you encounter an 'Unresolved Reference' or API mismatch after a successful sync, do not attempt to 'fix' it by downgrading the library version.

## Capabilities and tools

### Capability 1: Migration, updates, and adding features

Use this guidance when migrating from an older version of Wear OS Compose or
Horologist, updating an existing Wear Compose Material3 project, or adding
features.

1. Unless otherwise indicated by the developer, use the latest stable version of Wear Compose Material3 from `{VERSION}` (superseding any literal version strings in the migration guide).
2. When migrating from Material 2.5 or Horologist, read the [migration guide](references/android/training/wearables/compose/migrate-to-material3.md) and use its official component mappings.
3. Before refactoring any component (for example, `Chip` -\> `Button`), check the parameter names, slot types, and "Expressive" design tokens.
4. Do not use the Horologist Composables, Compose Layout, or Compose Material libraries; replace Horologist components with Wear Compose M3 equivalents (`AppScaffold` -\> M3 `AppScaffold`, `ScreenScaffold` -\> M3 `ScreenScaffold`, `ScalingLazyColumn` -\> `TransformingLazyColumn`, `ResponsiveListHeader` -\> `ListHeader`, `PagerScreen` -\> `HorizontalPagerScaffold`, `Chip` -\> `Button`, `Confirmation` -\> `ConfirmationDialog`).
5. **Always** check against the component guidance in Capability 3.
6. Expect screenshot tests to fail when migrating or updating components: expected defaults for padding and positioning will have changed. Do not seek to artificially match the previous screenshot, but give preference to the Material3 defaults.

### Capability 2: Component samples

Use this table of reference to find canonical samples for Wear Compose
components.
When working with a Wear Compose component, you must use the samples linked
from the table to ensure you know how to correctly use it.

**Important:** Symbols marked with `[alpha]`, `[beta]`, or `[rc]` are only
available in that pre-release track and are **not** in stable `{VERSION}`. Do
not use them unless the project explicitly targets that pre-release version.

#### Material 3 components in `androidx.wear.compose.material3.*`

| Component / Symbol | Reference Samples |
|---|---|
| `AlertDialog`, `AlertDialogDefaults` | [AlertDialogSample](references/material3/AlertDialogSample.kt.md) |
| `AnimatedPage`, `HorizontalPagerScaffold`, `VerticalPagerScaffold` | [OneHandedGestureSamples](references/material3/OneHandedGestureSamples.kt.md), [PageIndicatorSample](references/material3/PageIndicatorSample.kt.md), [PagerScaffoldSample](references/material3/PagerScaffoldSample.kt.md) |
| `AnimatedText`, `rememberAnimatedTextFontRegistry` | [AnimatedTextSample](references/material3/AnimatedTextSample.kt.md) |
| `AppCard` | [CardSample](references/material3/CardSample.kt.md) |
| `AppScaffold` | [ListHeaderSample](references/material3/ListHeaderSample.kt.md), [PagerScaffoldSample](references/material3/PagerScaffoldSample.kt.md), [ScaffoldSample](references/material3/ScaffoldSample.kt.md), [SurfaceTransformationSample](references/material3/SurfaceTransformationSample.kt.md), [TransformingLazyColumnNotificationsSample](references/material3/TransformingLazyColumnNotificationsSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `ArcProgressIndicator`, `ArcProgressIndicatorDefaults`, `CircularProgressIndicator`, `CircularProgressIndicatorDefaults`, `ProgressIndicatorDefaults`, `SegmentedCircularProgressIndicator`, `drawCircularProgressIndicator` | [ProgressIndicatorSample](references/material3/ProgressIndicatorSample.kt.md) |
| `Button` | [AlertDialogSample](references/material3/AlertDialogSample.kt.md), [AnimatedTextSample](references/material3/AnimatedTextSample.kt.md), [ButtonGroupSample](references/material3/ButtonGroupSample.kt.md), [ButtonSample](references/material3/ButtonSample.kt.md), [DatePickerSample](references/material3/DatePickerSample.kt.md), [DynamicColorSchemeSample](references/material3/DynamicColorSchemeSample.kt.md), [FadingExpandingLabelSample](references/material3/FadingExpandingLabelSample.kt.md), [ListHeaderSample](references/material3/ListHeaderSample.kt.md), [OneHandedGestureSamples](references/material3/OneHandedGestureSamples.kt.md), [PageIndicatorSample](references/material3/PageIndicatorSample.kt.md), [PagerScaffoldSample](references/material3/PagerScaffoldSample.kt.md), [PickerSample](references/material3/PickerSample.kt.md), [ScaffoldSample](references/material3/ScaffoldSample.kt.md), [ScrollIndicatorSample](references/material3/ScrollIndicatorSample.kt.md), [StepperSample](references/material3/StepperSample.kt.md), [SurfaceTransformationSample](references/material3/SurfaceTransformationSample.kt.md), [SwipeDismissableSceneStrategySample](references/navigation3/SwipeDismissableSceneStrategySample.kt.md), [SwipeToRevealSample](references/material3/SwipeToRevealSample.kt.md), [TimePickerSample](references/material3/TimePickerSample.kt.md), [TransformationSpecSample](references/material3/TransformationSpecSample.kt.md), [TransformingLazyColumnSample](references/foundation/TransformingLazyColumnSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `ButtonDefaults` | [ButtonSample](references/material3/ButtonSample.kt.md), [CurvedTextSamples](references/material3/CurvedTextSamples.kt.md), [DynamicColorSchemeSample](references/material3/DynamicColorSchemeSample.kt.md), [EdgeButtonSample](references/material3/EdgeButtonSample.kt.md), [ListHeaderSample](references/material3/ListHeaderSample.kt.md), [OneHandedGestureSamples](references/material3/OneHandedGestureSamples.kt.md), [PlaceholderSample](references/material3/PlaceholderSample.kt.md), [ScaffoldSample](references/material3/ScaffoldSample.kt.md), [ScrollAwaySample](references/material3/ScrollAwaySample.kt.md), [ScrollIndicatorSample](references/material3/ScrollIndicatorSample.kt.md), [SurfaceTransformationSample](references/material3/SurfaceTransformationSample.kt.md), [TextButtonSample](references/material3/TextButtonSample.kt.md), [TransformationSpecSample](references/material3/TransformationSpecSample.kt.md), [TransformingLazyColumnSample](references/foundation/TransformingLazyColumnSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `ButtonGroup` | [ButtonGroupSample](references/material3/ButtonGroupSample.kt.md), [TransformationSpecSample](references/material3/TransformationSpecSample.kt.md) |
| `Card` | [CardSample](references/material3/CardSample.kt.md), [SwipeDismissableSceneStrategySample](references/navigation3/SwipeDismissableSceneStrategySample.kt.md), [SwipeToRevealSample](references/material3/SwipeToRevealSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `CardDefaults` | [CardSample](references/material3/CardSample.kt.md), [SurfaceTransformationSample](references/material3/SurfaceTransformationSample.kt.md), [SwipeToRevealSample](references/material3/SwipeToRevealSample.kt.md), [TransformingLazyColumnSample](references/foundation/TransformingLazyColumnSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `CheckboxButton` | [CheckboxButtonSample](references/material3/CheckboxButtonSample.kt.md), [SwipeToDismissBoxSample](references/material3/SwipeToDismissBoxSample.kt.md) |
| `ChildButton`, `OutlinedButton` | [ButtonSample](references/material3/ButtonSample.kt.md) |
| `ColorScheme`, `dynamicColorScheme` | [DynamicColorSchemeSample](references/material3/DynamicColorSchemeSample.kt.md) |
| `CompactButton`, `CompactButtonDefaults` | [ButtonSample](references/material3/ButtonSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `ConfirmationDialog`, `ConfirmationDialogDefaults`, `FailureConfirmationDialog`, `SuccessConfirmationDialog`, `confirmationDialogCurvedText` | [ConfirmationDialogSample](references/material3/ConfirmationDialogSample.kt.md) |
| `CurvedTextDefaults` | [CurvedTextSamples](references/material3/CurvedTextSamples.kt.md) |
| `DatePicker`, `DatePickerType` | [DatePickerSample](references/material3/DatePickerSample.kt.md) |
| `EdgeButton` | [EdgeButtonSample](references/material3/EdgeButtonSample.kt.md), [OneHandedGestureSamples](references/material3/OneHandedGestureSamples.kt.md), [ScaffoldSample](references/material3/ScaffoldSample.kt.md), [SwipeDismissableSceneStrategySample](references/navigation3/SwipeDismissableSceneStrategySample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `EdgeButtonSize` | [EdgeButtonSample](references/material3/EdgeButtonSample.kt.md) |
| `FadingExpandingLabel` | [FadingExpandingLabelSample](references/material3/FadingExpandingLabelSample.kt.md) |
| `FilledIconButton`, `FilledTonalIconButton`, `IconButtonColors`, `IconButtonShapes`, `OutlinedIconButton` | [IconButtonSample](references/material3/IconButtonSample.kt.md) |
| `FilledTonalButton` | [AlertDialogSample](references/material3/AlertDialogSample.kt.md), [ButtonSample](references/material3/ButtonSample.kt.md), [ConfirmationDialogSample](references/material3/ConfirmationDialogSample.kt.md), [OpenOnPhoneDialogSample](references/material3/OpenOnPhoneDialogSample.kt.md), [PlaceholderSample](references/material3/PlaceholderSample.kt.md), [ScrollAwaySample](references/material3/ScrollAwaySample.kt.md), [SwipeToDismissBoxSample](references/material3/SwipeToDismissBoxSample.kt.md) |
| `HorizontalPageIndicator`, `VerticalPageIndicator` | [PageIndicatorSample](references/material3/PageIndicatorSample.kt.md) |
| `Icon` | [AlertDialogSample](references/material3/AlertDialogSample.kt.md), [ButtonSample](references/material3/ButtonSample.kt.md), [CardSample](references/material3/CardSample.kt.md), [CheckboxButtonSample](references/material3/CheckboxButtonSample.kt.md), [ConfirmationDialogSample](references/material3/ConfirmationDialogSample.kt.md), [CurvedTextSamples](references/material3/CurvedTextSamples.kt.md), [DatePickerSample](references/material3/DatePickerSample.kt.md), [EdgeButtonSample](references/material3/EdgeButtonSample.kt.md), [IconButtonSample](references/material3/IconButtonSample.kt.md), [ListHeaderSample](references/material3/ListHeaderSample.kt.md), [PlaceholderSample](references/material3/PlaceholderSample.kt.md), [ProgressIndicatorSample](references/material3/ProgressIndicatorSample.kt.md), [RadioButtonSample](references/material3/RadioButtonSample.kt.md), [SwipeToRevealSample](references/material3/SwipeToRevealSample.kt.md), [SwitchButtonSample](references/material3/SwitchButtonSample.kt.md), [TimePickerSample](references/material3/TimePickerSample.kt.md) |
| `IconButton` | [IconButtonSample](references/material3/IconButtonSample.kt.md), [LevelIndicatorSample](references/material3/LevelIndicatorSample.kt.md), [ProgressIndicatorSample](references/material3/ProgressIndicatorSample.kt.md) |
| `IconButtonDefaults` | [IconButtonSample](references/material3/IconButtonSample.kt.md), [ProgressIndicatorSample](references/material3/ProgressIndicatorSample.kt.md) |
| `IconToggleButton`, `IconToggleButtonDefaults` | [IconToggleButtonSample](references/material3/IconToggleButtonSample.kt.md) |
| `LevelIndicator` | [LevelIndicatorSample](references/material3/LevelIndicatorSample.kt.md) |
| `LinearProgressIndicator` | [LinearProgressIndicatorSample](references/material3/LinearProgressIndicatorSample.kt.md) |
| `ListHeader` | [ListHeaderSample](references/material3/ListHeaderSample.kt.md), [ScrollAwaySample](references/material3/ScrollAwaySample.kt.md), [SwipeDismissableSceneStrategySample](references/navigation3/SwipeDismissableSceneStrategySample.kt.md), [TransformingLazyColumnNotificationsSample](references/material3/TransformingLazyColumnNotificationsSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `ListHeaderDefaults` | [ListHeaderSample](references/material3/ListHeaderSample.kt.md), [ScrollAwaySample](references/material3/ScrollAwaySample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `ListSubHeader` | [ListHeaderSample](references/material3/ListHeaderSample.kt.md) |
| `LocalOneHandedGestureEnabled` \[rc\], `OneHandedGestureDefaults` \[rc\], `OneHandedGestureHorizontalPageIndicator` \[rc\], `OneHandedGesturePageIndicatorState` \[rc\], `OneHandedGesturePriority` \[rc\], `OneHandedGestureScrollIndicator` \[rc\], `OneHandedGestureScrollIndicatorState` \[rc\], `OneHandedGestureVerticalPageIndicator` \[rc\] | [OneHandedGestureSamples](references/material3/OneHandedGestureSamples.kt.md) |
| `MaterialTheme` | [AlertDialogSample](references/material3/AlertDialogSample.kt.md), [ButtonSample](references/material3/ButtonSample.kt.md), [CardSample](references/material3/CardSample.kt.md), [CurvedTextSamples](references/material3/CurvedTextSamples.kt.md), [DynamicColorSchemeSample](references/material3/DynamicColorSchemeSample.kt.md), [LinearProgressIndicatorSample](references/material3/LinearProgressIndicatorSample.kt.md), [PagerScaffoldSample](references/material3/PagerScaffoldSample.kt.md), [ProgressIndicatorSample](references/material3/ProgressIndicatorSample.kt.md), [SwipeToDismissBoxSample](references/material3/SwipeToDismissBoxSample.kt.md), [TimeTextSample](references/material3/TimeTextSample.kt.md), [TransformingLazyColumnNotificationsSample](references/material3/TransformingLazyColumnNotificationsSample.kt.md) |
| `OneHandedGestureAction` \[rc\], `OneHandedGestureClickIndicator` \[rc\], `OneHandedGestureClickIndicatorState` \[rc\], `oneHandedGesture` \[rc\], `rememberOneHandedGestureConfiguration` \[rc\] | [ButtonSample](references/material3/ButtonSample.kt.md), [CardSample](references/material3/CardSample.kt.md), [OneHandedGestureSamples](references/material3/OneHandedGestureSamples.kt.md) |
| `OpenOnPhoneDialog`, `OpenOnPhoneDialogDefaults`, `openOnPhoneDialogCurvedText` | [OpenOnPhoneDialogSample](references/material3/OpenOnPhoneDialogSample.kt.md) |
| `OutlinedCard` | [CardSample](references/material3/CardSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `PagerScaffoldDefaults` | [PageIndicatorSample](references/material3/PageIndicatorSample.kt.md), [PagerScaffoldSample](references/material3/PagerScaffoldSample.kt.md) |
| `Picker` | [PickerSample](references/material3/PickerSample.kt.md) |
| `PickerGroup` | [PickerGroupSample](references/material3/PickerGroupSample.kt.md) |
| `RadioButton`, `SplitRadioButton` | [RadioButtonSample](references/material3/RadioButtonSample.kt.md) |
| `ResponsiveTransformationSpec`, `TransformationVariableSpec` | [SurfaceTransformationSample](references/material3/SurfaceTransformationSample.kt.md), [TransformationSpecSample](references/material3/TransformationSpecSample.kt.md) |
| `RevealValue`, `SwipeToReveal`, `SwipeToRevealDefaults`, `rememberRevealState` | [SwipeToRevealSample](references/material3/SwipeToRevealSample.kt.md) |
| `ScreenScaffold` | [ListHeaderSample](references/material3/ListHeaderSample.kt.md), [OneHandedGestureSamples](references/material3/OneHandedGestureSamples.kt.md), [PagerScaffoldSample](references/material3/PagerScaffoldSample.kt.md), [ScaffoldSample](references/material3/ScaffoldSample.kt.md), [SurfaceTransformationSample](references/material3/SurfaceTransformationSample.kt.md), [SwipeDismissableSceneStrategySample](references/navigation3/SwipeDismissableSceneStrategySample.kt.md), [TransformingLazyColumnNotificationsSample](references/material3/TransformingLazyColumnNotificationsSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `ScreenScaffoldDefaults`, `ScrollIndicator` | [ScrollIndicatorSample](references/material3/ScrollIndicatorSample.kt.md) |
| `ScreenStage`, `scrollAway` | [ScrollAwaySample](references/material3/ScrollAwaySample.kt.md) |
| `Slider`, `SliderDefaults` | [SliderSample](references/material3/SliderSample.kt.md) |
| `SplitCheckboxButton` | [CheckboxButtonSample](references/material3/CheckboxButtonSample.kt.md) |
| `SplitSwitchButton` | [SwitchButtonSample](references/material3/SwitchButtonSample.kt.md) |
| `Stepper`, `StepperLevelIndicator`, `rangeSemantics` | [StepperSample](references/material3/StepperSample.kt.md) |
| `StepperDefaults` | [LevelIndicatorSample](references/material3/LevelIndicatorSample.kt.md), [StepperSample](references/material3/StepperSample.kt.md) |
| `SurfaceTransformation` | [AlertDialogSample](references/material3/AlertDialogSample.kt.md), [DynamicColorSchemeSample](references/material3/DynamicColorSchemeSample.kt.md), [ListHeaderSample](references/material3/ListHeaderSample.kt.md), [ScaffoldSample](references/material3/ScaffoldSample.kt.md), [SurfaceTransformationSample](references/material3/SurfaceTransformationSample.kt.md), [SwipeDismissableSceneStrategySample](references/navigation3/SwipeDismissableSceneStrategySample.kt.md), [TransformationSpecSample](references/material3/TransformationSpecSample.kt.md), [TransformingLazyColumnNotificationsSample](references/material3/TransformingLazyColumnNotificationsSample.kt.md), [TransformingLazyColumnSample](references/foundation/TransformingLazyColumnSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `SwipeToDismissBox` | [SwipeToDismissBoxSample](references/material3/SwipeToDismissBoxSample.kt.md) |
| `SwitchButton` | [AlertDialogSample](references/material3/AlertDialogSample.kt.md), [OneHandedGestureSamples](references/material3/OneHandedGestureSamples.kt.md), [SwitchButtonSample](references/material3/SwitchButtonSample.kt.md) |
| `Text` | [AlertDialogSample](references/material3/AlertDialogSample.kt.md), [AnimatedTextSample](references/material3/AnimatedTextSample.kt.md), [ButtonGroupSample](references/material3/ButtonGroupSample.kt.md), [ButtonSample](references/material3/ButtonSample.kt.md), [CardSample](references/material3/CardSample.kt.md), [CheckboxButtonSample](references/material3/CheckboxButtonSample.kt.md), [ConfirmationDialogSample](references/material3/ConfirmationDialogSample.kt.md), [DatePickerSample](references/material3/DatePickerSample.kt.md), [DynamicColorSchemeSample](references/material3/DynamicColorSchemeSample.kt.md), [EdgeButtonSample](references/material3/EdgeButtonSample.kt.md), [ListHeaderSample](references/material3/ListHeaderSample.kt.md), [OneHandedGestureSamples](references/material3/OneHandedGestureSamples.kt.md), [OpenOnPhoneDialogSample](references/material3/OpenOnPhoneDialogSample.kt.md), [PageIndicatorSample](references/material3/PageIndicatorSample.kt.md), [PagerScaffoldSample](references/material3/PagerScaffoldSample.kt.md), [PickerGroupSample](references/material3/PickerGroupSample.kt.md), [PickerSample](references/material3/PickerSample.kt.md), [PlaceholderSample](references/material3/PlaceholderSample.kt.md), [RadioButtonSample](references/material3/RadioButtonSample.kt.md), [ScaffoldSample](references/material3/ScaffoldSample.kt.md), [ScrollAwaySample](references/material3/ScrollAwaySample.kt.md), [ScrollIndicatorSample](references/material3/ScrollIndicatorSample.kt.md), [StepperSample](references/material3/StepperSample.kt.md), [SurfaceTransformationSample](references/material3/SurfaceTransformationSample.kt.md), [SwipeDismissableSceneStrategySample](references/navigation3/SwipeDismissableSceneStrategySample.kt.md), [SwipeToDismissBoxSample](references/material3/SwipeToDismissBoxSample.kt.md), [SwipeToRevealSample](references/material3/SwipeToRevealSample.kt.md), [SwitchButtonSample](references/material3/SwitchButtonSample.kt.md), [TextButtonSample](references/material3/TextButtonSample.kt.md), [TextToggleButtonSample](references/material3/TextToggleButtonSample.kt.md), [TimePickerSample](references/material3/TimePickerSample.kt.md), [TransformationSpecSample](references/material3/TransformationSpecSample.kt.md), [TransformingLazyColumnNotificationsSample](references/material3/TransformingLazyColumnNotificationsSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `TextButton` | [TextButtonSample](references/material3/TextButtonSample.kt.md) |
| `TextButtonDefaults` | [TextButtonSample](references/material3/TextButtonSample.kt.md), [TextToggleButtonSample](references/material3/TextToggleButtonSample.kt.md) |
| `TextToggleButton`, `TextToggleButtonDefaults`, `touchTargetAwareSize` | [TextToggleButtonSample](references/material3/TextToggleButtonSample.kt.md) |
| `TimePicker`, `TimePickerType` | [TimePickerSample](references/material3/TimePickerSample.kt.md) |
| `TimeText` | [ScrollAwaySample](references/material3/ScrollAwaySample.kt.md), [ScrollIndicatorSample](references/material3/ScrollIndicatorSample.kt.md), [TimeTextSample](references/material3/TimeTextSample.kt.md) |
| `TimeTextDefaults`, `timeTextCurvedText` | [TimeTextSample](references/material3/TimeTextSample.kt.md) |
| `TitleCard` | [CardSample](references/material3/CardSample.kt.md), [SurfaceTransformationSample](references/material3/SurfaceTransformationSample.kt.md), [SwipeDismissableSceneStrategySample](references/navigation3/SwipeDismissableSceneStrategySample.kt.md), [SwipeToRevealSample](references/material3/SwipeToRevealSample.kt.md), [TransformingLazyColumnNotificationsSample](references/material3/TransformingLazyColumnNotificationsSample.kt.md), [TransformingLazyColumnSample](references/foundation/TransformingLazyColumnSample.kt.md) |
| `TransformationSpec` | [TransformationSpecSample](references/material3/TransformationSpecSample.kt.md) |
| `curvedText` | [CurvedTextSamples](references/material3/CurvedTextSamples.kt.md), [ScrollAwaySample](references/material3/ScrollAwaySample.kt.md), [TimeTextSample](references/material3/TimeTextSample.kt.md) |
| `firstVisibleItemLayoutItemInfo` \[rc\], `layoutItemInfoOf` \[rc\], `rememberTransformingLazyColumnFirstLayoutItemProvider` \[rc\] | [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `placeholder`, `placeholderShimmer`, `rememberPlaceholderState` | [PlaceholderSample](references/material3/PlaceholderSample.kt.md) |
| `rememberPickerState` | [PickerGroupSample](references/material3/PickerGroupSample.kt.md), [PickerSample](references/material3/PickerSample.kt.md) |
| `rememberTransformationSpec`, `transformedHeight` | [AlertDialogSample](references/material3/AlertDialogSample.kt.md), [DynamicColorSchemeSample](references/material3/DynamicColorSchemeSample.kt.md), [ListHeaderSample](references/material3/ListHeaderSample.kt.md), [ScaffoldSample](references/material3/ScaffoldSample.kt.md), [SurfaceTransformationSample](references/material3/SurfaceTransformationSample.kt.md), [SwipeDismissableSceneStrategySample](references/navigation3/SwipeDismissableSceneStrategySample.kt.md), [SwipeToRevealSample](references/material3/SwipeToRevealSample.kt.md), [TransformationSpecSample](references/material3/TransformationSpecSample.kt.md), [TransformingLazyColumnNotificationsSample](references/material3/TransformingLazyColumnNotificationsSample.kt.md), [TransformingLazyColumnSample](references/foundation/TransformingLazyColumnSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `timeTextSeparator` | [ScrollAwaySample](references/material3/ScrollAwaySample.kt.md), [TimeTextSample](references/material3/TimeTextSample.kt.md) |

#### Foundation components in `androidx.wear.compose.foundation.*`

| Component / Symbol | Reference Samples |
|---|---|
| `AmbientMode`, `LocalAmbientModeManager`, `rememberAmbientModeManager` | [AmbientModeSample](references/foundation/AmbientModeSample.kt.md), [OneHandedGestureSamples](references/material3/OneHandedGestureSamples.kt.md) |
| `AmbientTickEffect` | [AmbientModeSample](references/foundation/AmbientModeSample.kt.md) |
| `BasicSwipeToDismissBox` | [SwipeToDismissBoxSample](references/foundation/SwipeToDismissBoxSample.kt.md) |
| `CurvedAlignment`, `CurvedTextStyle`, `angularGradientBackground`, `angularSize`, `basicCurvedText`, `clearAndSetSemantics`, `curvedColumn`, `padding`, `radialGradientBackground`, `radialSize`, `semantics`, `size` | [CurvedWorldSample](references/foundation/CurvedWorldSample.kt.md) |
| `CurvedDirection`, `CurvedLayout`, `angularSizeDp`, `background`, `curvedBox`, `curvedComposable`, `curvedRow` | [CurvedTextSamples](references/material3/CurvedTextSamples.kt.md), [CurvedWorldSample](references/foundation/CurvedWorldSample.kt.md) |
| `CurvedModifier` | [CurvedTextSamples](references/material3/CurvedTextSamples.kt.md), [CurvedWorldSample](references/foundation/CurvedWorldSample.kt.md), [TimeTextSample](references/material3/TimeTextSample.kt.md) |
| `HorizontalPager`, `VerticalPager`, `rememberPagerState` | [OneHandedGestureSamples](references/material3/OneHandedGestureSamples.kt.md), [PageIndicatorSample](references/material3/PageIndicatorSample.kt.md), [PagerSamples](references/foundation/PagerSamples.kt.md), [PagerScaffoldSample](references/material3/PagerScaffoldSample.kt.md) |
| `PagerDefaults` | [PagerScaffoldSample](references/material3/PagerScaffoldSample.kt.md) |
| `RotaryScrollableDefaults` | [PagerScaffoldSample](references/material3/PagerScaffoldSample.kt.md), [RotarySamples](references/foundation/RotarySamples.kt.md), [TransformingLazyColumnSample](references/foundation/TransformingLazyColumnSample.kt.md) |
| `RotarySnapLayoutInfoProvider`, `rotaryScrollable` | [RotarySamples](references/foundation/RotarySamples.kt.md) |
| `ScrollInfoProvider` | [ScrollAwaySample](references/material3/ScrollAwaySample.kt.md) |
| `SwipeToDismissValue`, `edgeSwipeToDismiss`, `rememberSwipeToDismissBoxState` | [SwipeToDismissBoxSample](references/foundation/SwipeToDismissBoxSample.kt.md), [SwipeToDismissBoxSample](references/material3/SwipeToDismissBoxSample.kt.md) |
| `TransformingLazyColumn` | [DynamicColorSchemeSample](references/material3/DynamicColorSchemeSample.kt.md), [ListHeaderSample](references/material3/ListHeaderSample.kt.md), [OneHandedGestureSamples](references/material3/OneHandedGestureSamples.kt.md), [ScaffoldSample](references/material3/ScaffoldSample.kt.md), [ScrollAwaySample](references/material3/ScrollAwaySample.kt.md), [ScrollIndicatorSample](references/material3/ScrollIndicatorSample.kt.md), [SurfaceTransformationSample](references/material3/SurfaceTransformationSample.kt.md), [SwipeDismissableSceneStrategySample](references/navigation3/SwipeDismissableSceneStrategySample.kt.md), [SwipeToRevealSample](references/material3/SwipeToRevealSample.kt.md), [TransformationSpecSample](references/material3/TransformationSpecSample.kt.md), [TransformingLazyColumnNotificationsSample](references/material3/TransformingLazyColumnNotificationsSample.kt.md), [TransformingLazyColumnSample](references/foundation/TransformingLazyColumnSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `TransformingLazyColumnDefaults` | [TransformingLazyColumnSample](references/foundation/TransformingLazyColumnSample.kt.md) |
| `TransformingLazyColumnItemScrollProgress` | [TransformationSpecSample](references/material3/TransformationSpecSample.kt.md) |
| `expandableButton`, `expandableItem`, `expandableItems`, `rememberExpandableState` | [ExpandableSample](references/foundation/ExpandableSample.kt.md) |
| `hierarchicalFocusGroup` | [HierarchicalFocusSample](references/foundation/HierarchicalFocusSample.kt.md) |
| `items` | [SwipeDismissableSceneStrategySample](references/navigation3/SwipeDismissableSceneStrategySample.kt.md), [SwipeToRevealSample](references/material3/SwipeToRevealSample.kt.md), [TransformingLazyColumnNotificationsSample](references/material3/TransformingLazyColumnNotificationsSample.kt.md) |
| `itemsIndexed` | [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `rememberTransformingLazyColumnState` | [ListHeaderSample](references/material3/ListHeaderSample.kt.md), [OneHandedGestureSamples](references/material3/OneHandedGestureSamples.kt.md), [ScaffoldSample](references/material3/ScaffoldSample.kt.md), [ScrollAwaySample](references/material3/ScrollAwaySample.kt.md), [ScrollIndicatorSample](references/material3/ScrollIndicatorSample.kt.md), [SurfaceTransformationSample](references/material3/SurfaceTransformationSample.kt.md), [SwipeDismissableSceneStrategySample](references/navigation3/SwipeDismissableSceneStrategySample.kt.md), [SwipeToRevealSample](references/material3/SwipeToRevealSample.kt.md), [TransformingLazyColumnNotificationsSample](references/material3/TransformingLazyColumnNotificationsSample.kt.md), [TransformingLazyColumnSample](references/foundation/TransformingLazyColumnSample.kt.md), [TransformingLazyColumnSample](references/material3/TransformingLazyColumnSample.kt.md) |
| `requestFocusOnHierarchyActive` | [HierarchicalFocusSample](references/foundation/HierarchicalFocusSample.kt.md), [RotarySamples](references/foundation/RotarySamples.kt.md) |
| `weight` | [CurvedWorldSample](references/foundation/CurvedWorldSample.kt.md), [TimeTextSample](references/material3/TimeTextSample.kt.md) |

#### Navigation 3 components in `androidx.wear.compose.navigation3.*`

| Component / Symbol | Reference Samples |
|---|---|
| `rememberSwipeDismissableSceneStrategy` | [SwipeDismissableSceneStrategySample](references/navigation3/SwipeDismissableSceneStrategySample.kt.md) |

### Capability 3: Component guidance

**Mandatory**: Use this capability as a checklist against any component use. It
provides more holistic guidance on how to use each component in practice, beyond
the component syntax.

1. `AppScaffold` and `ScreenScaffold`
   - \[ \] Use `AppScaffold` as the outer container, with `ScreenScaffold` children.
   - \[ \] Use only **ONE** `AppScaffold` and any number of `ScreenScaffold`.
2. `ScalingLazyColumn` - Use `TransformingLazyColumn` instead.
3. `TransformingLazyColumn` - You will need the following imports:


   ```kotlin
   import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
   import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
   import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
   // ...
   import androidx.wear.compose.material3.lazy.rememberTransformationSpec
   import androidx.wear.compose.material3.lazy.transformedHeight
   ```

   <br />

   **Canonical example**:


   ```kotlin
   val columnState = rememberTransformingLazyColumnState()
   val transformationSpec = rememberTransformationSpec()
   ScreenScaffold(
       scrollState = columnState
   ) { contentPadding ->
       TransformingLazyColumn(
           state = columnState,
           contentPadding = contentPadding
       ) {
           item {
               ListHeader(
                   modifier = Modifier
                       .fillMaxWidth()
                       .transformedHeight(this, transformationSpec)
                       .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                   transformation = SurfaceTransformation(transformationSpec)
               ) {
                   Text(text = "Header")
               }
           }
           // ... other items
           item {
               Button(
                   modifier = Modifier
                       .fillMaxWidth()
                       .transformedHeight(this, transformationSpec)
                       .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                   transformation = SurfaceTransformation(transformationSpec),
                   onClick = { /* ... */ },
                   icon = {
                       Icon(
                           imageVector = Icons.Default.Build,
                           contentDescription = "build",
                       )
                   },
               ) {
                   Text(
                       text = "Build",
                       maxLines = 1,
                       overflow = TextOverflow.Ellipsis,
                   )
               }
           }
       }
   }
   ```

   <br />

   - \[ \] Use `TransformingLazyColumn` instead of `ScalingLazyColumn`.
   - \[ \] You must pass the `contentPadding` parameter from `ScreenScaffold` to the `TransformingLazyColumn`.
   - \[ \] Use the `minimumVerticalContentPadding` modifier to achieve required padding top and bottom.
     - This expects a value from defaults, such as `ButtonDefaults`, `CardDefaults`, `ListHeaderDefaults`.
     - Note: This is a scoped modifier available within `TransformingLazyColumnItemScope`.
   - \[ \] Ensure items morph and scale (apply to components **within** `TransformingLazyColumn`, not the list itself):
     - \[ \] Use `Modifier.transformedHeight(this, transformationSpec)`.
     - \[ \] Use `transformation = SurfaceTransformation(transformationSpec)`.
   - \[ \] If configuring a list for snapping, use `flingBehavior` and `rotaryScrollableBehavior` **together**:


   ```kotlin
   val columnState = rememberTransformingLazyColumnState()
   ScreenScaffold(scrollState = columnState) { contentPadding ->
       TransformingLazyColumn(
           state = columnState,
           flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(columnState),
           rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(columnState)
       ) {
           // ...
           // ...
       }
   }
   ```

   <br />

4. `ScreenScaffold`

   - \[ \] Guard the `scrollIndicator` with `!LocalScrollCaptureInProgress.current` (`androidx.compose.ui.platform`).
5. `EdgeButton`

   - \[ \] Do **NOT** use as the final item within a `TransformingLazyColumn`. Instead, use the slot in `ScreenScaffold`.
   - \[ \] When used in a `TransformingLazyColumn`, add the required overscroll behavior:


   ```kotlin
   val columnState = rememberTransformingLazyColumnState()
   ScreenScaffold(
       scrollState = columnState,
       edgeButton = {
           EdgeButton(
               onClick = { /* TODO */ },
               modifier = Modifier.scrollable(
                   columnState,
                   orientation = Orientation.Vertical,
                   reverseDirection = true,
                   // Apply overscroll to the EdgeButton for proper scrolling behavior.
                   overscrollEffect = rememberOverscrollEffect(),
               )
           ) {
               Text("More")
           }
       }
   ) { contentPadding ->
       TransformingLazyColumn(
           contentPadding = contentPadding,
           state = columnState,
       ) {
           // ...
           // ...
       }
   }
   ```

   <br />

6. `Column`

   - \[ \] USE as a direct child of `ScreenScaffold` *if* the screen will **never** scroll, even with the largest system font.
   - \[ \] Use `TransformingLazyColumn` instead for all other cases.
7. Styles

   - \[ \] Do **NOT** hard-code text sizes, use `typography` from `MaterialTheme`.
   - \[ \] Do **NOT** hard-code colors, use `colorScheme` from `MaterialTheme`.
8. Use component defaults:

   - \[ \] Components such as `Button` have a corresponding `ButtonDefaults` object.
   - \[ \] Check for and use the `*Defaults` object for any component when working with padding and styling values, in preference to hard-coded values.
9. Use Wear-specific preview annotations
   (`androidx.wear.compose.ui.tooling.preview`):

   - \[ \] `@WearPreviewDevices`
   - \[ \] `@WearPreviewFontScales`
   - \[ \] Ensure the `androidx.wear.compose:compose-ui-tooling` dependency is included.
10. Ambient mode

    - \[ \] Use `LocalAmbientModeManager` (provided by `CompositionLocalProvider` and `rememberAmbientModeManager()`) instead of `AmbientLifecycleObserver`.
11. Navigation

    - \[ \] When adding navigation fresh, use Navigation3 (`androidx.wear.compose:compose-navigation3`).
    - \[ \] Use `rememberSwipeDismissableSceneStrategy()` / `SwipeDismissableSceneStrategy` (`androidx.wear.compose.navigation3`).
12. Comments

    - \[ \] Where any Kotlin file has been modified, ensure that the existing comments are up to date and accurately reflect any changes to the implementation.
13. `HorizontalPager` or `VerticalPager`

    - \[ \] Use the Composable hierarchy in this order: `AppScaffold`, `HorizontalPagerScaffold`, `HorizontalPager`, `AnimatedPage`, `ScreenScaffold`. Or similarly for `VerticalPager`.
