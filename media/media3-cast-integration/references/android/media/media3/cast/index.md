The Media3 [`cast`](https://developer.android.com/reference/androidx/media3/cast/package-summary) module extends your media playback experience to
Cast devices. It lets your app cast content to [compatible devices](https://developer.android.com/media/media3/cast/create-castplayer#get-device),
such as TVs and speakers. The [sender](https://developers.google.com/cast/glossary#sender) app acts as a remote controller for media
playback on the [receiver](https://developers.google.com/cast/glossary#receiver) device. This module integrates with the [Jetpack
Media3](https://developer.android.com/guide/topics/media/media3) library, so you can use the same `Player` interface and UI
components for both local and remote playback.


## Android skills

[View on GitHub](https://github.com/android/skills/tree/main/media/media3-cast-integration)

### Integrate Media3 Cast

Use the Media3 Cast [Android skill](https://developer.android.com/tools/agents/android-skills) to implement Google Cast support in your app. To install the skill from the [Android CLI](https://developer.android.com/tools/agents/android-cli), run:

    android skills add media3-cast-integration

<br />

## Media3 CastPlayer

The `CastPlayer` is a media player API included in Media3 that supports both
local and remote media playback. It implements the `Player` interface to manage
playback, simplifying the integration of casting into your media experience.

[Go to Media3 CastPlayer](https://developer.android.com/media/media3/cast/create-castplayer)

## Other Cast integrations

The Cast SDK overview explains how to integrate Cast with other platforms, such
as building a receiver app or integrating with the web sender SDK.

[Explore Cast SDK](https://developers.google.com/cast/docs/developers)