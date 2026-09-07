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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.RevealValue
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.SwipeToRevealDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.rememberRevealState
import kotlinx.coroutines.launch

@Composable
@Sampled
fun SwipeToRevealSample() {
    SwipeToReveal(
        primaryAction = {
            PrimaryActionButton(
                onClick = { /* This block is called when the primary action is executed. */ },
                icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                text = { Text("Delete") },
            )
        },
        onSwipePrimaryAction = { /* This block is called when the full swipe gesture is performed. */
        },
        secondaryAction = {
            SecondaryActionButton(
                onClick = { /* This block is called when the secondary action is executed. */ },
                icon = { Icon(Icons.Outlined.MoreVert, contentDescription = "Options") },
            )
        },
        undoPrimaryAction = {
            UndoActionButton(
                onClick = { /* This block is called when the undo primary action is executed. */ },
                text = { Text("Undo Delete") },
            )
        },
    ) {
        Button(
            modifier =
                Modifier.fillMaxWidth().semantics {
                    // Use custom actions to make the primary and secondary actions accessible
                    customActions =
                        listOf(
                            CustomAccessibilityAction("Delete") {
                                /* Add the primary action click handler here */
                                true
                            },
                            CustomAccessibilityAction("Options") {
                                /* Add the secondary click handler here */
                                true
                            },
                        )
                },
            onClick = {},
        ) {
            Text("This Button has two actions", modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
@Sampled
fun SwipeToRevealSingleActionCardSample() {
    SwipeToReveal(
        primaryAction = {
            PrimaryActionButton(
                onClick = { /* This block is called when the primary action is executed. */ },
                icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                text = { Text("Delete") },
                modifier = Modifier.height(SwipeToRevealDefaults.LargeActionButtonHeight),
            )
        },
        onSwipePrimaryAction = { /* This block is called when the full swipe gesture is performed. */
        },
        undoPrimaryAction = {
            UndoActionButton(
                onClick = { /* This block is called when the undo primary action is executed. */ },
                text = { Text("Undo Delete") },
            )
        },
    ) {
        Card(
            modifier =
                Modifier.fillMaxWidth().semantics {
                    // Use custom actions to make the primary action accessible
                    customActions =
                        listOf(
                            CustomAccessibilityAction("Delete") {
                                /* Add the primary action click handler here */
                                true
                            }
                        )
                },
            onClick = {},
        ) {
            Text(
                "This Card has one action, and the revealed button is taller",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview
@Composable
@Sampled
fun SwipeToRevealWithTransformingLazyColumnSample() {
    val transformationSpec = rememberTransformationSpec()
    val tlcState = rememberTransformingLazyColumnState()
    val coroutineScope = rememberCoroutineScope()
    val messages = remember {
        mutableStateListOf<String>().apply {
            for (i in 1..100) {
                add("Message #${i}")
            }
        }
    }

    TransformingLazyColumn(
        state = tlcState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        modifier = Modifier.background(Color.Black),
    ) {
        items(items = messages, key = { it }) { message ->
            val revealState = rememberRevealState()

            // SwipeToReveal should be reset to covered when scrolling occurs.
            LaunchedEffect(tlcState.isScrollInProgress) {
                if (
                    tlcState.isScrollInProgress && revealState.currentValue != RevealValue.Covered
                ) {
                    coroutineScope.launch {
                        revealState.animateTo(targetValue = RevealValue.Covered)
                    }
                }
            }

            SwipeToReveal(
                primaryAction = {
                    PrimaryActionButton(
                        onClick = { messages.remove(message) },
                        icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                        text = { Text("Delete") },
                        modifier = Modifier.height(SwipeToRevealDefaults.LargeActionButtonHeight),
                    )
                },
                revealState = revealState,
                onSwipePrimaryAction = { messages.remove(message) },
                modifier =
                    Modifier.transformedHeight(this@items, transformationSpec)
                        .animateItem()
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                            // Is needed to disable clipping.
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                            clip = false
                        }
                        .minimumVerticalContentPadding(
                            CardDefaults.minimumVerticalListContentPadding
                        ),
            ) {
                TitleCard(
                    onClick = {},
                    title = { Text(message) },
                    subtitle = { Text("Subtitle") },
                    modifier =
                        Modifier.semantics {
                            // Use custom actions to make the primary action accessible
                            customActions =
                                listOf(
                                    CustomAccessibilityAction("Delete") {
                                        messages.remove(message)
                                        true
                                    }
                                )
                        },
                ) {
                    Text("Message body which extends over multiple lines to extend the card")
                }
            }
        }
    }
}

@Preview
@Composable
@Sampled
fun SwipeToRevealWithScalingLazyColumnSample() {
    val slcState = rememberScalingLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val messages = remember {
        mutableStateListOf<String>().apply {
            for (i in 1..100) {
                add("This Button $i has two actions")
            }
        }
    }

    ScalingLazyColumn(
        state = slcState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        modifier = Modifier.background(Color.Black),
    ) {
        items(items = messages, key = { it }) { message ->
            val revealState = rememberRevealState()

            // SwipeToReveal should be reset to covered when scrolling occurs.
            LaunchedEffect(slcState.isScrollInProgress) {
                if (
                    slcState.isScrollInProgress && revealState.currentValue != RevealValue.Covered
                ) {
                    coroutineScope.launch {
                        revealState.animateTo(targetValue = RevealValue.Covered)
                    }
                }
            }

            SwipeToReveal(
                revealState = revealState,
                primaryAction = {
                    PrimaryActionButton(
                        onClick = { messages.remove(message) },
                        icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                        text = { Text("Delete") },
                    )
                },
                onSwipePrimaryAction = { messages.remove(message) },
                secondaryAction = {
                    SecondaryActionButton(
                        onClick = { /* This block is called when the secondary action is executed. */
                        },
                        icon = { Icon(Icons.Outlined.MoreVert, contentDescription = "Options") },
                    )
                },
            ) {
                Button(
                    modifier =
                        Modifier.fillMaxWidth().semantics {
                            // Use custom actions to make the primary and secondary actions
                            // accessible
                            customActions =
                                listOf(
                                    CustomAccessibilityAction("Delete") {
                                        messages.remove(message)
                                        true
                                    },
                                    CustomAccessibilityAction("Options") {
                                        /* Add the secondary click handler here */
                                        true
                                    },
                                )
                        },
                    onClick = {},
                ) {
                    Text(message, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Preview
@Composable
@Sampled
fun SwipeToRevealNoPartialRevealWithScalingLazyColumnSample() {
    val slcState = rememberScalingLazyListState()
    val messages = remember {
        mutableStateListOf<String>().apply {
            for (i in 1..100) {
                add("Message #${i}")
            }
        }
    }

    ScalingLazyColumn(
        state = slcState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        modifier = Modifier.background(Color.Black),
    ) {
        items(items = messages, key = { it }) { message ->
            SwipeToReveal(
                hasPartiallyRevealedState = false,
                primaryAction = {
                    PrimaryActionButton(
                        onClick = { messages.remove(message) },
                        icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                        text = { Text("Delete") },
                    )
                },
                onSwipePrimaryAction = { messages.remove(message) },
            ) {
                Button(
                    modifier =
                        Modifier.fillMaxWidth().semantics {
                            // Use custom actions to make the primary action accessible
                            customActions =
                                listOf(
                                    CustomAccessibilityAction("Delete") {
                                        messages.remove(message)
                                        true
                                    }
                                )
                        },
                    onClick = {},
                ) {
                    Text(message, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
```