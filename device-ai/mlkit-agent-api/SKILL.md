---
name: mlkit-agent-api
description: Builds autonomous on-device agentic workflows in Android apps using the
  ML Kit GenAI Agent API powered by Gemini Nano and AICore. Use this skill when implementing
  conversational agents, multi-turn chat with AgentSession in a ViewModel, autonomous
  tool calling using Android AppFunctions (@AppFunctionDeclaration and AppFunctionAgentTools.discover),
  ephemeral single-turn execution with runner.runSingleTurnAsync, or handling GenAiException
  error resilience. It solves the problem of orchestrating local reasoning loops,
  typed tool dispatch, and session persistence on-device with zero cloud latency and
  privacy preservation. Don't use for cloud-based Gemini models or Vertex AI, single-turn
  text generation without agent loops (suggest Prompt API), or non-Android platforms.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-24'
  keywords:
  - Android
  - ML Kit
  - GenAI
  - Agent API
  - Gemini Nano
  - AICore
  - AppFunctions
  - Tool Calling
  - Function Calling
  - Session Persistence
  - AgentSession
---

Targeting **AICore Developer Preview**. This skill provides authoritative
execution patterns and failure-prevention rules for on-device agentic workflows
in Android apps.

*** ** * ** ***

## 1. Prerequisites and setup

### Gradle dependencies

    // app/build.gradle.kts
    dependencies {
        // Core ML Kit GenAI Agent API
        implementation("com.google.mlkit:genai-agent:1.0.0-alpha1")

        // Android AppFunctions dependency for on-device tool declarations
        implementation("androidx.appfunctions:appfunctions:1.0.0-alpha13")
    }

> \[!IMPORTANT\] Always use official SDK interfaces from
> `com.google.mlkit.genai.agent.*`, `androidx.appfunctions.*`,
> `com.google.mlkit.genai.prompt.*`, and `com.google.mlkit.genai.common.*`.
> Never declare local placeholder interfaces or fallback mocks.

*** ** * ** ***

## 2. Minimal agent and runner initialization

Configure an `Agent` with `Agent.builder` and wrap execution in `AgentRunner`:

    import com.google.mlkit.genai.agent.Agent
    import com.google.mlkit.genai.agent.AgentRunner

    val agent = Agent.builder()
        .setName("OnDeviceAssistant")
        .setInstructions("You are a helpful on-device assistant.")
        .build()

    val runner = AgentRunner(agent)

- `Agent.builder` defaults to `maxTurns = 10` automatically to prevent infinite tool loops.
- `AgentRunner` defaults to greedy decoding (`temperature = 0.0f`) for deterministic tool selection and schema compliance.
- `AgentRunner(agent)` automatically resolves application context using `MlKitContext` for AppFunction execution in Android apps. An explicit `AgentRunner(context, agent)` overload is available for headless tests or custom context scopes.

*** ** * ** ***

## 3. Multi-turn chat and lifecycle (ViewModel)

`AgentSession` preserves conversational history in memory and is **not
`Parcelable`** . To preserve dialogue history across Android configuration
changes (such as screen rotation), retain `session` inside an AndroidX
`ViewModel`.

When executing single-turn ephemeral queries, managing concurrent
multi-sessions, or requiring explicit RAM cleanup, you must follow the
guidelines in [Session lifecycle](references/session_lifecycle.md).

    package com.example.app

    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.google.mlkit.genai.agent.Agent
    import com.google.mlkit.genai.agent.AgentRunner
    import com.google.mlkit.genai.agent.AgentSession
    import com.google.mlkit.genai.agent.textTokens
    import com.google.mlkit.genai.common.GenAiException
    import com.google.mlkit.genai.prompt.Content
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.asStateFlow
    import kotlinx.coroutines.launch

    class ChatViewModel : ViewModel() {
        private val agent = Agent.builder()
            .setInstructions("You are a helpful on-device assistant.")
            .build()
        private val runner = AgentRunner(agent)
        private val session: AgentSession =
            runner.getOrCreateSession("chat_session")

        private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
        val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

        fun sendMessage(prompt: String) {
            viewModelScope.launch {
                _uiState.value = ChatUiState.Loading("")
                try {
                    // Content.builder() required; runAsync takes Content
                    val content = Content.builder().text(prompt).build()
                    var fullResponse = ""
                    session.runAsync(content)
                        // Extension function requiring textTokens import
                        .textTokens()
                        .collect { token ->
                            fullResponse += token
                            _uiState.value = ChatUiState.Loading(fullResponse)
                        }
                    _uiState.value = ChatUiState.Success(fullResponse)
                } catch (e: GenAiException) {
                    // Catch GenAiException to prevent app crashes on AI errors
                    val err = e.localizedMessage ?: "AI error (${e.errorCode})"
                    _uiState.value = ChatUiState.Error(err)
                }
            }
        }

        override fun onCleared() {
            super.onCleared()
            // Evict conversation session from RAM when ViewModel is destroyed
            session.close()
        }
    }

