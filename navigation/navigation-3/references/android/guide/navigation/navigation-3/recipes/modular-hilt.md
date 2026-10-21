# Modular Navigation Recipe (Hilt)

This recipe demonstrates how to structure a multi-module application using Navigation 3 and Dagger/Hilt for dependency injection. The goal is to create a decoupled architecture where navigation is defined and implemented in separate feature modules.

## How it works

The application is divided into several modules:

- **`app` module** : This is the main application module. It provides `HiltDeepLinkActivity` (an entry point demonstrating regular and deep link intent launches), `HiltModularActivity` (which hosts the `NavDisplay` and delegates deep link intents to `Navigator.handleDeepLink(intent)`), the start destination binding, and builds the final `entryProvider`.

- **`common` module**: This module contains the core navigation logic, including:

  - A `Navigator` class that manages the back stack, resolves incoming deep link intents, and builds synthetic back stacks via `handleDeepLink(intent)`.
  - A `startDestination: NavKey` used to initialize the back stack.
  - An `EntryProviderInstaller` type, which is a function that feature modules use to contribute their navigation entries to the application's `entryProvider`.
  - `BackStackMatcher<*, NavKey>`, which feature modules use to contribute their deep link matchers with synthetic back stacks.
- **Feature modules (e.g., `conversation`, `profile`)**: Each feature is split into two sub-modules:

  - **`api` module**: Defines the public API for the feature, including its navigation routes and deep link URI patterns. This allows other modules to navigate to this feature without needing to know about its implementation details.
  - **`impl` module** : Provides the implementation of the feature, including its composables, an `EntryProviderInstaller` that maps the feature's routes to its composables, and a `BackStackMatcher<*, NavKey>` configured with `.withBackStack` to establish the appropriate hierarchical back stack (e.g., `[ConversationList, ConversationDetail(id)]`). Both are provided using Dagger/Hilt multibindings (`@IntoSet`).

This modular approach allows for a clean separation of concerns, making the codebase more scalable and maintainable. Each feature is responsible for its own navigation logic, and the `app` module only combines these pieces together.
[![](https://developer.android.com/static/images/picto-icons/code.svg) Explore View the full recipe on GitHub.](https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/modular/hilt)

```
package com.example.nav3recipes.modular.hilt

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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.Serializable

// API
@Serializable
object Profile : NavKey

const val PROFILE_URI = "https://www.nav3recipes.com/profile"

// IMPLEMENTATION
@Module
@InstallIn(ActivityRetainedComponent::class)
object ProfileModule {

    @IntoSet
    @Provides
    fun provideEntryProviderInstaller() : EntryProviderInstaller = {
        entry<Profile>{
            ProfileScreen()
        }
    }

    @IntoSet
    @Provides
    fun provideDeepLinkMatcher(): BackStackMatcher<*, NavKey> =
        UriDeepLinkMatcher(
            uriPattern = DeepLinkUri(PROFILE_URI),
            serializer = Profile.serializer(),
        ).withBackStack { matchResult ->
            listOf(ConversationList, matchResult.key)
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
package com.example.nav3recipes.modular.hilt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.nav3recipes.ui.setEdgeToEdgeConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HiltModularActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var entryProviderScopes: Set<@JvmSuppressWildcards EntryProviderInstaller>

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
                    entryProvider = entryProvider {
                        entryProviderScopes.forEach { builder -> this.builder() }
                    }
                )
            }
        }
    }
}
```

```
package com.example.nav3recipes.modular.hilt

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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.Serializable

// API
object ConversationList : NavKey

@Serializable
data class ConversationDetail(val id: Int) : NavKey {
    val color: Color
        get() = colors[id % colors.size]
}

const val CONVERSATION_URI = "https://www.nav3recipes.com/conversation"

// IMPL
@Module
@InstallIn(ActivityRetainedComponent::class)
object ConversationModule {

    @IntoSet
    @Provides
    fun provideEntryProviderInstaller(navigator: Navigator): EntryProviderInstaller =
        {
            entry<ConversationList> {
                ConversationListScreen(
                    onConversationClicked = { conversationDetail ->
                        navigator.goTo(conversationDetail)
                    }
                )
            }
            entry<ConversationDetail> { key ->
                ConversationDetailScreen(key) {
                    navigator.goTo(Profile)
                }
            }
        }

    @IntoSet
    @Provides
    fun provideDeepLinkMatcher(): BackStackMatcher<*, NavKey> =
        UriDeepLinkMatcher(
            uriPattern = DeepLinkUri("$CONVERSATION_URI/{id}"),
            serializer = ConversationDetail.serializer(),
        ).withBackStack { matchResult ->
            listOf(ConversationList, matchResult.key)
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
package com.example.nav3recipes.modular.hilt

import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.BackStackMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.invoke
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject

typealias EntryProviderInstaller = EntryProviderScope<NavKey>.() -> Unit

@ActivityRetainedScoped
class Navigator @Inject constructor(
    startDestination: NavKey,
    private val deepLinkMatchers: Set<@JvmSuppressWildcards BackStackMatcher<*, NavKey>>,
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
package com.example.nav3recipes.modular.hilt

import androidx.navigation3.runtime.NavKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent

@Module
@InstallIn(ActivityRetainedComponent::class)
object AppModule {

    @Provides
    fun provideStartDestination(): NavKey = ConversationList
}
```