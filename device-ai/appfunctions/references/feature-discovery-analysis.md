Analyzes Android codebases to identify and recommend high-value AppFunctions.

## Instructions

### Discover features

1. **Analyze manifest and entry points** : Scan `AndroidManifest.xml` and Activity, Fragment, and Service classes to identify core user journeys, for example, search, create, or share.
2. **Identify atomic tasks**: Look for methods or logic that represent distinct, self-contained user outcomes.
3. **Evaluate AI value**: Prioritize tasks that users perform often, or that are easier to express in conversation than through UI navigation, for example, "Remind me to call Alice when I get home."
4. **Recommend and justify**: List recommendations with a rationale. Focus on how an AI assistant adds value, such as efficiency, hands-free use, or multi-step automation.

## Critical constraints

### Tool-first thinking

Avoid recommending functions that are purely informational or redundant with
existing system actions. Focus on "mutations" (writing data) or "rich queries"
(finding specific entities).

### Security and privacy

Recommend only safe, non-destructive tasks that the app's UI already offers on
that form factor. Never recommend functions that require or return
passwords, financial details, private keys, or other highly sensitive personal
information, or that give agents wide-ranging access to user data or system
resources. On shared devices, such as TV, Android Auto, and Android
Automotive OS, respect the user's privacy.

### Focused functions

Recommend functions with a single, well-defined purpose. Never recommend
multi-purpose functions, several functions with similar intent, or more
functions than a use case needs, because agents confuse similar functions.

## Examples

### Example 1: Media app discovery

**Recommended AppFunction** : `playArtistRadio`

**Rationale**: Lets you start a personalized music stream using a voice
command, bypassing several layers of navigation in the "Search" and "Artist"
menus.

**Input required** : `artistName` as a `String`.

### Example 2: Chat app contact discovery

**Recommended AppFunction** : `searchContacts`

**Rationale** : Serves as a "rich query" to resolve a human-readable contact
name, email, or chat group to a unique identifier (`endpointValue`), which is a
prerequisite before executing actions like sending messages or
initiating calls. Also allows retrieving recently contacted entities when given
a blank query.

**Input required** : Query as a `String`, and Filter Type as a `String`
constrained to `"INDIVIDUAL"` or `"GROUP"`.

### Example 3: Chat app message sending

**Recommended AppFunction** : `send`

**Rationale**: This mutation function lets you send text messages and
optional image attachments to a contact or group using natural language
commands, for example, "Tell Alice I'm running 5 minutes late." This
eliminates multi-step UI navigation across contact lists and conversation
threads.

**Input required** : Endpoint Value as a `String`, Message Body as a `String`,
and Image URIs as an optional `List` of URIs.

### Example 4: Chat app voice calling

**Recommended AppFunction** : `makeCall`

**Rationale**: Lets you initiate voice calls hands-free to a contact or
group using an AI agent without navigating the app's UI.

**Input required** : `endpointValue` as a `String`.