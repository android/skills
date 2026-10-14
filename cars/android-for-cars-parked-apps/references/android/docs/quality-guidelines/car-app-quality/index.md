To provide a great experience for users in cars, complete the car compatibility
checklists and tests that follow as you design and develop your app.

The checklists and tests define an extensive set of quality requirements for the
various app categories supported by Android Auto and Android Automotive OS. Many
requirements apply only to specific categories, so be sure to filter by your
app's category. See the [Car quality tiers](https://developer.android.com/docs/quality-guidelines/car-app-quality#car-quality-tiers) definitions
for details on which guidelines your app must meet in order to be accepted on
the Google Play Store.

> [!NOTE]
> **September 2026 update:** The Games app category is now generally available. See [September 2026](https://developer.android.com/docs/quality-guidelines/car-app-quality#sep-26) for details.

## Test your app

Test your app for the applicable criteria listed on this page before submitting
your app to Google Play for review. As applicable, test both of the ways
users can access your app:

Android Automotive OS
:   Use the [Android Emulator](https://developer.android.com/studio/run/emulator) to validate each checklist
    item. For more information, see [Test using the Android Automotive OS
    emulator](https://developer.android.com/training/cars/testing/emulator).

Android Auto
:   Use the Android Auto Desktop Head Unit (DHU) to validate each checklist item.
    For more information, see [Test using the Desktop Head Unit](https://developer.android.com/training/cars/testing/dhu).

## App categories

> [!IMPORTANT]
> **Key Point:** Select your app's categories to filter the guidelines on this page.

| [Supported categories](https://developer.android.com/training/cars#supported-app-categories) | Android Auto | Android Automotive OS |
|---|---|---|
| Media | ✔ | ✔ |
| Media (templated) labs | ✔ | ✔ |
| Communication - messaging notifications | ✔ |   |
| Communication - templated messaging labs | ✔ |   |
| Communication - calling labs | ✔ |   |
| Navigation | ✔ | ✔ |
| Point of Interest (POI) | ✔ | ✔ |
| Internet of Things (IOT) | ✔ | ✔ |
| Weather | ✔ | ✔ |
| Video |   | ✔ |
| Games | ✔ | ✔ |
| Browsers labs |   | ✔ |

## Car quality tiers

The quality tiers define criteria to help you assess the level of support your
app provides for cars. Each category builds upon those below it. That is, for an
app to be *Car optimized* , it should also meet all of the applicable *Car ready*
requirements.

In addition to the guidelines specific to cars, each tier has associated
guidelines from [Adaptive app quality guidelines](https://developer.android.com/docs/quality-guidelines/large-screen-app-quality) that
are relevant for activities implemented by your app.

Levels of support include the following:

### Tier 3 - Car ready

Your app is large screen compatible and can be used while the car is parked.
While it may not have any car-optimized features, users can experience the app
just as they would on any other large screen Android-powered device.

This tier primarily applies to the [parked categories](https://developer.android.com/training/cars/parked) of apps,
which must meet all of the requirements in this tier to be accepted on the
Google Play Store.

> [!NOTE]
> **Note:** Through the [Car ready mobile apps program](https://developer.android.com/training/cars/car-ready-mobile-apps), mobile apps meeting the requirements in this tier can be made available for download from the Google Play Store on Android Automotive OS vehicles with Google built-in.

### Tier 2 - Car optimized

Your app provides a great experience on the car's center stack display. To
accomplish this, your app has some car-specific capabilities that users can
experience across driving or parked modes, depending on your app's category.

Apps in [categories built for use while driving](https://developer.android.com/training/cars#supported-app-categories) must
meet meet all of the applicable requirements in this tier to be accepted on the
Google Play Store.

### Tier 1 - Car differentiated

Your app is built to work across the variety of hardware in cars and can adapt
its experience across driving and parked modes. It provides the best user
experience designed for the different screens in cars such as the center
console, instrument cluster, and additional screens - like panoramic displays
seen in many premium cars.

### Car ready

| Criteria | ID | Applicable Categories | Description |
|---|---|---|---|
| Permitted Categories | `PC-1` | All | The app must not include features outside the app types intended for cars. See [Supported app categories](https://developer.android.com/training/cars#supported-app-categories). |
| Expected Performance | `EP-1` | All | The app must work as expected or described in the app's Google Play Store listing. |
| Expected Performance | `EP-2` | Media, Messaging (templated), Calling, Navigation, POI, IOT, Weather, Video, Games, Browsers | When the app is relaunched from the home screen, the app must restore the app state as closely as possible to the previous state. |
| Expected Performance | `EP-3` | Games | The app is responsive to input and does not freeze or stutter during gameplay. |
| Expected Performance | `EP-4` | Media, Messaging (templated), Calling, Navigation, POI, IOT, Weather | **Android Automotive OS only:** The app listing on Google Play Store must include screenshots for the Android Automotive OS form factor. See [Provide Android Automotive OS screenshots](https://developer.android.com/training/cars/distribute#opt-in-aaos). Note: For apps in the parked app categories, screenshots are optional. However, if your app includes features unique to cars, or the app differs significantly from the experience on mobile or tablet, it is recommended to include screenshots in your Google Play Store listing. See [Provide Android Automotive OS screenshots](https://developer.android.com/training/cars/distribute#opt-in-aaos). |
| Display Orientation | `DO-1` | Video, Games, Browsers | **Android Automotive OS only:** The app can be distributed to devices with fixed screen orientations. See [Required Android Automotive OS features](https://developer.android.com/training/cars/parked/automotive-os#required-features). |
| Display Orientation | `DO-2` | Games | **Android Auto only:** The app is not significantly pillarboxed when running on landscape displays. See [Support common Android Auto screen sizes](https://developer.android.com/training/cars/parked/auto#support-screen-sizes). |
| App Rendering | `AR-1` | All categories supported by Android Automotive OS | In activities implemented by the app, interactive UI elements must not be obstructed by system bars or display cutouts. See [Work with window insets and display cutouts](https://developer.android.com/training/cars/parked/automotive-os#insets-and-cutouts). |
| Driver Distraction | `DD-3` | Video, Games, Browsers | The app must not be launchable or usable while driving and must not play any audio. See [Meet driver distraction requirements](https://developer.android.com/training/cars/parked/automotive-os#driver-distraction). Note: If your app plays audio, it must meet the [`DD-2`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DD-2) requirement. |
| Irrelevant notifications | `IN-2` | Video, Games, Browsers | The app must not post any [heads up notifications](https://developer.android.com/training/cars/platforms/automotive-os/notifications#hun). |
| App Navigability | `AN-1` | Video, Games, Browsers | Users can navigate through the app without encountering any dead ends. |
| Sensitive Data | `SD-1` | Browsers | Browsers must not save or allow access to passwords or payment information unless the [user can block access to passwords](https://developer.android.com/training/cars/parked/browser#block-sensitive) using a profile lock. |
| Sensitive Data | `SD-2` | Browsers | Before syncing data to the car, browsers that synchronize passwords or payments data must do the following steps: 1. Prompt the user to authenticate. 2. Notify the user on the car screen their data will be synchronized to the car. For details, see [Allow users to block access to sensitive data](https://developer.android.com/training/cars/parked/browser#block-sensitive). |

#### Associated large screen quality guidelines

While these guidelines are relevant for all activities implemented by your app,
they are Tier 3 requirements only for [parked apps](https://developer.android.com/training/cars/parked).

| Criteria | ID | Guidance for cars |
|---|---|---|
| Configuration and continuity | [`LS-C1`](https://developer.android.com/docs/quality-guidelines/large-screen-app-quality#LS-C1) | Configuration changes in cars are primarily caused by moving an app between displays, such as when moving an app to or from a [distant display](https://developer.android.com/training/cars/testing/emulator/distant-display). |
| Configuration and continuity | [`LS-C2`](https://developer.android.com/docs/quality-guidelines/large-screen-app-quality#LS-C2) | No additional guidance. |

### Car optimized

| Criteria | ID | Applicable Categories | Description |
|---|---|---|---|
| Parked Experiences | `PE-1` | Media, Navigation, POI, IOT, Weather | **Android Automotive OS only:** with the exception of providing setup, settings, and sign-in flows while parked, the app must not provide any functionality through its own activities. |
| Screen Animation | `SA-1` | Media, Messaging, Calling, Navigation, POI, IOT, Weather | The app must not display animated elements on the screen, such as animated graphics or video. **Exception:** Canvas animations while the user is parked are allowed if they are relevant to the driving task. |
| Visual or Text Ads | `AD-1` | Media, Messaging, Calling, Navigation, POI, IOT, Weather | The app must not display text-based advertising other than the advertiser's name or the product name. |
| Image Usage | `IU-1` | Media, Messaging, Calling, Navigation, POI, IOT, Weather | App elements don't display any images on the screen. Exceptions include: - Apps can display a single static image for content context in the background of the consumption screen, such as album art or the advertiser's corporate logo. - Apps can display icons in the content navigation drawer. - Apps can display images and photographs to aid in driving decision-making. - Navigation apps can display an image for lane guidance or junction guidance. |
| Visual Information on Phone | `VI-1` | Media, Messaging (templated), Calling, Navigation, POI, IOT, Weather | **Android Auto only:** If the user must go to the phone screen---for example, to act on a permission request---then the app must display a message instructing the user to only look at their phone screen when it's safe to do so. For more information, see [Handle general errors](https://developer.android.com/training/cars/media/errors) for media, and [Handle user input](https://developer.android.com/training/cars/apps#handle-user-input) for navigation, point of interest, internet of things, and weather apps. |
| App Doesn't Crash | `AC-1` | Messaging (templated), Calling, Navigation, POI, IOT, Weather | Users must be able to complete tasks in the app using five screens or fewer. For more information, see [Template restrictions](https://developer.android.com/training/cars/apps#template-restrictions). |
| Scrolling Text | `ST-1` | Media, Messaging, Calling, Navigation, POI, IOT, Weather | The app must not display automatically scrolling text. |
| Voice Commands | `VC-1` | Media, Navigation | The app must support Gemini and Google Assistant voice commands. For more information, see [Support voice actions](https://developer.android.com/training/cars/media/voice-actions) for media, and [Support navigation intents](https://developer.android.com/training/cars/apps/navigation#support-navigation-intents) for navigation. |
| Delayed Response | `DR-1` | Media, Messaging (templated), Calling, Navigation, POI, IOT, Weather | App-specific buttons must respond to user actions with no more than a two-second delay. |
| Delayed Response | `DR-2` | Media, Messaging (templated), Calling, Navigation, POI, IOT, Weather | The app must launch in no more than 10 seconds. |
| Delayed Response | `DR-3` | Media, Messaging (templated), Calling, Navigation, POI, IOT, Weather | The app must load content in no more than 10 seconds. |
| Contrast | `VD-1` | Media, Messaging, Calling, Navigation, POI, IOT, Weather | The app must provide icons and colors that meet Android Auto's contrast requirements. For more information, see [Contrast](https://developers.google.com/cars/design/android-auto/design-system/color#contrast). |
| Contrast | `VD-2` | Media | The app must provide white icon sets that the system colorizes to provide automatic contrast compensation. |
| Contrast | `VD-3` | Media | The app must provide colors that the system can optimize for in-vehicle readability. |
| Theming | `TH-1` | Messaging (templated), Navigation, POI, IOT, Weather | Apps that add custom theming to Car App Library components with version 1.9 or higher must support light and dark theme. See [Contrast](https://developer.android.com/develop/ui/compose/styles/theming)for more information. |
| Grey Buttons | `GB-1` | Media | Interactive elements that are intentionally disabled must be nonfunctional. |
| Driver Distraction | `DD-1` | Navigation | The navigation audio channel can only be used by navigation apps and for navigation instructions. For more information, see [Voice guidance](https://developer.android.com/training/cars/apps/navigation#voice-guidance). |
| Driver Distraction | `DD-2` | Video, Games, Browsers | While driving, the app must not be launchable and the app's UI must not be visible. The app's audio must stop when the user starts driving and cannot be unpaused while driving. For more information, see [Meet driver distraction requirements](https://developer.android.com/training/cars/parked/automotive-os#driver-distraction). **Exception:** Video apps that support audio while driving can continue playback on supported devices. See [`DD-4`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DD-4) . |
| Payments | `PA-1` | Navigation, POI, IOT, Weather | The app must have simple flows if purchases are enabled, using shortcuts such as recent or favorite purchases. For more information, see [Purchase using an existing payment method](https://developers.google.com/cars/design/create-apps/sample-flows/purchase-with-existing-method). The app must not allow any of the following: - Setup of payment methods - Multiple items to be selected for purchase - Commitment to recurring payments, such as subscriptions |
| Notification Ads | `NA-1` | Media, Messaging, Calling, Navigation, POI, IOT, Weather | The app must not present advertisements through notifications. |
| Irrelevant Notifications | `IN-1` | Media, Messaging, Calling, Navigation, POI, IOT, Weather | The app must display notifications only when relevant to the driver's needs. Examples: Good: Notifying the user that a new message has arrived. Bad: Notifying the user about a new album release. Bad: For weather apps, your app posts notifications that are not related to imminent alerts for the current location, route, or destination. |
| Media Autoplay | `MA-1` | Media | The app must not autoplay on startup or without user initiated action to select the app or app media. For more information, see [Implement play commands](https://developer.android.com/training/cars/media#playback-commands). |
| Messaging Functionality | `MF-1` | Messaging | The app must receive incoming messages. For more information, see [Extend messaging notifications to Android Auto](https://developer.android.com/training/cars/communication/notification-messaging). |
| Messaging Functionality | `MF-2` | Messaging | Messages must be properly grouped and displayed in the correct order. For more information, see [Extend messaging notifications to Android Auto](https://developer.android.com/training/cars/communication/notification-messaging). |
| Messaging Functionality | `MF-3` | Messaging | The user can reply to a message. For more information, see [Extend messaging notifications to Android Auto](https://developer.android.com/training/cars/communication/notification-messaging). |
| Messaging Functionality | `MF-4` | Messaging | The app must use short-form messaging app design patterns. Long-form messaging apps, such as apps for email, are not permitted. |
| Messaging Functionality | `MF-5` | Messaging | The app must implement a peer-to-peer messaging service and not notification services, such as those for weather, stocks, and sport scores. |
| Navigation Functionality | `NF-1` | Navigation | The app must provide turn-by-turn navigation directions. |
| Navigation Functionality | `NF-2` | Navigation | The app draws only map content on the surface of the navigation templates. Text-based turn-by-turn directions, lane guidance, and estimated arrival time must be displayed on the relevant components of the navigation template. Additional information relevant to the drive, speed limit, road obstructions, etc., can be drawn on the safe area of the map. |
| Navigation Functionality | `NF-3` | Navigation | When the app provides text-based turn-by-turn directions, it must also trigger navigation notifications. For more information, see [Turn-by-turn notifications](https://developer.android.com/training/cars/apps/navigation#turn-by-turn-notifications). |
| Navigation Functionality | `NF-4` | Navigation | When the navigation app provides text-based turn-by-turn directions, it must send next-turn information to the vehicle's cluster display. For more information, see [Navigation metadata](https://developer.android.com/training/cars/apps/navigation#navigation-metadata). |
| Navigation Functionality | `NF-5` | Navigation | The app must not provide turn-by-turn notifications, voice guidance, or cluster information when another navigation app is providing turn-by-turn instructions. For more information, see [Start, end, and stop navigation](https://developer.android.com/training/cars/apps/navigation#starting-ending-stopping-navigation). |
| Navigation Functionality | `NF-6` | Navigation | The app must handle navigation requests from other apps. For more information, see [Support navigation intents](https://developer.android.com/training/cars/apps/navigation#support-navigation-intents). |
| Navigation Functionality | `NF-7` | Navigation | The app must provide a "test drive" mode that simulates driving. For more information, see [Simulate navigation](https://developer.android.com/training/cars/apps/navigation#simulating-navigation). |
| Point of Interest Functionality | `PF-1` | POI | The app must provide meaningful functionality relevant to driving. |
| Internet of Things Functionality | `IT-1` | IOT | The IOT app may allow the following while driving: - **View the current state of devices**. For example: to view if a garage door is open or closed, a light bulb is on or off, a security system is armed or disarmed, or a washer is running or completed. - **Simple, one-touch features that control on and off functions**. For example: the ability to turn various devices on and off or open and close them, including turning lights on and off, turning a thermostat on and off, or opening and closing a garage door or curtain. This also includes turning a pre-programmed scene or routine on and off. - **Notify users about an event in the home or another location**. For example: receiving notifications for a routine or scene, a security alert, or a change in a door's open or closed status. The IOT app must not allow the following while driving: <!-- --> - **Tasks related to app setup of any kind**. For example: the ability to select devices, systems, or locations for use with the IOT app. - **Tasks related to creation, modification, or reordering**. For example: the ability to create, modify, or reorder a scene or routine, such as a sequence of events when departing or leaving a location, including opening and closing a garage door, or turning lights on and off. - **Tasks related to fine-grained device control**. For example: the ability to control certain functions, including adjusting thermostat temperatures or the level of lighting luminescence. |
| Weather Functionality | `WE-1` | Weather | App must include weather related content, which must be relevant to the user's current location or a user specified location. |
| Weather Functionality | `WE-2` | Weather | Weather information on map tiles must be readable and may not include complex legends. Apps may include a maximum of three legends. Apps with multiple legends may have a maximum of three colors, whereas single legend apps may have more than three colors. |
| Weather Functionality | `WE-3` | Weather | Forecast information must include easily readable icons and symbols. |
| Weather Functionality | `WE-4` | Weather | Customization of forecast intervals must not be made possible using templates. |
| Weather Functionality | `WE-5` | Weather | Weather apps must not show more than five unique weather map annotations in a given view (for example: Temperature markers, wind speed markers, humidity, radar overlay, lightning indicators, road conditions all in the same view). |
| Map Rendering | `MR-1` | Navigation, POI, Weather | By default, apps that [draw maps](https://developer.android.com/training/cars/apps#draw-maps) must draw a light-themed or dark-themed map when instructed to do so. For more information, see [Support dark theme](https://developer.android.com/training/cars/apps#dark-theme). Apps can let users choose to always display the app in either light or dark theme. |
| Media Controls | `MC-1` | Video | The app integrates with media session. Depending on the content, the app must support either the play/pause or stop playback commands. Additionally, the app must provide title and thumbnail metadata for every media item. See [Control and advertise playback using a MediaSession](https://developer.android.com/media/media3/session/control-playback). |
| Deep Links | `DL-1` | Video, Games, Browsers | **Android Automotive OS only:** The app supports deep links. See [Handling Android App Links](https://developer.android.com/training/app-links). |
| Media Functionality (templated) | `MFT-1` | Media (templated) | User must be able to navigate to the `MediaPlaybackTemplate` from media browsing views or support the minimized control panel for apps using the Car App Library Version 1.9 or higher. See [Build templated media apps](https://developer.android.com/training/cars/apps/media) for more details. |
| Media Functionality (templated) | `MFT-3` | Media (templated) | Handle `SHOW_MEDIA_PLAYBACK` intents: Templated Media apps must handle the `androidx.car.app.media.action.SHOW_MEDIA_PLAYBACK` intent action to direct the user to the `MediaPlaybackTemplate`. See [handle system media playback intents](https://developer.android.com/training/cars/apps/media#sys-media-playback-intents) for more details. |

#### Associated large screen quality guidelines

While these guidelines are relevant for all activities implemented by your app,
they are Tier 2 requirements only for [parked apps](https://developer.android.com/training/cars/parked).

| Criteria | ID | Guidance for cars |
|---|---|---|
| Multi-window and multi-resume | [`LS-M2`](https://developer.android.com/docs/quality-guidelines/large-screen-app-quality#LS-M2) | When an activity is on a [distant display](https://developer.android.com/training/cars/testing/emulator/distant-display), it loses the top resumed activity position when the user interacts with an activity on the main display. |
| UX | [`LS-U1`](https://developer.android.com/docs/quality-guidelines/large-screen-app-quality#LS-U1) | Some cars have landscape screens with much greater aspect ratios than are common on other large screen devices. You can use the [Automotive Ultrawide](https://developer.android.com/training/cars/testing/emulator#bundled-profiles) hardware profile to test on such a display. |
| UX | [`LS-U2`](https://developer.android.com/docs/quality-guidelines/large-screen-app-quality#LS-U2) | Same as for LS-U1 |
| UX | [`LS-U3`](https://developer.android.com/docs/quality-guidelines/large-screen-app-quality#LS-U3) | To qualify as a car optimized app, your app must meet the LS-U3 requirement for touch target sizes. To qualify as car differentiated, it must meet the car [`UX-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#UX-1) requirement. |

### Car differentiated

| Criteria | ID | Applicable Categories | Description |
|---|---|---|---|
| Navigation Functionality | `NF-9` | Navigation | The app supports rendering on cluster displays, and only renders map tiles on these displays. See [Drawing to the cluster display](https://developer.android.com/training/cars/apps/navigation#drawing_to_the_cluster_display). **Warning:** Navigation apps that also support weather functionality must not include any weather-related information on the cluster display. |
| Templated Messaging Functionality | `TMF-1` | Messaging (templated) | The app displays conversations as described in [Display conversations](https://developer.android.com/training/cars/communication/templated-messaging#display-conversations): - The self property of each conversation is set accurately. - Users can send a message using the button provided for each conversation. - Conversations are refreshed to include new messages as they are sent and received. |
| Calling Functionality | `CF-1` | Calling | The app can be used to initiate, accept, decline, and leave calls. See [Build calling experiences for Android Auto](https://developer.android.com/training/cars/communication/calling#integrate-telecom). |
| Calling Functionality | `CF-2` | Calling | During a call, the app provides the following: - A call name and icon. - For meetings/group calls, the number of participants and the actively speaking participant. - The ability for a user to mute themselves. |
| Calling Functionality | `CF-3` | Calling | Calls initiated while Android Auto is not connected continue when a user connects to Android Auto and are displayed on Android Auto. |
| Deep Links | `DL-2` | Media, Navigation, POI, IOT, Weather | **Android Automotive OS only:** The app supports deep links: - For media apps, see [Add support for Android Automotive OS to your media app](https://developer.android.com/training/cars/media/automotive-os#support-deep-links). - For templated apps, see [Add support for Android Automotive OS to your templated app](https://developer.android.com/training/cars/apps/automotive-os#support-deep-links). |
| Driver distraction | `DD-4` | Video | The app supports audio while driving on capable devices. See [Support audio while driving](https://developer.android.com/training/cars/parked/video#audio-while-driving) for additional details. **Important:** The app must meet [`DD-2`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DD-2) on devices that don't support audio while driving. |
| App Rendering | `AR-2` | Video, Games, Browsers | The app renders into display cutouts to fully make use of the screen space available while maintaining the ability for users to continue interacting with all UI elements on the screen. See [Adapt to irregularly shaped displays](https://developer.android.com/training/cars/parked/automotive-os#irregular-displays) for more details. |
| Media Functionality (templated) | `MFT-2` | Media (templated) | App provides a differentiated experience by using templates for scenarios beyond basic browsing and playback. For example, by providing expanded playback controls or by allowing users to adjust driving relevant app settings. **Tip:** Try creating a car version of a feature that's unique to your mobile app. See [Build templated media apps](https://developer.android.com/training/cars/apps/media) for more details. |
| User Experience | `UX-1` | Video, Games, Browsers | Touch targets are at least 64dp. |
| User Experience | `UX-2` | Video, Games, Browsers | Touch targets are at least 24dp apart from each other and 24dp away from screen edges. |
| User Experience | `UX-3` | Video, Games, Browsers | Font sizes should be at least 24sp. |

#### Associated large screen quality guidelines

While these guidelines are relevant for all activities implemented by your app,
they are Tier 1 requirements only for [parked apps](https://developer.android.com/training/cars/parked).

| Criteria | ID | Guidance for cars |
|---|---|---|
| UX | [`LS-U4`](https://developer.android.com/docs/quality-guidelines/large-screen-app-quality#LS-U4) | Although keyboards are supported by Android Automotive OS, they're less common input methods in cars in comparison to other large screen devices. However, some cars have rotary input devices that rely on the same APIs as tab navigation with a keyboard, so they also require LS-U4 to be met to function properly. |
| Keyboard, mouse, and trackpad | [`LS-I3`](https://developer.android.com/docs/quality-guidelines/large-screen-app-quality#LS-I3) | Same as for LS-U4 |

## Frequently asked questions

**What happens after I submit my app?**

Apps for cars are subject to an additional manual review beyond normal Play
Store review processes. Your app is tested to ensure compliance against the
applicable criteria.

**After submitting my app, how will I find out if my app does not meet all the
requirements for Android Auto or Android Automotive OS?**

If your app does not meet the app quality requirements described on this page,
the Play Store team contacts you through the email address specified in the
[Google Play Console](https://play.google.com/console/) account associated with the app.

> [!NOTE]
> **Note:** For information about how to publish your app in Google Play, see [Distribute to cars](https://developer.android.com/training/cars/distribute).

**How do I manage policy violations and appeals?**

You can learn more about
[managing policy violations and appeals](https://support.google.com/googleplay/android-developer/answer/9899142)
in the Google Play policy center.

**My app targets more than just Android Auto or Android Automotive OS. If my app
does not meet the car requirements, will my new or updated app still appear on
Google Play for other devices?**

No. When Google begins the approval process, your app undergoes an app quality
review. Any subsequent updates are not available for distribution until the app
is approved. If you need to make updates to your app for other devices, consider
creating a separate release from the updates to your car app.

> [!CAUTION]
> **Caution:** Due to this restriction, you shouldn't use your production APK for Android Auto support prototyping.

## Change notes

### September 2026

- The Games parked app category has graduated from beta to general availability. Developers can now publish games to all track types on Google Play. See [Supported app categories](https://developer.android.com/training/cars#supported-app-categories) for more information.

### August 2026

- Expected Performance

  - Categories affected: Media, Messaging (templated), Calling, Navigation, POI, IOT, or Weather
  - New criterion:
    [`EP-4`](https://developer.android.com/docs/quality-guidelines/car-app-quality#EP-4)


    **Android Automotive OS only:** The app listing on
    Google Play Store must include screenshots for the Android
    Automotive OS form factor. See
    [Provide Android
    Automotive OS screenshots](https://developer.android.com/training/cars/distribute#opt-in-aaos).


    Note: For apps in the parked app categories, screenshots are
    optional. However, if your app includes features unique to cars,
    or the app differs significantly from the experience on mobile or
    tablet, it is recommended to include screenshots in your Google
    Play Store listing. See
    [Provide Android
    Automotive OS screenshots](https://developer.android.com/training/cars/distribute#opt-in-aaos).

### May 2026

- Voice Commands

  - Categories affected: Media, Media (templated), Navigation
  - Updated criterion: [`VC-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#VC-1)
  - Previous text:

    The app must support Google Assistant voice commands. For more
    information, see [Support voice actions](https://developer.android.com/training/cars/media/voice-actions) for
    media, and [Support navigation intents](https://developer.android.com/training/cars/apps/navigation#support-navigation-intents) for
    navigation.
  - New text:

    The app must support Gemini and Google Assistant voice commands. For
    more information, see [Support voice actions](https://developer.android.com/training/cars/media/voice-actions)
    for media, and [Support navigation intents](https://developer.android.com/training/cars/apps/navigation#support-navigation-intents) for
    navigation.
- Theming

  - Categories affected: Media (templated), Messaging (templated), Navigation, POI, IOT, Weather
  - New criterion:
    [`TH-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#TH-1)

    Apps that add custom theming to Car App Library components with version
    1.9 or higher must support light and dark theme. See
    [Contrast](https://developer.android.com/develop/ui/compose/styles/theming) for more information.
- Media Functionality (templated)

  - Categories affected: Media (templated)
  - Updated criterion: [`MFT-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#MFT-1)
  - Previous text:

    User must be able to navigate to the `MediaPlaybackTemplate` from media
    browsing views. See [Build templated media
    apps](https://developer.android.com/training/cars/apps/media) for more details.
  - New text:

    User must be able to navigate to the `MediaPlaybackTemplate` from media
    browsing views or support the minimized control panel for apps using the
    Car App Library Version 1.9 or higher. See [Build templated media
    apps](https://developer.android.com/training/cars/apps/media) for more details.
  - Categories affected: Media (templated)

  - New criterion:
    [`MFT-3`](https://developer.android.com/docs/quality-guidelines/car-app-quality#MFT-3)

    Handle `SHOW_MEDIA_PLAYBACK` intents: Templated Media apps must
    handle the `androidx.car.app.media.action.SHOW_MEDIA_PLAYBACK` intent
    action to direct the user to the `MediaPlaybackTemplate`. See [handle
    system media playback intents](https://developer.android.com/training/cars/apps/media#sys-media-playback-intents) for more
    details.

### May 2025

- [App Categories](https://developer.android.com/docs/quality-guidelines/car-app-quality#app-categories): "Communication --- messaging notifications" replaces the "Messaging" category. "Communication --- templated messaging" and "Communication --- calling" have been added, including the related "Templated Messaging Functionality" and "Calling Functionality" criteria.
- Expected Performance

  - Categories affected: Games
  - New criterion:
    [`EP-3`](https://developer.android.com/docs/quality-guidelines/car-app-quality#EP-3)

    The app is responsive to input and does not freeze or stutter during
    gameplay.
- Display Orientation

  - Categories affected: Games, Video, Browser
  - Updated criterion: [`DO-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DO-1)
  - Previous text:

    The app can be distributed to devices with fixed screen orientations.
    See [Required Android Automotive OS features](https://developer.android.com/training/cars/parked#required-features).
  - New text:

    Android Automotive OS only: The app can be distributed to devices
    with fixed screen orientations. See [Required Android Automotive OS
    features](https://developer.android.com/training/cars/parked/automotive-os#required-features).
  - Categories affected: Games

  - New criterion:
    [`DO-2`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DO-2)

    Android Auto only: The app is not significantly pillarboxed when
    running on landscape displays. See [Add support for Android Auto to your
    parked app](https://developer.android.com/training/cars/parked/auto).
- Deep Links

  - Categories affected: Video, Games, Browsers
  - New criterion:
    [`DL-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DL-1)

    Android Automotive OS only: The app supports deep links. See
    [Handling Android App Links](https://developer.android.com/training/app-links).
  - Categories affected: Media, Navigation, POI, IOT, Weather

  - New criterion:
    [`DL-2`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DL-2)

    Android Automotive OS only: The app supports deep links:
    - For media apps, see [Add support for Android Automotive OS to your
      media app](https://developer.android.com/training/cars/media/automotive-os#support-deep-links).
    - For templated apps, see [Add support for Android Automotive OS to
      your templated app](https://developer.android.com/training/cars/apps/automotive-os#support-deep-links).
- CPU Architectures

  - Categories affected: Video, Games, Browsers
  - Removed criterion: `CP-1`

    The app must support both x86_64 and ARM CPUs.
- Driver Distraction

  - Categories affected: Video
  - New criterion:
    [`DD-4`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DD-4)

    The app supports audio while driving on capable devices. See [Support
    audio while driving](https://developer.android.com/training/cars/parked/video#audio-while-driving) for additional details.

    > [!IMPORTANT]
    > **Important:** The app must meet [`DD-2`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DD-2) on devices that don't support audio while driving.

### December 2024

- [App Categories](https://developer.android.com/docs/quality-guidelines/car-app-quality#app-categories): "Weather" has been added, including the introduction of the Weather Functionality criteria.
- Map Rendering

  - Categories affected: Navigation, POI, Weather
  - New criterion:
    [`MR-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#MR-1)

    By default, apps that [draw maps](https://developer.android.com/training/cars/apps#draw-maps) must draw a
    light-themed or dark-themed map when instructed to do so. For more
    information, see [Support dark theme](https://developer.android.com/training/cars#dark-theme).

    Apps can let users choose to always display the app in either light or
    dark theme.
- Navigation Functionality

  - Removed criterion: `NF-8`
  - Categories affected: Navigation Replacement of criterion NF-8 by the [`MR-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#MR-1) criterion.
  - Updated criterion: [`NF-9`](https://developer.android.com/docs/quality-guidelines/car-app-quality#NF-9)
  - Categories affected: Navigation
  - Previous text:

    The app supports rendering on cluster displays, and only renders map
    tiles on these displays. See
    [Drawing to the cluster display](https://developer.android.com/training/cars/apps/navigation#drawing-cluster-display).
  - New text:

    The app supports rendering on cluster displays, and only renders map
    tiles on these displays. See
    [Drawing to the cluster display](https://developer.android.com/training/cars/apps/navigation#drawing-cluster-display).

    Navigation apps that also support weather functionality must not include
    any weather-related information on the cluster display.
- Screen Animation

  - Updated criterion: [`SA-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#SA-1)
  - Categories affected: Media, Messaging, Navigation, Point of Interest, Weather
  - Previous text:

    The app must not display animated elements on the screen, such as
    animated graphics or video.
  - New text:

    The app must not display animated elements on the screen, such as
    animated graphics or video.

    Exception: Canvas animations while the user is parked are allowed if
    they are relevant to the driving task.

### May 2024

- Introduction of the [Car quality tiers](https://developer.android.com/docs/quality-guidelines/car-app-quality#car-quality-tiers)
- Driver Distraction

  - Categories affected: Browsers, Games, Video
  - New criterion:
    [`DD-3`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DD-3)

    The app must not be launchable or usable while driving and must not play
    any audio. See

    [Ensure there are no distraction optimized
    activities](https://developer.android.com/training/cars/parked#ensure_there_are_no_distraction-optimized_activities).

    > [!NOTE]
    > **Note:** If your app plays audio, it must meet the [`DD-2`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DD-2) requirement.

- App Rendering

  - Categories affected: All categories supported by Android Automotive OS
  - New criterion:
    [`AR-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#AR-1)

    In activities implemented by the app, interactive UI elements must not
    be obstructed by system bars or display cutouts. See [Work with window
    insets and display cutouts](https://developer.android.com/training/cars/parked/automotive-os#insets-and-cutouts).
  - New criterion:
    [`AR-2`](https://developer.android.com/docs/quality-guidelines/car-app-quality#AR-2)

    The app renders into display cutouts to fully make use of the screen
    space available while ensuring interactive UI elements remain
    accessible. See
    [Adapt to irregularly shaped displays](https://developer.android.com/training/cars/parked/automotive-os#irregular-displays) for
    more details.
- Media Controls

  - Categories affected: Video
  - New criterion:
    [`MC-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#MC-1)

    The app integrates with media session. Depending on the content, the app
    must support either the play/pause or stop playback commands.
    Additionally, the app must provide title and thumbnail metadata for
    every media item. See [Control and advertise playback using a
    MediaSession](https://developer.android.com/media/media3/session/control-playback).
- UX

  - Categories affected: Browsers, Games, Video
  - New criterion:
    [`UX-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#UX-1)

    Touch targets are at least 64dp.
  - New criterion:
    [`UX-2`](https://developer.android.com/docs/quality-guidelines/car-app-quality#UX-2)

    Touch targets are at least 24dp apart from each other and 24dp away from
    screen edges.
  - New criterion:
    [`UX-3`](https://developer.android.com/docs/quality-guidelines/car-app-quality#UX-3)

    Font sizes should be at least 24sp.
- Permitted Experiences

  - Categories affected: Media, Navigation, POI, IOT
  - New criterion:
    [`PE-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#PE-1)

    Android Automotive OS only: with the exception of providing setup,
    settings, and sign-in flows while parked, the app must not provide any
    functionality through its own activities.
- Navigation functionality

  - Categories affected: Navigation
  - New criterion:
    [`NF-9`](https://developer.android.com/docs/quality-guidelines/car-app-quality#NF-9)

    The app supports rendering on cluster displays, and only renders map
    tiles on these displays. See
    [Drawing to the cluster display](https://developer.android.com/training/cars/apps/navigation#drawing-cluster-display).
- Delayed Response

  - Categories affected: Video
  - Removal of criteria [`DR-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DR-1) , [`DR-2`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DR-2) , and [`DR-3`](https://developer.android.com/docs/quality-guidelines/car-app-quality#DR-3) for the video category

### July 2023

- [App Categories](https://developer.android.com/docs/quality-guidelines/car-app-quality#app-categories): "Browsers" has been added
- Sensitive Data

  - Categories affected: Browsers
  - New criterion:
    [`SD-1`](https://developer.android.com/docs/quality-guidelines/car-app-quality#SD-1)

    Browsers must not save or allow access to passwords or payment
    information unless the user can block access to passwords using a
    profile lock. [Learn how](https://developer.android.com/training/cars/parked/browser#block-sensitive).
  - New criterion:
    [`SD-2`](https://developer.android.com/docs/quality-guidelines/car-app-quality#SD-2)

    Before syncing data to the car, browsers that synchronize passwords or
    payments data must do the following steps:
    1. Prompt the user to authenticate.
    2. Notify the user on the car screen their data will be synchronized to the car.

### April 2023

- [App Categories](https://developer.android.com/docs/quality-guidelines/car-app-quality#app-categories): "Games" has been added
- Irrelevant Notifications

  - Categories affected: Games, Video
  - New criterion:
    [`IN-2`](https://developer.android.com/docs/quality-guidelines/car-app-quality#IN-2)

    The app must not provide any heads up notifications.
- Settings Flow

  - Categories affected: Video
  - Removed criterion: `SF-1`

    The app must proceed to the home page after sign-in or attempted
    sign-in.
- Automotive Functionality

  - Categories affected: Video
  - Removed criterion: `AF-1`

    The app must not contain any distraction-optimized activities.

### March 2023

- Grammar and formatting changes.

### December 2022

- Internet of Things Functionality

  - Categories affected: Internet of Things
  - New criteria:

    The IOT app may allow the following while driving:
    - View the current state of devices. For example: to view if a garage door is open or closed, a light bulb is on or off, a security system is armed or disarmed, or a washer is running or completed.
    - Simple, one-touch features that control on and off functions. For example: the ability to turn various devices on and off or open and close them, including turning lights on and off, turning a thermostat on and off, or opening and closing a garage door or curtain. This also includes turning a pre-programmed scene or routine on and off.
    - Notify users about an event in the home or another location. For example: receiving notifications for a routine or scene, a security alert, or a change in a door's open or closed status.

    The IOT app must not allow the following while driving:
    - Tasks related to app setup of any kind. For example: the ability to select devices, systems, or locations for use with the IOT app.
    - Tasks related to creation, modification, or reordering. For example: the ability to create, modify, or reorder a scene or routine, such as a sequence of events when departing or leaving a location, including opening and closing a garage door, or turning lights on and off.
    - Tasks related to fine-grained device control. For example: the ability to control certain functions, including adjusting thermostat temperatures or the level of lighting luminescence.

### May 2022

- [App Categories](https://developer.android.com/docs/quality-guidelines/car-app-quality#app-categories): "Parking \& Charging" has been replaced with "Point of Interest"
- Screen Animation

  - Categories affected: Media, Messaging, Navigation, and Point of Interest
  - Previous text:

    The app does not display animated elements on the screen such as
    animated graphics, video, or progress bars.
  - New text:

    The app does not display animated elements on the screen such as
    animated graphics or video.
- Image Usage

  - Categories affected: Media, Messaging, Navigation, and Point of Interest
  - Previous text:

    App elements don't display any images on the screen. Exceptions
    include:
    - Apps may display a single static image for content context in the background of the consumption screen, such as album art or the advertiser's corporate logo.
    - Apps may display icons in the content navigation drawer.
    - Navigation, parking, and charging apps may display images and photographs to aid in driving decision-making.
    - Navigation apps may display an image for lane guidance or junction guidance.
  - New text:

    App elements don't display any images on the screen. Exceptions
    include:
    - Apps may display a single static image for content context in the background of the consumption screen, such as album art or the advertiser's corporate logo.
    - Apps may display icons in the content navigation drawer.
    - Apps may display images and photographs to aid in driving decision-making.
    - Navigation apps may display an image for lane guidance or junction guidance.
- Since cars are large screen devices, apps should fill the categories

  - Categories affected: Media, Messaging, Navigation, Point of Interest, and Video
  - Previous text:

    The app does not include games or other features outside the app types
    intended for cars. For more information, see [Supported app
    categories](https://developer.android.com/training/cars#supported-app-categories).
  - New text:

    The app does not include features outside the app types intended for
    cars. For more information, see
    [Supported app categories](https://developer.android.com/training/cars#supported-app-categories).
- Visual Information on Phone

  - Categories affected: Media, Navigation, and Point of Interest
  - Previous text:

    Android Auto only: If the user must go to the phone screen --- for
    example, to act on a permission request --- then the app must display a
    message instructing the user to only look at their phone screen when
    it's safe to do so. For more information, see
    [Handle general errors](https://developer.android.com/training/cars/media#errors) for media, and [Handle
    user input](https://developer.android.com/training/cars/apps#handle-user-input) for navigation, parking, and
    charging.
  - New text:

    Android Auto only: If the user must go to the phone screen --- for
    example, to act on a permission request --- then the app must display a
    message instructing the user to only look at their phone screen when
    it's safe to do so. For more information, see
    [Handle general errors](https://developer.android.com/training/cars/media#errors) for media, and [Handle
    user input](https://developer.android.com/training/cars/apps#handle-user-input) for navigation and point of
    interest apps.
- Navigation Functionality

  - Categories affected: Navigation
  - Previous text:

    The app draws only map content on the surface of the navigation
    templates. Text-based turn-by-turn directions, lane guidance, and
    estimated arrival time must be displayed on the relevant components of
    the Navigation template. Additional information relevant to the drive ---
    for example, speed limit and road obstructions --- can be drawn on the
    right side of the map.
  - New text:

    The app draws only map content on the surface of the navigation
    templates. Text-based turn-by-turn directions, lane guidance, and
    estimated arrival time must be displayed on the relevant components of
    the Navigation template. Additional information relevant to the drive ---
    speed limit and road obstructions --- can be drawn on the safe area of the
    map.
- Point of Interest Functionality

  - Categories affected: Point of Interest
  - Previous text:

    The app must provide meaningful functionality in its category relevant
    to driving.
  - New text:

    The app must provide meaningful functionality relevant to driving.
  - Removed criteria:

    If a parking app is displaying a list of locations next to a map --- for
    example, the Place List Map template --- then the locations in the list
    can only be parking spots.

    Similarly, for a charging app, the locations must be charging stations.

### April 2022

- Display Orientation

  - Categories affected: Video
  - New criteria:

    UIs drawn by the app support both landscape and portrait screens.
- Driver Distraction

  - Categories affected: Video
  - New criteria:

    While driving, the video app must not be launchable and the video app's
    screen must not be visible. The video app's audio must stop when the
    user starts driving.
- Settings Flow

  - Categories affected: Video
  - New criteria:

    App must proceed to the home page after (attempted) sign-in.
- Automotive Functionality

  - Categories affected: Video
  - New criteria:

    App does not contain driver optimized activities.
- CPU Architectures

  - Categories affected: Video
  - New criteria:

    App supports both x86_64 and ARM CPUs.
- App Navigability

  - Categories affected: Video
  - New criteria:

    App allows users to navigate through the app without encountering any
    dead ends.

### November 2021

- Visual Information on Phone

  - Categories affected: Media, Navigation, and Parking \& Charging
  - Previous text:

    While the app is interacting with the car screen and the car is not
    parked, the app does not activate the phone screen to present any form
    of visual information such as notifications, toasts, video, images,
    advertising, or similar. For more information, see [Build a navigation
    app](https://developer.android.com/training/cars/apps/navigation#handle-user-input) for navigation, parking, and charging.
    Similarly, while the app is running Android Auto UI on the phone screen,
    the app does not present any visual information on the phone screen that
    is unrelated to Android Auto.

    If the user must go to the phone screen --- for example, to act on a
    permission request --- then the app must display a message instructing the
    user to only look at their phone screen when it's safe to do so.
  - New text:

    Android Auto only: If the user must go to the phone screen --- for
    example, to act on a permission request --- then the app must display a
    message instructing the user to only look at their phone screen when
    it's safe to do so. For more information, see
    [Handle general errors](https://developer.android.com/training/cars/media#errors) for media, and [Handle
    user input](https://developer.android.com/training/cars/apps#handle-user-input) for navigation, parking, and
    charging.
- Payments

  - Categories affected: Navigation, Parking \& Charging
  - Previous text:

    The app must have simple flows if purchases are enabled.
  - New text:

    The app must have simple flows if purchases are enabled, using shortcuts
    such as recent or favorite purchases. For more information, see
    [Purchase using existing payment method](https://developers.google.com/cars/design/create-apps/sample-flows/purchase-with-existing-method).

    The app must not allow any of the following:
    - Setup of payment methods
    - Multiple items to be selected for purchase
    - The user to commit to recurring payments --- for example, subscriptions.