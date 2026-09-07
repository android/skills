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

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.RotarySnapLayoutInfoProvider
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.Text

@Sampled
@Composable
fun RotaryScrollSample() {
    val scrollableState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    LazyColumn(
        modifier =
            Modifier.fillMaxSize()
                .requestFocusOnHierarchyActive()
                .rotaryScrollable(
                    behavior = RotaryScrollableDefaults.behavior(scrollableState),
                    focusRequester = focusRequester,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        state = scrollableState,
    ) {
        items(300) {
            BasicText(
                text = "item $it",
                modifier = Modifier.background(Color.Gray),
                style = TextStyle.Default.copy(),
            )
        }
    }
}

@Sampled
@Composable
fun RotaryScrollWithOverscrollSample() {
    val scrollableState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val overscrollEffect = rememberOverscrollEffect()

    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp

    Column(
        Modifier.fillMaxSize()
            .requestFocusOnHierarchyActive()
            .rotaryScrollable(
                behavior = RotaryScrollableDefaults.behavior(scrollableState),
                focusRequester = focusRequester,
                overscrollEffect = overscrollEffect,
            )
            .verticalScroll(scrollableState, overscrollEffect)
            .overscroll(overscrollEffect),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Top")
        Spacer(modifier = Modifier.height(screenHeightDp / 2))
        Text("Scroll this list up and down with rotary input", textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(screenHeightDp / 2))
        Text("Bottom")
    }
}

@Sampled
@Composable
fun RotarySnapSample() {
    val scrollableState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    LazyColumn(
        modifier =
            Modifier.fillMaxSize()
                .requestFocusOnHierarchyActive()
                .rotaryScrollable(
                    behavior =
                        RotaryScrollableDefaults.snapBehavior(
                            scrollableState,
                            // This sample has a custom implementation of
                            // RotarySnapLayoutInfoProvider which is required for snapping behavior.
                            // ScalingLazyColumn has it built-in, so it's not required there.
                            remember(scrollableState) {
                                object : RotarySnapLayoutInfoProvider {

                                    override val averageItemSize: Float
                                        get() {
                                            val items = scrollableState.layoutInfo.visibleItemsInfo
                                            return (items.fastSumBy { it.size } / items.size)
                                                .toFloat()
                                        }

                                    override val currentItemIndex: Int
                                        get() = scrollableState.firstVisibleItemIndex

                                    override val currentItemOffset: Float
                                        get() =
                                            scrollableState.firstVisibleItemScrollOffset.toFloat()

                                    override val totalItemCount: Int
                                        get() = scrollableState.layoutInfo.totalItemsCount
                                }
                            },
                        ),
                    focusRequester = focusRequester,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
        state = scrollableState,
    ) {
        items(300) {
            BasicText(text = "item $it", modifier = Modifier.background(Color.Gray).height(50.dp))
        }
    }
}
```