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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.TextButtonDefaults

@Composable
@Sampled
fun TextButtonSample() {
    TextButton(onClick = { /* Do something */ }) { Text(text = "ABC") }
}

@Composable
@Sampled
fun FilledTextButtonSample() {
    TextButton(
        onClick = { /* Do something */ },
        colors = TextButtonDefaults.filledTextButtonColors(),
    ) {
        Text(text = "ABC")
    }
}

@Composable
@Sampled
fun FilledVariantTextButtonSample() {
    TextButton(
        onClick = { /* Do something */ },
        colors = TextButtonDefaults.filledVariantTextButtonColors(),
    ) {
        Text(text = "ABC")
    }
}

@Composable
@Sampled
fun LargeFilledTonalTextButtonSample() {
    TextButton(
        onClick = { /* Do something */ },
        colors = TextButtonDefaults.filledTonalTextButtonColors(),
        modifier = Modifier.size(TextButtonDefaults.LargeButtonSize),
    ) {
        Text(text = "ABC", style = TextButtonDefaults.largeButtonTextStyle)
    }
}

@Composable
@Sampled
fun FilledTonalTextButtonSample() {
    TextButton(
        onClick = { /* Do something */ },
        colors = TextButtonDefaults.filledTonalTextButtonColors(),
    ) {
        Text(text = "ABC")
    }
}

@Composable
@Sampled
fun OutlinedTextButtonSample() {
    TextButton(
        onClick = { /* Do something */ },
        colors = TextButtonDefaults.outlinedTextButtonColors(),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
    ) {
        Text(text = "ABC")
    }
}

@Sampled
@Composable
fun TextButtonWithOnLongClickSample(onLongClick: () -> Unit) {
    TextButton(
        onClick = { /* Do something for onClick*/ },
        onLongClick = onLongClick,
        onLongClickLabel = "Long click",
    ) {
        Text(text = "ABC")
    }
}

@Composable
@Sampled
fun TextButtonWithCornerAnimationSample() {
    TextButton(
        onClick = { /* Do something */ },
        colors = TextButtonDefaults.filledTextButtonColors(),
        shapes = TextButtonDefaults.animatedShapes(),
    ) {
        Text(text = "ABC")
    }
}
```