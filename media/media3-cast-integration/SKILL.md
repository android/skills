---
name: media3-cast-integration
description: Implements Google Cast support in Android apps using Jetpack Media3.
  Handles adding build dependencies, updating manifest, configuring OptionsProvider,
  and managing CastPlayer or RemoteCastPlayer for playback in both Compose and View-based
  UIs. Use when adding Cast functionality or migrating from legacy Cast SDK to Media3
  Cast.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-09-16'
  keywords:
  - Android
  - Media3
  - Cast
  - Integration
  - Migration
  - ExoPlayer
  - MediaSession
  - Jetpack Media3
---

## Prerequisites

- Jetpack Media3 version must be `>= 1.9.0`. Cast isn't available in lower versions.

## Glossary

- **`CastPlayer`** : Media3 `Player` that controls playback on both local and remote Cast devices.
- **`RemoteCastPlayer`** : Media3 `Player` that communicates with a Cast receiver, only used for remote playback.
- **Google Cast SDK**: Legacy casting SDK in maintenance mode, superseded by Jetpack Media3.
- **`OptionsProvider`** : Interface providing configuration options to initialize GMS `CastContext`.

## Common guidelines

- Legacy [Google Cast SDK](https://developers.google.com/cast) is in maintenance mode.
- For new Cast setups:
  - You must use [Jetpack Media3 Cast](references/android/media/media3/cast/index.md).
  - You mustn't use legacy Cast SDK unless explicitly requested.

## Step 1: Set up dependencies

To complete this step, you **MUST** ensure the following:

- In the app-level build file, declare the `media3-cast` dependency version
  1.9.0 or higher.

      implementation("androidx.media3:media3-cast:1.11.1")

- Ensure required Media3 dependencies are present:

  - `androidx.media3:media3-exoplayer`
  - `androidx.media3:media3-session`
  - `androidx.media3:media3-ui-compose`
- If the application uses legacy Views, add `media3-ui`.

- Enforce the same versions across all Media3 dependencies.

- Use configurations in "Add build dependencies" section of [Getting started
  with CastPlayer](references/android/media/media3/cast/create-castplayer.md) as the source of
  truth.

- **For apps without an existing Cast integration:**

  - Verify legacy Cast SDK (`libs.play.services.cast.framework`) is absent.
- **If Migrating from Legacy Cast SDK:**

  - Add Media3 Cast dependencies first.
  - Keep existing legacy dependencies untouched at this stage to prevent compilation errors.

## Step 2: Update the manifest

To complete this step, you **MUST** ensure the following:

- Inside the manifest's `<application>` tag, declare the Cast options provider.
- Use `DefaultCastOptionsProvider` by default. See the "OptionsProvider" section in [Getting started with
  CastPlayer](references/android/media/media3/cast/create-castplayer.md).
- Declare a custom `OptionsProvider` only if explicitly requested. See [Customize CastOptions](references/android/media/media3/cast/customize-castoptions.md).
- Ensure `INTERNET` permission is present. Don't add any unnecessary permissions.
- **If Migrating from Legacy Cast SDK:**
  - Don't delete existing custom options provider files or manifest entries.

## Step 3: Implement the player and service

### Architecture baseline

Before integrating Media3 Cast, an existing app follows one of two setups:

- **Local-only playback:** Uses Media3 `ExoPlayer` only to support local playback.
- **Legacy Cast setup:** Uses `ExoPlayer` for local playback, alongside a `Player` wrapper over the legacy `RemoteMediaClient` for remote playback. The UI interfaces with a `MediaSession` interacting with a `ForwardingPlayer`, which finally routes controls to either local or remote playback.

To complete this step, you **MUST** ensure the following:

- Inside the application's `MediaSessionService` (or `MediaLibraryService`) `onCreate()` method, initialize `ExoPlayer` and `CastPlayer`.
- Use `CastPlayer` by default unless `RemoteCastPlayer` is explicitly requested. See the "Build a CastPlayer" section in [Getting started with
  CastPlayer](references/android/media/media3/cast/create-castplayer.md).
- For `CastPlayer`, pass the instance directly to `MediaSession.Builder`.
- Replace all legacy forwarding player wrappers.
- Don't delete legacy class files yet to prevent compilation errors during migration.

### Advanced: `RemoteCastPlayer`

- Use `RemoteCastPlayer` only if explicitly requested by user.
- Initialize `MediaSession` with `localPlayer` and set a `SessionAvailabilityListener` on `RemoteCastPlayer` to transfer playback state on Cast session availability changes:

    class PlaybackService : MediaSessionService() {
      private var mediaSession: MediaSession? = null
      private lateinit var localPlayer: ExoPlayer
      private lateinit var remotePlayer: RemoteCastPlayer

      override fun onCreate() {
        super.onCreate()

        localPlayer = ExoPlayer.Builder(this).build()
        remotePlayer = RemoteCastPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, localPlayer).build()

        remotePlayer.setSessionAvailabilityListener(
          object : SessionAvailabilityListener {
            override fun onCastSessionAvailable() {
              transferPlaybackState(localPlayer, remotePlayer)
            }

            override fun onCastSessionUnavailable() {
              transferPlaybackState(remotePlayer, localPlayer)
            }
          }
        )
      }

      private fun transferPlaybackState(previousPlayer: Player, newPlayer: Player) {
        if (previousPlayer.mediaItemCount > 0) {
          val transferStateBuilder = PlayerTransferState.builderFromPlayer(previousPlayer)
          if (previousPlayer.playbackState == Player.STATE_ENDED ||
              previousPlayer.currentPosition == C.TIME_END_OF_SOURCE) {
            transferStateBuilder.setCurrentMediaItemIndex(0)
            transferStateBuilder.setCurrentPosition(0)
          }
          transferStateBuilder.build().setToPlayer(newPlayer)
        }

        previousPlayer.stop()
        previousPlayer.clearMediaItems()
        newPlayer.prepare()
        mediaSession?.setPlayer(newPlayer)
      }
    }

