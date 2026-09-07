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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.Text

@Sampled
@Composable
fun ButtonGroupSample() {
    val interactionSource1 = remember { MutableInteractionSource() }
    val interactionSource2 = remember { MutableInteractionSource() }

    Box(contentAlignment = Alignment.Center) {
        ButtonGroup(Modifier.fillMaxWidth()) {
            Button(
                onClick = {},
                modifier = Modifier.animateWidth(interactionSource1),
                interactionSource = interactionSource1,
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("L") }
            }
            Button(
                onClick = {},
                modifier = Modifier.animateWidth(interactionSource2),
                interactionSource = interactionSource2,
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("R") }
            }
        }
    }
}

@Sampled
@Composable
fun ButtonGroupThreeButtonsSample() {
    val interactionSource1 = remember { MutableInteractionSource() }
    val interactionSource2 = remember { MutableInteractionSource() }
    val interactionSource3 = remember { MutableInteractionSource() }

    Box(contentAlignment = Alignment.Center) {
        ButtonGroup(Modifier.fillMaxWidth()) {
            Button(
                onClick = {},
                modifier = Modifier.animateWidth(interactionSource1),
                interactionSource = interactionSource1,
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("A") }
            }
            Button(
                onClick = {},
                modifier = Modifier.weight(1.5f).animateWidth(interactionSource2),
                interactionSource = interactionSource2,
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("B") }
            }
            Button(
                onClick = {},
                modifier = Modifier.animateWidth(interactionSource3),
                interactionSource = interactionSource3,
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("C") }
            }
        }
    }
}
```