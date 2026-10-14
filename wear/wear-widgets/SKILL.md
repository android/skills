---
name: wear-widgets
description: Create, migrate, update, and verify Wear OS widgets using Glance Wear
  and Remote Compose in an Android project. Use this skill when implementing GlanceWearWidget,
  GlanceWearWidgetService, WearWidgetDocument, Remote Compose UI primitives (RemoteColumn,
  RemoteRow, RemoteBox, RemoteText, RemoteImage, RemoteButton), local state with rememberMutableRemoteInt
  and valueChange, click actions with pendingIntentAction, widget provider XML containers
  (SMALL and LARGE), rectangular widget picker PNG previews, or migrating legacy ProtoLayout
  TileService tiles to Wear OS widgets. Don't use for mobile home screen AppWidgetProvider
  or GlanceAppWidget implementations, watch face complications, or full-screen Wear
  OS activities.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-10'
  keywords:
  - Wear OS
  - Widgets
  - GlanceWearWidget
  - Remote Compose
  - TileService
---

Wear OS widgets use `androidx.glance.wear` (`GlanceWearWidget` and
`GlanceWearWidgetService`) with `androidx.compose.remote` (Remote Compose).
They serialize declarative UI and state documents to the watch host process.

## Prerequisites and setup

1. **Resolve library versions** : Resolve the latest versions for `{GLANCE_WEAR_VERSION}` (`androidx.glance.wear`), `{COMPOSE_REMOTE_VERSION}` (`androidx.compose.remote`), and `{WEAR_COMPOSE_REMOTE_VERSION}` (`androidx.wear.compose.remote`). Include `-alpha`, `-beta`, or `-rc` releases when no stable version exists yet. Don't run `./gradlew dependencies` to resolve versions:
   - Use an available dependency lookup tool if present.
   - Otherwise, fetch the latest version from Google Maven metadata ([Glance Wear metadata](https://dl.google.com/dl/android/maven2/androidx/glance/wear/wear/maven-metadata.xml), [Compose Remote metadata](https://dl.google.com/dl/android/maven2/androidx/compose/remote/remote-creation-compose/maven-metadata.xml), [Wear Compose Remote metadata](https://dl.google.com/dl/android/maven2/androidx/wear/compose/remote/remote-material3/maven-metadata.xml)).
2. **SDK and Compose compiler requirements** : Ask permission before modifying repo-wide Gradle files unless the user requested widget creation, tile migration, or dependency setup. Set `compileSdk = 37` or higher and enable `buildFeatures { compose = true }`. Keep the project's existing Android and Kotlin Gradle plugin declarations intact.

<br />

    dependencies {
        implementation("androidx.glance.wear:wear:{GLANCE_WEAR_VERSION}")
        implementation("androidx.glance.wear:wear-core:{GLANCE_WEAR_VERSION}")
        implementation("androidx.glance.wear:wear-tooling-preview:{GLANCE_WEAR_VERSION}")
        implementation("androidx.compose.remote:remote-creation-compose:{COMPOSE_REMOTE_VERSION}")
        implementation("androidx.compose.remote:remote-creation:{COMPOSE_REMOTE_VERSION}")
        implementation("androidx.compose.remote:remote-core:{COMPOSE_REMOTE_VERSION}")
        implementation("androidx.compose.remote:remote-tooling-preview:{COMPOSE_REMOTE_VERSION}")
        implementation("androidx.wear.compose.remote:remote-material3:{WEAR_COMPOSE_REMOTE_VERSION}")
    }

## Codebase exploration

1. Search the module for existing `TileService`, `Material3TileService`, or `GlanceWearWidget` implementations, `<service>` entries in `app/src/main/AndroidManifest.xml`, and `@drawable/` preview assets under `app/src/main/res/`.
2. When migrating an existing tile or modifying an unfamiliar multi-module project, run `./gradlew :app:assembleDebug` to verify baseline build health.

## Workflow

### Step 1: Configure AndroidManifest.xml and provider XML

Register the `GlanceWearWidgetService` subclass in
`app/src/main/AndroidManifest.xml` with the `BIND_TILE_PROVIDER` permission and
`BIND_WIDGET_PROVIDER` intent action. Attach `<meta-data>` pointing to an
`@xml/` `<wearwidget-provider>` resource with `SMALL` and `LARGE` `<container>`
entries. When configuring the service, provider XML, or legacy `TileService`
migration, you MUST follow [Widget service and manifest reference](references/widget-service-and-manifest.md).

<br />

    <service
      android:name=".MyWidgetService"
      android:exported="true"
      android:permission="com.google.android.wearable.permission.BIND_TILE_PROVIDER"
      >
      <intent-filter>
        <action android:name="androidx.glance.wear.action.BIND_WIDGET_PROVIDER" />
      </intent-filter>
      <meta-data
        android:name="androidx.glance.wear.widget.provider"
        android:resource="@xml/my_widget_info" />
    </service>

### Step 2: Implement GlanceWearWidget and GlanceWearWidgetService

Extend `GlanceWearWidget` and override `provideWidgetData` returning
`WearWidgetDocument`. Annotate the `GlanceWearWidgetService` subclass with
`@AssociateWithGlanceWearWidget` for static provider resolution:

<br />

    // WRONG: Mobile GlanceAppWidget API (don't use on Wear OS)
    class BrokenWidget : GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) {}
    }

    // CORRECT: Wear OS GlanceWearWidget + GlanceWearWidgetService
    @AssociateWithGlanceWearWidget(MyWidget::class)
    class MyWidgetService : GlanceWearWidgetService() {
        override val widget: GlanceWearWidget = MyWidget()
    }

    class MyWidget : GlanceWearWidget() {
        override suspend fun provideWidgetData(
            context: Context,
            params: WearWidgetParams,
        ): WearWidgetData = WearWidgetDocument(
            background = WearWidgetBrush.color(Color(0xFF1C1B1F).rc)
        ) {
            MyWidgetContent(params = params)
        }
    }

