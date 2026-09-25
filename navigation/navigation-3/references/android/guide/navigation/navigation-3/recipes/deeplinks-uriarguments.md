# Deep Link URI Arguments Recipe

This recipe demonstrates how to parse a deep link URL from an Android Intent into a Navigation key.

## Recipe components

It consists of two activities

1. `UriWithArgumentsDeepLinkActivity` constructs and triggers the deeplink request
2. `MainActivity` parses the intent into the target navigation key.

## How it works

The `MainActivity` handles the request with these steps

1. Declare a `UriDeepLinkMatcher` for each url pattern that can be deep linked into. Each matcher accepts a uri pattern and the KSerializer of the NavKey that supports this deep link.

2. Create a `DeepLinkRequest` with the incoming intent.

3. Match all candidate `UriDeepLinkMatchers` with the request and compare the resulting `UriMatchResults` for the best match.

4. Read the matching key from `UriMatchResult.key` or use default key if no match.

This recipe focuses on handing an intent and does not include these considerations:

- Create synthetic backStack
- Multi-modular setup
- DI
- Managing TaskStack
- Up button vs Back Button

## Demonstrated forms of deeplink

The `MainActivity` has several backStack keys to demonstrate different types of supported deep links:

1. `HomeKey` - deeplink with an exact url (no deeplink arguments)
2. `UsersKey` - deeplink with path arguments
3. `SearchKey` - deeplink with query arguments

