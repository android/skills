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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material3.LinearProgressIndicator
import androidx.wear.compose.material3.MaterialTheme

@Sampled
@Composable
fun LinearProgressIndicatorSample(progress: () -> Float, enabled: Boolean = true) {
    Box(
        modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LinearProgressIndicator(
            progress = progress,
            enabled = enabled,
            modifier =
                Modifier.semantics(mergeDescendants = true) {
                    progressBarRangeInfo = ProgressBarRangeInfo(progress(), 0f..1f)
                },
        )
    }
}
```