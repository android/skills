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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Stepper
import androidx.wear.compose.material3.StepperDefaults
import androidx.wear.compose.material3.StepperLevelIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rangeSemantics
import androidx.wear.compose.material3.samples.icons.HeadphoneIcon
import androidx.wear.compose.material3.samples.icons.VolumeDownIcon
import androidx.wear.compose.material3.samples.icons.VolumeUpIcon

@Sampled
@Composable
fun StepperSample() {
    var value by remember { mutableFloatStateOf(2f) }
    val valueRange = remember { 0f..4f }
    Box(modifier = Modifier.fillMaxSize()) {
        Stepper(
            value = value,
            onValueChange = { value = it },
            valueRange = valueRange,
            steps = 7,
            decreaseIcon = { VolumeDownIcon(StepperDefaults.IconSize) },
            increaseIcon = { VolumeUpIcon(StepperDefaults.IconSize) },
        ) {
            Text(String.format("Value: %.1f".format(value)))
        }
        StepperLevelIndicator(
            value = { value },
            valueRange = valueRange,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

@Sampled
@Composable
fun StepperWithIntegerSample() {
    var value by remember { mutableIntStateOf(3) }
    val valueProgression = remember { 0..10 }
    Box(modifier = Modifier.fillMaxSize()) {
        Stepper(
            value = value,
            onValueChange = { value = it },
            valueProgression = valueProgression,
            decreaseIcon = { VolumeDownIcon(StepperDefaults.IconSize) },
            increaseIcon = { VolumeUpIcon(StepperDefaults.IconSize) },
        ) {
            Text(String.format("Value: %d".format(value)))
        }
        StepperLevelIndicator(
            value = { value },
            valueProgression = valueProgression,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

@Sampled
@Composable
fun StepperWithRangeSemanticsSample() {
    var value by remember { mutableFloatStateOf(2f) }
    val valueRange = remember { 0f..4f }
    val onValueChange = { i: Float -> value = i }
    val steps = 7
    Box(modifier = Modifier.fillMaxSize()) {
        Stepper(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.rangeSemantics(value, true, onValueChange, valueRange, steps),
            steps = steps,
            decreaseIcon = { VolumeDownIcon(StepperDefaults.IconSize) },
            increaseIcon = { VolumeUpIcon(StepperDefaults.IconSize) },
        ) {
            Text("Value: $value")
        }
        StepperLevelIndicator(
            value = { value },
            valueRange = valueRange,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}

@Sampled
@Composable
fun StepperWithButtonSample() {
    var value by remember { mutableFloatStateOf(2f) }
    val valueRange = remember { 0f..4f }
    Box(modifier = Modifier.fillMaxSize()) {
        Stepper(
            value = value,
            onValueChange = { value = it },
            valueRange = valueRange,
            increaseIcon = { VolumeUpIcon(StepperDefaults.IconSize) },
            decreaseIcon = { VolumeDownIcon(StepperDefaults.IconSize) },
            steps = 7,
        ) {
            Text(String.format("Value: %.1f".format(value)))
            Button(
                onClick = {},
                modifier = Modifier.width(150.dp),
                label = { Text(text = "This watch", modifier = Modifier.fillMaxWidth()) },
                secondaryLabel = { Text(text = "Headphones", modifier = Modifier.fillMaxWidth()) },
                icon = { HeadphoneIcon(24.dp) },
            )
        }
        StepperLevelIndicator(
            value = { value },
            valueRange = valueRange,
            modifier = Modifier.align(Alignment.CenterStart),
        )
    }
}
```