Remote Compose uses dedicated `@RemoteComposable` layout containers, modifiers,
and state wrappers in `androidx.compose.remote.creation.compose.*` and
Material 3 components in `androidx.wear.compose.remote.material3.*`.

## 1. Adaptive container sizing (`SMALL` versus `LARGE`)

Inspect `params.containerType` in `provideWidgetData` to distinguish
`ContainerInfo.CONTAINER_TYPE_SMALL` (`1x1` cards) from
`ContainerInfo.CONTAINER_TYPE_LARGE` (`2x1` cards). Adapt layout density,
padding, visible rows, and typography for each container size:

<br />

    import androidx.compose.remote.creation.compose.layout.RemoteAlignment
    import androidx.compose.remote.creation.compose.layout.RemoteArrangement
    import androidx.compose.remote.creation.compose.layout.RemoteColumn
    import androidx.compose.remote.creation.compose.layout.RemoteComposable
    import androidx.compose.remote.creation.compose.modifier.RemoteModifier
    import androidx.compose.remote.creation.compose.modifier.fillMaxSize
    import androidx.compose.remote.creation.compose.modifier.padding
    import androidx.compose.remote.creation.compose.state.rc
    import androidx.compose.remote.creation.compose.state.rdp
    import androidx.compose.remote.creation.compose.state.rs
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.graphics.Color
    import androidx.wear.compose.remote.material3.RemoteText

    @RemoteComposable
    @Composable
    fun AdaptiveCardContent(
        isLarge: Boolean,
        title: String,
        subtitle: String,
    ) {
        RemoteColumn(
            modifier = RemoteModifier.fillMaxSize()
                .padding(if (isLarge) 12.rdp else 6.rdp),
            verticalArrangement = RemoteArrangement.Center,
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
        ) {
            RemoteText(
                text = title.rs,
                color = Color(0xFFE6E1E5).rc,
            )
            if (isLarge) {
                RemoteText(
                    text = subtitle.rs,
                    color = Color(0xFFD0BCFF).rc,
                    modifier = RemoteModifier.padding(top = 4.rdp),
                )
            }
        }
    }

## 2. Unmasked full-bleed background images

When a widget displays background artwork or photography:

1. Decode the drawable in `provideWidgetData` with `BitmapFactory.decodeResource(context.resources, R.drawable.<bg_res>)`.
2. Convert the `Bitmap` using `RemoteImageBitmap` (`bitmap.asImageBitmap`). Render it as the first child of a root `RemoteBox` using `RemoteImage` with `RemoteModifier.fillMaxSize`.
3. **NEVER** apply `CircleShape`, `RoundedCornerShape`, or manual `cornerRadius` clipping to full-bleed backgrounds. The Wear OS host automatically masks the widget surface to match the watch's round or squircle border.

<br />

    import android.graphics.Bitmap
    import androidx.compose.remote.creation.compose.layout.RemoteAlignment
    import androidx.compose.remote.creation.compose.layout.RemoteArrangement
    import androidx.compose.remote.creation.compose.layout.RemoteBox
    import androidx.compose.remote.creation.compose.layout.RemoteColumn
    import androidx.compose.remote.creation.compose.layout.RemoteComposable
    import androidx.compose.remote.creation.compose.layout.RemoteImage
    import androidx.compose.remote.creation.compose.modifier.RemoteModifier
    import androidx.compose.remote.creation.compose.modifier.fillMaxSize
    import androidx.compose.remote.creation.compose.modifier.padding
    import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
    import androidx.compose.remote.creation.compose.state.rdp
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.graphics.asImageBitmap

    @RemoteComposable
    @Composable
    fun FullBleedBackgroundLayout(
        backgroundBitmap: Bitmap?,
        isLarge: Boolean,
        content: @RemoteComposable @Composable () -> Unit,
    ) {
        RemoteBox(modifier = RemoteModifier.fillMaxSize()) {
            if (backgroundBitmap != null) {
                RemoteImage(
                    remoteBitmap =
                        RemoteImageBitmap(backgroundBitmap.asImageBitmap()),
                    contentDescription = null,
                    modifier = RemoteModifier.fillMaxSize(),
                )
            }
            RemoteColumn(
                modifier = RemoteModifier.fillMaxSize()
                    .padding(if (isLarge) 12.rdp else 6.rdp),
                verticalArrangement = RemoteArrangement.Center,
                horizontalAlignment = RemoteAlignment.CenterHorizontally,
            ) {
                content()
            }
        }
    }

## 3. Responsive multi-item grids and rows

Combine `RemoteColumn` and `RemoteRow` with `RemoteArrangement.spacedBy` to
arrange avatars, quick actions, or status badges with a bottom action button.
Display fewer items in `SMALL` containers and the full grid in `LARGE`
containers. Wrap each item in a `.clickable(onItemClick)` container so both
`RemoteImage` and fallback `RemoteText` branches respond to clicks:

<br />

    @RemoteComposable
    @Composable
    fun GridWithBottomActionLayout(
        items: List<Pair<String, Bitmap?>>,
        isLarge: Boolean,
        onItemClick: androidx.compose.remote.creation.compose.action.Action,
        onMoreClick: androidx.compose.remote.creation.compose.action.Action,
    ) {
        val visibleItems = if (isLarge) items.take(6) else items.take(3)
        val rows = visibleItems.chunked(3)

        RemoteColumn(
            modifier = RemoteModifier.fillMaxSize().padding(8.rdp),
            verticalArrangement = RemoteArrangement.Center,
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
        ) {
            for (rowItems in rows) {
                RemoteRow(
                    horizontalArrangement = RemoteArrangement.spacedBy(8.rdp),
                    verticalAlignment = RemoteAlignment.CenterVertically,
                ) {
                    for ((label, avatar) in rowItems) {
                        RemoteBox(
                            modifier = RemoteModifier.size(36.rdp)
                                .background(Color(0xFF4F378B).rc)
                                .clickable(onItemClick),
                            contentAlignment = RemoteAlignment.Center,
                        ) {
                            if (avatar != null) {
                                RemoteImage(
                                    remoteBitmap =
                                        RemoteImageBitmap(avatar.asImageBitmap()),
                                    contentDescription = label.rs,
                                    modifier = RemoteModifier.fillMaxSize(),
                                )
                            } else {
                                RemoteText(text = label.rs, color = Color.White.rc)
                            }
                        }
                    }
                }
            }
            RemoteButton(
                onClick = onMoreClick,
                modifier = RemoteModifier.padding(top = 6.rdp),
            ) {
                RemoteText(text = "More".rs)
            }
        }
    }