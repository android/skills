## 1. Host-evaluated local state (`rememberMutableRemoteInt` and `valueChange`)

A Wear OS widget renders inside the system host process. Standard Jetpack
Compose in-memory state (`mutableStateOf`, `mutableIntStateOf`) can't recompose
when tapped on the watch surface.

Allocate a remote state register with `rememberMutableRemoteInt(initialValue)`.
Mutate it directly inside the host player using
`valueChange(remoteInt, remoteInt + delta)` without an IPC round-trip:

- Call `remoteInt.toRemoteString` as a **member function** on `RemoteInt` (for example, `"Label: ".rs + remoteInt.toRemoteString`). **NEVER** import `toRemoteString` as a top-level extension.

```kotlin
import androidx.compose.remote.creation.compose.action.valueChange
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.wear.compose.remote.material3.RemoteButton
import androidx.wear.compose.remote.material3.RemoteText

@RemoteComposable
@Composable
fun StatefulAdjusterSection() {
    val value = rememberMutableRemoteInt(0)

    RemoteColumn(
        modifier = RemoteModifier.fillMaxSize(),
        verticalArrangement = RemoteArrangement.Center,
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
    ) {
        // Call value.toRemoteString() as a member method (no extension import)
        RemoteText(text = "Value: ".rs + value.toRemoteString())
        RemoteRow(horizontalArrangement = RemoteArrangement.spacedBy(8.rdp)) {
            RemoteButton(onClick = valueChange(value, value - 1)) {
                RemoteText("-".rs)
            }
            RemoteButton(onClick = valueChange(value, value + 1)) {
                RemoteText("+".rs)
            }
        }
    }
}
```

## 2. Activity launches and background refresh callbacks (`pendingIntentAction`)

Use `pendingIntentAction` to launch an `Activity` (such as `MainActivity`) or
dispatch a background `BroadcastReceiver` refresh when tapped:

- Always pass a `(Context) -> PendingIntent` lambda: `pendingIntentAction { ctx -> PendingIntent.getActivity(...) }` or `pendingIntentAction { ctx -> PendingIntent.getBroadcast(...) }`.
- Attach the returned `Action` either to `RemoteButton(onClick = action)` or to any container using `RemoteModifier.clickable(action)`.
- In a `BroadcastReceiver`, call `goAsync` and invoke `widget.triggerUpdateAll(context)` to refresh all active widget instances. Register the `<receiver>` in `AndroidManifest.xml` when using a manifest broadcast.

```kotlin
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.remote.creation.compose.action.pendingIntentAction
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.wear.compose.remote.material3.RemoteButton
import androidx.wear.compose.remote.material3.RemoteText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@RemoteComposable
@Composable
fun InteractiveHeaderAndRefreshSection(
    activityClass: Class<*>,
    receiverClass: Class<out BroadcastReceiver>,
) {
    val launchActivityAction = pendingIntentAction { ctx ->
        val intent = Intent(ctx, activityClass).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        PendingIntent.getActivity(
            ctx,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    val backgroundRefreshAction = pendingIntentAction { ctx ->
        val intent = Intent(ctx, receiverClass).apply {
            action = ACTION_REFRESH_WIDGET
        }
        PendingIntent.getBroadcast(
            ctx,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    RemoteColumn(modifier = RemoteModifier.fillMaxSize()) {
        RemoteText(
            text = "Open App".rs,
            modifier = RemoteModifier.clickable(launchActivityAction),
        )
        RemoteButton(onClick = backgroundRefreshAction) {
            RemoteText(text = "Refresh".rs)
        }
    }
}

const val ACTION_REFRESH_WIDGET = "com.example.wear.ACTION_REFRESH_WIDGET"

class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                MyWidget().triggerUpdateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
```