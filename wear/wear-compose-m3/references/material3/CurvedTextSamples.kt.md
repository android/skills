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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap.Companion.Round
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.angularSizeDp
import androidx.wear.compose.foundation.background
import androidx.wear.compose.foundation.curvedBox
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CurvedTextDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.curvedText

@Sampled
@Composable
fun CurvedTextTop() {
    val backgroundColor = MaterialTheme.colorScheme.onPrimary
    val customColor = MaterialTheme.colorScheme.tertiaryDim
    CurvedLayout {
        curvedRow(CurvedModifier.background(backgroundColor, Round)) {
            curvedText("Calling", color = customColor)
            curvedBox(CurvedModifier.angularSizeDp(5.dp)) {}
            curvedText("Camilia Garcia")
        }
    }
}

@Sampled
@Composable
fun CurvedTextBottom() {
    val backgroundColor = MaterialTheme.colorScheme.onPrimary
    CurvedLayout(anchor = 90f, angularDirection = CurvedDirection.Angular.Reversed) {
        curvedRow(CurvedModifier.background(backgroundColor, Round)) {
            curvedComposable {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = "Warning",
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
            }
            curvedText(
                "Network lost",
                maxSweepAngle = CurvedTextDefaults.StaticContentMaxSweepAngle,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
```