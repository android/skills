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
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.CompactButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureAction
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicatorState
import androidx.wear.compose.material3.onehandedgesture.oneHandedGesture
import androidx.wear.compose.material3.onehandedgesture.rememberOneHandedGestureConfiguration
import kotlinx.coroutines.launch

@Sampled
@Composable
fun SimpleButtonSample(modifier: Modifier = Modifier) {
    Button(onClick = { /* Do something */ }, label = { Text("Simple Button") }, modifier = modifier)
}

@Sampled
@Composable
fun ButtonSample() {
    Button(
        onClick = { /* Do something */ },
        label = { Text("Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Sampled
@Composable
fun ButtonLargeIconSample(enabled: Boolean = true) {
    // When customising the icon size, it is recommended to also specify
    // the associated content padding
    Button(
        onClick = { /* Do something */ },
        enabled = enabled,
        label = { Text("Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.LargeIconSize),
            )
        },
        contentPadding = ButtonDefaults.ButtonWithLargeIconContentPadding,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Sampled
@Composable
fun ButtonExtraLargeIconSample(enabled: Boolean = true) {
    // When customising the icon size, it is recommended to also specify
    // the associated content padding
    Button(
        onClick = { /* Do something */ },
        enabled = enabled,
        label = { Text("Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.ExtraLargeIconSize),
            )
        },
        contentPadding = ButtonDefaults.ButtonWithExtraLargeIconContentPadding,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Sampled
@Composable
fun ButtonWithImageSample(enabled: Boolean = true) {
    Button(
        onClick = { /* Do something */ },
        containerPainter =
            ButtonDefaults.containerPainter(
                image = painterResource(id = R.drawable.backgroundimage)
            ),
        enabled = enabled,
        label = { Text("Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Sampled
@Composable
fun SimpleFilledTonalButtonSample() {
    FilledTonalButton(
        onClick = { /* Do something */ },
        label = { Text("Filled Tonal Button") },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Sampled
@Composable
fun FilledTonalButtonSample() {
    FilledTonalButton(
        onClick = { /* Do something */ },
        label = { Text("Filled Tonal Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Sampled
@Composable
fun SimpleFilledVariantButtonSample() {
    Button(
        onClick = { /* Do something */ },
        colors = ButtonDefaults.filledVariantButtonColors(),
        label = { Text("Filled Variant Button") },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Sampled
@Composable
fun FilledVariantButtonSample() {
    Button(
        onClick = { /* Do something */ },
        colors = ButtonDefaults.filledVariantButtonColors(),
        label = { Text("Filled Variant Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Sampled
@Composable
fun SimpleOutlinedButtonSample() {
    OutlinedButton(
        onClick = { /* Do something */ },
        label = { Text("Outlined Button") },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Sampled
@Composable
fun OutlinedButtonSample() {
    OutlinedButton(
        onClick = { /* Do something */ },
        label = { Text("Outlined Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Sampled
@Composable
fun SimpleChildButtonSample() {
    ChildButton(
        onClick = { /* Do something */ },
        label = {
            Text("Child Button", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Sampled
@Composable
fun ChildButtonSample() {
    ChildButton(
        onClick = { /* Do something */ },
        label = { Text("Child Button") },
        secondaryLabel = { Text("Secondary label") },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Sampled
@Composable
fun CompactButtonSample(modifier: Modifier = Modifier) {
    CompactButton(
        onClick = { /* Do something */ },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(CompactButtonDefaults.ExtraSmallIconSize),
            )
        },
        modifier = modifier,
        label = { Text("Compact Button", maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Sampled
@Composable
fun CompactButtonWithContentSample(modifier: Modifier = Modifier) {
    CompactButton(
        onClick = { /* Do something */ },
        modifier = modifier,
        content = {
            Box(
                Modifier.size(CompactButtonDefaults.ExtraSmallIconSize)
                    .clip(CircleShape)
                    .background(Color.Green)
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(
                "Custom content",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.5.sp,
            )
        },
    )
}

@Sampled
@Composable
fun CompactButtonWithOnLongClickSample(
    onClickHandler: () -> Unit,
    onLongClickHandler: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompactButton(
        onClick = onClickHandler,
        onLongClick = onLongClickHandler,
        onLongClickLabel = "Long click",
        label = { Text("Long clickable") },
        modifier =
            modifier.semantics {
                // Also override the 'click label' to say 'Double tap to press' instead of
                // the usual 'Double tap to activate'.
                onClick("press") { false }
            },
    )
}

@Sampled
@Composable
fun FilledTonalCompactButtonSample(modifier: Modifier = Modifier) {
    CompactButton(
        onClick = { /* Do something */ },
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_favorite_rounded),
                contentDescription = "Favorite icon",
                modifier = Modifier.size(CompactButtonDefaults.ExtraSmallIconSize),
            )
        },
        colors = ButtonDefaults.filledTonalButtonColors(),
        modifier = modifier,
        label = {
            Text("Filled Tonal Compact Button", maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
    )
}

@Sampled
@Composable
fun OutlinedCompactButtonSample(modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        var expanded by remember { mutableStateOf(false) }
        if (expanded) {
            Text("A multiline string showing two lines")
        } else {
            Text("One line text")
        }
        Spacer(Modifier.height(ButtonDefaults.IconSpacing))
        CompactButton(
            onClick = { expanded = !expanded },
            colors = ButtonDefaults.outlinedButtonColors(),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            modifier = modifier,
            content = {
                if (expanded) {
                    Text("Show Less", maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    Text("Show More", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                if (expanded) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Collapse",
                        modifier = Modifier.size(CompactButtonDefaults.ExtraSmallIconSize),
                    )
                } else {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Expand",
                        modifier = Modifier.size(CompactButtonDefaults.ExtraSmallIconSize),
                    )
                }
            },
        )
    }
}

@Sampled
@Composable
fun ButtonContentWithOneHandedGestureSample() {
    var label by remember { mutableStateOf("Filled Button") }
    val onClick = remember { { label = "Gestured" } }
    val interactionSource = remember { MutableInteractionSource() }
    val gestureConfig =
        rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
    val indicatorState = remember { OneHandedGestureClickIndicatorState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier =
                Modifier.oneHandedGesture(
                    gestureConfiguration = gestureConfig,
                    onGestureLabel = "click",
                    interactionSource = interactionSource,
                    onGestureAvailable = {
                        coroutineScope.launch { indicatorState.showIndicator() }
                    },
                    onGesture = onClick,
                ),
            content = {
                OneHandedGestureClickIndicator(
                    gestureConfiguration = gestureConfig,
                    state = indicatorState,
                    gestureIndicatorTint = MaterialTheme.colorScheme.onPrimary,
                ) {
                    ButtonDefaults.Content(
                        secondaryLabel = { Text("Secondary Label") },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_favorite_rounded),
                                contentDescription = "Favorite icon",
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                        colors = ButtonDefaults.buttonColors(),
                        label = { Text(label) },
                    )
                }
            },
        )
    }
}

@Sampled
@Composable
fun CompactButtonContentWithOneHandedGestureSample() {
    var label by remember { mutableStateOf("Compact Button") }
    val onClick = remember { { label = "Gestured" } }
    val interactionSource = remember { MutableInteractionSource() }
    val gestureConfig =
        rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
    val indicatorState = remember { OneHandedGestureClickIndicatorState() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CompactButton(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier =
                Modifier.oneHandedGesture(
                    gestureConfiguration = gestureConfig,
                    onGestureLabel = "click",
                    interactionSource = interactionSource,
                    onGestureAvailable = {
                        coroutineScope.launch { indicatorState.showIndicator() }
                    },
                    onGesture = onClick,
                ),
            content = {
                OneHandedGestureClickIndicator(
                    gestureConfiguration = gestureConfig,
                    state = indicatorState,
                    gestureIndicatorTint = MaterialTheme.colorScheme.onPrimary,
                ) {
                    CompactButtonDefaults.Content(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_favorite_rounded),
                                contentDescription = "Favorite icon",
                                modifier = Modifier.size(CompactButtonDefaults.ExtraSmallIconSize),
                            )
                        },
                        colors = ButtonDefaults.buttonColors(),
                        label = { Text(label) },
                    )
                }
            },
        )
    }
}
```