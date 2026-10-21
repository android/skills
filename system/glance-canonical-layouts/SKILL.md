---
name: glance-canonical-layouts
description: Use this skill to create home screen widgets using Jetpack Glance Canonical
  Layouts from the Android Platform Samples project, or when asked to implement standard
  widget layout patterns (including Check List, Image Grid, Snap Scrolling, and Standard
  Toolbar).
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-13'
  keywords:
  - android
  - glance
  - widgets
  - appwidget
  - canonical-layouts
  - platform-samples
  - action-list
  - check-list
  - image-grid
  - image-text-list
  - snap-scrolling
  - full-bleed
  - expressive-toolbar
  - search-toolbar
  - standard-toolbar
---

To implement a widget Canonical Layout that follows the recommended design
patterns:

1. The project **MUST** use Jetpack Glance.
2. You **MUST** locate the requested layout from the list under **Canonical Layout Reference Files**.
3. Copy the layout reference files into your project's target app module (e.g., `src/main/java/` and `src/main/res/`) in one step to implement the exact design and reduce the number of tool calls.
4. You **MUST** set override val `sizeMode: SizeMode = SizeMode.Exact` in the widget provider class.
5. Validate that your project compiles, updating sample package declarations, import paths, and class names to match the target app's package name.
6. Clean your project's widget code, removing unused classes (for example, unused buttons).
7. You **MUST** register the widget receiver in `AndroidManifest.xml` inside `<application>` after `<activity>`, ensuring `android:resource="@xml/..."` matches the filename of the copied Widget Info XML. Otherwise, the widget won't appear in the widget picker:

<br />

```xml
<receiver android:name=".glance.MyReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/my_app_widget_info" />
</receiver>
```

<br />

## Dependencies

Add Jetpack Glance dependencies to the app module's `build.gradle.kts`
(`dependencies` block):

- `androidx.glance:glance-appwidget:1.2.0` or higher
- `androidx.glance:glance-material3:1.2.0` or higher
- `androidx.glance:glance-appwidget-preview:1.2.0` or higher
- `androidx.glance:glance-preview:1.2.0` or higher

Use version `1.3.0-alpha02` or higher with `compileSdk = 37` and Android Gradle
Plugin (AGP) 9.1.0 or higher (for example, `agp = "9.1.0"` in
`gradle/libs.versions.toml`) when implementing Snap Scrolling.

If copying sample data repositories that load bitmaps with Coil (such as
`FakeImageGridDataRepository.kt`, `FakeImageTextListDataRepository.kt`, or
`FakeTextWithImageRepository.kt`), also add an image loading library dependency
such as `io.coil-kt:coil` (e.g., `implementation("io.coil-kt:coil:2.7.0")`) if
one doesn't already exist.

## Canonical layout reference files

#### Action list layout (`action_list`)

- **Widget Provider** : [ActionListAppWidget.kt](references/collections/ActionListAppWidget.kt)
- **Layout Composable** : [ActionListLayout.kt](references/collections/layout/ActionListLayout.kt)
- **Widget Info XML** : [sample_action_list_widget_info.xml](references/res/xml/sample_action_list_widget_info.xml)
- **Data Repository** : [FakeActionListDataRepository.kt](references/collections/data/FakeActionListDataRepository.kt)
- **Dimensions** : [dimens.xml](references/res/values/dimens.xml)
- **Strings** : [strings.xml](references/res/values/strings.xml)
- **Drawables** : [sample_action_list_preview.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable-nodpi/sample_action_list_preview.png), [sample_home_icon.xml](references/res/drawable/sample_home_icon.xml), [sample_power_settings_icon.xml](references/res/drawable/sample_power_settings_icon.xml), [sample_bulb_icon.xml](references/res/drawable/sample_bulb_icon.xml), [sample_thermostat_icon.xml](references/res/drawable/sample_thermostat_icon.xml), [sample_ac_icon.xml](references/res/drawable/sample_ac_icon.xml), [sample_door_icon.xml](references/res/drawable/sample_door_icon.xml), [sample_arrow_right_icon.xml](references/res/drawable/sample_arrow_right_icon.xml), [sample_no_data_icon.xml](references/res/drawable/sample_no_data_icon.xml), [sample_add_icon.xml](references/res/drawable/sample_add_icon.xml)
- **Components** : [RoundedScrollingLazyVerticalGrid.kt](references/collections/layout/RoundedScrollingLazyVerticalGrid.kt), [ListItem.kt](references/collections/layout/ListItem.kt), [VerticalListItem.kt](references/collections/layout/VerticalListItem.kt), [RoundedScrollingLazyColumn.kt](references/collections/layout/RoundedScrollingLazyColumn.kt), [EmptyListContent.kt](references/collections/layout/EmptyListContent.kt), [NoDataContent.kt](references/collections/layout/NoDataContent.kt)
- **Utilities** : [CollectionsKtx.kt](references/CollectionsKtx.kt), [ActionUtils.kt](references/utils/ActionUtils.kt), [PreviewAnnotations.kt](references/utils/PreviewAnnotations.kt)
- **Theme** : [Color.kt](references/ui/theme/Color.kt), [Theme.kt](references/ui/theme/Theme.kt), [Type.kt](references/ui/theme/Type.kt)
- **Additional References** : [CanonicalLayoutActivity.kt](references/CanonicalLayoutActivity.kt), [ActionDemonstrationActivity.kt](references/ActionDemonstrationActivity.kt)
- **Guidance** :
  - Handle list item actions with Glance action callbacks and ensure proper state updates.

