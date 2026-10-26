This reference details conversation lifecycle management and memory hygiene with
the **ML Kit GenAI Agent API**.

*** ** * ** ***

## 1. Multi-turn session persistence

Multi-turn conversation history resides in RAM indexed by `sessionId`:

- Obtain a scoped `AgentSession` using `runner.getOrCreateSession(sessionId)`.
- Store `sessionId` in `SavedStateHandle` to preserve multi-turn context across Android configuration changes (for example, screen rotation).
- Calling `session.runAsync(content)` automatically continues the conversation thread.

    import com.google.mlkit.genai.prompt.Content

    class ChatViewModel(
        private val runner: AgentRunner,
        savedStateHandle: SavedStateHandle
    ) : ViewModel() {
        private val KEY_SESSION = "session_id"
        val sessionId: String =
            savedStateHandle[KEY_SESSION] ?: UUID.randomUUID().toString().also {
                savedStateHandle[KEY_SESSION] = it
            }
        val session: AgentSession = runner.getOrCreateSession(sessionId)

        fun sendMessage(prompt: String): Flow<AgentEvent> {
            val content = Content.builder().text(prompt).build()
            return session.runAsync(content)
        }

        override fun onCleared() {
            super.onCleared()
            // Evict specific session when user exits chat
            session.close() // or runner.deleteSession(sessionId)
        }
    }

*** ** * ** ***

## 2. Ephemeral single-turn execution

For tasks where conversation history isn't needed (for example, photo editing
macros, one-off text summaries), invoke `runner.runSingleTurnAsync(content)`
directly:

    val content = Content.builder().text("Summarize this document: ...").build()
    runner.runSingleTurnAsync(content)
        .textTokens()
        .collect { token ->
            showSummary(token)
        }

When executing in single-turn mode using `runner.runSingleTurnAsync(content)`,
`AgentRunner` allocates an ephemeral session ID internally and evicts it from
memory automatically upon Flow completion.

*** ** * ** ***

## 3. Explicit RAM cleanup and disposal

- `session.close`: Non-suspending; idempotently releases session resources and cancels in-flight coroutines in `ViewModel.onCleared`.
- `session.delete` or `runner.deleteSession(sessionId)`: Suspending function that frees the specified session from RAM and awaits completion.
- `runner.close`: `AgentRunner` implements `AutoCloseable`. Calling `runner.close` closes all active sessions, cancels in-flight coroutines, and releases the underlying model client.

*** ** * ** ***

## 4. Concurrent multi-session management

`AgentRunner` supports multiple ongoing conversation sessions simultaneously:

- Each unique `sessionId` isolates its own dialogue history in memory.
- Calls across distinct session IDs execute concurrently without contention.
- Requests within the same `sessionId` are safely serialized.
- Calling `deleteSession(sessionId)` frees only that session's memory without affecting other active conversations.