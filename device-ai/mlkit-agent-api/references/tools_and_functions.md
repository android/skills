This reference details patterns for registering and executing local tools with
the **ML Kit GenAI Agent API** on Android (AICore Developer Preview).

*** ** * ** ***

## 1. Android AppFunctions (@AppFunctionDeclaration)

The primary declarative mechanism for defining on-device tools is **Android
AppFunctions** (`androidx.appfunctions`). AppFunctions provide a standardized
platform model for declaring typed in-app capabilities with automatic schema
generation.

### 1.1 Declaring an AppFunction

Annotate public methods with
`@androidx.appfunctions.AppFunctionDeclaration(isDescribedByKDoc = true)`. Tool
parameter descriptions are extracted automatically from KDoc `@param` tags:

    package com.example.ai.agent.tools

    import androidx.appfunctions.AppFunctionDeclaration
    import androidx.appfunctions.AppFunctionSerializable

    @AppFunctionSerializable
    data class CalendarEvents(
        val date: String,
        val events: List<String>,
    )

    class CalendarTools {

        /**
         * Fetches scheduled calendar events for a specific date in
         * YYYY-MM-DD format.
         *
         * @param date Date string in YYYY-MM-DD format (for example, "2026-10-15")
         */
        @AppFunctionDeclaration(isDescribedByKDoc = true)
        fun fetch_calendar(
            date: String
        ): CalendarEvents {
            return CalendarEvents(
                date = date,
                events = listOf(
                    "Team Sync at 10:00 AM",
                    "Product Review at 2:00 PM"
                )
            )
        }
    }

> \[!NOTE\] **AppFunction Parameter and Return Rules** : 1. **Parameters** : Tool
> methods take domain parameters directly (such as `date`). You don't need a
> framework context parameter. 2. **Return Types** : AppFunctions must return
> primitive types (`String`, `Int`, `Boolean`, and other primitives) or data
> classes annotated with `@AppFunctionSerializable`. Arbitrary `Map<String,
> Any?>` dictionaries aren't supported by the AppFunctions compiler.

### 1.2 Declarative path: whole-app discovery and scoping

For apps where all or specific AppFunctions must execute automatically
when the model calls them:

    import com.google.mlkit.genai.agent.Agent
    import com.google.mlkit.genai.agent.AgentRunner

    // 1. Configure the Agent with AppFunctions enabled
    val agent = Agent.builder()
        .setName("CalendarAssistant")
        .setInstructions("You assist with calendar queries.")
        // Automatically discovers AppFunctions in your app package
        .enableAppFunctions(true)
        // Optional: scope to specific function IDs or method names
        .addAppFunctionIds("fetch_calendar")
        .build()

    // 2. Initialize the Runner
    val runner = AgentRunner(agent)

- **Scoping to Specific Functions** : Use `.addAppFunctionId(id)` or `.addAppFunctionIds(listOf(...))` to restrict the agent's available functions. Supply either the full identifier (`"com.example.MyApp#createNote"`) or the short method name (`"createNote"`).
- **Context Resolution** : `AgentRunner(agent)` automatically resolves the application context using `MlKitContext.getInstance().applicationContext`. An explicit `AgentRunner(context, agent)` overload is available for headless tests or custom context scopes.

### 1.3 Advanced path: programmatic discovery

When your app needs to inspect discovered AppFunctions, filter them, or decorate
them (for example, adding Human-in-the-Loop confirmation dialogs) before
registering them with the agent:

    import com.google.mlkit.genai.agent.Agent
    import com.google.mlkit.genai.agent.AgentRunner
    import com.google.mlkit.genai.agent.AgentTool
    import com.google.mlkit.genai.agent.AppFunctionAgentTools

    // Suspending discovery scoped to calling app package on Dispatchers.IO:
    // Overloads:
    // - AppFunctionAgentTools.discover() (uses applicationContext)
    // - AppFunctionAgentTools.discover(context)
    // - AppFunctionAgentTools.discover(enabledAppFunctionIds)
    // - AppFunctionAgentTools.discover(context, enabledAppFunctionIds)
    val discoveredTools: List<AgentTool> = AppFunctionAgentTools.discover(
        enabledAppFunctionIds = listOf("fetch_calendar")
    )

    val agent = Agent.builder()
        .setName("CalendarAssistant")
        .setInstructions("You assist with calendar queries.")
        .addTools(discoveredTools)
        .build()

    val runner = AgentRunner(agent)

