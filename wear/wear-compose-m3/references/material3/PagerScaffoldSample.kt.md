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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerDefaults
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.VerticalPagerScaffold

@Sampled
@Composable
fun HorizontalPagerScaffoldSample(navigateBack: () -> Unit) {
    AppScaffold {
        val pagerState = rememberPagerState(pageCount = { 10 })

        HorizontalPagerScaffold(pagerState = pagerState) {
            HorizontalPager(
                state = pagerState,
                flingBehavior =
                    PagerDefaults.snapFlingBehavior(
                        state = pagerState,
                        maxFlingPages = 1,
                        snapPositionalThreshold = PagerScaffoldDefaults.HighSnapPositionalThreshold,
                        snapAnimationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    ),
                rotaryScrollableBehavior = null,
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
                            if (page == 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = navigateBack) { Text("Exit") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Sampled
@Composable
fun HorizontalPagerScaffoldWithLowSensitivitySample(navigateBack: () -> Unit) {
    AppScaffold {
        val pagerState = rememberPagerState(pageCount = { 3 })

        HorizontalPagerScaffold(pagerState = pagerState) {
            HorizontalPager(
                state = pagerState,
                flingBehavior =
                    PagerDefaults.snapFlingBehavior(
                        state = pagerState,
                        maxFlingPages = 0,
                        snapPositionalThreshold = PagerScaffoldDefaults.LowSnapPositionalThreshold,
                        snapAnimationSpec = PagerDefaults.SnapAnimationSpec,
                    ),
                rotaryScrollableBehavior =
                    RotaryScrollableDefaults.snapBehavior(
                        pagerState = pagerState,
                        snapSensitivity = RotaryScrollableDefaults.LowSnapSensitivity,
                    ),
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
                            if (page == 0) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = navigateBack) { Text("Exit") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Sampled
@Composable
fun VerticalPagerScaffoldSample() {
    AppScaffold {
        val pagerState = rememberPagerState(pageCount = { 10 })

        VerticalPagerScaffold(pagerState = pagerState) {
            VerticalPager(
                state = pagerState,
                flingBehavior =
                    PagerDefaults.snapFlingBehavior(
                        state = pagerState,
                        maxFlingPages = 1,
                        snapPositionalThreshold = PagerScaffoldDefaults.HighSnapPositionalThreshold,
                        snapAnimationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    ),
                rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(pagerState),
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
}

@Sampled
@Composable
fun VerticalPagerScaffoldWithLowSensitivitySample() {
    AppScaffold {
        val pagerState = rememberPagerState(pageCount = { 3 })

        VerticalPagerScaffold(pagerState = pagerState) {
            VerticalPager(
                state = pagerState,
                flingBehavior =
                    PagerDefaults.snapFlingBehavior(
                        state = pagerState,
                        maxFlingPages = 0,
                        snapPositionalThreshold = PagerScaffoldDefaults.LowSnapPositionalThreshold,
                        snapAnimationSpec = PagerDefaults.SnapAnimationSpec,
                    ),
                rotaryScrollableBehavior =
                    RotaryScrollableDefaults.snapBehavior(
                        pagerState = pagerState,
                        snapSensitivity = RotaryScrollableDefaults.LowSnapSensitivity,
                    ),
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
}
```