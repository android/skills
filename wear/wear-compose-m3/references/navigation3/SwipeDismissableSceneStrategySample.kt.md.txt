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

package androidx.wear.compose.navigation3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import kotlinx.serialization.Serializable

@Serializable object NotificationList : NavKey

@Serializable data class NotificationDetail(val id: Int) : NavKey

@Serializable object First : NavKey

@Serializable object Second : NavKey

data class NotificationItem(val id: Int, val title: String, val body: String)

@Sampled
@Composable
fun ListDetailNavDisplaySample(onExit: () -> Unit = {}) {
    // Example of using a NavDisplay with SwipeDismissableSceneStrategy for list-detail navigation.

    // Strongly-typed, serializable navigation destinations are defined at global scope as follows:
    // @Serializable object NotificationList : NavKey
    // @Serializable data class NotificationDetail(val id: Int) : NavKey

    // NotificationItem needs to be defined at top-level or class-level to ensure stability in
    // Compose:
    // data class NotificationItem(val id: Int, val title: String, val body: String)
    val backStack = rememberNavBackStack(NotificationList)

    val notifications = remember {
        listOf(
            NotificationItem(
                0,
                "☕ Coffee Break? Grab a pick-me-up",
                "Step away from the screen and grab a pick-me-up. Step away from the screen and grab a pick-me-up.",
            ),
            NotificationItem(
                1,
                "🌟 You're Awesome!",
                "Just a little reminder in case you forgot 😊",
            ),
            NotificationItem(
                2,
                "👀 Did you know our latest feature?",
                "Check out [app name]'s latest feature update.",
            ),
            NotificationItem(
                3,
                "📅 Appointment Time In 15 Minutes",
                "Your meeting with [name] is in 15 minutes.",
            ),
        )
    }

    NavDisplay(
        backStack = backStack,
        sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
        entryProvider =
            entryProvider {
                entry<NotificationList> {
                    val state = rememberTransformingLazyColumnState()
                    val transformationSpec = rememberTransformationSpec()
                    ScreenScaffold(
                        state,
                        edgeButton = { EdgeButton(onClick = onExit) { Text("Exit") } },
                    ) { contentPadding ->
                        TransformingLazyColumn(state = state, contentPadding = contentPadding) {
                            item {
                                ListHeader(
                                    transformation = SurfaceTransformation(transformationSpec),
                                    modifier =
                                        Modifier.transformedHeight(this, transformationSpec)
                                            .animateItem(),
                                ) {
                                    Text("Notifications")
                                }
                            }
                            items(notifications) { notification ->
                                Card(
                                    onClick = {
                                        backStack.add(NotificationDetail(notification.id))
                                    },
                                    transformation = SurfaceTransformation(transformationSpec),
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .transformedHeight(this, transformationSpec)
                                            .animateItem(),
                                ) {
                                    Text(notification.title)
                                }
                            }
                        }
                    }
                }

                entry<NotificationDetail> { key ->
                    val item = requireNotNull(notifications.find { item -> item.id == key.id })
                    ScreenScaffold { contentPadding ->
                        Box(
                            Modifier.fillMaxSize().padding(contentPadding),
                            contentAlignment = Alignment.Center,
                        ) {
                            TitleCard(title = { Text(item.title) }, content = { Text(item.body) })
                        }
                    }
                }
            },
    )
}

@Sampled
@Composable
fun NavDisplayWithOnBackBehaviorSample() {
    // Example of using a NavDisplay with SwipeDismissableSceneStrategy and on back behavior.

    // Strongly-typed, serializable navigation destinations are defined at global scope as follows:
    // @Serializable object First: NavKey
    // @Serializable object Second: NavKey
    val backStack = rememberNavBackStack(First)

    var swipedBackCount by remember { mutableIntStateOf(0) }

    NavDisplay(
        backStack = backStack,
        onBack = {
            swipedBackCount++
            backStack.removeLastOrNull()
        },
        sceneStrategies = listOf(rememberSwipeDismissableSceneStrategy()),
        entryProvider =
            entryProvider {
                entry<First> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("First")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { backStack.add(Second) }) { Text("Second") }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Swiped back $swipedBackCount times")
                    }
                }
                entry<Second> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Second")
                    }
                }
            },
    )
}
```