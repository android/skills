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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

@Sampled
@Preview
@Composable
fun ListHeaderSample() {
    val transformationSpec = rememberTransformationSpec()
    val scrollState = rememberTransformingLazyColumnState()

    AppScaffold {
        ScreenScaffold(scrollState = scrollState) { contentPadding ->
            TransformingLazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = contentPadding,
            ) {
                item {
                    ListHeader(
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                    ListHeaderDefaults.minimumTopListContentPadding,
                                    ListHeaderDefaults.minimumBottomListContentPadding,
                                )
                                .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Settings")
                    }
                }
                item {
                    ListSubHeader(
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                    ListHeaderDefaults.minimumTopListContentPadding,
                                    ListHeaderDefaults.minimumBottomListContentPadding,
                                )
                                .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_connectivity),
                                contentDescription = "Connectivity",
                            )
                        },
                        label = { Text("Connectivity") },
                    )
                }
                item {
                    Button(
                        modifier =
                            Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(
                                    ButtonDefaults.minimumVerticalListContentPadding
                                )
                                .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        onClick = {},
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_bluetooth),
                                contentDescription = "Bluetooth",
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                    ) {
                        Text("Bluetooth")
                    }
                }
                item {
                    Button(
                        modifier =
                            Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(
                                    ButtonDefaults.minimumVerticalListContentPadding
                                )
                                .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        onClick = {},
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_wifi),
                                contentDescription = "Wifi",
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                        },
                    ) {
                        Text("Wifi")
                    }
                }
                item {
                    ListSubHeader(
                        modifier =
                            Modifier.minimumVerticalContentPadding(
                                    ListHeaderDefaults.minimumTopListContentPadding,
                                    ListHeaderDefaults.minimumBottomListContentPadding,
                                )
                                .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Display")
                    }
                }
                item {
                    Button(
                        modifier =
                            Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(
                                    ButtonDefaults.minimumVerticalListContentPadding
                                )
                                .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        onClick = {},
                    ) {
                        Text("Change Watchface")
                    }
                }
                item {
                    Button(
                        modifier =
                            Modifier.fillMaxWidth()
                                .minimumVerticalContentPadding(
                                    ButtonDefaults.minimumVerticalListContentPadding
                                )
                                .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        onClick = {},
                    ) {
                        Text("Brightness")
                    }
                }
            }
        }
    }
}
```