> \[!WARNING\] **Mutual exclusivity guard** : Use either the declarative path
> (`enableAppFunctions(true)` or `addAppFunctionIds` on `Agent.Builder`) or the
> advanced path (`AppFunctionAgentTools.discover` passed to `addTools`), but
> not both. Registering both emits a runtime warning and exposes duplicate
> tool schemas to the model.

### 1.4 Caller scoping and access levels

AppFunctions do not have to be public to the system or third-party assistants.
In Android, access control is handled through caller access levels rather than
manual caller identity checks (see Android AppFunctions caller scoping
documentation).

By default (`ACCESS_LEVEL_ANDROID_TRUSTED`), platform-certified on-device
assistants holding `android.permission.EXECUTE_APP_FUNCTIONS` discover and
execute an AppFunction. However, for functions intended exclusively for your
app's internal agent (for example, querying private local databases, modifying
user preferences, or invoking transactional APIs), restrict caller access
using `ACCESS_LEVEL_SELF`:

    import androidx.appfunctions.AppFunctionAccessLevel
    import androidx.appfunctions.AppFunctionDeclaration
    import androidx.appfunctions.ExperimentalAppFunctionsApi
    import androidx.appfunctions.metadata.AppFunctionMetadata

    class SecureAccountTools {

        /**
         * Updates an internal account setting. Restricted strictly to this app's
         * agent.
         */
        @OptIn(ExperimentalAppFunctionsApi::class)
        @AppFunctionDeclaration(isDescribedByKDoc = true)
        @AppFunctionAccessLevel(
            level = AppFunctionMetadata.ACCESS_LEVEL_SELF,
            isCompatEnforcementEnabled = true,
        )
        fun update_preference(
            key: String,
            value: String
        ): Boolean {
            // Only your app's in-process agent invokes this function
            return true
        }
    }

#### Access level hierarchy

| Access level | Constant | Scope and permissions |
|---|---|---|
| **Self Only** | `AppFunctionMetadata.ACCESS_LEVEL_SELF` (`100`) | Restricted strictly to the host app itself. External assistants, system surfaces, and other apps cannot discover or invoke the function over IPC. |
| **System** | `AppFunctionMetadata.ACCESS_LEVEL_SYSTEM` (`200`) | Restricted to privileged system callers holding `android.Manifest.permission#EXECUTE_APP_FUNCTIONS_SYSTEM` (for example, Launcher, Recents) and self. |
| **App Trusted** | `AppFunctionMetadata.ACCESS_LEVEL_APP_TRUSTED` | Restricted to explicit agent package names declared in your application's manifest. |
| **Android Trusted (Default)** | `AppFunctionMetadata.ACCESS_LEVEL_ANDROID_TRUSTED` (`300`) | Open to platform-certified on-device assistants holding `EXECUTE_APP_FUNCTIONS`, system callers, and self. |

> \[!NOTE\] **HITL Interception and Platform IPC** : When an AppFunction is
> invoked externally by a system assistant over IPC, the request routes
> directly to your app's `AppFunctionService`, completely bypassing any
> in-process `HitlConfirmationTool` wrappers. To require sensitive operations
> to run through your confirmation UI, restrict the AppFunction to
> `ACCESS_LEVEL_SELF` so that only your in-app agent invokes it.

*** ** * ** ***

## 2. Decorator pattern and human-in-the-loop (HITL)

Sensitive tools---such as financial transfers, data deletion, or outbound
communication---MUST NOT execute autonomously without user consent. The Agent
API provides built-in support for the **Decorator pattern** on `AgentTool`.

### 2.1 Tool delegation

