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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

@Sampled
@Preview
@Composable
fun TransformingLazyColumnNotificationsSample() {
    val state = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    data class NotificationItem(val title: String, val body: String)

    val notifications =
        listOf(
            NotificationItem(
                "☕ Coffee Break?",
                "Step away from the screen and grab a pick-me-up. Step away from the screen and grab a pick-me-up.",
            ),
            NotificationItem("🌟 You're Awesome!", "Just a little reminder in case you forgot 😊"),
            NotificationItem("👀 Did you know?", "Check out [app name]'s latest feature update."),
            NotificationItem("📅 Appointment Time", "Your meeting with [name] is in 15 minutes."),
        )
    AppScaffold {
        ScreenScaffold(state) { contentPadding ->
            TransformingLazyColumn(state = state, contentPadding = contentPadding) {
                item {
                    ListHeader(
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier =
                            Modifier.transformedHeight(this, transformationSpec).animateItem(),
                    ) {
                        Text("Notifications")
                    }
                }
                items(notifications) { notification ->
                    TitleCard(
                        onClick = {},
                        title = {
                            Text(
                                notification.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                        },
                        subtitle = { Text(notification.body) },
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier =
                            Modifier.transformedHeight(this, transformationSpec).animateItem(),
                    )
                }
            }
        }
    }
}
```