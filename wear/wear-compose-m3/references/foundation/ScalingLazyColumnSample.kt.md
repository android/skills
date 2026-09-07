```
/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import kotlinx.coroutines.launch

@Sampled
@Composable
fun SimpleScalingLazyColumn() {
    ScalingLazyColumn(modifier = Modifier.fillMaxWidth()) {
        item { ListHeader { Text(text = "List Header") } }
        items(20) {
            Chip(
                onClick = {},
                label = { Text("List item $it") },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
    }
}

@Sampled
@Composable
fun SimpleScalingLazyColumnWithSnap() {
    val state = rememberScalingLazyListState()
    ScalingLazyColumn(
        rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollableState = state),
        flingBehavior = ScalingLazyColumnDefaults.snapFlingBehavior(state = state),
        modifier = Modifier.fillMaxWidth(),
        state = state,
    ) {
        item { ListHeader { Text(text = "List Header") } }
        items(20) {
            Chip(
                onClick = {},
                label = { Text("List item $it") },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
    }
}

@Sampled
@Composable
fun ScalingLazyColumnEdgeAnchoredAndAnimatedScrollTo() {
    val coroutineScope = rememberCoroutineScope()
    val itemSpacing = 6.dp
    // Line up the gap between the items on the center-line
    val scrollOffset = with(LocalDensity.current) { -(itemSpacing / 2).roundToPx() }
    val state =
        rememberScalingLazyListState(
            initialCenterItemIndex = 1,
            initialCenterItemScrollOffset = scrollOffset,
        )

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        anchorType = ScalingLazyListAnchorType.ItemStart,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        state = state,
        autoCentering = AutoCenteringParams(itemOffset = scrollOffset),
    ) {
        item { ListHeader { Text(text = "List Header") } }
        items(20) {
            Chip(
                onClick = {
                    coroutineScope.launch {
                        // Add +1 to allow for the ListHeader
                        state.animateScrollToItem(it + 1, scrollOffset)
                    }
                },
                label = { Text("List item $it") },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
    }
}

@Sampled
@Composable
fun SimpleScalingLazyColumnWithContentPadding() {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp),
        autoCentering = null,
    ) {
        item { ListHeader { Text(text = "List Header") } }
        items(20) {
            Chip(
                onClick = {},
                label = { Text("List item $it") },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
    }
}
```