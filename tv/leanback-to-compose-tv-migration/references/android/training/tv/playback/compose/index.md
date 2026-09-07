[Video](https://www.youtube.com/watch?v=_X4tswgV67Y)

Compose for TV is the modern approach for building Android TV user
interfaces. Compose for TV unlocks all the benefits of Android's
Jetpack Compose for your TV apps, making building beautiful and functional UIs
for your app much easier.

Some specific benefits of using Compose for TV include the
following:

- **Flexibility**: Compose can be used to create any type of UI, from basic layouts to complex animations. Components work out of the box but can also be customized and styled to fit your app's needs.
- **Simplified \& Accelerated Development**: Compose is compatible with existing code and enables developers to more efficiently build apps with less code.
- **Intuitive**: Compose uses a declarative syntax that lets you to make changes to your UI, debug, understand and review your code.

If you are unfamiliar with using the Jetpack Compose toolkit, check out the
[Compose pathway](https://developer.android.com/courses/pathways/compose). Many of the development principles for mobile Compose
apply to TV as well. See [Why Compose](https://developer.android.com/jetpack/compose/why-adopt) for more information about the general
advantages of a declarative UI framework. To learn more, also see [the Compose
for TV samples repository on GitHub](https://github.com/android/tv-samples/).

<br />


## AI Prompt

### Learn how to add support for Android TV

This prompt asks for guidance around adding Android TV support to your app using Jetpack Compose for TV instead of Leanback.

    How do I add support for Android TV to my application?

    <br />




    `Use Jetpack Compose for TV as part of the response instead of
    Leanback.`

### Using AI prompts

AI prompts are intended to be used within Gemini in Android Studio.

Learn more about Gemini in Studio here: [https://developer.android.com/studio/gemini/overview](https://developer.android.com/studio/gemini/overview)
<button class="devsite-dialog-close">Close</button> <button class="button icon-button android-ai-prompt-help-button" data-modal-dialog-id="ai-prompt_help_modal__learn-how-to-add-support-for-android-tv"> </button> <button class="button google-feedback" data-p="5207477" data-b="llm-prompts" data-context="learn-how-to-add-support-for-android-tv"> Share your thoughts </button>

<br />

## Compatibility

Compose for TV works on Android TVs with Android 5.0 (API level 21)
or higher. Using version 1.0 of Compose for TV requires version
1.3.0 of [androidx.compose](https://developer.android.com/jetpack/androidx/releases/compose) libraries and Kotlin 1.7.10.

## Setup

Using Jetpack Compose on Android TV is similar to using Jetpack Compose for any
other Android project. The main difference is that Compose for TV
adds libraries that offer TV-optimized components and make it easier to create
user interfaces tailored to TV. In some cases those components share the same
name as their non-TV counterparts, such as [`androidx.tv.material3.Button`](https://developer.android.com/reference/kotlin/androidx/tv/material3/package-summary#Button(kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.tv.material3.ButtonScale,androidx.tv.material3.ButtonGlow,androidx.tv.material3.ButtonShape,androidx.tv.material3.ButtonColors,androidx.compose.ui.unit.Dp,androidx.tv.material3.ButtonBorder,androidx.compose.foundation.layout.PaddingValues,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Function1))
and [`androidx.compose.material3.Button`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Button.composable#Button(kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.ui.graphics.Shape,androidx.compose.material3.ButtonColors,androidx.compose.material3.ButtonElevation,androidx.compose.foundation.BorderStroke,androidx.compose.foundation.layout.PaddingValues,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Function1)).

## Jetpack Compose toolkit dependencies

To use Compose for TV, include Jetpack Compose toolkit dependencies
in your app's `build.gradle` file as follows:

### Kotlin (build.gradle.kts)

    dependencies {
       val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
       implementation(composeBom)

       // General compose dependencies.
       implementation("androidx.activity:activity-compose:1.13.0")

       implementation("androidx.compose.ui:ui-tooling-preview")
       debugImplementation("androidx.compose.ui:ui-tooling")

       // Compose for TV dependencies.
       implementation("androidx.tv:tv-material:1.0.0")
    }

### Groovy (build.gradle)

    dependencies {
       def composeBom = platform('androidx.compose:compose-bom:2026.08.00')
       implementation composeBom

       // General compose dependencies.
       implementation 'androidx.activity:activity-compose:1.13.0'

       implementation 'androidx.compose.ui:ui-tooling-preview'
       debugImplementation 'androidx.compose.ui:ui-tooling'

       // Compose for TV dependencies.
       implementation 'androidx.tv:tv-material:1.0.0'
    }

## What's different

The TV material components are designed for the living room, with **clear focus
indicators** and **remote-friendly input behavior** . For details on how
to use these specific components, check the [TV UI design guides](https://developer.android.com/design/ui/tv/guides/components).
![](https://developer.android.com/static/training/tv/images/tv-material.svg) **Figure 1.** Sample components from the TV material library.

Use the [TV version](https://developer.android.com/reference/kotlin/androidx/tv/material3/package-summary) of APIs wherever possible to benefit from
these features.

While it is technically possible to use the mobile version of Compose Material,
it is not optimized for the unique style of interactions on Android TV. In
addition, mixing Compose Material with Compose Material from
Compose for TV can result in unexpected behavior. For example,
because each library has its own `MaterialTheme` object, there's the possibility
of colors, typography, or shapes being inconsistent if both versions are used.

The following table outlines the dependency differences between TV and
Mobile:

| **TV Dependency** (androidx.tv.\*) | **Comparison** | **Mobile Dependency** (androidx.compose.\*) |
|---|---|---|
| [androidx.tv:tv-material](https://developer.android.com/reference/kotlin/androidx/tv/material3/package-summary) | ***instead of*** | androidx.compose.material3:material3 |

## Additional resources

- [TV design guides](https://developer.android.com/design/ui/tv/guides/components)   

  An overview of dedicated TV components for building user interfaces with
  links to relevant developer resources.

- [TV Material Catalog sample](https://github.com/android/tv-samples/tree/main/TvMaterialCatalog)   

  A catalog app that demonstrates how to implement
  [Material Design](https://www.material.io) principles using Compose for TV.

- [JetStream sample](https://github.com/android/tv-samples/tree/main/JetStreamCompose)   

  A media streaming app that demonstrates the use of TV Compose with a
  typical Material app and real-world architecture.

- [Introduction to Compose for TV](https://developer.android.com/codelabs/compose-for-tv-introduction)   

  This codelab steps through building a video-player app with a catalog-browser
  screen and a details screen.

## Further reading

Explore these guides to learn about building great TV-optimized experiences for:

- [Create a catalog browser](https://developer.android.com/training/tv/playback/compose/browse)
- [Build a details screen](https://developer.android.com/training/tv/playback/compose/details)
- [Create scrollable layouts](https://developer.android.com/training/tv/playback/compose/lists)