# Custom DeepLinkMatcher Recipe

This recipe demonstrates how to create a custom `DeepLinkMatcher` in Navigation 3 using custom request extras and Kotlinx Serialization.

## How it works

This recipe consists of two activities:

- `CustomDeepLinkMatcherActivity`: Accepts user input, serializes a `HomeKey` instance into JSON, attaches it to an `Intent` extra via a `RequestExtrasKey`, and launches `MainActivity`.
- `MainActivity`: Constructs a `DeepLinkRequest(intent)`, evaluates it with `JsonDeepLinkMatcher`, decodes the `HomeKey`, and sets it as the starting route in `NavDisplay`.

## Key Concepts

1. **Custom `RequestExtrasKey`** :
   `JsonDeepLinkMatcherKey` defines a custom extra key implementing `RequestExtrasKey<String>` to type-safely store and read serialized JSON payloads in `DeepLinkRequest.extras`.

2. **Custom `DeepLinkMatcher`** :
   `JsonDeepLinkMatcher<T>` extends `DeepLinkMatcher<T, MatchResult<T>>` and implements `matchRequest(request)` to extract `request.extras[JsonDeepLinkMatcherKey]` and decode it into a strongly typed `NavKey` using Kotlinx Serialization.

[![](https://developer.android.com/static/images/picto-icons/code.svg) Explore View the full recipe on GitHub.](https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/deeplink/usecases/matcher)

```
package com.example.nav3recipes.deeplink.usecases.matcher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.DeepLinkMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.RequestExtrasKey
import com.example.nav3recipes.common.deeplink.EntryScreen
import com.example.nav3recipes.ui.setEdgeToEdgeConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class CustomDeepLinkMatcherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)

        setContent {
            EntryScreen {
                Column(
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    var text by remember { mutableStateOf("") }
                    OutlinedTextField(
                        placeholder = { Text("Your name...", color = Color.Black.copy(alpha = 0.5f)) },
                        value = text,
                        singleLine = true,
                        onValueChange = { text = it },
                    )

                    ElevatedButton(
                        onClick =
                            dropUnlessResumed {
                                val intent = Intent(
                                    this@CustomDeepLinkMatcherActivity,
                                    MainActivity::class.java
                                )
                                val json = Json.encodeToString(HomeKey.serializer(), HomeKey(text))
                                intent.putExtra(JsonDeepLinkMatcherKey.toString(), json)
                                startActivity(intent)
                        }
                    ) {
                        Text("Sign up")
                    }
                }
            }
        }
    }
}

internal data object JsonDeepLinkMatcherKey: RequestExtrasKey<String>

internal class JsonDeepLinkMatcher<T: NavKey>(val serializer: KSerializer<T>): DeepLinkMatcher<T, DeepLinkMatcher.MatchResult<T>>() {
    override fun matchRequest(request: DeepLinkRequest): MatchResult<T>? {
        val json = request.extras[JsonDeepLinkMatcherKey] ?: return null
        return try {
            val result = Json.decodeFromString(serializer, json)
            MatchResult(result)
        } catch (e: SerializationException) {
            Log.v("DeepLinkMatcher", "Failed to decode json", e)
            return null
        }
    }
}
```

```
package com.example.nav3recipes.deeplink.usecases.matcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.invoke
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.nav3recipes.common.deeplink.EntryScreen
import com.example.nav3recipes.common.deeplink.TextContent
import com.example.nav3recipes.ui.setEdgeToEdgeConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
internal data class HomeKey(val name: String): NavKey

@Serializable
internal object FallbackKey: NavKey

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)

        val request = DeepLinkRequest(intent)
        val deepLinkMatcher = createJsonDeepLinkMatcher<HomeKey>()

        val matchResult = deepLinkMatcher.match(request)
        val key = matchResult?.key ?: FallbackKey

        setContent {
            val backStack: NavBackStack<NavKey> = rememberNavBackStack(key)
            NavDisplay(
                backStack = backStack,
                onBack = backStack::removeLastOrNull,
                entryProvider = entryProvider {
                    entry<HomeKey> { key ->
                        EntryScreen("Welcome") {
                            TextContent(key.name)
                        }
                    }
                    entry<FallbackKey> { key ->
                        EntryScreen("Fallback Key") {
                            TextContent(
                                "Failed to deep link - DeepLinkRequest " +
                                        "did not match with any DeepLinkMatcher"
                            )
                        }
                    }
                }
            )
        }
    }
}

// Optional JsonDeepLinkMatcher factory function that automatically captures KSerializer for T.
private inline fun <reified T : NavKey> createJsonDeepLinkMatcher(): JsonDeepLinkMatcher<T> {
    val serializer = serializer<T>()
    return JsonDeepLinkMatcher(serializer)
}
```