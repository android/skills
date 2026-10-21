## 1. GlanceWearWidget and GlanceWearWidgetService

Every Wear OS widget requires two classes:

1. A `GlanceWearWidget` subclass overriding `provideWidgetData(context, params)` and returning a `WearWidgetDocument`.
2. A `GlanceWearWidgetService` subclass annotated with `@AssociateWithGlanceWearWidget` so build tools, linters, and `GlanceWearWidgetManager` resolve the widget provider statically without reflective instantiation.

```kotlin
import android.content.Context
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.ui.graphics.Color
import androidx.glance.wear.AssociateWithGlanceWearWidget
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.GlanceWearWidgetService
import androidx.glance.wear.WearWidgetBrush
import androidx.glance.wear.WearWidgetData
import androidx.glance.wear.WearWidgetDocument
import androidx.glance.wear.color
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams


@AssociateWithGlanceWearWidget(MyWidget::class)
class MyWidgetService : GlanceWearWidgetService() {
    override val widget: GlanceWearWidget = MyWidget()
}

class MyWidget : GlanceWearWidget() {
    override suspend fun provideWidgetData(
        context: Context,
        params: WearWidgetParams,
    ): WearWidgetData {
        val isLarge = params.containerType == ContainerInfo.CONTAINER_TYPE_LARGE
        return WearWidgetDocument(
            background = WearWidgetBrush.color(Color(0xFF1C1B1F).rc)
        ) {
            MyWidgetContent(isLarge = isLarge)
        }
    }
}
```

## 2. AndroidManifest.xml registration

Register the `GlanceWearWidgetService` subclass in
`app/src/main/AndroidManifest.xml`. The `<service>` tag **MUST** declare the
`com.google.android.wearable.permission.BIND_TILE_PROVIDER` permission and the
`androidx.glance.wear.action.BIND_WIDGET_PROVIDER` intent filter. Include the
`androidx.glance.wear.widget.provider` `<meta-data>` pointing to the `@xml/`
provider configuration file:

```xml
<service
    android:name=".snippets.widget.skill.MyWidgetService"
    android:exported="true"
    android:permission="com.google.android.wearable.permission.BIND_TILE_PROVIDER">
    <intent-filter>
        <action android:name="androidx.glance.wear.action.BIND_WIDGET_PROVIDER" />
    </intent-filter>
    <meta-data
        android:name="androidx.glance.wear.widget.provider"
        android:resource="@xml/my_widget_info" />
</service>
```

## 3. Provider XML (`res/xml/<widget_name>_info.xml`)

Create the `<wearwidget-provider>` XML resource in `app/src/main/res/xml/`
declaring `SMALL` and `LARGE` `<container>` elements with `@drawable/` picker
preview images:

```xml
<wearwidget-provider
    xmlns:android="http://schemas.android.com/apk/res/android"
    label="@string/app_name"
    description="@string/app_name"
    preferredType="LARGE">
    <container
        type="SMALL"
        previewImage="@drawable/my_widget_preview_small" />
    <container
        type="LARGE"
        previewImage="@drawable/my_widget_preview_large" />
</wearwidget-provider>
```

## 4. Migrating a legacy ProtoLayout TileService

When migrating a legacy `androidx.wear.tiles.TileService` (or
`Material3TileService`) to a Glance Wear widget:

1. **Replace the service and manifest registration** : For a full replacement migration, remove the legacy `TileService` class. Replace the legacy `<service>` registration (`BIND_TILE_PROVIDER` and `androidx.wear.tiles.PREVIEW`) in `app/src/main/AndroidManifest.xml` with the `GlanceWearWidgetService` declaration. Don't delete existing `res/drawable*/` image assets such as legacy tile preview PNGs. Add new `448x168` and `464x288` `res/drawable-nodpi/` widget picker PNGs alongside existing drawables. Depict the migrated widget's actual UI layout, such as contact avatar badges with initials and a bottom action button. **NEVER** render generic `"Small"` or `"Large"` text labels as preview images. For backward-compatible coexistence with a legacy tile, set `group="<FullyQualifiedLegacyTileService>"` on `<wearwidget-provider>` and add `androidx.wear.tiles.GROUP` `<meta-data>`.
2. **Remove legacy tile dependencies** : After migrating the legacy `TileService` and ProtoLayout code, remove unused `androidx.wear.tiles` and `androidx.wear.protolayout.*` dependencies from `app/build.gradle.kts`. Keep existing Android and Kotlin Gradle plugin configurations intact. If other tiles, previews, or source sets still depend on ProtoLayout, retain or scope those dependencies. Verify that `./gradlew :app:assembleDebug` compiles without errors and without deleting unrelated app features.