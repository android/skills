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

@file:OptIn(ExperimentalWearFoundationApi::class)
@file:Suppress("DEPRECATION")

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.foundation.SwipeToReveal
import androidx.wear.compose.foundation.expandableItem
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.rememberExpandableState
import androidx.wear.compose.foundation.rememberRevealState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Sampled
@Composable
fun SwipeToRevealSample() {
    val state = rememberRevealState()
    val coroutineScope = rememberCoroutineScope()
    SwipeToReveal(
        state = state,
        primaryAction = {
            Box(
                modifier =
                    Modifier.fillMaxSize().clickable {
                        /* Add the primary action */
                        coroutineScope.launch { state.animateTo(RevealValue.RightRevealed) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete")
            }
        },
        undoAction = {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    /* Add the undo action */
                    coroutineScope.launch { state.animateTo(RevealValue.Covered) }
                },
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text(text = "Undo") },
            )
        },
    ) {
        Chip(
            modifier =
                Modifier.fillMaxWidth().semantics {
                    // Use custom actions to make the primary and secondary actions accessible
                    customActions =
                        listOf(
                            CustomAccessibilityAction("Delete") {
                                /* Add the primary action click handler */
                                true
                            }
                        )
                },
            onClick = { /* the click action associated with chip */ },
            colors = ChipDefaults.secondaryChipColors(),
            label = { Text(text = "Swipe Me") },
        )
    }
}

@Sampled
@Composable
fun SwipeToRevealWithDelayedText() {
    val state = rememberRevealState()
    val coroutineScope = rememberCoroutineScope()
    SwipeToReveal(
        state = state,
        primaryAction = {
            Row(
                modifier =
                    Modifier.fillMaxSize().clickable {
                        /* Add the primary action */
                        coroutineScope.launch { state.animateTo(RevealValue.RightRevealed) }
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete")
                if (abs(state.offset) > state.revealThreshold) {
                    // Delay the text appearance so that it has enough space to be displayed
                    val textAlpha =
                        animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 250, delayMillis = 250),
                            label = "PrimaryActionTextAlpha",
                        )
                    Box(modifier = Modifier.graphicsLayer { alpha = textAlpha.value }) {
                        Spacer(Modifier.size(5.dp))
                        Text("Clear")
                    }
                }
            }
        },
        undoAction = {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    /* Add the undo action */
                    coroutineScope.launch { state.animateTo(RevealValue.Covered) }
                },
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text(text = "Undo") },
            )
        },
    ) {
        Chip(
            modifier =
                Modifier.fillMaxWidth().semantics {
                    // Use custom actions to make the primary and secondary actions accessible
                    customActions =
                        listOf(
                            CustomAccessibilityAction("Delete") {
                                /* Add the primary action click handler */
                                true
                            }
                        )
                },
            onClick = { /* the click action associated with chip */ },
            colors = ChipDefaults.secondaryChipColors(),
            label = { Text(text = "Swipe Me") },
        )
    }
}

/**
 * A sample on how to use Swipe To Reveal within a list of items, preferably [ScalingLazyColumn].
 */
@Sampled
@Composable
fun SwipeToRevealWithExpandables() {
    // Shape of actions should match with the overlay content. For example, Chips
    // should use RoundedCornerShape(CornerSize(percent = 50)), Cards should use
    // RoundedCornerShape with appropriate radius, based on the theme.
    val actionShape = RoundedCornerShape(corner = CornerSize(percent = 50))
    val itemCount = 10
    val coroutineScope = rememberCoroutineScope()
    val expandableStates = List(itemCount) { rememberExpandableState(initiallyExpanded = true) }
    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item { ListHeader { Text("Scaling Lazy Column") } }
        repeat(itemCount) { current ->
            expandableItem(state = expandableStates[current]) { isExpanded ->
                val revealState = rememberRevealState()
                if (isExpanded) {
                    SwipeToReveal(
                        state = revealState,
                        primaryAction = {
                            Box(
                                modifier =
                                    Modifier.fillMaxSize()
                                        .background(Color.Red, actionShape)
                                        .clickable {
                                            coroutineScope.launch {
                                                revealState.animateTo(RevealValue.RightRevealed)
                                            }
                                        },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                )
                            }
                        },
                        secondaryAction = {
                            Box(
                                modifier =
                                    Modifier.fillMaxSize()
                                        .background(Color.Gray, actionShape)
                                        .clickable { /* trigger the optional action */ },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More Options",
                                )
                            }
                        },
                        undoAction = {
                            Chip(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    coroutineScope.launch {
                                        revealState.animateTo(RevealValue.Covered)
                                    }
                                },
                                colors = ChipDefaults.secondaryChipColors(),
                                label = { Text(text = "Undo") },
                            )
                        },
                        onFullSwipe = {
                            coroutineScope.launch {
                                delay(1000)
                                expandableStates[current].expanded = false
                            }
                        },
                    ) {
                        Chip(
                            modifier =
                                Modifier.fillMaxWidth().semantics {
                                    // Use custom actions to make the primary and secondary actions
                                    // accessible
                                    customActions =
                                        listOf(
                                            CustomAccessibilityAction("Delete") {
                                                /* Add the primary action click handler */
                                                coroutineScope.launch {
                                                    revealState.animateTo(RevealValue.RightRevealed)
                                                }
                                                true
                                            },
                                            CustomAccessibilityAction("More Options") {
                                                /* Add the secondary action click handler */
                                                true
                                            },
                                        )
                                },
                            onClick = { /* the click action associated with chip */ },
                            colors = ChipDefaults.secondaryChipColors(),
                            label = { Text(text = "Swipe Me") },
                        )
                    }
                }
            }
        }
    }
}
```