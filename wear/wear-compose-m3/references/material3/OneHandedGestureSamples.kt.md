```
/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.Sampled
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.rememberAmbientModeManager
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.VerticalPagerScaffold
import androidx.wear.compose.material3.onehandedgesture.LocalOneHandedGestureEnabled
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureAction
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureClickIndicatorState
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureDefaults
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureHorizontalPageIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGesturePageIndicatorState
import androidx.wear.compose.material3.onehandedgesture.OneHandedGesturePriority
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureScrollIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureScrollIndicatorState
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureVerticalPageIndicator
import androidx.wear.compose.material3.onehandedgesture.oneHandedGesture
import androidx.wear.compose.material3.onehandedgesture.rememberOneHandedGestureConfiguration
import kotlinx.coroutines.launch

@Sampled
@Composable
fun OneHandedGestureButtonSample() {
    var label by remember { mutableStateOf("Gesturable Button") }
    val onClick = { label = "Clicked/Gestured" }
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
                Modifier.fillMaxWidth()
                    .oneHandedGesture(
                        gestureConfiguration = gestureConfig,
                        interactionSource = interactionSource,
                        onGestureLabel = "activate the button",
                        onGestureAvailable = {
                            coroutineScope.launch { indicatorState.showIndicator() }
                        },
                        onGesture = onClick,
                    ),
        ) {
            OneHandedGestureClickIndicator(gestureConfig, indicatorState) {
                Text(label, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureButtonInAmbientSample() {
    var label by remember { mutableStateOf("Gesturable Button") }
    val onClick = { label = "Clicked/Gestured" }
    val interactionSource = remember { MutableInteractionSource() }
    val gestureConfig =
        rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
    val indicatorState = remember { OneHandedGestureClickIndicatorState() }
    val coroutineScope = rememberCoroutineScope()
    val activityAmbientModeManager = rememberAmbientModeManager()
    var showGestureIndicator by remember { mutableStateOf(true) }

    CompositionLocalProvider(LocalAmbientModeManager provides activityAmbientModeManager) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val isInAmbientMode =
                LocalAmbientModeManager.current?.currentAmbientMode is AmbientMode.Ambient
            Button(
                onClick = onClick,
                interactionSource = interactionSource,
                modifier =
                    Modifier.fillMaxWidth()
                        .oneHandedGesture(
                            gestureConfiguration = gestureConfig,
                            interactionSource = interactionSource,
                            enabledInAmbient = true,
                            onGestureLabel = "activate the button",
                            onGestureAvailable = {
                                // Bypass repeating the gesture indicator on every recomposition.
                                if (showGestureIndicator) {
                                    coroutineScope.launch { indicatorState.showIndicator() }
                                    showGestureIndicator = false
                                }
                            },
                            onGesture = onClick,
                        ),
                colors =
                    if (isInAmbientMode) ButtonDefaults.outlinedButtonColors()
                    else ButtonDefaults.buttonColors(),
                border =
                    if (isInAmbientMode) ButtonDefaults.outlinedButtonBorder(enabled = true)
                    else null,
            ) {
                OneHandedGestureClickIndicator(gestureConfig, indicatorState) {
                    Text(label, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureDisableButtonSample() {
    var counter by remember { mutableIntStateOf(0) }
    var enabled by remember { mutableStateOf(true) }
    val interactionSource = remember { MutableInteractionSource() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SwitchButton(checked = enabled, onCheckedChange = { enabled = it }) {
                Text("Gestures enabled")
            }
            Spacer(modifier = Modifier.height(6.dp))
            CompositionLocalProvider(LocalOneHandedGestureEnabled provides enabled) {
                val gestureConfig =
                    rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
                val indicatorState = remember { OneHandedGestureClickIndicatorState() }
                Button(
                    onClick = {},
                    interactionSource = interactionSource,
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfiguration = gestureConfig,
                            interactionSource = interactionSource,
                            onGestureLabel = "increase the counter",
                            onGestureAvailable = {
                                coroutineScope.launch { indicatorState.showIndicator() }
                            },
                            onGesture = { counter++ },
                        ),
                ) {
                    OneHandedGestureClickIndicator(gestureConfig, indicatorState) {
                        Text("Gestured $counter times")
                    }
                }
            }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureTransformingLazyColumnSample() {
    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val onClick =
        remember<() -> Unit> { { backDispatcherOwner?.onBackPressedDispatcher?.onBackPressed() } }
    val scrollState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()

    val buttonInteractionSource = remember { MutableInteractionSource() }
    val buttonGestureConfig =
        rememberOneHandedGestureConfiguration(
            action = OneHandedGestureAction.Primary,
            priority = OneHandedGesturePriority.Clickable,
        )
    val buttonIndicatorState = remember { OneHandedGestureClickIndicatorState() }

    val scrollGestureConfig =
        rememberOneHandedGestureConfiguration(
            action = OneHandedGestureAction.Primary,
            priority = OneHandedGesturePriority.Scrollable,
        )
    val scrollIndicatorState =
        remember(scrollGestureConfig) { OneHandedGestureScrollIndicatorState() }

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onClick,
                interactionSource = buttonInteractionSource,
                modifier =
                    if (scrollState.canScrollForward) {
                        Modifier
                    } else {
                        // Apply the one-handed gesture modifier only when the container cannot
                        // scroll further, ensuring the EdgeButton is fully visible and interactive
                        Modifier.oneHandedGesture(
                            gestureConfiguration = buttonGestureConfig,
                            interactionSource = buttonInteractionSource,
                            onGestureLabel = "close",
                            onGestureAvailable = {
                                coroutineScope.launch { buttonIndicatorState.showIndicator() }
                            },
                            onGesture = onClick,
                        )
                    } then
                        Modifier.scrollable(
                            state = scrollState,
                            orientation = Orientation.Vertical,
                            reverseDirection = true,
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
            ) {
                OneHandedGestureClickIndicator(buttonGestureConfig, buttonIndicatorState) {
                    Text("Close")
                }
            }
        },
        scrollIndicator = {
            OneHandedGestureScrollIndicator(
                gestureConfiguration = scrollGestureConfig,
                indicatorState = scrollIndicatorState,
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier =
                Modifier.fillMaxSize()
                    .oneHandedGesture(
                        gestureConfiguration = scrollGestureConfig,
                        onGestureLabel = "scroll",
                        onGestureAvailable = {
                            coroutineScope.launch { scrollIndicatorState.showIndicator() }
                        },
                        onGesture = { OneHandedGestureDefaults.scrollDown(scrollState) },
                    ),
        ) {
            items(10) { Text("Item $it") }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureScalingLazyColumnSample() {
    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val onClick =
        remember<() -> Unit> { { backDispatcherOwner?.onBackPressedDispatcher?.onBackPressed() } }
    val scrollState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val buttonInteractionSource = remember { MutableInteractionSource() }
    val buttonGestureConfig =
        rememberOneHandedGestureConfiguration(
            action = OneHandedGestureAction.Primary,
            priority = OneHandedGesturePriority.Clickable,
        )
    val buttonIndicatorState = remember { OneHandedGestureClickIndicatorState() }

    val scrollGestureConfig =
        rememberOneHandedGestureConfiguration(
            action = OneHandedGestureAction.Primary,
            priority = OneHandedGesturePriority.Scrollable,
        )
    val scrollIndicatorState =
        remember(scrollGestureConfig) { OneHandedGestureScrollIndicatorState() }

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onClick,
                interactionSource = buttonInteractionSource,
                modifier =
                    if (scrollState.canScrollForward) {
                        Modifier
                    } else {
                        // Apply the one-handed gesture modifier only when the container cannot
                        // scroll further, ensuring the EdgeButton is fully visible and interactive
                        Modifier.oneHandedGesture(
                            gestureConfiguration = buttonGestureConfig,
                            interactionSource = buttonInteractionSource,
                            onGestureLabel = "close",
                            onGestureAvailable = {
                                coroutineScope.launch { buttonIndicatorState.showIndicator() }
                            },
                            onGesture = onClick,
                        )
                    } then
                        Modifier.scrollable(
                            state = scrollState,
                            orientation = Orientation.Vertical,
                            reverseDirection = true,
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
            ) {
                OneHandedGestureClickIndicator(buttonGestureConfig, buttonIndicatorState) {
                    Text("Close")
                }
            }
        },
        scrollIndicator = {
            OneHandedGestureScrollIndicator(
                gestureConfiguration = scrollGestureConfig,
                indicatorState = scrollIndicatorState,
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        },
    ) { contentPadding ->
        ScalingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier =
                Modifier.fillMaxSize()
                    .oneHandedGesture(
                        gestureConfiguration = scrollGestureConfig,
                        onGestureLabel = "scroll",
                        onGestureAvailable = {
                            coroutineScope.launch { scrollIndicatorState.showIndicator() }
                        },
                        onGesture = { OneHandedGestureDefaults.scrollDown(scrollState) },
                    ),
            autoCentering = null,
        ) {
            items(10) { Text("Item $it") }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureTransformingLazyColumnScrollToNextItemSample() {
    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val onClick =
        remember<() -> Unit> { { backDispatcherOwner?.onBackPressedDispatcher?.onBackPressed() } }
    val scrollState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()

    val buttonInteractionSource = remember { MutableInteractionSource() }
    val buttonGestureConfig =
        rememberOneHandedGestureConfiguration(
            action = OneHandedGestureAction.Primary,
            priority = OneHandedGesturePriority.Clickable,
        )
    val buttonIndicatorState = remember { OneHandedGestureClickIndicatorState() }

    val scrollGestureConfig =
        rememberOneHandedGestureConfiguration(
            action = OneHandedGestureAction.Primary,
            priority = OneHandedGesturePriority.Scrollable,
        )
    val scrollIndicatorState =
        remember(scrollGestureConfig) { OneHandedGestureScrollIndicatorState() }

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onClick,
                interactionSource = buttonInteractionSource,
                modifier =
                    if (scrollState.canScrollForward) {
                        Modifier
                    } else {
                        // Apply the one-handed gesture modifier only when the container cannot
                        // scroll further, ensuring the EdgeButton is fully visible and interactive
                        Modifier.oneHandedGesture(
                            gestureConfiguration = buttonGestureConfig,
                            interactionSource = buttonInteractionSource,
                            onGestureLabel = "close",
                            onGestureAvailable = {
                                coroutineScope.launch { buttonIndicatorState.showIndicator() }
                            },
                            onGesture = onClick,
                        )
                    } then
                        Modifier.scrollable(
                            state = scrollState,
                            orientation = Orientation.Vertical,
                            reverseDirection = true,
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
            ) {
                OneHandedGestureClickIndicator(buttonGestureConfig, buttonIndicatorState) {
                    Text("Close")
                }
            }
        },
        scrollIndicator = {
            OneHandedGestureScrollIndicator(
                gestureConfiguration = scrollGestureConfig,
                indicatorState = scrollIndicatorState,
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier =
                Modifier.fillMaxSize()
                    .oneHandedGesture(
                        gestureConfiguration = scrollGestureConfig,
                        onGestureLabel = "scroll",
                        onGestureAvailable = {
                            coroutineScope.launch { scrollIndicatorState.showIndicator() }
                        },
                        onGesture = { OneHandedGestureDefaults.scrollDownToNextItem(scrollState) },
                    ),
        ) {
            items(10) { Text("Item $it") }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureScalingLazyColumnScrollToNextItemSample() {
    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val onClick =
        remember<() -> Unit> { { backDispatcherOwner?.onBackPressedDispatcher?.onBackPressed() } }
    val scrollState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val buttonInteractionSource = remember { MutableInteractionSource() }
    val buttonGestureConfig =
        rememberOneHandedGestureConfiguration(
            action = OneHandedGestureAction.Primary,
            priority = OneHandedGesturePriority.Clickable,
        )
    val buttonIndicatorState = remember { OneHandedGestureClickIndicatorState() }

    val scrollGestureConfig =
        rememberOneHandedGestureConfiguration(
            action = OneHandedGestureAction.Primary,
            priority = OneHandedGesturePriority.Scrollable,
        )
    val scrollIndicatorState =
        remember(scrollGestureConfig) { OneHandedGestureScrollIndicatorState() }

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onClick,
                interactionSource = buttonInteractionSource,
                modifier =
                    if (scrollState.canScrollForward) {
                        Modifier
                    } else {
                        // Apply the one-handed gesture modifier only when the container cannot
                        // scroll further, ensuring the EdgeButton is fully visible and interactive
                        Modifier.oneHandedGesture(
                            gestureConfiguration = buttonGestureConfig,
                            interactionSource = buttonInteractionSource,
                            onGestureLabel = "close",
                            onGestureAvailable = {
                                coroutineScope.launch { buttonIndicatorState.showIndicator() }
                            },
                            onGesture = onClick,
                        )
                    } then
                        Modifier.scrollable(
                            state = scrollState,
                            orientation = Orientation.Vertical,
                            reverseDirection = true,
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
            ) {
                OneHandedGestureClickIndicator(buttonGestureConfig, buttonIndicatorState) {
                    Text("Close")
                }
            }
        },
        scrollIndicator = {
            OneHandedGestureScrollIndicator(
                gestureConfiguration = scrollGestureConfig,
                indicatorState = scrollIndicatorState,
                scrollState = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        },
    ) { contentPadding ->
        ScalingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier =
                Modifier.fillMaxSize()
                    .oneHandedGesture(
                        gestureConfiguration = scrollGestureConfig,
                        onGestureLabel = "scroll",
                        onGestureAvailable = {
                            coroutineScope.launch { scrollIndicatorState.showIndicator() }
                        },
                        onGesture = { OneHandedGestureDefaults.scrollDownToNextItem(scrollState) },
                    ),
            autoCentering = null,
        ) {
            items(10) { Text("Item $it") }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureHorizontalPagerSample() {
    val pagerState = rememberPagerState(pageCount = { 10 })
    val gestureConfig =
        rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
    val indicatorState = remember { OneHandedGesturePageIndicatorState() }
    val coroutineScope = rememberCoroutineScope()

    HorizontalPagerScaffold(
        pagerState = pagerState,
        pageIndicator = {
            OneHandedGestureHorizontalPageIndicator(
                gestureConfiguration = gestureConfig,
                indicatorState = indicatorState,
                pagerState = pagerState,
            )
        },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier.oneHandedGesture(
                    gestureConfiguration = gestureConfig,
                    onGestureLabel = "scroll to the next page",
                    onGestureAvailable = {
                        coroutineScope.launch { indicatorState.showIndicator() }
                    },
                ) {
                    OneHandedGestureDefaults.scrollToNextPage(pagerState)
                },
        ) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
                ScreenScaffold {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(text = "Page #$page")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Swipe left and right")
                    }
                }
            }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureVerticalPagerSample() {
    val pagerState = rememberPagerState(pageCount = { 10 })
    val gestureConfig =
        rememberOneHandedGestureConfiguration(action = OneHandedGestureAction.Primary)
    val indicatorState = remember { OneHandedGesturePageIndicatorState() }
    val coroutineScope = rememberCoroutineScope()

    VerticalPagerScaffold(
        pagerState = pagerState,
        pageIndicator = {
            OneHandedGestureVerticalPageIndicator(
                gestureConfiguration = gestureConfig,
                indicatorState = indicatorState,
                pagerState = pagerState,
            )
        },
    ) {
        VerticalPager(
            state = pagerState,
            modifier =
                Modifier.oneHandedGesture(
                    gestureConfiguration = gestureConfig,
                    onGestureLabel = "scroll to the next page",
                    onGestureAvailable = {
                        coroutineScope.launch { indicatorState.showIndicator() }
                    },
                ) {
                    OneHandedGestureDefaults.scrollToNextPage(pagerState)
                },
        ) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
                ScreenScaffold {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(text = "Page #$page")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Swipe up and down")
                    }
                }
            }
        }
    }
}
```