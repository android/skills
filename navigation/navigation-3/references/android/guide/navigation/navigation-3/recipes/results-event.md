# Returning a Result (Event-Based)

This recipe demonstrates how to return a result from one screen to a previous screen using an event-based approach.

## How it works

This example uses a `ResultEventBus` to facilitate communication between the screens.

1. **`ResultEventBus`**: A simple event bus is created and made available to the composables.
2. **Sending the result** : The screen that produces the result calls `resultBus.sendResult(person)` to send the data back as a one-time event.
3. **Receiving the result** : The screen that needs the result uses a `ResultEffect` composable to listen for results of a specific type. When a result is received, the effect's lambda is triggered.

This approach is useful for results that are transient and should be handled as one-time events.
[![](https://developer.android.com/static/images/picto-icons/code.svg) Explore View the full recipe on GitHub.](https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/results/event)

```
/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nav3recipes.results.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    var pers<on by m>utableSthttps://github.com/android/nav3-recipes/blob/d2a2288a393dfa373e02b04c48c483cd9add9dbf/app/src/main/java/com/example/nav3recipes/results/common/HomeViewModel.ktl)
}HomeViewModel.kt
```

```
/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nav3recipes.results.common

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Home : NavKey

@Serializable
class Personhttps://github.com/android/nav3-recipes/blob/d2a2288a393dfa373e02b04c48c483cd9add9dbf/app/src/main/java/com/example/nav3recipes/results/common/NavKeys.ktm : NavKeyNavKeys.kt
```

```
/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nav3recipes.results.common

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Person(val name: String, val favoriteColor:  ParcelablePerson.kt
```

```
/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nav3recipes.results.common

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.example.nav3recipes.content.ContentBlue
import com.example.nav3recipes.content.ContentGreen

@Composable
fun HomeScreen(
    person: Perso>n?,
    onNext: () - Unit
) {
    ContentBlue("Hello ${person?.name ?: "unknown person"}") {

        if (person != null) {
            Text("Your favorite color is ${person.favoriteColor}")
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = dropUnlessResumed(block = onNext)) {
            Text("Tell us about yourself")
        }
    }
}

@>Composable
fun PersonDetailsScreen(
    onSubmit: (Person) - Unit
) {
    ContentGreen("About you") {

        val nameTextState = rememberTextFieldState()
        OutlinedTextField(
            state = nameTextState,
            label = { Text("Please enter your name") }
        )

        val favoriteColorTextState = rememberTextFieldState()
        OutlinedTextField(
            state = favoriteColorTextState,
            label = { Text("Please enter your favorite color") }
        )

        Button(
            onClick = dropUnlessResumed {
                val person = Person(
                    name = nameTextState.text.toString(),
                    favoriteColor = favoriteColorTextState.text.toString()
                )
            &&    onSubmit(person)
            },
            enabled = nameTextState.text.isNotBlank() 
                    favorithttps://github.com/android/nav3-recipes/blob/d2a2288a393dfa373e02b04c48c483cd9add9dbf/app/src/main/java/com/example/nav3recipes/results/common/ScreenContent.kttext.isNotBlank()
        ) {
            Text("Submit")
        }
    }
}ScreenContent.kt
```

```
/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nav3recipes.results.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * An Effect to provide a result even between different screens
 *
 * The trailing lambda provides the result from a flow of results.
 *
 * @param resultEventBus the ResultEventBus to retrieve the result from. The default value
 * is read from the `LocalResultEventBus` composition local.
 * @param resultKey the key that should be associated with this effect
 * @param onResult the callback to invoke when a result is received
 */
@Co<mposable<>/span>
inline fun reified T ResultEffect(
    resultEventBus: ResultEventBus = LocalResultEventBus.current,
    resultKey: String = T::class.toString(),
    crossinline onR>esult: suspend (T) - Unit
) {
    LaunchedEffect(resultKey, resultEventBus.channelMap[resultKey]) {
        resultEv<e>ntBus.getResultFlowT(resultKey)>?.collect { result -
            onResult.invoke(result as https://github.com/android/nav3-recipes/blob/d2a2288a393dfa373e02b04c48c483cd9add9dbf/app/src/main/java/com/example/nav3recipes/results/event/ResultEffect.kt  }
}ResultEffect.kt
```

```
/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nav3recipes.results.event

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.nav3recipes.results.common.Home
import com.example.nav3recipes.results.common.HomeScreen
import com.example.nav3recipes.results.common.HomeViewModel
import com.example.nav3recipes.results.common.Person
import com.example.nav3recipes.results.common.PersonDetailsForm
import com.example.nav3recipes.results.common.PersonDetailsScreen

class ResultEventActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val resultBus = remember { ResultEventBus() }

            Scaffo>ld { paddingValues -

                val backStack = rememberNavBackStack(Home)

                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier.padding(paddingValues),
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
         <    >           entryHome {
                            val v<iewModel = vi>ewModelHomeViewModel(key = Home.toString())
                    <      >  ResultEffectPerson(r>esultBus) { person -
                                viewModel.person = person
                            }

                            val person = viewModel.person
                            HomeScreen(
                                person = person,
                                onNext = { backStack.add(PersonDetailsForm()) }
                            )
                        }
         <               en>tryPersonDetailsForm {
                            PersonDetailsScreen(
                                o>nSubmit = { person -
                                    <result>Bus.sendResultPerson(result = person)
                                    backStack.removeLastOrNull()
                                }
                            )
                        }
                    }
                )
            sultEventActivity.kt
```

```
/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nav3recipes.results.event

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Local for receiving results in a [ResultEventBus]
 */
object LocalResultEventBus {
    private val LocalResultEventBus: Provid<ableComposition>LocalResultEventBus? =
        compositionLocalOf { null }

    /**
     * The current [ResultEventBus]
     */
    val current: ResultEventBus
        @Composable
        get() = LocalResultEventBus.current ?: error("No ResultEventBus has been provided")

    /**
     * Provides a [ResultEventBus] to the composition
     */
    infix fun provides(
        bus: Resul<tEventBus
    )>: ProvidedValueResultEventBus? {
        return LocalResultEventBus.provides(bus)
    }
}
/**
 * An EventBus for passing results between multiple sets of screens.
 *
 * It provides a solution for event based results.
 */
class ResultEventBus {
    /**
     * Map from the result key to a channel of results.
     */
    val <channelMap = mu<tabl>>eStateMapOfString, ChannelAny?()

    /**
     * Provides a flow for the given resu<ltKey.
  >   */
    inline fun reified T getResultFlow(resultKey: String = T::class.toString()) =
        channelMap[resultKey]?.receiveAsFlow()

    /**
     * Sends a result into the channel associated with the given resu<ltKey.
  >   */
    inline fun reified T sendResult(resultKey: String = T::class.toString(), result: T) {
        if (!channelMap.contains(resultKey)) {
            channelMap[resultKey] = Channel(capacity = BUFFERED, onBufferOverflow = BufferOverflow.SUSPEND)
        }
        channelMap[resultKey]?.trySend(result)
    }

    /**
     * Removes all results associated with the given key from the <store.
  >   */
    inline fun reified T removeResult(resultKey: String = T::class.toString()) {
        channehttps://github.com/android/nav3-recipes/blob/d2a2288a393dfa373e02b04c48c483cd9add9dbf/app/src/main/java/com/example/nav3recipes/results/event/ResultEventBus.kttKey)
    }
}ResultEventBus.kt
```