All tools registered with `Agent` must originate from Android AppFunctions
(validated at construction time). `AgentTool` provides a public delegating
constructor `AgentTool(delegate: AgentTool, name: String = delegate.name,
description: String = delegate.description)` that wraps an underlying
AppFunction tool discovered using `AppFunctionAgentTools.discover` (or another
decorator wrapping one).

### 2.2 Implementing a confirmation wrapper

    import com.google.mlkit.genai.agent.AgentTool

    /**
     * Decorator tool that intercepts execution to prompt for user confirmation.
     */
    class HitlConfirmationTool(
        delegate: AgentTool,
        private val onConfirm: suspend (
            toolName: String,
            args: Map<String, Any>
        ) -> Boolean
    ) : AgentTool(delegate) {

        override suspend fun execute(args: Map<String, Any>): Map<String, Any?> {
            // 1. Request user confirmation before invoking the sensitive tool
            val confirmed = onConfirm(name, args)
            if (!confirmed) {
                return mapOf(
                    "status" to "CANCELLED_BY_USER",
                    "message" to "The user declined to confirm this action."
                )
            }

            // 2. Delegate to the underlying tool execution
            return checkNotNull(delegate).execute(args)
        }
    }

### 2.3 Wrapping discovered AppFunctions

    val allTools = AppFunctionAgentTools.discover(context)

    val configuredTools = allTools.map { tool ->
        if (tool.name == "deleteCalendarEvent" || tool.name == "sendInvites") {
            HitlConfirmationTool(tool) { name, args ->
                showUserConfirmationDialog(name, args)
            }
        } else {
            tool
        }
    }

    val agent = Agent.builder()
        .setName("SafeAssistant")
        .addTools(configuredTools)
        .build()

    val runner = AgentRunner(agent)

*** ** * ** ***

## 3. Automated orchestration loop

Unlike manual function calling where the app must parse raw function call
responses and manually craft follow-up turns, `AgentRunner` handles the complete
loop internally:

1. The model analyzes user prompt context and decides to invoke a tool.
2. `AgentRunner` emits `AgentEvent.FunctionCall(name, args)`.
3. `AgentRunner` executes the corresponding AppFunction tool.
4. `AgentRunner` emits `AgentEvent.FunctionResponse(name, response)`.
5. `AgentRunner` feeds the response back into inference until the model emits `AgentEvent.TextMessage(text, isPartial)` or `maxTurns` is reached.

    import com.google.mlkit.genai.agent.AgentEvent

    session.runAsync(content).collect { event ->
        when (event) {
            is AgentEvent.TextMessage -> appendText(event.text)
            is AgentEvent.FunctionCall ->
                showToolProgress("Running ${event.name}...")
            is AgentEvent.FunctionResponse ->
                showToolProgress("Completed ${event.name}")
            else -> {} // Required: AgentEvent is non-exhaustive for forward compat
        }
    }

*** ** * ** ***

## 4. Deprecated and removed tool patterns

The annotations `@Tool` and `@Param` from package
`com.google.mlkit.genai.schema.annotations.*`, reflection-based registration
using `AgentTool.fromAnnotatedObject` and
`Agent.Builder.addTools(annotatedObject: Any)`, and dynamic `FunctionTool`
have been deprecated or removed. All tools added to `Agent` must be backed by
Android AppFunctions.

### Migration guide

| Legacy pattern (`@Tool`) | AppFunctions pattern (`@AppFunctionDeclaration`) |
|---|---|
| `@Tool(description = "...")` | `@androidx.appfunctions.AppFunctionDeclaration(isDescribedByKDoc = true)` |
| `@Param(description = "...")` | KDoc `@param` tags or AppFunctions metadata |
| `Agent.builder().addTools(MyTools())` | `Agent.builder().enableAppFunctions(true)` |
| `AgentTool.fromAnnotatedObject(instance)` | `AppFunctionAgentTools.discover(context)` |

Migrate existing code to `@androidx.appfunctions.AppFunctionDeclaration`
with `Agent.Builder.enableAppFunctions(true)` and `AgentRunner(agent)`
for declarative registration, or `AppFunctionAgentTools.discover` for
custom filtering and decoration.