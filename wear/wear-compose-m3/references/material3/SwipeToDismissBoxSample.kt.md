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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.SwipeToDismissValue
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SwipeToDismissBox
import androidx.wear.compose.material3.Text

@Sampled
@Composable
fun SimpleSwipeToDismissBox(navigateBack: () -> Unit) {
    SwipeToDismissBox(onDismissed = navigateBack) { isBackground ->
        if (isBackground) {
            Box(
                modifier =
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Swipe right to dismiss", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Sampled
@Composable
fun StatefulSwipeToDismissBox() {
    // State for managing a 2-level navigation hierarchy between
    // MainScreen and ItemScreen composables.
    // Alternatively, use SwipeDismissableNavHost from wear.compose.navigation.
    var showMainScreen by remember { mutableStateOf(true) }
    val saveableStateHolder = rememberSaveableStateHolder()

    // Swipe gesture dismisses ItemScreen to return to MainScreen.
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissValue.Dismissed) {
            state.snapTo(SwipeToDismissValue.Default)
            showMainScreen = !showMainScreen
        }
    }

    // Hierarchy is ListScreen -> ItemScreen, so we show ListScreen as the background behind
    // the ItemScreen, otherwise there's no background to show.
    SwipeToDismissBox(
        state = state,
        userSwipeEnabled = !showMainScreen,
        backgroundKey = if (!showMainScreen) "MainKey" else "Background",
        contentKey = if (showMainScreen) "MainKey" else "ItemKey",
    ) { isBackground ->
        if (isBackground || showMainScreen) {
            // Best practice would be to use State Hoisting and leave this composable stateless.
            // Here, we want to support MainScreen being shown from different destinations
            // (either in the foreground or in the background during swiping) - that can be achieved
            // using SaveableStateHolder and rememberSaveable as shown below.
            saveableStateHolder.SaveableStateProvider(
                key = "MainKey",
                content = {
                    // Composable that maintains its own state
                    // and can be shown in foreground or background.
                    val checked = rememberSaveable { mutableStateOf(true) }
                    Column(
                        modifier =
                            Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        FilledTonalButton(
                            onClick = { showMainScreen = false },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Item details")
                        }
                        CheckboxButton(
                            label = { Text("Checkbox", maxLines = 1) },
                            checked = checked.value,
                            onCheckedChange = { checked.value = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Show details here...", color = MaterialTheme.colorScheme.onPrimary)
                Text("Swipe right to dismiss", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Sampled
@Composable
fun EdgeSwipeForSwipeToDismiss(navigateBack: () -> Unit) {
    val state = rememberSwipeToDismissBoxState()

    // When using Modifier.edgeSwipeToDismiss, it is required that the element on which the
    // modifier applies exists within a SwipeToDismissBox which shares the same state.
    SwipeToDismissBox(state = state, onDismissed = navigateBack) { isBackground ->
        val horizontalScrollState = rememberScrollState(0)
        if (isBackground) {
            Box(
                modifier =
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer)
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    modifier =
                        Modifier.align(Alignment.Center)
                            .edgeSwipeToDismiss(state)
                            .horizontalScroll(horizontalScrollState),
                    text =
                        "This text can be scrolled horizontally - to dismiss, swipe " +
                            "right from the left edge of the screen (called Edge Swiping)",
                )
            }
        }
    }
}
```