#### Check list layout (`check_list`)

- **Widget Provider** : [CheckListAppWidget.kt](references/collections/CheckListAppWidget.kt)
- **Layout Composable** : [CheckListLayout.kt](references/collections/layout/CheckListLayout.kt)
- **Widget Info XML** : [sample_check_list_widget_info.xml](references/res/xml/sample_check_list_widget_info.xml)
- **Data Repository** : [FakeCheckListDataRepository.kt](references/collections/data/FakeCheckListDataRepository.kt)
- **Dimensions** : [dimens.xml](references/res/values/dimens.xml)
- **Strings** : [strings.xml](references/res/values/strings.xml)
- **Drawables** : [sample_check_list_preview.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable-nodpi/sample_check_list_preview.png), [sample_pin_icon.xml](references/res/drawable/sample_pin_icon.xml), [sample_checked_circle_icon.xml](references/res/drawable/sample_checked_circle_icon.xml), [sample_circle_icon.xml](references/res/drawable/sample_circle_icon.xml), [sample_delete_icon.xml](references/res/drawable/sample_delete_icon.xml), [sample_edit_icon.xml](references/res/drawable/sample_edit_icon.xml), [sample_snooze_icon.xml](references/res/drawable/sample_snooze_icon.xml), [sample_no_data_icon.xml](references/res/drawable/sample_no_data_icon.xml), [sample_add_icon.xml](references/res/drawable/sample_add_icon.xml)
- **Components** : [ListItem.kt](references/collections/layout/ListItem.kt), [VerticalListItem.kt](references/collections/layout/VerticalListItem.kt), [RoundedScrollingLazyColumn.kt](references/collections/layout/RoundedScrollingLazyColumn.kt), [EmptyListContent.kt](references/collections/layout/EmptyListContent.kt), [NoDataContent.kt](references/collections/layout/NoDataContent.kt)
- **Utilities** : [CollectionsKtx.kt](references/CollectionsKtx.kt), [ActionUtils.kt](references/utils/ActionUtils.kt), [PreviewAnnotations.kt](references/utils/PreviewAnnotations.kt)
- **Theme** : [Color.kt](references/ui/theme/Color.kt), [Theme.kt](references/ui/theme/Theme.kt), [Type.kt](references/ui/theme/Type.kt)
- **Guidance** :
  - Implement checkbox state toggling using action parameters or callbacks to update collection items.
  - Use Glance Action callbacks (for example, `actionRunCallback`, `ActionCallback(actionParametersOf(...)` or `actionStartActivity(...)`) for widget onClick parameters to handle click events like tapping a checkbox.

#### Expressive toolbar layout (`expressive_toolbar`)