### Essential lifecycle and coroutine rules

1. **`Content` object** : `session.runAsync` and `runner.runSingleTurnAsync` require a `Content` object. Always build it using `Content.builder().text(userPrompt).build()`.
2. **`textTokens` extension** : `textTokens` is a Kotlin extension function on `Flow<AgentEvent>`. Always include `import
   com.google.mlkit.genai.agent.textTokens`.
3. **Session cleanup** : Always invoke `session.close` in `ViewModel.onCleared` to evict conversational RAM and cancel pending tasks.
4. **`AgentEvent` stream** : When collecting `Flow<AgentEvent>` directly instead of `textTokens`, always include `import
   com.google.mlkit.genai.agent.AgentEvent`. Match against `AgentEvent.TextMessage(text, isPartial)`, `AgentEvent.FunctionCall(name, args)`, and `AgentEvent.FunctionResponse(name, response)`, and always include an `else
   -> {}` branch in `when (event)` expressions (`AgentEvent` is non-exhaustive for forward compatibility):

    import com.google.mlkit.genai.agent.AgentEvent

    session.runAsync(content).collect { event ->
        when (event) {
            is AgentEvent.FunctionCall -> {
                _uiState.update {
                    it.copy(activeToolStatus = "Tool started: ${event.name}")
                }
            }
            is AgentEvent.FunctionResponse -> {
                _uiState.update {
                    it.copy(activeToolStatus = "Tool completed: ${event.name}")
                }
            }
            is AgentEvent.TextMessage -> {
                _uiState.update {
                    it.copy(responseText = it.responseText + event.text)
                }
            }
            else -> {} // Required: AgentEvent is non-exhaustive for forward compat
        }
    }

*** ** * ** ***

## 4. Tool calling recipe

All tools used with `Agent` originate from Android AppFunctions
(`@AppFunctionDeclaration`), registered using the declarative path
(`enableAppFunctions(true)` or `addAppFunctionIds(...)`) or advanced discovery
(`AppFunctionAgentTools.discover(...)`).

When you need to restrict caller access levels for private in-app tools or wrap
discovered AppFunctions with human-in-the-loop (HITL) confirmation decorators,
you must consult [Tools and functions](references/tools_and_functions.md).

### Option A: Declarative AppFunctions path

Define tools declaratively in your app using AndroidX AppFunctions:

    package com.example.app.tools

    import androidx.appfunctions.AppFunctionDeclaration
    import androidx.appfunctions.AppFunctionSerializable

    @AppFunctionSerializable
    data class CalendarEvents(val date: String, val events: List<String>)

    class CalendarTools {
        /**
         * Fetches scheduled calendar appointments for a specific date in
         * YYYY-MM-DD format.
         *
         * @param date Date string in YYYY-MM-DD format
         */
        @AppFunctionDeclaration(isDescribedByKDoc = true)
        fun fetch_calendar(
            date: String
        ): CalendarEvents {
            return CalendarEvents(
                date = date,
                events = listOf(
                    "Team Standup at 10:00 AM",
                    "Product Review at 2:00 PM"
                )
            )
        }
    }

Enable AppFunctions on `Agent.Builder` and initialize `AgentRunner`:

    import com.google.mlkit.genai.agent.Agent
    import com.google.mlkit.genai.agent.AgentRunner

    val agent = Agent.builder()
        .setName("CalendarAssistant")
        .setInstructions("You assist with calendar queries.")
        // Automatically discovers AppFunctions in your app package
        .enableAppFunctions(true)
        // Optional: scope to specific function IDs or method names
        .addAppFunctionIds("fetch_calendar")
        .build()

    val runner = AgentRunner(agent)

