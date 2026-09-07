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

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState

@Sampled
@Composable
fun SimpleHorizontalPagerSample() {
    // Creates a horizontal pager with 10 elements
    val state = rememberPagerState { 10 }
    HorizontalPager(modifier = Modifier.fillMaxSize(), state = state) { page ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BasicText(text = "Page $page", style = TextStyle(color = Color.White))
        }
    }
}

@Sampled
@Composable
fun SimpleVerticalPagerSample() {
    // Creates a vertical pager with 10 elements
    val state = rememberPagerState { 10 }
    VerticalPager(modifier = Modifier.fillMaxSize(), state = state) { page ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            BasicText(text = "Page $page", style = TextStyle(color = Color.White))
        }
    }
}
```