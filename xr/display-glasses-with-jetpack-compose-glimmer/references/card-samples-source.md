When creating a Glimmer Card component, refer to the following implementation
samples in `CardSamples.kt`:


```kotlin
/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.glimmer.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.xr.glimmer.ActionCard
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.ImageCard
import androidx.xr.glimmer.LeadingImageCard
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.TrailingImageCard
import androidx.xr.glimmer.list.GlimmerLazyColumn

@Composable
fun CardSampleUsage() {
    GlimmerLazyColumn {
        item { CardSample() }
        item { CardWithTrailingIconSample() }
        item { CardWithTitleAndSubtitleAndLeadingIconSample() }
        item { CardWithLongText() }
        item { CardWithTitleAndSubtitleAndLeadingIconLongText() }
        item { ActionCardWithTitleSample() }
        item { ActionCardWithTitleAndLeadingIcon() }
        item { ImageCardSample() }
        item { ImageCardWithTitleAndSubtitleAndLeadingIconSample() }
        item { LeadingImageCardSample() }
        item { TrailingImageCardSample() }
    }
}

// ---
// Card samples
// ---

@Sampled
@Composable
fun CardSample() {
    Card { Text("This is a card") }
}

@Sampled
@Composable
fun CardWithTrailingIconSample() {
    Card(trailingIcon = { Icon(FavoriteIcon, "Localized description") }) {
        Text("This is a card with a trailing icon")
    }
}

@Sampled
@Composable
fun CardWithTitleAndSubtitleAndLeadingIconSample() {
    Card(
        title = { Text("Title") },
        subtitle = { Text("Subtitle") },
        leadingIcon = { Icon(FavoriteIcon, "Localized description") },
    ) {
        Text("This is a card with a title, subtitle, and leading icon")
    }
}

@Sampled
@Composable
fun ClickableCardSample() {
    Card(onClick = {}) { Text("This is a card") }
}

@Sampled
@Composable
fun ClickableCardWithTrailingIconSample() {
    Card(onClick = {}, trailingIcon = { Icon(FavoriteIcon, "Localized description") }) {
        Text("This is a card with a trailing icon")
    }
}

@Sampled
@Composable
fun ClickableCardWithTitleAndSubtitleAndLeadingIconSample() {
    Card(
        onClick = {},
        title = { Text("Title") },
        subtitle = { Text("Subtitle") },
        leadingIcon = { Icon(FavoriteIcon, "Localized description") },
    ) {
        Text("This is a card with a title, subtitle, and leading icon")
    }
}

@Composable
fun CardWithLongText() {
    Card {
        Text(
            "This is a card with a lot of text that will wrap to multiple lines. The maximum recommend number of lines of text for a card is 10.",
            maxLines = 10,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun CardWithTitleAndSubtitleAndLeadingIconLongText() {
    Card(
        title = { Text("Title") },
        subtitle = { Text("Subtitle") },
        leadingIcon = { Icon(FavoriteIcon, "Localized description") },
    ) {
        Text(
            "This is a card with a lot of text that will wrap to multiple lines. The maximum recommend number of lines of text for a card is 10."
        )
    }
}

@Composable
fun CardWithTitleAndSubtitleAndLeadingIconAndTrailingIconLongText() {
    Card(
        title = { Text("Title") },
        subtitle = { Text("Subtitle") },
        leadingIcon = { Icon(FavoriteIcon, "Localized description") },
        trailingIcon = { Icon(FavoriteIcon, "Localized description") },
    ) {
        Text(
            "This is a card with a lot of text that will wrap to multiple lines. The maximum recommend number of lines of text for a card is 10."
        )
    }
}

// ---
// ActionCard samples
// ---

@Sampled
@Composable
fun ActionCardWithTitleSample() {
    ActionCard(action = { Button(onClick = {}) { Text("Send") } }, title = { Text("Title") }) {
        Text("This is an action card with a title")
    }
}

@Composable
fun ActionCardWithTitleAndLeadingIcon() {
    ActionCard(
        action = {
            Button(onClick = {}, trailingIcon = { Icon(FavoriteIcon, "Localized description") }) {
                Text("Send")
            }
        },
        title = { Text("Title") },
        leadingIcon = { Icon(FavoriteIcon, "Localized description") },
    ) {
        Text("This is an action card with a title and leading icon")
    }
}

// ---
// ImageCard samples
// ---

@Sampled
@Composable
fun ImageCardSample() {
    ImageCard(
        image = { Image(MyImage, "Localized description", contentScale = ContentScale.FillWidth) }
    ) {
        Text("This is an image card")
    }
}

@Sampled
@Composable
fun ImageCardWithTitleAndSubtitleAndLeadingIconSample() {
    ImageCard(
        image = { Image(MyImage, "Localized description", contentScale = ContentScale.FillWidth) },
        title = { Text("Title") },
        subtitle = { Text("Subtitle") },
        leadingIcon = { Icon(FavoriteIcon, "Localized description") },
    ) {
        Text("This is an image card with a title, subtitle, and leading icon")
    }
}

@Sampled
@Composable
fun ClickableImageCardSample() {
    ImageCard(
        onClick = {},
        image = { Image(MyImage, "Localized description", contentScale = ContentScale.FillWidth) },
    ) {
        Text("This is an image card")
    }
}

@Sampled
@Composable
fun ClickableImageCardWithTitleAndSubtitleAndLeadingIconSample() {
    ImageCard(
        onClick = {},
        image = { Image(MyImage, "Localized description", contentScale = ContentScale.FillWidth) },
        title = { Text("Title") },
        subtitle = { Text("Subtitle") },
        leadingIcon = { Icon(FavoriteIcon, "Localized description") },
    ) {
        Text("This is an image card with a title, subtitle, and leading icon")
    }
}

// ---
// Leading/Trailing ImageCard samples
// ---

@Composable
fun LeadingImageCardSample() {
    LeadingImageCard(
        image = { Image(MyImage, "Localized description") },
        title = { Text("Title") },
        subtitle = { Text("Subtitle") },
    ) {
        Text("This is a card with a leading image.")
    }
}

@Composable
fun TrailingImageCardSample() {
    TrailingImageCard(
        image = { Image(MyImage, "Localized description") },
        title = { Text("Title") },
        subtitle = { Text("Subtitle") },
    ) {
        Text("This is a card with a trailing image.")
    }
}

// ---
// Previews
// ---

@Preview
@Composable
private fun CardPreview() {
    GlimmerTheme { CardSample() }
}

@Preview
@Composable
private fun CardWithTrailingIconPreview() {
    GlimmerTheme { CardWithTrailingIconSample() }
}

@Preview
@Composable
private fun CardWithTitleAndSubtitleAndLeadingIconPreview() {
    GlimmerTheme { CardWithTitleAndSubtitleAndLeadingIconSample() }
}

@Preview
@Composable
private fun CardWithLongTextPreview() {
    GlimmerTheme { CardWithLongText() }
}

@Preview
@Composable
private fun CardWithTitleAndSubtitleAndLeadingIconLongTextPreview() {
    GlimmerTheme { CardWithTitleAndSubtitleAndLeadingIconLongText() }
}

@Preview
@Composable
private fun CardWithTitleAndSubtitleAndLeadingIconAndTrailingIconLongTextPreview() {
    GlimmerTheme { CardWithTitleAndSubtitleAndLeadingIconAndTrailingIconLongText() }
}

@Preview
@Composable
private fun ActionCardWithTitlePreview() {
    GlimmerTheme { ActionCardWithTitleSample() }
}

@Preview
@Composable
private fun ActionCardWithTitleAndLeadingIconPreview() {
    GlimmerTheme { ActionCardWithTitleAndLeadingIcon() }
}

@Preview
@Composable
private fun ImageCardPreview() {
    GlimmerTheme { ImageCardSample() }
}

@Preview
@Composable
private fun ImageCardWithTitleAndSubtitleAndLeadingIconPreview() {
    GlimmerTheme { ImageCardWithTitleAndSubtitleAndLeadingIconSample() }
}

@Preview
@Composable
private fun LeadingImageCardPreview() {
    GlimmerTheme { LeadingImageCardSample() }
}

@Preview
@Composable
private fun TrailingImageCardPreview() {
    GlimmerTheme { TrailingImageCardSample() }
}

fun placeholderImagePainter(intrinsicSize: Size): Painter =
    BrushPainter(
        Brush.linearGradient(
            0.0f to Color(0xFF3C8CDE),
            0.4f to Color(0xFFED73A8),
            0.6f to Color(0xFFED73A8),
            1.0f to Color(0xFFE763F9),
            start = Offset.Zero,
            end = Offset(intrinsicSize.width, intrinsicSize.height),
        )
    )

/**
 * Placeholder image with a large intrinsic size, to simulate a real life use case of loading a
 * bitmap
 */
private val MyImage = placeholderImagePainter(Size(1000f, 1000f))
```

<br />