### Step 3: Build Remote Compose layouts, state, and actions

Remote Compose evaluates UI and state inside the Wear OS host process rather
than the app process. When building adaptive layouts or full-bleed backgrounds,
you MUST follow [Remote Compose layouts reference](references/remote-compose-layouts.md). When implementing state
or click actions, you MUST follow [State and actions reference](references/state-and-actions.md).

- **Remote value wrappers** : Convert primitives using `.rs` (`RemoteString`), `.rc` (`RemoteColor`), `.rdp` (`RemoteDp`), and `.ri` (`RemoteInt`). Format integers with the `RemoteInt.toRemoteString` member method.
- **Adaptive container sizing** : Branch layout density, typography, and item counts on `params.containerType` (`ContainerInfo.CONTAINER_TYPE_SMALL` versus `ContainerInfo.CONTAINER_TYPE_LARGE`).
- **Unmasked full-bleed backgrounds** : Render background bitmaps inside a root `RemoteBox` (`RemoteModifier.fillMaxSize`) using `RemoteImage` and `RemoteImageBitmap` (`bitmap.asImageBitmap`). Keep background artwork unmasked (`cornerRadius = 0dp`) without `CircleShape` or `RoundedCornerShape` clipping. The host clips edges to the watch shape.
- **Declarative host state and PendingIntent actions** : Mutate local widget state with `rememberMutableRemoteInt` and `valueChange(state, newValue)` instead of in-memory Compose state (`mutableStateOf` or `mutableIntStateOf`). Trigger activity launches or background broadcasts using `pendingIntentAction { ctx -> ... }` (always passing a `(Context) -> PendingIntent` lambda). Refresh active widgets with `widget.triggerUpdateAll(context)`.

### Step 4: Add WearWidgetPreview functions and picker preview PNGs

1. **Composable border previews (`androidx.glance.wear.tooling.preview`)** : Import `WearWidgetPreview`, `SquircleAllWidgetPreviewParams`, `RoundAllWidgetPreviewParams`, and `RectangularAllWidgetPreviewParams` from `androidx.glance.wear.tooling.preview` (**NEVER** `androidx.glance.wear.testing`). Define `@Preview` functions invoking `WearWidgetPreview(widget, params)`.
2. **Rectangular picker PNGs (`res/drawable-nodpi/`)** : Use `RectangularAllWidgetPreviewParams` at `320 dpi` or render non-blank, unmasked rectangular PNGs for every `<container>` in the provider XML:
   - `SMALL` (`CONTAINER_TYPE_SMALL`): **`448x168` px** (`224x84` dp at 320 dpi, aspect ratio `2.67`).
   - `LARGE` (`CONTAINER_TYPE_LARGE`): **`464x288` px** (`232x144` dp at 320 dpi, aspect ratio `1.61`).
   - Each preview PNG **MUST** depict the widget's actual UI elements, including backgrounds, text labels, icons, avatar initials, and buttons. **NEVER** use a blank placeholder, empty unlabeled shapes, or a 1:1 tile preview. When adding composable previews, picker PNGs, or unit tests, you MUST follow [Previews and testing reference](references/previews-and-testing.md).

## Component mapping

Use this sample table to load complete implementation patterns and imports:

