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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.IconToggleButtonDefaults
import androidx.wear.compose.material3.samples.icons.WifiOffIcon
import androidx.wear.compose.material3.samples.icons.WifiOnIcon

@Sampled
@Composable
fun IconToggleButtonSample() {
    var firstChecked by remember { mutableStateOf(true) }
    var secondChecked by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconToggleButton(
            checked = firstChecked,
            onCheckedChange = { firstChecked = !firstChecked },
            shapes = IconToggleButtonDefaults.animatedShapes(),
        ) {
            if (firstChecked) {
                WifiOnIcon()
            } else {
                WifiOffIcon()
            }
        }

        Spacer(modifier = Modifier.width(5.dp))

        IconToggleButton(
            checked = secondChecked,
            onCheckedChange = { secondChecked = !secondChecked },
            shapes = IconToggleButtonDefaults.animatedShapes(),
        ) {
            if (secondChecked) {
                WifiOnIcon()
            } else {
                WifiOffIcon()
            }
        }
    }
}

@Sampled
@Composable
fun IconToggleButtonVariantSample() {
    var firstChecked by remember { mutableStateOf(true) }
    var secondChecked by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconToggleButton(
            checked = firstChecked,
            onCheckedChange = { firstChecked = !firstChecked },
            shapes = IconToggleButtonDefaults.variantAnimatedShapes(),
        ) {
            if (firstChecked) {
                WifiOnIcon()
            } else {
                WifiOffIcon()
            }
        }

        Spacer(modifier = Modifier.width(5.dp))

        IconToggleButton(
            checked = secondChecked,
            onCheckedChange = { secondChecked = !secondChecked },
            shapes = IconToggleButtonDefaults.variantAnimatedShapes(),
        ) {
            if (secondChecked) {
                WifiOnIcon()
            } else {
                WifiOffIcon()
            }
        }
    }
}
```