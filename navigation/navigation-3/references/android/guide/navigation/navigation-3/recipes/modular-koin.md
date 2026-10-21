# Modular Navigation Recipe (Koin)

This recipe demonstrates how to structure a multi-module application using Navigation 3 and Koin for dependency injection. The goal is to create a decoupled architecture where navigation is defined and implemented in separate feature modules. It relies on the [`koin-compose-navigation3`](https://insert-koin.io/docs/reference/koin-compose/navigation3) artifact.

## How it works

The application is divided into several Android modules:

- **`app` module** : This is the main application module. It provides `KoinDeepLinkActivity` (an entry point demonstrating regular and deep link intent launches), `KoinModularActivity` (which hosts the `NavDisplay` and delegates deep link intents to `Navigator.handleDeepLink(intent)`), and initializes a common `Navigator` (resolving all registered `BackStackMatcher<*, NavKey>`s via `getAll()`).

- **`common` module**: This module contains the core navigation logic used by both the application module and the feature modules, including:

  - A `Navigator` class that manages the back stack, resolves incoming deep link intents, and builds synthetic back stacks via `handleDeepLink(intent)`.
  - A `startDestination: NavKey` used to initialize the back stack.
  - `BackStackMatcher<*, NavKey>`, which feature modules use to contribute their deep link matchers with synthetic back stacks.
- **Feature modules (e.g., `conversation`, `profile`)**: Each feature is split into two sub-modules:

  - **`api` module**: Defines the public API for the feature, including its navigation routes and deep link URI patterns. This allows other modules to navigate to this feature without needing to know about its implementation details.
  - **`impl` module** : Provides the implementation of the feature, including its composables and Koin `Module`. The Koin module uses the [`navigation`](https://insert-koin.io/docs/reference/koin-compose/navigation3/#declaring-navigation-entries) DSL to define the entry provider installers and `single<BackStackMatcher<*, NavKey>>(named<...>())` configured with `.withBackStack` to establish the appropriate hierarchical back stack (e.g., `[ConversationList, ConversationDetail(id)]`).

This modular approach allows for a clean separation of concerns, making the codebase more scalable and maintainable. Each feature is responsible for its own navigation logic, and the `app` module only combines these pieces together.
[![](https://developer.android.com/static/images/picto-icons/code.svg) Explore View the full recipe on GitHub.](https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/modular/koin)

```
package com.example.nav3recipes.modular.koin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.BackStackMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkUri
import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
import androidx.navigation3.runtime.deeplink.withBackStack
import kotlinx.serialization.Serializable
import org.koin.androidx.scope.dsl.activityRetainedScope
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

// API
@Serializable
object Profile : NavKey

const val PROFILE_URI = "https://www.nav3recipes.com/profile"

// IMPL
@OptIn(KoinExperimentalAPI::class)
val profileModule = module {
    activityRetainedScope {
        navigation<Profile> { ProfileScreen() }
    }

    single<BackStackMatcher<*, NavKey>>(named<Profile>()) {
        UriDeepLinkMatcher(
            uriPattern = DeepLinkUri(PROFILE_URI),
            serializer = Profile.serializer(),
        ).withBackStack { matchResult ->
            listOf(ConversationList, matchResult.key)
        }
    }
}

@Composable
private fun ProfileScreen() {
    val profileColor = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(profileColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Profile Screen",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
```

```
package com.example.nav3recipes.modular.koin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.BackStackMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkUri
import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
import androidx.navigation3.runtime.deeplink.withBackStack
import com.example.nav3recipes.ui.theme.colors
import kotlinx.serialization.Serializable
import org.koin.androidx.scope.dsl.activityRetainedScope
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

// API
object ConversationList : NavKey

@Serializable
data class ConversationDetail(val id: Int) : NavKey {
    val color: Color
        get() = colors[id % colors.size]
}

const val CONVERSATION_URI = "https://www.nav3recipes.com/conversation"

// IMPL
@OptIn(KoinExperimentalAPI::class)
val conversationModule = module {
    activityRetainedScope {
        navigation<ConversationList> {
            ConversationListScreen(
                onConversationClicked = { conversationDetail ->
                    get<Navigator>().goTo(conversationDetail)
                }
            )
        }

        navigation<ConversationDetail> { key ->
            ConversationDetailScreen(key) {
                get<Navigator>().goTo(Profile)
            }
        }
    }

    single<BackStackMatcher<*, NavKey>>(named<ConversationDetail>()) {
        UriDeepLinkMatcher(
            uriPattern = DeepLinkUri("$CONVERSATION_URI/{id}"),
            serializer = ConversationDetail.serializer(),
        ).withBackStack { matchResult ->
            listOf(ConversationList, matchResult.key)
        }
    }
}

@Composable
private fun ConversationListScreen(
    onConversationClicked: (ConversationDetail) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        items(10) { index ->
            val conversationId = index + 1
            val conversationDetail = ConversationDetail(conversationId)
            val backgroundColor = conversationDetail.color
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = dropUnlessResumed {
                        onConversationClicked(conversationDetail)
                    }),
                headlineContent = {
                    Text(
                        text = "Conversation $conversationId",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = backgroundColor // Set container color directly
                )
            )
        }
    }
}

@Composable
private fun ConversationDetailScreen(
    conversationDetail: ConversationDetail,
    onProfileClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(conversationDetail.color)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Conversation Detail Screen: ${conversationDetail.id}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = dropUnlessResumed(block = onProfileClicked)) {
            Text("View Profile")
        }
    }
}
```

```
package com.example.nav3recipes.modular.koin

import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.BackStackMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.invoke

class Navigator(
    startDestination: NavKey,
    private val deepLinkMatchers: List<BackStackMatcher<*, NavKey>>,
) {
    val backStack: SnapshotStateList<NavKey> = mutableStateListOf(startDestination)

    fun goTo(destination: NavKey) {
        backStack.add(destination)
    }

    fun handleDeepLink(intent: Intent) {
        if (intent.data == null && intent.extras == null) return

        val request = DeepLinkRequest(intent)
        val matchResult = deepLinkMatchers.firstNotNullOfOrNull { matcher ->
            matcher.match(request)
        } ?: return

        backStack.clear()
        backStack.addAll(matchResult.backStack)
    }

    fun goBack() {
        backStack.removeLastOrNull()
    }
}
```

```
package com.example.nav3recipes.modular.koin

import org.koin.androidx.scope.dsl.activityRetainedScope
import org.koin.dsl.module

val appModule = module {
    includes(profileModule, conversationModule)

    activityRetainedScope {
        scoped {
            Navigator(
                startDestination = ConversationList,
                deepLinkMatchers = getAll(),
            )
        }
    }
}
```

```
package com.example.nav3recipes.modular.koin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.NavDisplay
import com.example.nav3recipes.ui.setEdgeToEdgeConfig
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.compose.navigation3.getEntryProvider
import org.koin.androidx.scope.activityRetainedScope
import org.koin.core.Koin
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.component.KoinComponent
import org.koin.core.scope.Scope
import org.koin.dsl.koinApplication

/**
 * This recipe demonstrates how to use a modular approach with Navigation 3,
 * where different parts of the application are defined in separate modules and injected
 * into the main app using Koin.
 * 
 * Features (Conversation and Profile) are split into two modules: 
 * - api: defines the public facing routes for this feature
 * - impl: defines the entryProviders for this feature, these are injected into the app's main activity
 * The common module defines:
 * - a common navigator class that exposes a back stack and methods to modify that back stack
 * - a type that should be used by feature modules to inject entryProviders into the app's main activity
 * The app module creates the navigator by supplying a start destination and provides this navigator
 * to the rest of the app module (i.e. MainActivity) and the feature modules.
 */
@OptIn(KoinExperimentalAPI::class)
class KoinModularActivity : ComponentActivity(), AndroidScopeComponent, KoinComponent {
    // Local Koin Context Instance
    companion object {
        private val localKoin = koinApplication {
            modules(appModule)
        }.koin
    }
    // Override default Koin context to use the local one
    override fun getKoin(): Koin = localKoin
    override val scope : Scope by activityRetainedScope()
    val navigator: Navigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setEdgeToEdgeConfig()
        navigator.handleDeepLink(intent)
        setContent {
            Scaffold { paddingValues ->
                NavDisplay(
                    backStack = navigator.backStack,
                    modifier = Modifier.padding(paddingValues),
                    onBack = { navigator.goBack() },
                    entryProvider = getEntryProvider()
                )
            }
        }
    }

}
```