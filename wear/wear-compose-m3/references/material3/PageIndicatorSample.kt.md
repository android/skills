```
/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.VerticalPageIndicator
import androidx.wear.compose.material3.VerticalPagerScaffold

@Sampled
@Composable
fun HorizontalPageIndicatorWithPagerSample(navigateBack: () -> Unit) {
    val pageCount = 9
    val pagerState = rememberPagerState { pageCount }

    Box {
        HorizontalPagerScaffold(
            pagerState = pagerState,
            pageIndicator = { HorizontalPageIndicator(pagerState = pagerState) },
        ) {
            HorizontalPager(
                state = pagerState,
                flingBehavior =
                    PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
            ) { page ->
                AnimatedPage(pageIndex = page, pagerState = pagerState) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(text = "Page #$page")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Swipe left and right")
                        if (page == 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = navigateBack) { Text("Exit") }
                        }
                    }
                }
            }
        }
    }
}

@Sampled
@Composable
fun VerticalPageIndicatorWithPagerSample() {
    val pageCount = 9
    val pagerState = rememberPagerState { pageCount }

    Box {
        VerticalPagerScaffold(
            pagerState = pagerState,
            pageIndicator = { VerticalPageIndicator(pagerState = pagerState) },
        ) {
            VerticalPager(
                state = pagerState,
                flingBehavior =
                    PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
            ) { page ->
                AnimatedPage(pageIndex = page, pagerState = pagerState) {
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