- **Widget Provider** : [ExpressiveToolbarAppWidget.kt](references/toolbars/ExpressiveToolbarAppWidget.kt)
- **Layout Composable** : [ExpressiveToolbarLayout.kt](references/toolbars/layout/ExpressiveToolbarLayout.kt)
- **Widget Info XML** : [sample_expressive_toolbar_widget_info.xml](references/res/xml/sample_expressive_toolbar_widget_info.xml)
- **Dimensions** : [dimens.xml](references/res/values/dimens.xml)
- **Strings** : [strings.xml](references/res/values/strings.xml)
- **Drawables** : [sample_expressive_toolbar_preview.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable-nodpi/sample_expressive_toolbar_preview.png), [four_side_cookie_background.xml](references/res/drawable/four_side_cookie_background.xml), [sample_edit_icon.xml](references/res/drawable/sample_edit_icon.xml), [sample_share_icon.xml](references/res/drawable/sample_share_icon.xml), [sample_delete_icon.xml](references/res/drawable/sample_delete_icon.xml), [sample_info_icon.xml](references/res/drawable/sample_info_icon.xml), [sample_add_icon.xml](references/res/drawable/sample_add_icon.xml), [sample_file_upload_icon.xml](references/res/drawable/sample_file_upload_icon.xml), [sample_mic_icon.xml](references/res/drawable/sample_mic_icon.xml), [sample_camera_icon.xml](references/res/drawable/sample_camera_icon.xml)
- **Components** : [Buttons.kt](references/toolbars/layout/Buttons.kt), [LayoutComponents.kt](references/toolbars/layout/LayoutComponents.kt)
- **Utilities** : [ActionUtils.kt](references/utils/ActionUtils.kt), [PreviewAnnotations.kt](references/utils/PreviewAnnotations.kt)
- **Theme** : [Color.kt](references/ui/theme/Color.kt), [Theme.kt](references/ui/theme/Theme.kt), [Type.kt](references/ui/theme/Type.kt)
- **Guidance** :
  - Avoid `ColorProvider` import errors: import `androidx.glance.unit.ColorProvider` and use `androidx.glance.color.ColorProvider(dayColor, nightColor)`.
  - Use `GlanceModifier.background(Color)` directly for solid background colors rather than `ColorProvider`.

#### Image grid layout (`image_grid`)

- **Widget Provider** : [ImageGridAppWidget.kt](references/collections/ImageGridAppWidget.kt)
- **Layout Composable** : [ImageGridLayout.kt](references/collections/layout/ImageGridLayout.kt)
- **Widget Info XML** : [sample_image_grid_widget_info.xml](references/res/xml/sample_image_grid_widget_info.xml)
- **Data Repository** : [FakeImageGridDataRepository.kt](references/collections/data/FakeImageGridDataRepository.kt)
- **Dimensions** : [dimens.xml](references/res/values/dimens.xml)
- **Strings** : [strings.xml](references/res/values/strings.xml)
- **Drawables** : [sample_image_grid_preview.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable-nodpi/sample_image_grid_preview.png), [sample_placeholder_image.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable/sample_placeholder_image.png), [sample_grid_icon.xml](references/res/drawable/sample_grid_icon.xml), [sample_refresh_icon.xml](references/res/drawable/sample_refresh_icon.xml), [sample_no_data_icon.xml](references/res/drawable/sample_no_data_icon.xml), [sample_add_icon.xml](references/res/drawable/sample_add_icon.xml)
- **Components** : [RoundedScrollingLazyVerticalGrid.kt](references/collections/layout/RoundedScrollingLazyVerticalGrid.kt), [EmptyListContent.kt](references/collections/layout/EmptyListContent.kt), [NoDataContent.kt](references/collections/layout/NoDataContent.kt), [VerticalListItem.kt](references/collections/layout/VerticalListItem.kt)
- **Utilities** : [CollectionsKtx.kt](references/CollectionsKtx.kt), [ImageUtils.kt](references/utils/ImageUtils.kt), [ImageAspectRatio.kt](references/utils/ImageAspectRatio.kt), [PreviewAnnotations.kt](references/utils/PreviewAnnotations.kt)
- **Theme** : [Color.kt](references/ui/theme/Color.kt), [Theme.kt](references/ui/theme/Theme.kt), [Type.kt](references/ui/theme/Type.kt)
- **Guidance** :
  - Use solid colors (`GlanceModifier.background`) or local drawables for image items unless an image loading library is configured or requested.

#### Image text list layout (`image_text_list`)

