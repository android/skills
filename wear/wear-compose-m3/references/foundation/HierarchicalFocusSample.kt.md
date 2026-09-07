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

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive

@Sampled
@Composable
fun HierarchicalFocusSample() {
    var selected by remember { mutableIntStateOf(0) }

    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { colIx ->
            Box(
                Modifier.hierarchicalFocusGroup(active = selected == colIx)
                    .weight(1f)
                    .clickable { selected = colIx }
                    .then(
                        if (selected == colIx) {
                            Modifier.border(BorderStroke(2.dp, Color.Red))
                        } else {
                            Modifier
                        }
                    )
            ) {
                // This is used a Gray background to the currently focused item, as seen by the
                // focus system.
                var focused by remember { mutableStateOf(false) }

                BasicText(
                    "$colIx",
                    style =
                        TextStyle(
                            color = Color.White,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                        ),
                    modifier =
                        Modifier.fillMaxWidth()
                            .requestFocusOnHierarchyActive()
                            .onFocusChanged { focused = it.isFocused }
                            .focusable()
                            .then(
                                if (focused) {
                                    Modifier.background(Color.Gray)
                                } else {
                                    Modifier
                                }
                            ),
                )
            }
        }
    }
}

@Sampled
@Composable
fun HierarchicalFocus2Levels() {
    Column(Modifier.fillMaxSize()) {
        var selectedRow by remember { mutableIntStateOf(0) }
        repeat(2) { rowIx ->
            Row(
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .hierarchicalFocusGroup(active = selectedRow == rowIx)
            ) {
                var selectedItem by remember { mutableIntStateOf(0) }
                repeat(2) { itemIx ->
                    Box(
                        Modifier.weight(1f).hierarchicalFocusGroup(active = selectedItem == itemIx)
                    ) {
                        // ScalingLazyColumn uses requestFocusOnHierarchyActive internally
                        ScalingLazyColumn(
                            Modifier.fillMaxWidth().clickable {
                                selectedRow = rowIx
                                selectedItem = itemIx
                            }
                        ) {
                            val prefix = (rowIx * 2 + itemIx + 'A'.code).toChar()
                            items(20) {
                                BasicText(
                                    "$prefix $it",
                                    style =
                                        TextStyle(
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            textAlign = TextAlign.Center,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```