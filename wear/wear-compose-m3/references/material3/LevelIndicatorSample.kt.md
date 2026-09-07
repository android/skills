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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.LevelIndicator
import androidx.wear.compose.material3.StepperDefaults
import androidx.wear.compose.material3.samples.icons.VolumeDownIcon
import androidx.wear.compose.material3.samples.icons.VolumeUpIcon

@Sampled
@Composable
fun LevelIndicatorSample() {
    var value by remember { mutableFloatStateOf(0.5f) }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            IconButton(
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = value < 1f,
                onClick = { value += 0.1f },
            ) {
                VolumeUpIcon(StepperDefaults.IconSize)
            }
            IconButton(
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = value > 0f,
                onClick = { value -= 0.1f },
            ) {
                VolumeDownIcon(StepperDefaults.IconSize)
            }
        }
        LevelIndicator(value = { value }, modifier = Modifier.align(Alignment.CenterStart))
    }
}
```