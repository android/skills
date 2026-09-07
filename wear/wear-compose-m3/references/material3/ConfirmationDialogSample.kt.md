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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.ConfirmationDialog
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.FailureConfirmationDialog
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SuccessConfirmationDialog
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.confirmationDialogCurvedText
import androidx.wear.compose.material3.samples.icons.FavoriteIcon

@Sampled
@Composable
fun ConfirmationDialogSample() {
    var showConfirmation by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showConfirmation = true },
            label = { Text("Show Confirmation") },
        )
    }

    // Has an icon and a short curved text content, which will be displayed along the bottom edge of
    // the screen.
    val curvedTextStyle = ConfirmationDialogDefaults.curvedTextStyle
    ConfirmationDialog(
        visible = showConfirmation,
        onDismissRequest = { showConfirmation = false },
        curvedText = { confirmationDialogCurvedText("Confirmed", curvedTextStyle) },
    ) {
        FavoriteIcon(ConfirmationDialogDefaults.IconSize)
    }
}

@Sampled
@Composable
fun LongTextConfirmationDialogSample() {
    var showConfirmation by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showConfirmation = true },
            label = { Text("Show Confirmation") },
        )
    }

    // Has an icon and a text content. Text will be displayed in the center of the screen below the
    // icon.
    ConfirmationDialog(
        visible = showConfirmation,
        onDismissRequest = { showConfirmation = false },
        text = { Text(text = "Your message has been sent") },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = null,
            modifier = Modifier.size(ConfirmationDialogDefaults.SmallIconSize),
        )
    }
}

@Sampled
@Composable
fun FailureConfirmationDialogSample() {
    var showConfirmation by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showConfirmation = true },
            label = { Text("Show Confirmation") },
        )
    }

    val text = "Failure"
    val style = ConfirmationDialogDefaults.curvedTextStyle
    FailureConfirmationDialog(
        visible = showConfirmation,
        onDismissRequest = { showConfirmation = false },
        curvedText = { confirmationDialogCurvedText(text, style) },
    )
}

@Sampled
@Composable
fun FailureConfirmationDialogWithGenericFailureIconSample() {
    var showConfirmation by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showConfirmation = true },
            label = { Text("Show Confirmation") },
        )
    }

    val text = "Failure"
    val style = ConfirmationDialogDefaults.curvedTextStyle
    FailureConfirmationDialog(
        visible = showConfirmation,
        onDismissRequest = { showConfirmation = false },
        curvedText = { confirmationDialogCurvedText(text, style) },
        content = { ConfirmationDialogDefaults.GenericFailureIcon() },
    )
}

@Sampled
@Composable
fun SuccessConfirmationDialogSample() {
    var showConfirmation by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        FilledTonalButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { showConfirmation = true },
            label = { Text("Show Confirmation") },
        )
    }

    val text = "Success"
    val style = ConfirmationDialogDefaults.curvedTextStyle
    SuccessConfirmationDialog(
        visible = showConfirmation,
        onDismissRequest = { showConfirmation = false },
        curvedText = { confirmationDialogCurvedText(text, style) },
    )
}
```