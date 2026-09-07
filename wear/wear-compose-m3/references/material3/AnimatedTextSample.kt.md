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
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.AnimatedText
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberAnimatedTextFontRegistry
import kotlinx.coroutines.launch

@Sampled
@Composable
fun AnimatedTextSample() {
    val scope = rememberCoroutineScope()
    val animatable = remember { Animatable(0f) }
    val animate = {
        scope.launch {
            // Animate from 0 to 1 and then back to 0.
            animatable.animateTo(1f)
            animatable.animateTo(0f)
        }
    }
    val animatedTextFontRegistry =
        rememberAnimatedTextFontRegistry(
            // Variation axes at the start of the animation, width 10, weight 200
            startFontVariationSettings =
                FontVariation.Settings(FontVariation.width(10f), FontVariation.weight(200)),
            // Variation axes at the end of the animation, width 100, weight 500
            endFontVariationSettings =
                FontVariation.Settings(FontVariation.width(100f), FontVariation.weight(500)),
            startFontSize = 30.sp,
            endFontSize = 40.sp,
        )
    AnimatedText(
        text = "Hello!",
        fontRegistry = animatedTextFontRegistry,
        // Content alignment anchors the animation at the vertical center, expanding horizontally
        contentAlignment = Alignment.CenterStart,
        progressFraction = { animatable.value },
        modifier = Modifier.clickable(onClick = { animate() }),
    )
    LaunchedEffect(Unit) { animate() }
}

@Sampled
@Composable
fun AnimatedTextSampleButtonResponse() {
    val scope = rememberCoroutineScope()
    val animatedTextFontRegistry =
        rememberAnimatedTextFontRegistry(
            // Variation axes at the start of the animation, width 10, weight 200
            startFontVariationSettings =
                FontVariation.Settings(FontVariation.width(10f), FontVariation.weight(200)),
            // Variation axes at the end of the animation, width 100, weight 500
            endFontVariationSettings =
                FontVariation.Settings(FontVariation.width(100f), FontVariation.weight(500)),
            startFontSize = 30.sp,
            endFontSize = 30.sp,
        )
    val number = remember { mutableIntStateOf(0) }
    val textAnimatable = remember { Animatable(0f) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = {
                number.value -= 1
                scope.launch {
                    textAnimatable.animateTo(1f)
                    textAnimatable.animateTo(0f)
                }
            },
            label = {
                Text(modifier = Modifier.semantics { contentDescription = "Decrease" }, text = "-")
            },
        )
        AnimatedText(
            text = "${number.value}",
            fontRegistry = animatedTextFontRegistry,
            progressFraction = { textAnimatable.value },
        )
        Button(
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = {
                number.value += 1
                scope.launch {
                    textAnimatable.animateTo(1f)
                    textAnimatable.animateTo(0f)
                }
            },
            label = {
                Text(modifier = Modifier.semantics { contentDescription = "Increase" }, text = "+")
            },
        )
    }
}

@Sampled
@Composable
fun AnimatedTextSampleSharedFontRegistry() {
    val animatedTextFontRegistry =
        rememberAnimatedTextFontRegistry(
            // Variation axes at the start of the animation, width 50, weight 300
            startFontVariationSettings =
                FontVariation.Settings(FontVariation.width(50f), FontVariation.weight(300)),
            // Variation axes at the end of the animation are the same as the start axes
            endFontVariationSettings =
                FontVariation.Settings(FontVariation.width(50f), FontVariation.weight(300)),
            startFontSize = 15.sp,
            endFontSize = 25.sp,
        )
    val firstAnimatable = remember { Animatable(0f) }
    val secondAnimatable = remember { Animatable(0f) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedText(
            text = "Top Text",
            fontRegistry = animatedTextFontRegistry,
            progressFraction = { firstAnimatable.value },
        )
        AnimatedText(
            text = "Bottom Text",
            fontRegistry = animatedTextFontRegistry,
            progressFraction = { secondAnimatable.value },
        )
    }
    LaunchedEffect(Unit) {
        firstAnimatable.animateTo(1f)
        firstAnimatable.animateTo(0f)
        secondAnimatable.animateTo(1f)
        secondAnimatable.animateTo(0f)
    }
}
```