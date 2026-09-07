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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

@Preview
@Sampled
@Composable
fun ScaffoldSample() {
    // Declare just one [AppScaffold] per app such as in the activity.
    // [AppScaffold] allows static screen elements (i.e. [TimeText]) to remain visible
    // during in-app transitions such as swipe-to-dismiss.
    AppScaffold {
        val transformationSpec = rememberTransformationSpec()

        // Define the navigation hierarchy within the AppScaffold,
        // such as using SwipeDismissableNavHost.
        // For this sample, we will define a single screen inline.
        val listState = rememberTransformingLazyColumnState()

        // By default, ScreenScaffold will handle transitions showing/hiding ScrollIndicator,
        // showing/hiding/scrolling away TimeText and optionally hosting the EdgeButton.
        ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(10) {
                    Button(
                        onClick = {},
                        label = { Text("Item ${it + 1}") },
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier =
                            Modifier.transformedHeight(this, transformationSpec)
                                .minimumVerticalContentPadding(
                                    ButtonDefaults.minimumVerticalListContentPadding
                                )
                                .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Preview
@Sampled
@Composable
fun ScaffoldWithTLCEdgeButtonSample() {
    // Declare just one [AppScaffold] per app such as in the activity.
    // [AppScaffold] allows static screen elements (i.e. [TimeText]) to remain visible
    // during in-app transitions such as swipe-to-dismiss.
    AppScaffold(modifier = Modifier.background(Color.Black)) {
        val transformationSpec = rememberTransformationSpec()

        // Define the navigation hierarchy within the AppScaffold,
        // such as using SwipeDismissableNavHost.
        // For this sample, we will define a single screen inline.
        val listState = rememberTransformingLazyColumnState()
        // By default, ScreenScaffold will handle transitions showing/hiding ScrollIndicator,
        // showing/hiding/scrolling away TimeText and optionally hosting the EdgeButton.
        ScreenScaffold(
            scrollState = listState,
            // Define custom spacing between [EdgeButton] and [TransformingLazyColumn].
            edgeButtonSpacing = 15.dp,
            edgeButton = {
                EdgeButton(
                    onClick = {},
                    modifier =
                        // In case user starts scrolling from the EdgeButton.
                        Modifier.scrollable(
                            listState,
                            orientation = Orientation.Vertical,
                            reverseDirection = true,
                            // An overscroll effect should be applied to the EdgeButton for proper
                            // scrolling behavior.
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
                ) {
                    Text("Clear All")
                }
            },
        ) { contentPadding ->
            TransformingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // Bottom spacing is derived from [ScreenScaffold.edgeButtonSpacing].
                contentPadding = contentPadding,
            ) {
                items(4) {
                    Button(
                        onClick = {},
                        label = { Text("Item ${it + 1}") },
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier =
                            Modifier.transformedHeight(this, transformationSpec)
                                .minimumVerticalContentPadding(
                                    ButtonDefaults.minimumVerticalListContentPadding
                                )
                                .fillMaxWidth(),
                    )
                }
            }
        }
    }
}
```