# Returning a Result (State-Based)

This recipe demonstrates how to return a result from one screen to a previous screen using a state-based approach.

## How it works

This example uses a `ResultStore` to manage the result as state.

1. **`ResultStore`** : A `ResultStore` is created and made available to the composables. This store holds the results.
2. **Setting the result** : The screen that produces the result calls `resultStore.setResult(person)` to save the data in the store.
3. **Observing the result** : The screen that needs the result calls `resultStore.getResultState<Person?>()` to get a `State` object representing the result. The UI then observes this state and recomposes whenever the result changes.

This approach is suitable when the result should be treated as persistent state that survives recomposition and configuration changes.
[![](https://developer.android.com/static/images/picto-icons/code.svg) Explore View the full recipe on GitHub.](https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/results/state)

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
    var person by mutableStateOf<Person?>(null)
}
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
class PersonDetailsForm : NavKeyhttps://github.com/android/nav3-recipes/blob/d2a2288a393dfa373e02b04c48c483cd9add9dbf/app/src/main/java/com/example/nav3recipes/results/common/NavKeys.kt
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
data class Person(val name: String, val favoriteColor: String) : Parcelablehttps://github.com/android/nav3-recipes/blob/d2a2288a393dfa373e02b04c48c483cd9add9dbf/app/src/main/java/com/example/nav3recipes/results/common/Person.kt
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
    person: Person?,
    onNext: () -> Unit
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

@Composable
fun PersonDetailsScreen(
    onSubmit: (Person) -> Unit
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
                onSubmit(person)
            },
            enabled = nameTextState.text.isNotBlank() &&
                    favoriteColorTextState.text.isNotBlank()
        ) {
            Text("Submit")
        }
    }
}
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

package com.example.nav3recipes.results.state

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.nav3recipes.results.common.Home
import com.example.nav3recipes.results.common.HomeScreen
import com.example.nav3recipes.results.common.Person
import com.example.nav3recipes.results.common.PersonDetailsForm
import com.example.nav3recipes.results.common.PersonDetailsScreen

class ResultStateActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val resultStore = rememberResultStore()

            Scaffold { paddingValues ->
                val backStack = rememberNavBackStack(Home)

                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier.padding(paddingValues),
                    onBack = { backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        entry<Home> {
                            val person = resultStore.getResultState<Person?>()
                            HomeScreen(
                                person = person,
                                onNext = { backStack.add(PersonDetailsForm()) }
                            )
                        }
                        entry<PersonDetailsForm> {
                            PersonDetailsScreen(
                                onSubmit = { person ->
                                    resultStore.setResult<Person>(result = person)
                                    backStack.removeLastOrNull()
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}
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

package com.example.nav3recipes.results.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Local for storing results in a [ResultStore]
 */
object LocalResultStore {
    private val LocalResultStore: ProvidableCompositionLocal<ResultStore?> =
        compositionLocalOf { null }

    /**
     * The current [ResultStore]
     */
    val current: ResultStore
        @Composable
        get() = LocalResultStore.current ?: error("No ResultStore has been provided")

    /**
     * Provides a [ResultStore] to the composition
     */
    infix fun provides(
        store: ResultStore
    ): ProvidedValue<ResultStore?> {
        return LocalResultStore.provides(store)
    }
}

/**
 * Provides a [ResultStore] that will be remembered across configuration changes.
 */
@Composable
fun rememberResultStore(): ResultStore {
    return rememberSaveable(saver = ResultStoreSaver()) {
        ResultStore()
    }
}

/**
 * A store for passing results between multiple sets of screens.
 *
 * It provides a solution for state based results.
 */
class ResultStore {

    /**
     * Map from the result key to a mutable state of the result.
     */
    val resultStateMap = mutableStateMapOf<String, MutableState<Any?>>()

    /**
     * Retrieves the current result of the given resultKey.
     */
    inline fun <reified T> getResultState(resultKey: String = T::class.toString()) =
        resultStateMap[resultKey]?.value as T

    /**
     * Sets the result for the given resultKey.
     */
    inline fun <reified T> setResult(resultKey: String = T::class.toString(), result: T) {
        resultStateMap[resultKey] = mutableStateOf(result)
    }

    /**
     * Removes all results associated with the given key from the store.
     */
    inline fun <reified T> removeResult(resultKey: String = T::class.toString()) {
        resultStateMap.remove(resultKey)
    }
}

/** Saver to save and restore the ResultStore across config change and process death. */
private fun ResultStoreSaver(): Saver<ResultStore, *> =
    mapSaver(
        save = { resultStore ->
            resultStore.resultStateMap.mapValues { it.value.value }
        },
        restore = { restoredMap ->
            ResultStore().apply {
                restoredMap.forEach { (key, value) ->
                    resultStateMap[key] = mutableStateOf(value)
                }
            }
        }
    )
```