```
/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy.Companion.ModulateAlpha
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.ResponsiveTransformationSpec
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.TransformationVariableSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

@Composable
@Sampled
@Preview
fun CustomTransformationSpecSample() {
    val transformationSpec = rememberTransformationSpec()
    val morphingTransformationSpec =
        object : TransformationSpec by transformationSpec {
            override fun GraphicsLayerScope.applyContainerTransformation(
                scrollProgress: TransformingLazyColumnItemScrollProgress
            ) {
                with(transformationSpec) { applyContainerTransformation(scrollProgress) }
                rotationX = (scrollProgress.topOffsetFraction - 0.5f).coerceIn(0f..1f) * 270f
            }
        }

    TransformingLazyColumn(
        contentPadding = PaddingValues(20.dp),
        modifier = Modifier.background(Color.Black),
    ) {
        items(count = 100) { index ->
            Button(
                onClick = {},
                modifier =
                    Modifier.fillMaxWidth()
                        .transformedHeight(this, morphingTransformationSpec)
                        .graphicsLayer {
                            with(morphingTransformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        },
            ) {
                Text(
                    "Item $index",
                    modifier =
                        Modifier.graphicsLayer {
                            with(morphingTransformationSpec) {
                                applyContentTransformation(scrollProgress)
                            }
                        },
                )
            }
        }
    }
}

@Composable
@Sampled
@Preview
fun CustomCompositingStrategyTransformationSpecSample() {
    val transformationSpec = rememberTransformationSpec()

    TransformingLazyColumn(
        contentPadding = PaddingValues(20.dp),
        modifier = Modifier.background(Color.Black),
    ) {
        items(count = 100) { index ->
            Button(
                onClick = {},
                modifier =
                    Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                            // Using CompositingStrategy.ModulateAlpha can provide better
                            // performance when a container transformation involves alpha rendering,
                            // as it avoids an extra offscreen buffer.
                            //
                            // However, care must be taken with overlapping or transparent content
                            // inside the container. If the content itself uses alpha, ModulateAlpha
                            // can lead to multiple, incorrect alpha blending with the background
                            // (double-blending artifacts).
                            //
                            // To prevent this, the content's drawing layer must explicitly use
                            // CompositingStrategy.Offscreen to ensure internal elements are
                            // correctly pre-blended before the outer ModulateAlpha is applied.
                            compositingStrategy = ModulateAlpha
                        },
            ) {
                Text(
                    text = "Item $index",
                    modifier =
                        Modifier.graphicsLayer {
                            // Ensure content layer uses CompositingStrategy.Offscreen when
                            // container uses CompositingStrategy.ModulateAlpha.
                            // This composition is required to guarantee correct visual blending of
                            // content that contains internal alpha or complex blending.
                            // compositingStrategy is set to CompositingStrategy.Offscreen inside
                            // applyContentTransformation.
                            with(transformationSpec) { applyContentTransformation(scrollProgress) }
                        },
                )
            }
        }
    }
}

@Composable
@Sampled
@Preview
fun ResponsiveTransformationSpecButtonSample() {
    val transformationSpec =
        rememberTransformationSpec(
            ResponsiveTransformationSpec.smallScreen(
                // Makes the content disappear on the edges.
                contentAlpha = TransformationVariableSpec(0f)
            ),
            ResponsiveTransformationSpec.largeScreen(
                // Makes the content disappear on the edges, but a bit more aggressively.
                contentAlpha =
                    TransformationVariableSpec(0f, transformationZoneEnterFraction = 0.2f)
            ),
        )
    TransformingLazyColumn(
        contentPadding = PaddingValues(20.dp),
        modifier = Modifier.background(Color.Black),
    ) {
        items(count = 100) { index ->
            Button(
                onClick = {},
                modifier =
                    Modifier.fillMaxWidth().transformedHeight(this@items, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
            ) {
                Text("Item $index")
            }
        }
    }
}

@Composable
@Sampled
@Preview
fun TransformationSpecButtonRowSample() {
    // Use the spec derived from default small and large screen specs.
    val transformationSpec = rememberTransformationSpec()

    TransformingLazyColumn(
        contentPadding = PaddingValues(),
        modifier = Modifier.background(Color.Black),
    ) {
        items(count = 100) {
            val interactionSource1 = remember { MutableInteractionSource() }
            val interactionSource2 = remember { MutableInteractionSource() }

            ButtonGroup(
                modifier =
                    Modifier.fillMaxWidth()
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        )
            ) {
                Button(
                    onClick = {},
                    modifier = Modifier.animateWidth(interactionSource1),
                    interactionSource = interactionSource1,
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "L",
                            modifier =
                                Modifier.graphicsLayer {
                                    with(transformationSpec) {
                                        applyContentTransformation(scrollProgress)
                                    }
                                },
                        )
                    }
                }
                Button(
                    onClick = {},
                    modifier = Modifier.animateWidth(interactionSource2),
                    interactionSource = interactionSource2,
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            "R",
                            modifier =
                                Modifier.graphicsLayer {
                                    with(transformationSpec) {
                                        applyContentTransformation(scrollProgress)
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}
```