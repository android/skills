Optimizes AppFunction KDoc and the app description for AI agents and Model
Context Protocol.

## Instructions

### Workflow: Agent-centric documentation

1. **Identify the core outcome** : Start the description with a third-person verb and the resource it acts on. Examples: "Searches contacts", "Creates a task", and "Updates a channel". Put key information first, because some agents don't read the whole description. Focus on the *user benefit*, not the code implementation.
2. **Behavior** : State conditional behavior, for example, "If `query` is null, returns the 3 most recent contacts." Never describe call order across functions, such as "Search for the contact first." Write call order in the app description instead.
3. **Parameter documentation** :
   - For **functions** : Use specific `@param` tags. Isolate validation rules and default values here. Describe the result with `@return`. Only the summary, `@param`, and `@return` reach agents; other tags, such as `@throws`, don't.
   - For **serializables** : Use inline KDoc directly for each property declaration. KSP **won't** extract documentation from class-level tags.
4. **Error surface mapping** : The agent receives the type and message of the exception that a function throws. Throw the matching `AppFunctionException` subclass with a plain-language message, for example, "No contact matches `query`."

### Workflow: App description

When writing the app description in the `appfn:instructions` attribute of
`app_metadata.xml`, follow these instructions. Before 1.0.0-beta01, this
attribute was `appfn:description`.

1. **Lead with the domain**: Start with the app's primary domain, then give a concise list of the tasks that its AppFunctions perform.
2. **Capture cross-function relationships** : Explain dependencies or sequences between functions, for example, "Call 'searchContacts' to get the `endpointValue` for 'send'." Agents decide whether to follow the app description, so never rely on it to enforce critical behavior.
3. **Document operational patterns**: Guide the LLM on token-conserving usage, for example, "Use 'batch_update' over multiple 'update' calls."
4. **Specify constraints** : State constraints that apply across functions, for example, "File operations limited to workspace" or "Rate limit: 10 requests per minute." Put limits on a single parameter in that function's `@param` tag.
5. **Anti-patterns** :
   - Don't repeat individual function descriptions.
   - Don't include marketing claims or subjective praise.
   - Don't attempt to prompt model personality or conversation style.

## Critical constraints

### Descriptive, not commanding

Describe what the function *does* , not what the LLM *must* do. Avoid phrases
like "You must call this..." or "You must ask the user for...". Start with the
outcome instead, such as "Creates a task with a title and due date."

### No "fluff"

Remove conversational padding like "This method is used to..." or "Helpful
for...". Be concise, specific, and neutral.

### No emphasis markup

Never use excessive capitalization or special characters to draw attention,
such as `**CRITICAL**` or `VERY IMPORTANT`.

### Accurate and self-contained

The description matches what the code does, including its parameters and
output. A function described as read-only never writes or deletes data. Never
describe more than the function does, because agents then call it for
unrelated requests. Never reference or try to influence other AppFunctions,
including other apps' AppFunctions.

### Instruction annotations override KDoc

If an element has `@AppFunctionInstruction`, its text replaces that element's
KDoc in the generated metadata. Refine the annotation text instead of the KDoc.

### Inline KDoc for serializables

**Mandatory** : For `@AppFunctionSerializable` classes, documentation must be
inline for each property. KSP ignores class-level `@param` or `@property` tags
for these classes.

## Examples

### Example: MCP refactoring

**Original**:

`/** This function helps you find people. */`

**Refined**:

     /**
       * Searches for message recipients by name or email.
       * @param query Search string for name/email. If null, returns 3 most recent contacts.
       * @return List of "Recipient" objects matching the query.
       */

### Example: App description

**Refined**:

    This app manages tasks for teams: creating and assigning tasks, updating task
    status, and searching users.

    Operational Patterns:
    - Always use 'searchUsers' to resolve user handles to internal IDs before calling 'assignTask'.
    - Prefer 'batchUpdateStatus' when modifying more than three tasks simultaneously to reduce latency.

    Constraints:
    - Task titles are limited to 100 characters.
    - Attachment uploads are limited to 5 MB.