## Step 4: Set up the UI

### Compose-based UI

To complete this step, you **MUST** ensure the following:

- See the "Add a MediaRouteButton Composable to the Player" section in [Getting started with CastPlayer](references/android/media/media3/cast/create-castplayer.md) for Compose integration guidelines.
- Use the [`MediaRouteButton` composable](https://developer.android.com/reference/kotlin/androidx/media3/cast/MediaRouteButton.composable) from `androidx.media3.cast` package.
- Don't use `AndroidView` in the Compose UI hierarchy.
- Place `MediaRouteButton` in an area next to playback controls. Don't hide it behind system UI.
- Don't use `PlayerSurface` for custom player UI. Use the Material3 [`Player`
  composable](https://developer.android.com/reference/kotlin/androidx/media3/ui/compose/material3/Player.composable).
- Force recomposition on playback location shifts to ensure UI sync. Use key
  constraints on `DeviceInfo` changes:

      @OptIn(UnstableApi::class)
      @Composable
      fun MainScreen() {
         val player = rememberMediaController()
         val deviceInfo = rememberDeviceInfo(player)
         player?.let { activePlayer -> key(deviceInfo) { PlayerScreen(player = activePlayer) } }
      }

      @Composable
      private fun rememberMediaController(): Player? {
         // Logic to connect MediaController to MediaSession and release it
      }

      @Composable
      private fun rememberDeviceInfo(player: Player?): DeviceInfo? {
         var deviceInfo by remember(player) { mutableStateOf(player?.deviceInfo) }
         DisposableEffect(player) {
             val activePlayer = player ?: return@DisposableEffect onDispose {}
             deviceInfo = activePlayer.deviceInfo
             val listener = object : Player.Listener {
                 override fun onDeviceInfoChanged(info: DeviceInfo) {
                     deviceInfo = info
                 }
             }
            activePlayer.addListener(listener)
            onDispose { activePlayer.removeListener(listener) }
         }
         return deviceInfo
      }

### View-based UI

To complete this step, you **MUST** ensure the following:

- For View-based UI setups, see the "Add UI elements" section in [Getting
  started with CastPlayer](references/android/media/media3/cast/create-castplayer.md).
- Casting Activities must extend `AppCompatActivity` or `FragmentActivity` and use a `Theme.AppCompat` descendant.
- Ensure the `AppCompat` theme has a visible `ActionBar` if adding `MediaRouteButton` to the options menu.
- Replace all instances and imports of `CastButtonFactory` with `MediaRouteButtonFactory`.
- Rebind `PlayerView.player` references upon `onDeviceInfoChanged` events to
  prevent black screens or UI freezes:

      private val playerListener: Player.Listener =
        object : Player.Listener {
          override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
            // Resetting to null bypasses PlayerView.setPlayer()'s instance equality check
            // (this.player == player), forcing it to re-bind the video surface to the controller.
            playerView.player = null
            playerView.player = controller
          }
        }

- **Migration to Compose:**

  - Don't use `AndroidView` to wrap the legacy `PlayerView`.
  - Implement Material3 [`Player` composable](https://developer.android.com/reference/kotlin/androidx/media3/ui/compose/material3/Player.composable) and [`MediaRouteButton` composable](https://developer.android.com/reference/kotlin/androidx/media3/cast/MediaRouteButton.composable) as per [Getting started with CastPlayer](references/android/media/media3/cast/create-castplayer.md).
  - Remove legacy XML layout declarations, menu files, and View component references.

## Step 5: Clean up legacy Cast SDK code

> [!WARNING]
> **Warning:** Don't perform cleanup directly. Remove legacy files and dependencies only when explicitly requested by the user.

To complete this step, you **MUST** ensure the following:

- Remove legacy GMS Cast SDK (`libs.play.services.cast.framework`) and MediaRouter (`libs.androidx.mediarouter`) dependencies.
- Delete custom `OptionsProvider` classes and manifest entries if `DefaultCastOptionsProvider` is adopted.
- Remove legacy `MediaTransferReceiver` manifest declarations if present.
- Remove all references to legacy Cast SDK components such as legacy helper wrappers, forwarding players, and `RemoteMediaClient` interfaces.
- Delete legacy View XML layouts, menu files, and references to `PlayerView` if the migration to Compose is complete.
