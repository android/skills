```
/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.dynamicColorScheme
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

@Sampled
@Composable
fun DynamicColorSchemeSample() {
    val dynamicColorScheme = dynamicColorScheme(LocalContext.current)
    val transformationSpec = rememberTransformationSpec()

    // Fallback to the default color scheme if dynamic colors are unavailable
    MaterialTheme(colorScheme = dynamicColorScheme ?: ColorScheme()) {
        val hasDynamicColors = dynamicColorScheme != null
        TransformingLazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(20.dp),
        ) {
            if (!hasDynamicColors) {
                item { Text("Dynamic color is not available.") }
            }
            items(5) { index ->
                // The button's defaults will pick up the dynamic primary color from the
                // MaterialTheme
                Button(
                    label = { Text("Primary Button ${index + 1}") },
                    modifier =
                        Modifier.fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            ),
                    onClick = {},
                    transformation = SurfaceTransformation(transformationSpec),
                )
            }
        }
    }
}
```