- **AppFunction Contract** : Tool methods take domain parameters directly (no framework context parameter required). Returns must be primitives or `@AppFunctionSerializable` data classes (`Map<String, Any?>` isn't supported for AppFunctions).
- **Restricting Callers with `ACCESS_LEVEL_SELF`** : AppFunctions don't have to be public to external callers. To restrict execution strictly to your app's internal agent (blocking external system assistants and IPC callers), annotate with `@OptIn(ExperimentalAppFunctionsApi::class)` and `@AppFunctionAccessLevel(level = AppFunctionMetadata.ACCESS_LEVEL_SELF,
  isCompatEnforcementEnabled = true)`.

### Option B: Advanced discovery and decorator pattern

For inspecting, filtering, or decorating discovered AppFunctions (such as
Human-in-the-Loop confirmation wrappers using `AgentTool(delegate)`) before
registering with the agent:

    import com.google.mlkit.genai.agent.Agent
    import com.google.mlkit.genai.agent.AgentRunner
    import com.google.mlkit.genai.agent.AgentTool
    import com.google.mlkit.genai.agent.AppFunctionAgentTools

    // Discover tools (optionally scoped to function IDs)
    val discoveredTools = AppFunctionAgentTools.discover(
        enabledAppFunctionIds = listOf("fetch_calendar")
    )

    // Optional: Wrap sensitive tools using AgentTool(delegate) decorator
    class HitlConfirmationTool(
        delegate: AgentTool,
        private val onConfirm: suspend (
            toolName: String,
            args: Map<String, Any>
        ) -> Boolean
    ) : AgentTool(delegate) {
        override suspend fun execute(args: Map<String, Any>): Map<String, Any?> {
            if (!onConfirm(name, args)) {
                return mapOf("status" to "CANCELLED_BY_USER")
            }
            return checkNotNull(delegate).execute(args)
        }
    }

    val agent = Agent.builder()
        .setName("CalendarAssistant")
        .setInstructions("You assist with calendar queries.")
        .addTools(discoveredTools)
        .build()

    val runner = AgentRunner(agent)

> \[!WARNING\] **Mutual exclusivity** : Configure AppFunctions using either the
> declarative path (`enableAppFunctions(true)` on `Agent.Builder`) or the
> advanced path (`AppFunctionAgentTools.discover` passed to `addTools`), but
> not both. Using both simultaneously triggers a warning and registers
> duplicate tool schemas.

### Deprecated annotations (@Tool and @Param)

The legacy `@Tool` and `@Param` annotations in
`com.google.mlkit.genai.schema.annotations.*`, along with
`AgentTool.fromAnnotatedObject`, `Agent.Builder.addTools(annotatedObject:
Any)`, and standalone `FunctionTool`, have been deprecated or removed. All tools
added to `Agent` must originate from
`@androidx.appfunctions.AppFunctionDeclaration` using
`Agent.Builder.enableAppFunctions(true)` or `AppFunctionAgentTools.discover`.

*** ** * ** ***

## 5. Ephemeral single-turn execution

For one-off tasks where conversation history isn't needed (such as summarizing
text, formatting data, or answering standalone questions), invoke
`runner.runSingleTurnAsync(content)` directly:

    import com.google.mlkit.genai.agent.AgentRunner
    import com.google.mlkit.genai.agent.textTokens
    import com.google.mlkit.genai.prompt.Content
    import kotlinx.coroutines.flow.Flow

    // Executes a single-turn query ephemerally without session persistence:
    fun askQuickQuestion(runner: AgentRunner, question: String): Flow<String> {
        val content = Content.builder().text(question).build()
        return runner.runSingleTurnAsync(content).textTokens()
    }

- `runner.runSingleTurnAsync(content)` automatically creates and evicts an ephemeral session upon Flow completion.
- **Cleanup reference** :
  - `session.close`: Releases session resources and cancels coroutines (call in `ViewModel.onCleared`).
  - `session.delete` or `runner.deleteSession(sessionId)`: Permanently deletes the session from the runner.

*** ** * ** ***

## 6. Error handling and GenAiException reference

Runtime failures during on-device execution (such as model unavailability or
exceeding maximum turn limits) are emitted or thrown as `GenAiException`:

    import com.google.mlkit.genai.common.GenAiException

### Key error codes

- `GenAiException.ErrorCode.MAX_TURNS_EXCEEDED` (`-112`): The agent exceeded `maxTurns` (default 10) without completing the reasoning loop.
- `GenAiException.ErrorCode.NOT_AVAILABLE` (`8`): The Gemini Nano model or AICore service isn't available on this device.
- `GenAiException.ErrorCode.BUSY` (`9`): System or AICore is busy, or battery or thermal quota has been reached.
