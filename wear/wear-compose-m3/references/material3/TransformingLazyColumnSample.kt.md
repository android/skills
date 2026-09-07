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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnFirstLayoutItemProvider
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.CompactButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.OutlinedCard
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.firstVisibleItemLayoutItemInfo
import androidx.wear.compose.material3.lazy.layoutItemInfoOf
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformingLazyColumnFirstLayoutItemProvider
import androidx.wear.compose.material3.lazy.transformedHeight
import kotlinx.coroutines.delay

@Sampled
@Preview
@Composable
fun TransformingLazyColumnButtonsSample() {
    val state = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    AppScaffold {
        ScreenScaffold(state) { contentPadding ->
            TransformingLazyColumn(state = state, contentPadding = contentPadding) {
                items(count = 50) {
                    Button(
                        onClick = {},
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                    ButtonDefaults.minimumVerticalListContentPadding
                                )
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Item $it")
                    }
                }
            }
        }
    }
}

@Sampled
@Preview
@Composable
fun TransformingLazyColumnMinimumVerticalContentPaddingSample() {
    val transformationSpec = rememberTransformationSpec()
    val state = rememberTransformingLazyColumnState()
    var elements by remember { mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)) }
    var nextElement by remember { mutableIntStateOf(10) }

    fun addElementAfter(index: Int) {
        elements =
            elements.subList(0, index + 1) +
                listOf(nextElement++) +
                elements.subList(index + 1, elements.count())
    }

    fun removeElementAt(index: Int) {
        elements = elements.subList(0, index) + elements.subList(index + 1, elements.count())
    }

    AppScaffold {
        ScreenScaffold(
            state,
            edgeButton = {
                EdgeButton(onClick = { elements = elements.shuffled() }) { Text("Shuffle") }
            },
        ) { contentPadding ->
            TransformingLazyColumn(state = state, contentPadding = contentPadding) {
                itemsIndexed(elements, key = { _, key -> key }) { index, key ->
                    if (key % 3 == 0) {
                        CompactButton(
                            onClick = { removeElementAt(index) },
                            modifier =
                                Modifier.minimumVerticalContentPadding(
                                        CompactButtonDefaults.minimumVerticalListContentPadding
                                    )
                                    .transformedHeight(this, transformationSpec)
                                    .animateItem(),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text("CompactButton $key")
                        }
                    } else if (key % 4 == 0) {
                        ListHeader(
                            modifier =
                                Modifier.minimumVerticalContentPadding(
                                        ListHeaderDefaults.minimumTopListContentPadding,
                                        ListHeaderDefaults.minimumBottomListContentPadding,
                                    )
                                    .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text("ListHeader heading")
                        }
                    } else {
                        Card(
                            onClick = {},
                            modifier =
                                Modifier.fillMaxWidth()
                                    .minimumVerticalContentPadding(
                                        CardDefaults.minimumVerticalListContentPadding
                                    )
                                    .transformedHeight(this, transformationSpec)
                                    .animateItem(),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text("Card $key")
                            Row {
                                Spacer(modifier = Modifier.weight(1f))
                                CompactButton(
                                    onClick = { removeElementAt(index) },
                                    enabled = elements.count() > 1,
                                ) {
                                    Text("-")
                                }
                                CompactButton(onClick = { addElementAfter(index) }) { Text("+") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Sampled
@Preview
@Composable
fun TransformingLazyColumnFirstLayoutItemProviderSample() {
    val state = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    // This sample demonstrates how to use rememberTransformingLazyColumnFirstLayoutItemProvider
    // to control the expansion direction of a dynamically resizing item. By using the voice input
    // card's Bottom/End edge as the layout reference, the card predictably expands upwards.
    // The constant key safely falls back to default layout behavior when scrolled off-screen.
    val firstLayoutItemProvider = rememberTransformingLazyColumnFirstLayoutItemProvider {
        state.layoutItemInfoOf(
            itemKey = "voice_input",
            itemEdge = TransformingLazyColumnFirstLayoutItemProvider.ItemEdge.End,
        )
    }

    var isListening by remember { mutableStateOf(false) }
    var lineCount by remember { mutableIntStateOf(0) }
    // Simple loop simulating text lines arriving over time
    LaunchedEffect(isListening) {
        if (isListening) {
            while (lineCount < 5) {
                delay(300)
                lineCount++
            }
            isListening = false
        }
    }

    AppScaffold {
        ScreenScaffold(state) { contentPadding ->
            TransformingLazyColumn(
                state = state,
                contentPadding = contentPadding,
                firstLayoutItemProvider = firstLayoutItemProvider,
            ) {
                items(count = 3, key = { "message_$it" }) { index ->
                    Card(
                        onClick = {},
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                    CardDefaults.minimumVerticalListContentPadding
                                )
                                .transformedHeight(this, transformationSpec)
                                .animateItem(placementSpec = null),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Previous message $index")
                    }
                }

                item(key = "voice_input") {
                    val voiceText = (1..lineCount).joinToString("\n") { "Voice Input line $it" }

                    OutlinedCard(
                        onClick = {
                            if (lineCount >= 5) lineCount = 0
                            isListening = !isListening
                        },
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                    CardDefaults.minimumVerticalListContentPadding
                                )
                                .transformedHeight(this, transformationSpec)
                                .animateItem(placementSpec = null),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text(text = voiceText.ifEmpty { "Tap to speak" })
                    }
                }
            }
        }
    }
}

@Sampled
@Preview
@Composable
fun TransformingLazyColumnFirstVisibleItemLayoutItemProviderSample() {
    val transformationSpec = rememberTransformationSpec()
    val state = rememberTransformingLazyColumnState()
    var elements by remember { mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)) }
    var nextElement by remember { mutableIntStateOf(10) }

    fun addCardAfter(index: Int) {
        elements =
            elements.subList(0, index + 1) +
                listOf(nextElement++) +
                elements.subList(index + 1, elements.count())
    }

    fun removeCardAt(index: Int) {
        elements = elements.subList(0, index) + elements.subList(index + 1, elements.count())
    }

    // This sample demonstrates how to use rememberTransformingLazyColumnFirstVisibleItemProvider
    // to maintain a stable viewport top when items are added or removed dynamically.
    // It achieves this by using the first visible item as the layout reference.
    AppScaffold {
        ScreenScaffold(state) { contentPadding ->
            TransformingLazyColumn(
                state = state,
                contentPadding = contentPadding,
                firstLayoutItemProvider =
                    rememberTransformingLazyColumnFirstLayoutItemProvider {
                        if (state.isScrollInProgress) null else state.firstVisibleItemLayoutItemInfo
                    },
            ) {
                itemsIndexed(elements, key = { _, key -> key }) { index, cardKey ->
                    Card(
                        onClick = {},
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                    CardDefaults.minimumVerticalListContentPadding
                                )
                                .transformedHeight(this, transformationSpec)
                                .animateItem(),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Card $cardKey")
                        Row {
                            Spacer(modifier = Modifier.weight(1f))
                            CompactButton(
                                onClick = { removeCardAt(index) },
                                enabled = elements.count() > 1,
                            ) {
                                Text("-")
                            }
                            CompactButton(onClick = { addCardAfter(index) }) { Text("+") }
                        }
                    }
                }
            }
        }
    }
}
```