See `MainActivity.deepLinkMatchers` for the actual url pattern of each.
[![](https://developer.android.com/static/images/picto-icons/code.svg) Explore View the full recipe on GitHub.](https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/deeplink/handlerequests/uriarguments)

```
package com.example.nav3recipes.deeplink.handlerequests.uriarguments

import androidx.navigation3.runtime.NavKey
import com.example.nav3recipes.deeplink.handlerequests.uriarguments.ui.STRING_LITERAL_FILTER
import com.example.nav3recipes.deeplink.handlerequests.uriarguments.ui.STRING_LITERAL_HOME
import com.example.nav3recipes.deeplink.handlerequests.uriarguments.ui.STRING_LITERAL_SEARCH
import com.example.nav3recipes.deeplink.handlerequests.uriarguments.ui.STRING_LITERAL_USERS
import kotlinx.serialization.Serializable

internal interface NavRecipeKey: NavKey {
    val name: String
}

@Serializable
internal object HomeKey: NavRecipeKey {
    override val name: String = STRING_LITERAL_HOME
}

@Serializable
internal data class UsersKey(
    val filter: String,
): NavRecipeKey {
    override val name: String = STRING_LITERAL_USERS
    companion object {
        const val FILTER_KEY = STRING_LITERAL_FILTER
        const val FILTER_OPTION_RECENTLY_ADDED = "recentlyAdded"
        const val FILTER_OPTION_ALL = "all"
    }
}

@Serializable
internal data class SearchKey(
    val firstName: String? = null,
    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val location: String? = null,
): NavRecipeKey {
    override val name: String = STRING_LITERAL_SEARCH
}
```

```
package com.example.nav3recipes.deeplink.handlerequests.uriarguments

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
import androidx.navigation3.runtime.deeplink.invoke
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.nav3recipes.common.deeplink.EntryScreen
import com.example.nav3recipes.common.deeplink.FriendsList
import com.example.nav3recipes.common.deeplink.LIST_USERS
import com.example.nav3recipes.common.deeplink.TextContent
import com.example.nav3recipes.deeplink.handlerequests.uriarguments.ui.URL_HOME_EXACT
import com.example.nav3recipes.deeplink.handlerequests.uriarguments.ui.URL_SEARCH
import com.example.nav3recipes.deeplink.handlerequests.uriarguments.ui.URL_USERS_WITH_FILTER
import com.example.nav3recipes.ui.setEdgeToEdgeConfig
import kotlinx.serialization.serializer

/**
 * See README.md for how this recipe works.
 */
class MainActivity : ComponentActivity() {
    /** STEP 1. Declare supported deep links */
    internal val deepLinkMatchers: List<UriDeepLinkMatcher<NavKey>> = listOf(
        // "https://www.nav3recipes.com/home"
        UriDeepLinkMatcher(URL_HOME_EXACT.toUri(), serializer<HomeKey>()),
        // "https://www.nav3recipes.com/users/with/{filter}"
        UriDeepLinkMatcher(URL_USERS_WITH_FILTER.toUri(), serializer<UsersKey>()),
        // "https://www.nav3recipes.com/users/search?{firstName}&{age}&{location}"
        UriDeepLinkMatcher(URL_SEARCH.toUri(), serializer<SearchKey>()),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)

        /** STEP 2. Create a [DeepLinkRequest] from the intent */
        val request = DeepLinkRequest(intent)

        /** STEP 3. Match the request to the DeepLinkMatchers*/
        // First get all the possible matching UriMatchResult
        val matches = deepLinkMatchers.mapNotNull {
            // returns null if no match
            it.match(request)
        }
        // compare all matches to find best match
        val bestMatch = matches.maxOrNull()
        /** STEP 4. Get the key from the match or use default key if no match*/
        val key = bestMatch?.key ?: HomeKey

        /**
         * STEP 5. pass the initial key to backstack
         */
        setContent {
            val backStack: NavBackStack<NavKey> = rememberNavBackStack(key)
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<HomeKey> { key ->
                        EntryScreen(key.name) {
                            TextContent("<matches exact url>")
                        }
                    }
                    entry<UsersKey> { key ->
                        EntryScreen("${key.name} : ${key.filter}") {
                            TextContent("<matches path argument>")
                            val list = when {
                                key.filter.isEmpty() -> LIST_USERS
                                key.filter == UsersKey.FILTER_OPTION_ALL -> LIST_USERS
                                else -> LIST_USERS.take(5)
                            }
                            FriendsList(list)
                        }
                    }
                    entry<SearchKey> { search ->
                        EntryScreen(search.name) {
                            TextContent("<matches query parameters, if any>")
                            val matchingUsers = LIST_USERS.filter { user ->
                                (search.firstName == null || user.firstName == search.firstName) &&
                                        (search.location == null || user.location == search.location) &&
                                        (search.ageMin == null || user.age >= search.ageMin) &&
                                        (search.ageMax == null || user.age <= search.ageMax)
                            }
                            FriendsList(matchingUsers)
                        }
                    }
                }
            )
        }
    }
}
```

```
package com.example.nav3recipes.deeplink.handlerequests.uriarguments

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.compose.dropUnlessResumed
import com.example.nav3recipes.common.deeplink.EMPTY
import com.example.nav3recipes.common.deeplink.EntryScreen
import com.example.nav3recipes.common.deeplink.FIRST_NAME_JOHN
import com.example.nav3recipes.common.deeplink.FIRST_NAME_JULIE
import com.example.nav3recipes.common.deeplink.FIRST_NAME_MARY
import com.example.nav3recipes.common.deeplink.FIRST_NAME_TOM
import com.example.nav3recipes.common.deeplink.LOCATION_BC
import com.example.nav3recipes.common.deeplink.LOCATION_BR
import com.example.nav3recipes.common.deeplink.LOCATION_CA
import com.example.nav3recipes.common.deeplink.LOCATION_US
import com.example.nav3recipes.common.deeplink.MenuDropDown
import com.example.nav3recipes.common.deeplink.MenuTextInput
import com.example.nav3recipes.common.deeplink.PaddedButton
import com.example.nav3recipes.common.deeplink.TextContent
import com.example.nav3recipes.deeplink.handlerequests.uriarguments.ui.PATH_BASE
import com.example.nav3recipes.deeplink.handlerequests.uriarguments.ui.PATH_INCLUDE
import com.example.nav3recipes.deeplink.handlerequests.uriarguments.ui.PATH_SEARCH
import com.example.nav3recipes.deeplink.handlerequests.uriarguments.ui.STRING_LITERAL_HOME
import com.example.nav3recipes.ui.setEdgeToEdgeConfig

/**
 * See README.md for how this recipe works.
 *
 * See [MainActivity] for how the requested deeplink is handled.
 */
class UriWithArgumentsDeepLinkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setEdgeToEdgeConfig()
        super.onCreate(savedInstanceState)

        setContent {
            /**
             * UI for deeplink sandbox
             */
            EntryScreen("Sandbox - Build Your Deeplink") {
                TextContent("Base url:\n${PATH_BASE}/")
                var showFilterOptions by remember { mutableStateOf(false) }
                val selectedPath = remember { mutableStateOf(MENU_OPTIONS_PATH[KEY_PATH]?.first()) }

                var showQueryOptions by remember { mutableStateOf(false) }
                var selectedFilter by remember { mutableStateOf("") }
                val selectedSearchQuery = remember { mutableStateMapOf<String, String>() }

                // manage path options
                MenuDropDown(
                    menuOptions = MENU_OPTIONS_PATH,
                ) { _, selection ->
                    selectedPath.value = selection
                    when (selection) {
                        PATH_SEARCH -> {
                            showQueryOptions = true
                            showFilterOptions = false
                        }

                        PATH_INCLUDE -> {
                            showQueryOptions = false
                            showFilterOptions = true
                        }

                        else -> {
                            showQueryOptions = false
                            showFilterOptions = false
                        }
                    }
                }

                // manage path filter options, reset state if menu is closed
                LaunchedEffect(showFilterOptions) {
                    selectedFilter = if (showFilterOptions) {
                        MENU_OPTIONS_FILTER.values.first().first()
                    } else {
                        ""
                    }
                }
                if (showFilterOptions) {
                    MenuDropDown(
                        menuOptions = MENU_OPTIONS_FILTER,
                    ) { _, selected ->
                        selectedFilter = selected
                    }
                }

                // manage query options, reset state if menu is closed
                LaunchedEffect(showQueryOptions) {
                    if (showQueryOptions) {
                        val initEntry = MENU_OPTIONS_SEARCH.entries.first()
                        selectedSearchQuery[initEntry.key] = initEntry.value.first()
                    } else {
                        selectedSearchQuery.clear()
                    }
                }
                if (showQueryOptions) {
                    MenuTextInput(
                        menuLabels = MENU_LABELS_SEARCH,
                    ) { label, selected ->
                        selectedSearchQuery[label] = selected
                    }
                    MenuDropDown(
                        menuOptions = MENU_OPTIONS_SEARCH,
                    ) { label, selected ->
                        selectedSearchQuery[label] = selected
                    }
                }

                // form final deeplink url
                val arguments = when (selectedPath.value) {
                    PATH_INCLUDE -> "/${selectedFilter}"
                    PATH_SEARCH -> {
                        buildString {
                            selectedSearchQuery.forEach { entry ->
                                if (entry.value.isNotEmpty()) {
                                    val prefix = if (isEmpty()) "?" else "&"
                                    append("$prefix${entry.key}=${entry.value}")
                                }
                            }
                        }
                    }

                    else -> ""
                }
                val finalUrl = "${PATH_BASE}/${selectedPath.value}$arguments"
                TextContent("Final url:\n$finalUrl")
                // deeplink to target
                PaddedButton("Deeplink Away!", onClick = dropUnlessResumed {
                    val intent = Intent(
                        this@UriWithArgumentsDeepLinkActivity,
                        MainActivity::class.java
                    )
                    // start activity with the url
                    intent.data = finalUrl.toUri()
                    startActivity(intent)
                })
            }
        }
    }
}

private const val KEY_PATH = "path"
private val MENU_OPTIONS_PATH = mapOf(
    KEY_PATH to listOf(
        STRING_LITERAL_HOME,
        PATH_INCLUDE,
        PATH_SEARCH,
    ),
)

private val MENU_OPTIONS_FILTER = mapOf(
    UsersKey.FILTER_KEY to listOf(UsersKey.FILTER_OPTION_RECENTLY_ADDED, UsersKey.FILTER_OPTION_ALL),
)

private val MENU_OPTIONS_SEARCH = mapOf(
    SearchKey::firstName.name to listOf(
        EMPTY,
        FIRST_NAME_JOHN,
        FIRST_NAME_TOM,
        FIRST_NAME_MARY,
        FIRST_NAME_JULIE
    ),
    SearchKey::location.name to listOf(EMPTY, LOCATION_CA, LOCATION_BC, LOCATION_BR, LOCATION_US)
)

private val MENU_LABELS_SEARCH = listOf(SearchKey::ageMin.name, SearchKey::ageMax.name)

```

```
package com.example.nav3recipes.deeplink.handlerequests.uriarguments.ui

import com.example.nav3recipes.deeplink.handlerequests.uriarguments.SearchKey

/**
 * String resources
 */
internal const val STRING_LITERAL_FILTER = "filter"
internal const val STRING_LITERAL_HOME = "home"
internal const val STRING_LITERAL_USERS = "users"
internal const val STRING_LITERAL_SEARCH = "search"
internal const val STRING_LITERAL_INCLUDE = "include"
internal const val PATH_BASE = "https://www.nav3recipes.com"
internal const val PATH_INCLUDE = "$STRING_LITERAL_USERS/$STRING_LITERAL_INCLUDE"
internal const val PATH_SEARCH = "$STRING_LITERAL_USERS/$STRING_LITERAL_SEARCH"
internal const val URL_HOME_EXACT = "$PATH_BASE/$STRING_LITERAL_HOME"

internal const val URL_USERS_WITH_FILTER = "$PATH_BASE/$PATH_INCLUDE/{$STRING_LITERAL_FILTER}"
internal val URL_SEARCH = "$PATH_BASE/$PATH_SEARCH" +
        "?${SearchKey::ageMin.name}={${SearchKey::ageMin.name}}" +
        "&${SearchKey::ageMax.name}={${SearchKey::ageMax.name}}" +
        "&${SearchKey::firstName.name}={${SearchKey::firstName.name}}" +
        "&${SearchKey::location.name}={${SearchKey::location.name}}"
```