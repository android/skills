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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ScrollInfoProvider
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenStage
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.curvedText
import androidx.wear.compose.material3.scrollAway
import androidx.wear.compose.material3.timeTextSeparator

@Sampled
@Composable
fun ScrollAwaySample() {
    val state = rememberTransformingLazyColumnState()

    Box(modifier = Modifier.fillMaxSize()) {
        TransformingLazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
            item {
                ListHeader(
                    modifier =
                        Modifier.minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding,
                        )
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "TLC",
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(50) {
                FilledTonalButton(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 36.dp)
                            .minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            ),
                    onClick = {},
                    label = { Text("Item ${it + 1}") },
                )
            }
        }
        TimeText(
            // In practice, it is recommended to use the [AppScaffold] and [ScreenScaffold],
            // so that the Material3 scroll away behavior is provided by default, rather than using
            // [Modifier.scrollAway] directly.
            modifier =
                Modifier.scrollAway(
                    scrollInfoProvider = ScrollInfoProvider(state),
                    screenStage = {
                        if (state.isScrollInProgress) ScreenStage.Scrolling else ScreenStage.Idle
                    },
                ),
            content = { time ->
                curvedText("List")
                timeTextSeparator()
                curvedText(time)
            },
        )
    }
}
```