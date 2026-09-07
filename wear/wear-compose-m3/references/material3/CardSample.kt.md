```
/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedCard
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureAction
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicatorState
import androidx.wear.compose.material3.onehandedgesture.oneHandedGesture
import androidx.wear.compose.material3.onehandedgesture.rememberOneHandedGestureConfiguration
import kotlinx.coroutines.launch

@Preview
@Sampled
@Composable
fun CardSample() {
    Card(onClick = { /* Do something */ }) { Text("Card") }
}

@Sampled
@Composable
fun NonClickableCardSample() {
    Card { Text("Non Clickable Card") }
}

@Sampled
@Composable
fun CardWithOnLongClickSample(onLongClickHandler: () -> Unit) {
    Card(
        onClick = { /* Do something */ },
        onLongClick = onLongClickHandler,
        onLongClickLabel = "Long click",
    ) {
        Text("Card with long click")
    }
}

@Sampled
@Composable
fun AppCardSample() {
    AppCard(
        onClick = { /* Do something */ },
        appName = { Text("App name") },
        title = { Text("Card title") },
        time = { Text("Now") },
    ) {
        Text("Card content")
    }
}

@Sampled
@Composable
fun NonClickableAppCardSample() {
    AppCard(
        appName = { Text("App name") },
        title = { Text("Card title") },
        time = { Text("Now") },
    ) {
        Text("Non clickable card content")
    }
}

@Sampled
@Composable
fun AppCardWithIconSample() {
    AppCard(
        onClick = { /* Do something */ },
        appName = { Text("App name") },
        appImage = {
            Icon(
                painter = painterResource(id = android.R.drawable.star_big_off),
                contentDescription = "Star icon",
                modifier =
                    Modifier.size(CardDefaults.AppImageSize)
                        .wrapContentSize(align = Alignment.Center),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text("Card title") },
        time = { Text("Now") },
    ) {
        Text("Card content")
    }
}

@Sampled
@Composable
fun AppCardWithImageSample() {
    val configuration = LocalConfiguration.current
    // Add padding to the end of the image in order to maintain the correct proportions
    // between the image and the card.
    val imageEndPaddingDp = (0.15f * configuration.screenWidthDp).dp
    AppCard(
        onClick = { /* Do something */ },
        appName = { Text("App name") },
        appImage = {
            Icon(
                painter = painterResource(id = android.R.drawable.star_big_off),
                contentDescription = "Star icon",
                modifier =
                    Modifier.size(CardDefaults.AppImageSize)
                        .wrapContentSize(align = Alignment.Center),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text("With image") },
        time = { Text("Now") },
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(
                modifier =
                    Modifier.weight(1f).aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)),
                painter = painterResource(id = R.drawable.card_content_image),
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(imageEndPaddingDp))
        }
    }
}

@Sampled
@Composable
fun TitleCardSample() {
    TitleCard(
        onClick = { /* Do something */ },
        title = { Text("Title card") },
        time = { Text("Now") },
    ) {
        Text("Card content")
    }
}

@Sampled
@Composable
fun NonClickableTitleCardSample() {
    TitleCard(title = { Text("Title card") }, time = { Text("Now") }) {
        Text("Non clickable Card content")
    }
}

@Sampled
@Composable
fun TitleCardWithSubtitleAndTimeSample() {
    TitleCard(
        onClick = { /* Do something */ },
        time = { Text("Now") },
        title = { Text("Title card") },
        subtitle = { Text("Subtitle") },
    )
}

@Preview
@Sampled
@Composable
fun TitleCardWithMultipleImagesSample() {
    TitleCard(
        onClick = { /* Do something */ },
        title = { Text("Title card") },
        time = { Text("Now") },
        modifier = Modifier.semantics { contentDescription = "Background image" },
    ) {
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(
                modifier =
                    Modifier.weight(2f)
                        .height(68.dp)
                        .align(Alignment.CenterVertically)
                        .clip(RoundedCornerShape(16.dp)),
                painter = painterResource(id = R.drawable.card_content_image),
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
            Spacer(Modifier.width(4.dp))
            Image(
                modifier =
                    Modifier.weight(1f)
                        .height(68.dp)
                        .align(Alignment.CenterVertically)
                        .clip(RoundedCornerShape(16.dp)),
                painter = painterResource(id = R.drawable.card_content_image),
                contentScale = ContentScale.Crop,
                contentDescription = null,
            )
        }
    }
}

@Sampled
@Composable
fun TitleCardWithImageWithTimeAndTitleSample() {
    TitleCard(
        onClick = { /* Do something */ },
        containerPainter =
            CardDefaults.containerPainter(image = painterResource(id = R.drawable.backgroundimage)),
        title = { Text("Card title") },
        subtitle = { Text("Subtitle") },
        time = { Text("Now") },
        contentPadding = CardDefaults.CardWithContainerPainterContentPadding,
        modifier = Modifier.semantics { contentDescription = "Background image" },
    ) {
        Text("Card content")
    }
}

@Sampled
@Composable
fun NonClickableTitleCardWithImageWithTimeAndTitleSample() {
    TitleCard(
        containerPainter =
            CardDefaults.containerPainter(image = painterResource(id = R.drawable.backgroundimage)),
        title = { Text("Card title") },
        subtitle = { Text("Subtitle") },
        time = { Text("Now") },
        contentPadding = CardDefaults.CardWithContainerPainterContentPadding,
        modifier = Modifier.semantics { contentDescription = "Background image" },
    ) {
        Text("Card content")
    }
}

@Sampled
@Composable
fun OutlinedCardSample() {
    OutlinedCard(onClick = { /* Do something */ }) { Text("Outlined card") }
}

@Sampled
@Composable
fun NonClickableOutlinedCardSample() {
    OutlinedCard { Text("Non-clickable outlined card") }
}

@Sampled
@Composable
fun ImageCardSample() {
    Card(
        onClick = { /* Do something */ },
        containerPainter =
            CardDefaults.containerPainter(image = painterResource(id = R.drawable.backgroundimage)),
    ) {
        Text("Image card")
    }
}

@Sampled
@Composable
fun NonClickableImageCardSample() {
    Card(
        containerPainter =
            CardDefaults.containerPainter(image = painterResource(id = R.drawable.backgroundimage))
    ) {
        Text("Non clickable image card")
    }
}

@Sampled
@Composable
fun OutlinedAppCardSample() {
    AppCard(
        onClick = { /* Do something */ },
        appName = { Text("App name") },
        appImage = {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Favorite icon",
                modifier = Modifier.size(CardDefaults.AppImageSize),
            )
        },
        title = { Text("App card") },
        time = { Text("Now") },
        colors = CardDefaults.outlinedCardColors(),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Text("Card content")
    }
}

@Sampled
@Composable
fun OutlinedTitleCardSample() {
    TitleCard(
        onClick = { /* Do something */ },
        title = { Text("Title card") },
        time = { Text("Now") },
        colors = CardDefaults.outlinedCardColors(),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Text("Card content")
    }
}

@Sampled
@Preview
@Composable
fun CardFillContentSample() {
    Card(
        onClick = { /* Do something */ },
        // Constrains the card to fill background up to the intrinsic height.
        modifier = Modifier.height(IntrinsicSize.Min),
    ) {
        Text(
            "Filled Content",
            color = MaterialTheme.colorScheme.onPrimary,
            modifier =
                Modifier.fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .wrapContentSize(Alignment.Center),
        )
    }
}

@Sampled
@Composable
fun AppCardContentWithOneHandedGestureSample() {
    var label by remember { mutableStateOf("App Card") }
    val onClick = remember { { label = "Gestured" } }
    val interactionSource = remember { MutableInteractionSource() }
    val gestureConfig =
        rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
    val indicatorState = remember { OneHandedGestureClickIndicatorState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier =
                Modifier.padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .oneHandedGesture(
                        gestureConfiguration = gestureConfig,
                        onGestureLabel = "click",
                        onGestureAvailable = {
                            coroutineScope.launch { indicatorState.showIndicator() }
                        },
                        interactionSource = interactionSource,
                        onGesture = onClick,
                    ),
        ) {
            OneHandedGestureClickIndicator(
                gestureConfiguration = gestureConfig,
                state = indicatorState,
                gestureIndicatorTint = MaterialTheme.colorScheme.onSurface,
            ) {
                CardDefaults.AppCardContent(
                    appName = { Text("App Name") },
                    title = { Text(label) },
                    appImage = {
                        Icon(
                            painter = painterResource(R.drawable.ic_favorite_rounded),
                            contentDescription = "Favorite icon",
                            modifier = Modifier.size(CardDefaults.AppImageSize),
                        )
                    },
                    time = { Text("now") },
                ) {
                    Text("Card body")
                }
            }
        }
    }
}

@Sampled
@Composable
fun TitleCardContentWithOneHandedGestureSample() {
    var label by remember { mutableStateOf("Title Card") }
    val onClick = remember { { label = "Gestured" } }
    val interactionSource = remember { MutableInteractionSource() }
    val gestureConfig =
        rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
    val indicatorState = remember { OneHandedGestureClickIndicatorState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier =
                Modifier.padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .oneHandedGesture(
                        gestureConfiguration = gestureConfig,
                        onGestureLabel = "click",
                        onGestureAvailable = {
                            coroutineScope.launch { indicatorState.showIndicator() }
                        },
                        interactionSource = interactionSource,
                        onGesture = onClick,
                    ),
        ) {
            OneHandedGestureClickIndicator(
                gestureConfiguration = gestureConfig,
                state = indicatorState,
                gestureIndicatorTint = MaterialTheme.colorScheme.onSurface,
            ) {
                CardDefaults.TitleCardContent(
                    title = { Text(label) },
                    time = { Text("now") },
                    subtitle = { Text("Subtitle") },
                ) {
                    Text("Card body")
                }
            }
        }
    }
}
```