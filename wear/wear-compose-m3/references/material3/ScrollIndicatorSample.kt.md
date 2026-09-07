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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ScreenScaffoldDefaults
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText

@Sampled
@Preview
@Composable
fun ScrollIndicatorWithTLCSample() {
    val scrollState = rememberTransformingLazyColumnState()
    Box(modifier = Modifier.fillMaxSize()) {
        TransformingLazyColumn(
            modifier = Modifier.background(Color.Black),
            state = scrollState,
            contentPadding = ScreenScaffoldDefaults.contentPadding,
        ) {
            items(15) {
                Button(
                    onClick = {},
                    label = { Text("Button $it") },
                    modifier =
                        Modifier.minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            )
                            .fillMaxWidth(),
                )
            }
        }
        ScrollIndicator(modifier = Modifier.align(Alignment.CenterEnd), state = scrollState)
        TimeText()
    }
}
```