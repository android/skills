```
/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.placeholder
import androidx.wear.compose.material3.placeholderShimmer
import androidx.wear.compose.material3.rememberPlaceholderState
import kotlinx.coroutines.delay

/**
 * This sample applies placeholders directly over the content that is waiting to be loaded. This
 * approach is suitable for situations where the developer has an approximate knowledge of how big
 * the content is going to be and it doesn't have cached data that can be shown.
 */
@Sampled
@Composable
fun ButtonWithIconAndLabelAndPlaceholders() {
    var labelText by remember { mutableStateOf("") }
    var imageVector: ImageVector? by remember { mutableStateOf(null) }
    val buttonPlaceholderState =
        rememberPlaceholderState(isVisible = labelText.isEmpty() || imageVector == null)

    FilledTonalButton(
        onClick = { /* Do something */ },
        enabled = true,
        modifier = Modifier.fillMaxWidth().placeholderShimmer(buttonPlaceholderState),
        label = {
            Text(
                text = labelText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().placeholder(buttonPlaceholderState),
            )
        },
        icon = {
            Box(
                modifier =
                    Modifier.size(ButtonDefaults.IconSize).placeholder(buttonPlaceholderState)
            ) {
                if (imageVector != null) {
                    Icon(
                        imageVector = imageVector!!,
                        contentDescription = "Heart",
                        modifier =
                            Modifier.wrapContentSize(align = Alignment.Center)
                                .size(ButtonDefaults.IconSize)
                                .fillMaxSize(),
                    )
                }
            }
        },
    )
    // Simulate content loading completing in stages
    LaunchedEffect(Unit) {
        delay(2000)
        imageVector = Icons.Filled.Favorite
        delay(1000)
        labelText = "A label"
    }
}

/**
 * This sample doesn't use placeholders for the label as there is some cached data that can be shown
 * while loading.
 */
@Sampled
@Composable
fun ButtonWithIconAndLabelCachedData() {
    var labelText by remember { mutableStateOf("Cached Data") }
    var imageVector: ImageVector? by remember { mutableStateOf(null) }
    val buttonPlaceholderState =
        rememberPlaceholderState(isVisible = labelText.isEmpty() || imageVector == null)

    // Put placeholderShimmer in the container and placeholder in the elements of the content that
    // have no cached data.
    FilledTonalButton(
        onClick = { /* Do something */ },
        enabled = true,
        modifier = Modifier.fillMaxWidth().placeholderShimmer(buttonPlaceholderState),
        label = {
            Text(
                text = labelText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        icon = {
            Box(
                modifier =
                    Modifier.size(ButtonDefaults.IconSize).placeholder(buttonPlaceholderState)
            ) {
                if (imageVector != null) {
                    Icon(
                        imageVector = imageVector!!,
                        contentDescription = "Heart",
                        modifier =
                            Modifier.wrapContentSize(align = Alignment.Center)
                                .size(ButtonDefaults.IconSize)
                                .fillMaxSize(),
                    )
                }
            }
        },
    )
    // Simulate content loading completing in stages
    LaunchedEffect(Unit) {
        delay(2000)
        imageVector = Icons.Filled.Favorite
        delay(1000)
        labelText = "A label"
    }
}

/**
 * This sample applies a placeholder and placeholderShimmer directly over a single composable.
 *
 * Note that the modifier ordering is important, the placeholderShimmer must be before the
 * placeholder in the modifier chain - otherwise the shimmer will be drawn underneath the
 * placeholder and will not be visible.
 */
@Sampled
@Composable
fun TextPlaceholder() {
    var labelText by remember { mutableStateOf("") }
    val placeholderState = rememberPlaceholderState(isVisible = labelText.isEmpty())

    Text(
        text = labelText,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier =
            Modifier.width(90.dp).placeholderShimmer(placeholderState).placeholder(placeholderState),
    )

    // Simulate content loading
    LaunchedEffect(Unit) {
        delay(3000)
        labelText = "A label"
    }
}
```