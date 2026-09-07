```
/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ArcProgressIndicator
import androidx.wear.compose.material3.ArcProgressIndicatorDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.SegmentedCircularProgressIndicator
import androidx.wear.compose.material3.drawCircularProgressIndicator

@Sampled
@Composable
fun FullScreenProgressIndicatorSample() {
    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        CircularProgressIndicator(progress = { 0.25f }, startAngle = 120f, endAngle = 60f)
    }
}

@Sampled
@Composable
fun MediaButtonProgressIndicatorSample() {
    var isPlaying by remember { mutableStateOf(false) }
    val buttonPadding = 4.dp
    val progressStrokeWidth = 4.dp
    val progress = 0.75f

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // The CircularProgressIndicator should be around the IconButton, with an extra gap between
        // then of 'buttonPadding'. We multiply by 2 because the size includes progressStrokeWidth
        // at top and bottom and the buttonPadding at top and bottom.
        CircularProgressIndicator(
            modifier =
                Modifier.align(Alignment.Center)
                    .size(
                        IconButtonDefaults.DefaultButtonSize +
                            progressStrokeWidth * 2 +
                            buttonPadding * 2
                    ),
            progress = { progress },
            strokeWidth = progressStrokeWidth,
        )

        IconButton(
            modifier =
                Modifier.align(Alignment.Center)
                    .semantics {
                        // Set custom progress semantics for accessibility.
                        contentDescription =
                            String.format(
                                "Play/pause button, track progress: %.0f%%",
                                progress * 100,
                            )
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            onClick = { isPlaying = !isPlaying },
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Close else Icons.Filled.PlayArrow,
                contentDescription = null,
            )
        }
    }
}

@Sampled
@Composable
fun OverflowProgressIndicatorSample() {
    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        CircularProgressIndicator(
            // Overflow value of 120%
            progress = { 1.2f },
            allowProgressOverflow = true,
            startAngle = 120f,
            endAngle = 60f,
        )
    }
}

@Sampled
@Composable
fun SmallValuesProgressIndicatorSample() {
    Box {
        CircularProgressIndicator(
            // Small progress values like 2% will be rounded up to at least the stroke width.
            progress = { 0.02f },
            modifier =
                Modifier.fillMaxSize().padding(CircularProgressIndicatorDefaults.FullScreenPadding),
            startAngle = 120f,
            endAngle = 60f,
            strokeWidth = 10.dp,
            colors =
                ProgressIndicatorDefaults.colors(
                    indicatorColor = Color.Green,
                    trackColor = Color.White,
                ),
        )
    }
}

@Sampled
@Composable
fun CircularProgressIndicatorCustomAnimationSample() {
    val animatedProgress = remember { Animatable(0f) }
    val colors =
        ProgressIndicatorDefaults.colors(indicatorColor = Color.Green, trackColor = Color.White)

    LaunchedEffect(Unit) {
        animatedProgress.animateTo(1f, tween(durationMillis = 1024, easing = LinearEasing))
        animatedProgress.animateTo(0f, tween(durationMillis = 1024, easing = LinearEasing))
    }

    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        // Draw the circular progress indicator with custom animation
        Spacer(
            Modifier.fillMaxSize().focusable().drawBehind {
                drawCircularProgressIndicator(
                    progress = animatedProgress.value,
                    strokeWidth = 10.dp,
                    colors = colors,
                    startAngle = 120f,
                    endAngle = 60f,
                )
            }
        )
    }
}

@Sampled
@Composable
fun IndeterminateProgressIndicatorSample() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Sampled
@Composable
fun IndeterminateProgressArcSample() {
    Box(modifier = Modifier.fillMaxSize()) {
        ArcProgressIndicator(
            modifier =
                Modifier.align(Alignment.Center)
                    .size(ArcProgressIndicatorDefaults.recommendedIndeterminateDiameter)
        )
    }
}

@Sampled
@Composable
fun SegmentedProgressIndicatorSample() {
    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        SegmentedCircularProgressIndicator(segmentCount = 5, progress = { 0.5f })
    }
}

@Sampled
@Composable
fun SegmentedProgressIndicatorBinarySample() {
    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        SegmentedCircularProgressIndicator(segmentCount = 5, segmentValue = { it % 2 != 0 })
    }
}

@Sampled
@Composable
fun SmallSegmentedProgressIndicatorSample() {
    Box(modifier = Modifier.fillMaxSize()) {
        SegmentedCircularProgressIndicator(
            segmentCount = 6,
            progress = { 0.75f },
            modifier = Modifier.align(Alignment.Center).size(80.dp),
        )
    }
}

@Sampled
@Composable
fun SmallSegmentedProgressIndicatorBinarySample() {
    Box(modifier = Modifier.fillMaxSize()) {
        SegmentedCircularProgressIndicator(
            segmentCount = 8,
            segmentValue = { it % 2 != 0 },
            modifier = Modifier.align(Alignment.Center).size(80.dp),
        )
    }
}
```