- **Widget Provider** : [ImageTextListAppWidget.kt](references/collections/ImageTextListAppWidget.kt)
- **Layout Composable** : [ImageTextListLayout.kt](references/collections/layout/ImageTextListLayout.kt)
- **Widget Info XML** : [sample_image_text_list_widget_info.xml](references/res/xml/sample_image_text_list_widget_info.xml)
- **Data Repository** : [FakeImageTextListDataRepository.kt](references/collections/data/FakeImageTextListDataRepository.kt)
- **Dimensions** : [dimens.xml](references/res/values/dimens.xml)
- **Strings** : [strings.xml](references/res/values/strings.xml)
- **Drawables** : [sample_image_text_list_preview.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable-nodpi/sample_image_text_list_preview.png), [sample_placeholder_image.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable/sample_placeholder_image.png), [sample_image_icon.xml](references/res/drawable/sample_image_icon.xml), [sample_heart_icon.xml](references/res/drawable/sample_heart_icon.xml), [sample_refresh_icon.xml](references/res/drawable/sample_refresh_icon.xml), [sample_no_data_icon.xml](references/res/drawable/sample_no_data_icon.xml), [sample_add_icon.xml](references/res/drawable/sample_add_icon.xml)
- **Components** : [ListItem.kt](references/collections/layout/ListItem.kt), [VerticalListItem.kt](references/collections/layout/VerticalListItem.kt), [RoundedScrollingLazyColumn.kt](references/collections/layout/RoundedScrollingLazyColumn.kt), [EmptyListContent.kt](references/collections/layout/EmptyListContent.kt), [NoDataContent.kt](references/collections/layout/NoDataContent.kt)
- **Utilities** : [CollectionsKtx.kt](references/CollectionsKtx.kt), [ImageUtils.kt](references/utils/ImageUtils.kt), [ImageAspectRatio.kt](references/utils/ImageAspectRatio.kt), [PreviewAnnotations.kt](references/utils/PreviewAnnotations.kt)
- **Theme** : [Color.kt](references/ui/theme/Color.kt), [Theme.kt](references/ui/theme/Theme.kt), [Type.kt](references/ui/theme/Type.kt)
- **Guidance** :
  - Common Use Cases: News feeds, article lists, workout histories, message feeds, and vertical thumbnail lists.
  - Balance list row height and text clipping when combining thumbnail images with primary and secondary text.

#### Long text layout (`long_text`)

- **Widget Provider** : [LongTextAppWidget.kt](references/text/LongTextAppWidget.kt)
- **Layout Composable** : [LongTextLayout.kt](references/text/layout/LongTextLayout.kt)
- **Widget Info XML** : [sample_long_text_widget_info.xml](references/res/xml/sample_long_text_widget_info.xml)
- **Data Repository** : [FakeLongTextRespository.kt](references/text/data/FakeLongTextRespository.kt)
- **Dimensions** : [dimens.xml](references/res/values/dimens.xml)
- **Strings** : [strings.xml](references/res/values/strings.xml)
- **Drawables** : [sample_long_text_preview.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable-nodpi/sample_long_text_preview.png), [sample_text_icon.xml](references/res/drawable/sample_text_icon.xml), [sample_refresh_icon.xml](references/res/drawable/sample_refresh_icon.xml)
- **Utilities** : [FontUtils.kt](references/utils/FontUtils.kt), [PreviewAnnotations.kt](references/utils/PreviewAnnotations.kt)
- **Theme** : [Color.kt](references/ui/theme/Color.kt), [Theme.kt](references/ui/theme/Theme.kt), [Type.kt](references/ui/theme/Type.kt)
- **Guidance** :
  - Import `androidx.compose.ui.unit.times` if multiplying Dp (for example, `2 * padding`).
  - Follow sizing rules: ensure dimensions from dimens.xml are used for padding and typography scaling.

#### Search toolbar layout (`search_toolbar`)