| Capability or API surface | Reference guide |
|---|---|
| `GlanceWearWidget`, `GlanceWearWidgetService`, `AndroidManifest.xml`, `<wearwidget-provider>`, legacy `TileService` migration | [Widget service and manifest reference](references/widget-service-and-manifest.md) |
| `RemoteBox`, `RemoteColumn`, `RemoteRow`, `RemoteText`, `RemoteImage`, `ContainerInfo` (`SMALL` and `LARGE` adaptive layouts) | [Remote Compose layouts reference](references/remote-compose-layouts.md) |
| `RemoteButton`, `rememberMutableRemoteInt`, `valueChange`, `pendingIntentAction`, `BroadcastReceiver`, `triggerUpdateAll` | [State and actions reference](references/state-and-actions.md) |
| `WearWidgetPreview`, `SquircleAllWidgetPreviewParams`, `RoundAllWidgetPreviewParams`, `RectangularAllWidgetPreviewParams`, picker PNGs, `captureRawContent` tests | [Previews and testing reference](references/previews-and-testing.md) |

### Legacy ProtoLayout and mobile Glance equivalents

| Legacy or mobile API | Wear OS widget API | Action to take |
|---|---|---|
| `androidx.wear.tiles.TileService` | `GlanceWearWidgetService` + `GlanceWearWidget` | Replace service and annotate with `@AssociateWithGlanceWearWidget`. |
| `GlanceAppWidget` / `provideGlance` | `GlanceWearWidget` / `provideWidgetData` | Return `WearWidgetDocument` from `provideWidgetData`. |
| `LayoutElementBuilders` / `primaryLayout` | `RemoteColumn`, `RemoteRow`, `RemoteBox` | Build layouts with `@RemoteComposable` functions. |
| `mutableStateOf` / `mutableIntStateOf` | `rememberMutableRemoteInt` + `valueChange` | Mutate remote state declaratively using `valueChange`. |
| `ModifiersBuilders.Clickable` | `RemoteModifier.clickable(pendingIntentAction { ... })` | Pass a `(Context) -> PendingIntent` lambda to `pendingIntentAction`. |

## Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Unresolved reference `GlanceWearWidget` | Missing `androidx.glance.wear:wear:{GLANCE_WEAR_VERSION}` or low `compileSdk` | Set `compileSdk = 37` and add `androidx.glance.wear:wear:{GLANCE_WEAR_VERSION}`. |
| Unresolved reference `toRemoteString` | Imported `toRemoteString` as top-level extension | Remove the import; call `remoteInt.toRemoteString` as a member method. |
| Type mismatch on `pendingIntentAction` | Passed bare `PendingIntent` instead of lambda | Wrap in a lambda: `pendingIntentAction { ctx -> pendingIntent }`. |
| Unresolved `wear.tiles` or `protolayout` references after migration | Other classes or source sets in the module still use legacy tile APIs | Migrate or update the remaining references before removing legacy `wear.tiles` and `protolayout` dependencies from `build.gradle.kts`. |

## Verification

1. Run `./gradlew :app:assembleDebug` to verify that the widget, service, `AndroidManifest.xml` declarations, and `@xml/` `<wearwidget-provider>` resources compile and link without errors.
2. If the project includes unit tests or new widget tests, run `./gradlew :app:testDebugUnitTest` and confirm all tests pass. When adding composable previews, picker PNGs, or unit tests, you MUST follow [Previews and testing reference](references/previews-and-testing.md).

## Antipatterns

- **NEVER** extend mobile `GlanceAppWidget` or `AppWidgetProvider`, or use deprecated `GlanceTileService` or `TileService` for Wear OS widgets.
- **NEVER** use in-memory client Compose state (`mutableStateOf`, `mutableIntStateOf`) or in-process click lambdas for Remote Compose UI.
- **NEVER** pass a bare `PendingIntent` instance to `pendingIntentAction`; always pass a `(Context) -> PendingIntent` lambda.
- **NEVER** apply `CircleShape` or `RoundedCornerShape` clipping to full-bleed widget background artwork. **NEVER** reuse 1:1 (`400x400`) tile previews or blank solid-color placeholders for widget picker PNGs.

## Best practices

- **MUST** declare `com.google.android.wearable.permission.BIND_TILE_PROVIDER` and `androidx.glance.wear.action.BIND_WIDGET_PROVIDER` on `<service>`.
- **MUST** declare `SMALL` and `LARGE` `<container>` entries in `@xml/` with domain-accurate `448x168` and `464x288` PNGs in `res/drawable-nodpi/`.
- **MUST** adapt layout content to `params.containerType` and include `@Preview` functions for both `SquircleAllWidgetPreviewParams` and `RoundAllWidgetPreviewParams`.
