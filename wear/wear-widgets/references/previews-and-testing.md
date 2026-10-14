## 1. Composable preview suites (`Squircle`, `Round`, and `Rectangular`)

Annotate preview functions with `@Preview` and pass one of the predefined
`PreviewParameterProvider` suites to `@PreviewParameter`. Call
`WearWidgetPreview(widget, params)` to render `SMALL` and `LARGE` containers:

- **`SquircleAllWidgetPreviewParams`**: Rounded rectangle containers.
- **`RoundAllWidgetPreviewParams`**: Pill-shaped containers.
- **`RectangularAllWidgetPreviewParams`** : Uncropped rectangular containers with display-safe padding at `320 dpi` for generating widget picker preview assets.

<br />

    import androidx.compose.runtime.Composable
    import androidx.compose.ui.tooling.preview.Preview
    import androidx.compose.ui.tooling.preview.PreviewParameter
    import androidx.glance.wear.core.WearWidgetParams
    import androidx.glance.wear.tooling.preview.RectangularAllWidgetPreviewParams
    import androidx.glance.wear.tooling.preview.RoundAllWidgetPreviewParams
    import androidx.glance.wear.tooling.preview.SquircleAllWidgetPreviewParams
    import androidx.glance.wear.tooling.preview.WearWidgetPreview

    @Preview(
        name = "Squircle",
        device = "spec:width=1000dp,height=1000dp,dpi=320",
    )
    @Composable
    fun MyWidgetSquirclePreview(
        @PreviewParameter(SquircleAllWidgetPreviewParams::class)
        params: WearWidgetParams,
    ) {
        WearWidgetPreview(MyWidget(), params)
    }

    @Preview(
        name = "Round",
        device = "spec:width=1000dp,height=1000dp,dpi=320",
    )
    @Composable
    fun MyWidgetRoundPreview(
        @PreviewParameter(RoundAllWidgetPreviewParams::class)
        params: WearWidgetParams,
    ) {
        WearWidgetPreview(MyWidget(), params)
    }

    @Preview(
        name = "Widget Preview Asset",
        device = "spec:width=1000dp,height=1000dp,dpi=320",
    )
    @Composable
    fun MyWidgetCatalogPreview(
        @PreviewParameter(RectangularAllWidgetPreviewParams::class)
        params: WearWidgetParams,
    ) {
        WearWidgetPreview(MyWidget(), params)
    }

## 2. Widget picker preview PNGs (`res/drawable-nodpi/`)

Wear OS displays static preview images in the system widget picker for each
`<container>` declared in the `@xml/` provider configuration.

### Generating and saving rectangular preview assets

1. Render the `@Preview` function configured with `RectangularAllWidgetPreviewParams::class` and `device = "spec:width=1000dp,height=1000dp,dpi=320"`. This suite produces uncropped rectangular variants for both `SMALL` and `LARGE` containers.
2. Extract the rendered images from the Android Studio **Design** surface (**Copy Image** or save) or using CLI preview tools.
3. Save the `SMALL` and `LARGE` PNGs into `app/src/main/res/drawable-nodpi/` to prevent density scaling.

### Platform asset requirements

- **Directory** : Place raster PNG files in `app/src/main/res/drawable-nodpi/` so the system doesn't apply density scaling.
- **Dimensions (at 320 dpi, or 2.0x watch density)** :
  - `SMALL` (`CONTAINER_TYPE_SMALL`, `224x84` dp): **`448x168` px** (aspect ratio `2.67`).
  - `LARGE` (`CONTAINER_TYPE_LARGE`, `232x144` dp): **`464x288` px** (aspect ratio `1.61`).
- **Visual fidelity** : Every preview PNG **MUST** be a non-blank, unmasked rectangular image depicting the widget's actual UI elements. Include the widget's title or value text, interactive buttons, icons, avatar initials, or composited background drawable. **NEVER** use a solid-color placeholder, empty unlabeled shapes, a raw background texture, or a 1:1 (`400x400`) tile preview.

## 3. Robolectric unit testing (`provideWidgetData` and `captureRawContent`)

When unit-testing a widget with JUnit 4 and Robolectric, call
`widget.provideWidgetData(context, params)` followed by
`data.captureRawContent(context, params)`. This evaluates the
`WearWidgetDocument` composable lambda and verifies that the Remote Compose
`rcDocument` payload serializes without errors. Because `WearWidgetParams` and
`captureRawContent` carry `@RestrictTo(LIBRARY_GROUP)` in alpha releases, add
`@file:SuppressLint("RestrictedApi")` to the test file:

<br />

    @file:SuppressLint("RestrictedApi")

    import android.annotation.SuppressLint
    import android.content.Context
    import androidx.glance.wear.core.ContainerInfo
    import androidx.glance.wear.core.WearWidgetParams
    import androidx.glance.wear.core.WidgetInstanceId
    import androidx.test.core.app.ApplicationProvider
    import kotlinx.coroutines.runBlocking
    import org.junit.Assert.assertNotNull
    import org.junit.Assert.assertTrue
    import org.junit.Test
    import org.junit.runner.RunWith
    import org.robolectric.RobolectricTestRunner
    import org.robolectric.annotation.Config

    @RunWith(RobolectricTestRunner::class)
    @Config(sdk = [34])
    class MyWidgetTest {
        @Test
        fun provideWidgetData_serializesRemoteComposeDocument() = runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val widget = MyWidget()
            for (containerType in listOf(
                ContainerInfo.CONTAINER_TYPE_SMALL,
                ContainerInfo.CONTAINER_TYPE_LARGE,
            )) {
                val params = WearWidgetParams(
                    instanceId = WidgetInstanceId(namespace = "test", id = 1),
                    containerType = containerType,
                    widthDp = 220f,
                    heightDp = 120f,
                    horizontalPaddingDp = 8f,
                    verticalPaddingDp = 8f,
                    cornerRadiusDp = 16f,
                )
                val data = widget.provideWidgetData(context, params)
                val rawContent = data.captureRawContent(context, params)
                assertNotNull(rawContent.rcDocument)
                assertTrue(rawContent.rcDocument.isNotEmpty())
            }
        }
    }