- **Widget Provider** : [SearchToolBarAppWidget.kt](references/toolbars/SearchToolBarAppWidget.kt)
- **Layout Composable** : [SearchToolBarLayout.kt](references/toolbars/layout/SearchToolBarLayout.kt)
- **Widget Info XML** : [sample_search_toolbar_widget_info.xml](references/res/xml/sample_search_toolbar_widget_info.xml)
- **Dimensions** : [dimens.xml](references/res/values/dimens.xml)
- **Strings** : [strings.xml](references/res/values/strings.xml)
- **Drawables** : [sample_search_toolbar_preview.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable-nodpi/sample_search_toolbar_preview.png), [sample_search_icon.xml](references/res/drawable/sample_search_icon.xml), [sample_refresh_icon.xml](references/res/drawable/sample_refresh_icon.xml), [sample_mic_icon.xml](references/res/drawable/sample_mic_icon.xml), [sample_camera_icon.xml](references/res/drawable/sample_camera_icon.xml), [sample_delete_icon.xml](references/res/drawable/sample_delete_icon.xml), [sample_add_icon.xml](references/res/drawable/sample_add_icon.xml), [sample_pin_icon.xml](references/res/drawable/sample_pin_icon.xml)
- **Components** : [Buttons.kt](references/toolbars/layout/Buttons.kt), [LayoutComponents.kt](references/toolbars/layout/LayoutComponents.kt)
- **Utilities** : [ActionUtils.kt](references/utils/ActionUtils.kt), [PreviewAnnotations.kt](references/utils/PreviewAnnotations.kt)
- **Theme** : [Color.kt](references/ui/theme/Color.kt), [Theme.kt](references/ui/theme/Theme.kt), [Type.kt](references/ui/theme/Type.kt)
- **Guidance** :
  - Avoid `ColorProvider` import errors: import `androidx.glance.unit.ColorProvider` and use `androidx.glance.color.ColorProvider(dayColor, nightColor)`.
  - Use `GlanceModifier.background(Color)` directly for solid background colors rather than `ColorProvider`.

#### Full-bleed image layout and snap scrolling (`snap_scrolling`)

- **Widget Provider** : [FullBleedImageAppWidget.kt](references/text/FullBleedImageAppWidget.kt)
- **Layout Composable** : [FullBleedImageLayout.kt](references/text/layout/FullBleedImageLayout.kt)
- **Widget Info XML** : [sample_full_bleed_image_widget_info.xml](references/res/xml/sample_full_bleed_image_widget_info.xml)
- **Dimensions** : [dimens.xml](references/res/values/dimens.xml)
- **Strings** : [strings.xml](references/res/values/strings.xml)
- **Drawables** : [sample_full_bleed_image_preview.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable-nodpi/sample_full_bleed_image_preview.png), [sample_scrim_gradient.xml](references/res/drawable/sample_scrim_gradient.xml), [sample_placeholder_image.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable/sample_placeholder_image.png), [sample_app_logo.xml](references/res/drawable/sample_app_logo.xml), [sample_info_icon.xml](references/res/drawable/sample_info_icon.xml), [sample_no_data_icon.xml](references/res/drawable/sample_no_data_icon.xml)
- **Utilities** : [FontUtils.kt](references/utils/FontUtils.kt), [ImageAspectRatio.kt](references/utils/ImageAspectRatio.kt), [PreviewAnnotations.kt](references/utils/PreviewAnnotations.kt)
- **Theme** : [Color.kt](references/ui/theme/Color.kt), [Theme.kt](references/ui/theme/Theme.kt), [Type.kt](references/ui/theme/Type.kt)
- **Guidance** :
  - Set `verticalScrollMode =
    VerticalScrollMode.SnapScrollMatchHeight(size.height)` on `LazyColumn` for Snap Scrolling.
  - Glance 1.3.0-alpha02 (or higher), Compile SDK 37 (Baklava) or higher, and Android Gradle Plugin (AGP) 9.1.0 or higher are required if implementing Snap Scrolling.
  - Set override val sizeMode: SizeMode = SizeMode.Exact in the widget provider class.

#### Standard toolbar layout (`standard_toolbar`)

