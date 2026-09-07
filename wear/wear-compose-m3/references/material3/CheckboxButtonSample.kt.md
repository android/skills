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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.CheckboxButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SplitCheckboxButton
import androidx.wear.compose.material3.Text

@Sampled
@Preview
@Composable
fun CheckboxButtonSample() {
    var checked by remember { mutableStateOf(true) }
    CheckboxButton(
        label = { Text("Checkbox Button", maxLines = 3, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = {
            Text("With secondary label", maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        checked = checked,
        onCheckedChange = { checked = it },
        icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite icon") },
        enabled = true,
    )
}

@Sampled
@Preview
@Composable
fun SplitCheckboxButtonSample() {
    var checked by remember { mutableStateOf(true) }
    SplitCheckboxButton(
        label = { Text("Split Checkbox Button", maxLines = 3, overflow = TextOverflow.Ellipsis) },
        checked = checked,
        onCheckedChange = { checked = it },
        toggleContentDescription = "Split Checkbox Button Sample",
        onContainerClick = {
            /* Do something */
        },
        containerClickLabel = "click",
        enabled = true,
    )
}
```