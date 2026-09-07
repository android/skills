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

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.AmbientTickEffect
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.rememberAmbientModeManager
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay

@Sampled
@Composable
fun AmbientModeBasicSample() {
    // **Best Practice Note:** In a production application, the AmbientModeManager should be
    // instantiated and provided at the highest level of the Compose hierarchy (typically in
    // the host Activity's setContent block) using a CompositionLocalProvider. This ensures
    // proper lifecycle management and broad accessibility.

    // For this self-contained demo, AmbientModeManager is created and provided locally:
    val activityAmbientModeManager = rememberAmbientModeManager()
    CompositionLocalProvider(LocalAmbientModeManager provides activityAmbientModeManager) {
        val ambientModeManager = LocalAmbientModeManager.current
        val ambientMode = ambientModeManager?.currentAmbientMode
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            val ambientModeName =
                when (ambientMode) {
                    is AmbientMode.Interactive -> "Interactive"
                    is AmbientMode.Ambient -> "Ambient"
                    else -> "Unknown"
                }

            val color = if (ambientMode is AmbientMode.Ambient) Color.Gray else Color.Yellow
            Text(text = "$ambientModeName Mode", color = color)
        }
    }
}

@Sampled
@Composable
fun AmbientModeWithAmbientTickSample() {
    // **Best Practice Note:** In a production application, the AmbientModeManager should be
    // instantiated and provided at the highest level of the Compose hierarchy (typically in
    // the host Activity's setContent block) using a CompositionLocalProvider. This ensures
    // proper lifecycle management and broad accessibility.

    // For this self-contained demo, AmbientModeManager is created and provided locally:
    val activityAmbientModeManager = rememberAmbientModeManager()
    CompositionLocalProvider(LocalAmbientModeManager provides activityAmbientModeManager) {
        var counter by remember { mutableIntStateOf(0) }

        val ambientModeManager = LocalAmbientModeManager.current
        ambientModeManager?.AmbientTickEffect {
            // While device is in ambient mode, update counter in onAmbientTick approx. every minute
            counter++
        }

        val ambientMode = ambientModeManager?.currentAmbientMode
        if (ambientMode is AmbientMode.Interactive) {
            // While device is not in ambient mode, update counter approx. every second
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1000L)
                    counter++
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            val ambientModeName =
                when (ambientMode) {
                    is AmbientMode.Interactive -> "Interactive"
                    is AmbientMode.Ambient -> "Ambient"
                    else -> "Unknown"
                }

            val updateInterval = if (ambientMode is AmbientMode.Ambient) "minute" else "second"
            val color = if (ambientMode is AmbientMode.Ambient) Color.Gray else Color.Yellow

            Text(text = "$ambientModeName Mode", color = color)
            Text(text = "Updates every $updateInterval")
            Text(text = "$counter")
        }
    }
}
```