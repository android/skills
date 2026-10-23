## Description

Use this document when you need to add, redesign, or expose keyboard
shortcuts in a Jetpack Compose application.

## Workflow

### Step 1: Inventory the actions

Before writing any handler, list the actions a keyboard user would want and
the screen or container that owns each one:

1. Top-level navigation destinations (bottom bar, rail, drawer, tabs), in display order.
2. The primary actions of each screen. Count actions, not buttons: a "New" button that opens a menu with two options is two actions.
3. Existing keyboard handling to extend rather than duplicate: `onKeyEvent` / `onPreviewKeyEvent`, `KeyboardActions`, `onProvideKeyboardShortcuts`, and the text fields that hold focus while the user types.

Map each entry to a shortcut with the table in Step 2 and keep the list.

### Step 2: Choose the shortcuts

- Prefer common OS conventions and use Ctrl as the primary modifier on Android.
- Add domain shortcuts only for frequent, central actions.
- Do not override standard text editing shortcuts.
- Use Escape to cancel or dismiss the current temporary UI. Do not use it as general Back navigation.

You must implement these keyboard shortcuts when they are relevant to
the app. The category tells you which app types each group applies to:

| Category | Action | Shortcut | Notes |
|---|---|---|---|
| General | Save | Ctrl+S | Use when the app has an explicit save action. |
| General | Find/Search | Ctrl+F | Focus the search field. |
| General | New item | Ctrl+N | Primary creation action (note, message, document, ...). Use Ctrl+Shift+N for a secondary creation action. |
| General | Open | Ctrl+O | Use for open/import flows. |
| General | Refresh | Ctrl+R or F5 | Avoid browser reload if web content is focused. |
| General | Dismiss temporary UI | Escape | Close the current dialog, menu, sheet or search mode. |
| Editing | Undo / Redo | Ctrl+Z / Ctrl+Shift+Z | Documents, canvases and lists; text fields already handle it. |
| Editing | Cut / Copy / Paste | Ctrl+X / Ctrl+C / Ctrl+V | Text fields already handle it; implement for custom selections (list items, canvas objects). |
| Editing | Select all | Ctrl+A | Lists and canvases; never override it in text fields. |
| Editing | Delete selected item | Delete or Backspace | Never trigger during text edit. |
| Editing | Zoom in / out / reset | Ctrl+Plus / Ctrl+Minus / Ctrl+0 | Documents, images, maps and canvases. |
| Navigation | Navigation destinations | Ctrl+1 to Ctrl+9 | Switch between primary navigation items (e.g., Tabs, Navigation Rail, or Drawer items) in order. |
| Navigation | Next / previous tab or document | Ctrl+Tab / Ctrl+Shift+Tab | Cycle through the open items of the current screen. |
| Communication | Send a message or submit a form | Enter | Chat-style input. Use Shift+Enter for a new line in multi-line text fields. |
| Communication | Send long-form content | Ctrl+Enter | Email, posts, comments: when Enter must insert a new line. |
| Communication | Switch active chat | Ctrl+Tab / Ctrl+Shift+Tab | Next / previous conversation in the list. |
| Media | Play / pause | Space | Only when the player or media list has focus. |
| Media | Seek backward / forward | Left / Right arrow | 5-10s step; Shift+arrow for a 30s-1m step. |
| Media | Next / previous track | Ctrl+Right / Ctrl+Left | Playlists and queues. |
| Media | Volume up / down | Up / Down arrow | Only when the player has focus. |
| Media | Mute / unmute | M (or Ctrl+M) | Use Ctrl+M when M would type into a focused field. |
| Media | Fullscreen | F | Escape exits fullscreen. |

### Step 3: Decide shortcut scope

Attach your shortcut dispatcher to the correct level of the UI hierarchy:

- **App-wide commands:** Attach the handler to the root navigation container or main scaffold (e.g., for switching root navigation destinations).
- **Screen-wide commands:** Attach the handler to a common ancestor of the focused screen content.
- **Modal commands:** Attach the handler inside the active dialog, menu, or sheet.
- **Contextual commands:** Attach the handler to the focused editor, canvas, list, or media surface.

### Step 4: Implement shortcut handling

Handle keyboard shortcuts in the relevant composable with
`onPreviewKeyEvent`. Prefer it over `onKeyEvent` for shortcuts that must work
while a text field has focus (Enter or Ctrl+Enter to send, Ctrl+S in an editor):
the field consumes those keys first, so an `onKeyEvent` handler never sees them.

- **Filter for KeyDown:** Check `event.type == KeyEventType.KeyDown` and ignore repeats (`event.nativeKeyEvent.repeatCount > 0`); otherwise the action also runs on key release and while the key is held.
- **Manage focus:** Key events are only dispatched to the active focus path. Ensure your target container or component uses `Modifier.focusable()` and actively holds focus (e.g., using `FocusRequester.requestFocus()`) to listen for these events.

### Step 5: Publish to Keyboard Shortcut Helper

Every shortcut on the Step 1 list must be visible to users using the system
Keyboard Shortcut Helper, with the action's visible name as the label (e.g.
"New drawing note"). Before finishing, compare the Helper entries with the
handlers you wrote: same keys, same modifiers, nothing missing.

Follow the Keyboard Shortcut Helper [guide](https://developer.android.com/develop/ui/compose/touch-input/keyboard-input/keyboard-shortcuts-helper).

## Common pitfalls

- Adding shortcuts for rare actions.
- Attaching handlers at the wrong scope or outside the active focus path.
- Stealing focus with an invisible root container.
- Intercepting focused-child behavior unintentionally with `onPreviewKeyEvent`.
- Repeating one-time commands on repeated `KeyDown` events.
- Consuming disabled or unhandled commands.
- Publishing Keyboard Shortcut Helper entries that do not match actual dispatch.

## Reference links

Consult these references before proceeding with the implementation.

- Compose keyboard input [guide](https://developer.android.com/develop/ui/compose/touch-input/keyboard-input/commands)
- Android desktop keyboard interaction [guidance](https://developer.android.com/design/ui/desktop/guides/interaction/keyboard)