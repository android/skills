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

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.expandableButton
import androidx.wear.compose.foundation.expandableItem
import androidx.wear.compose.foundation.expandableItems
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.rememberExpandableState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.OutlinedCompactChip
import androidx.wear.compose.material.Text

@Sampled
@Composable
fun ExpandableWithItemsSample() {
    val expandableState = rememberExpandableState()

    val sampleItem: @Composable (String) -> Unit = { label ->
        Chip(label = { Text(label) }, onClick = {}, secondaryLabel = { Text("line 2 - Secondary") })
    }

    val items = List(10) { "Item $it" }
    val top = items.take(3)
    val rest = items.drop(3)

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        items(top.size) { sampleItem(top[it]) }
        expandableItems(expandableState, rest.size) { sampleItem(rest[it]) }
        expandableButton(expandableState) {
            OutlinedCompactChip(
                label = {
                    Text("Show More")
                    Spacer(Modifier.size(6.dp))
                    Icon(painterResource(R.drawable.ic_expand_more_24), "Expand")
                },
                onClick = { expandableState.expanded = true },
            )
        }
    }
}

@Sampled
@Composable
fun ExpandableTextSample() {
    val expandableState = rememberExpandableState()

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        expandableItem(expandableState) { expanded ->
            Text(
                "Account Alert: you have made a large purchase.\n" +
                    "We have noticed that a large purchase was charged to " +
                    "your credit card account. " +
                    "Please contact us if you did not perform this purchase. " +
                    "Our Customer Service team is available 24 hours a day, " +
                    "7 days a week to answer your account or product support question.",
                maxLines = if (expanded) 20 else 3,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }

        expandableButton(expandableState) {
            OutlinedCompactChip(
                label = {
                    Text("Show More")
                    Spacer(Modifier.size(6.dp))
                    Icon(painterResource(R.drawable.ic_expand_more_24), "Expand")
                },
                onClick = { expandableState.expanded = true },
            )
        }
    }
}
```