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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FadingExpandingLabel

@Sampled
@Composable
fun FadingExpandingLabelButtonSample() {
    var text by remember { mutableStateOf("Line of Text One.") }
    var lines by remember { mutableIntStateOf(1) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            onClick = {
                lines = lines % 3 + 1
                text =
                    (1..lines).joinToString(separator = "\n") {
                        when (it) {
                            1 -> "Line of Text One."
                            2 -> "Line of Text Two."
                            else -> "Line of Text Three."
                        }
                    }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { FadingExpandingLabel(text = text, textAlign = TextAlign.Left) },
        )
    }
}
```