- **Widget Provider** : [ToolBarAppWidget.kt](references/toolbars/ToolBarAppWidget.kt)
- **Layout Composable** : [ToolBarLayout.kt](references/toolbars/layout/ToolBarLayout.kt)
- **Widget Info XML** : [sample_toolbar_widget_info.xml](references/res/xml/sample_toolbar_widget_info.xml)
- **Dimensions** : [dimens.xml](references/res/values/dimens.xml)
- **Strings** : [strings.xml](references/res/values/strings.xml)
- **Drawables** : [sample_toolbar_preview.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable-nodpi/sample_toolbar_preview.png), [sample_app_logo.xml](references/res/drawable/sample_app_logo.xml), [sample_search_icon.xml](references/res/drawable/sample_search_icon.xml), [sample_add_icon.xml](references/res/drawable/sample_add_icon.xml), [sample_edit_icon.xml](references/res/drawable/sample_edit_icon.xml), [sample_share_icon.xml](references/res/drawable/sample_share_icon.xml), [sample_mic_icon.xml](references/res/drawable/sample_mic_icon.xml), [sample_camera_icon.xml](references/res/drawable/sample_camera_icon.xml), [sample_videocam_icon.xml](references/res/drawable/sample_videocam_icon.xml)
- **Components** : [Buttons.kt](references/toolbars/layout/Buttons.kt), [LayoutComponents.kt](references/toolbars/layout/LayoutComponents.kt)
- **Utilities** : [ActionUtils.kt](references/utils/ActionUtils.kt), [PreviewAnnotations.kt](references/utils/PreviewAnnotations.kt)
- **Theme** : [Color.kt](references/ui/theme/Color.kt), [Theme.kt](references/ui/theme/Theme.kt), [Type.kt](references/ui/theme/Type.kt)
- **Additional References** : [CanonicalLayoutActivity.kt](references/CanonicalLayoutActivity.kt)
- **Guidance** :
  - See Troubleshooting to avoid `ColorProvider` import errors: use androidx.glance.color.ColorProvider(dayColor, nightColor).
  - Use GlanceModifier.background(Color) directly for solid background colors rather than `ColorProvider`.

#### Text with image layout (`text_with_image`)

- **Widget Provider** : [TextWithImageAppWidget.kt](references/text/TextWithImageAppWidget.kt)
- **Layout Composable** : [TextWithImageLayout.kt](references/text/layout/TextWithImageLayout.kt)
- **Widget Info XML** : [sample_text_with_image_widget_info.xml](references/res/xml/sample_text_with_image_widget_info.xml)
- **Data Repository** : [FakeTextWithImageRepository.kt](references/text/data/FakeTextWithImageRepository.kt)
- **Dimensions** : [dimens.xml](references/res/values/dimens.xml)
- **Strings** : [strings.xml](references/res/values/strings.xml)
- **Drawables** : [sample_text_image_preview.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable-nodpi/sample_text_image_preview.png), [sample_text_icon.xml](references/res/drawable/sample_text_icon.xml), [sample_refresh_icon.xml](references/res/drawable/sample_refresh_icon.xml), [sample_placeholder_image.png](https://developer.android.com/static/agents/skills/system/glance-canonical-layouts/references/res/drawable/sample_placeholder_image.png), [sample_info_icon.xml](references/res/drawable/sample_info_icon.xml), [sample_no_data_icon.xml](references/res/drawable/sample_no_data_icon.xml)
- **Utilities** : [FontUtils.kt](references/utils/FontUtils.kt), [ImageAspectRatio.kt](references/utils/ImageAspectRatio.kt), [PreviewAnnotations.kt](references/utils/PreviewAnnotations.kt)
- **Theme** : [Color.kt](references/ui/theme/Color.kt), [Theme.kt](references/ui/theme/Theme.kt), [Type.kt](references/ui/theme/Type.kt)
- **Guidance** :
  - Follow responsive layout sizing rules to accommodate text and image side-by-side or stacked based on available widget dimensions.

## Troubleshooting

1. **Background Colors** : Use `GlanceModifier.background(Color)` directly for solid background colors rather than `ColorProvider`, otherwise the background will be transparent.
2. **When using `ColorProvider` (outside of the background), avoid import errors** :
   1. Import type: `import androidx.glance.unit.ColorProvider`
   2. Use fully qualified creator: `androidx.glance.color.ColorProvider(dayColor, nightColor)`

## Checklist

- \[ \] Did you register the widget in the AndroidManifest?
- \[ \] Did you copy the corresponding **Widget Info XML** file and leave the given values including `minWidth`, `minHeight`, and `minResizeHeight` unchanged?
- \[ \] Did you use the given Glance Theme color values from the corresponding **Layout Composable** file?
- \[ \] Did you use dimension resource references for `android:minWidth` and `android:minHeight` in appwidget-provider XML definitions?
- \[ \] Did you override `val sizeMode: SizeMode = SizeMode.Exact` in the widget provider class?
