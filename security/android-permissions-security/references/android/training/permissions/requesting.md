[Video](https://www.youtube.com/watch?v=x38dYUm7tCY)

Every Android app runs in a limited-access sandbox. If your app needs to use
resources or information outside of its own sandbox, you can [declare a
runtime permission](https://developer.android.com/training/permissions/declaring) and set up a permission request that provides this
access. These steps are part of the [workflow for using permissions](https://developer.android.com/training/basics/permissions#workflow).

If you declare any [dangerous permissions](https://developer.android.com/guide/topics/permissions/overview#dangerous_permissions), and if your app is installed on
a device that runs Android 6.0 (API level 23) or higher, you must request the
dangerous permissions at runtime by following the steps in this guide.

If you don't declare any dangerous permissions, or if your app is installed on
a device that runs Android 5.1 (API level 22) or lower, the permissions are
automatically granted, and you don't need to complete any of the remaining
steps on this page.

> [!NOTE]
> **Note:** Some permissions guard access to system resources that are particularly sensitive or are not directly related to user privacy. There's a [different
> process for requesting these special permissions](https://developer.android.com/training/permissions/requesting-special).

## Basic principles

The basic principles for requesting permissions at runtime are as follows:

- Ask for a permission in context, when the user starts to interact with the feature that requires it.
- Don't block the user. Always provide the option to cancel an educational UI flow, such as a flow that explains the rationale for requesting permissions.
- If the user denies or revokes a permission that a feature needs, gracefully degrade your app so that the user can continue using your app, possibly by disabling the feature that requires the permission.
- Don't assume any system behavior. For example, don't assume that permissions appear in the same *permission group*. A permission group merely helps the system minimize the number of system dialogs that are presented to the user when an app requests closely related permissions.

## Workflow for requesting permissions

Before you declare and request runtime permissions in your app, [evaluate
whether your app needs to do so](https://developer.android.com/training/permissions/evaluating). You can fulfill many use cases in your
app, such as taking photos, pausing media playback, and displaying relevant
ads, without needing to declare any permissions.

If you conclude that your app needs to declare and request runtime permissions,
complete these steps:

1. [**Declare** the permissions](https://developer.android.com/training/permissions/declaring) your app might need to request in your app's manifest file.
2. **Design** your app's user experience (UX) so that specific actions in your app are associated with specific runtime permissions. Let users know which actions might require them to grant permission for your app to access private user data.
3. [**Wait** for the user](https://developer.android.com/training/permissions/requesting#principles) to invoke the task or action in your app that requires access to specific private user data. At that time, your app can request the runtime permission that is required for accessing that data.
4. [**Check** whether the user has already granted](https://developer.android.com/training/permissions/declaring)

   runtime permission your app requires. If so, your app can access the private
   user data. If not, continue to the next step.

   You must check whether you have a permission every time you perform an
   operation that requires that permission.
5. [**Check** whether your app should show a rationale](https://developer.android.com/training/permissions/requesting#explain) to the user,
   explaining why your app needs the user to grant a particular runtime
   permission. If the system determines that your app shouldn't show a
   rationale, continue to the next step directly, without showing a UI element.

   If the system determines that your app should show a rationale, however,
   present the rationale to the user in a UI element. In this rationale,
   clearly explain what data your app is trying to access and what benefits the
   app can provide to the user if they grant the runtime permission. After the
   user acknowledges the rationale, continue to the next step.
6. [**Request** the runtime permission](https://developer.android.com/training/permissions/requesting#request-permission) your app requires
   to access the private user data. The system displays a runtime permission
   prompt, such as the one shown on the [permissions overview page](https://developer.android.com/guide/topics/permissions/overview#fig-runtime).

7. **Check** the user's response---whether they chose to grant or deny the
   runtime permission.

8. If the user granted the permission to your app, you can **access** the
   private user data. If the user denied the permission instead, [gracefully
   degrade your app experience](https://developer.android.com/training/permissions/requesting#handle-denial) so that it provides
   functionality to the user without the information that's protected by that
   permission.

Figure 1 illustrates the workflow and set of decisions associated with this
process:
![A flowchart illustrating the decision process for requesting runtime permissions, starting with an app action, checking if permission is granted, checking if rationale is needed, requesting permission, and handling the user's response.](https://developer.android.com/static/images/training/permissions/workflow-runtime.svg) **Figure 1.** Diagram that shows the workflow for declaring and requesting runtime permissions on Android.

## Determine whether your app was already granted the permission

To check whether the user already granted your app a particular permission, pass
that permission into the [`ContextCompat.checkSelfPermission()`](https://developer.android.com/reference/androidx/core/content/ContextCompat#checkSelfPermission(android.content.Context,%20java.lang.String)) method.
This method returns either [`PERMISSION_GRANTED`](https://developer.android.com/reference/android/content/pm/PackageManager#PERMISSION_GRANTED) or
[`PERMISSION_DENIED`](https://developer.android.com/reference/android/content/pm/PackageManager#PERMISSION_DENIED), depending on whether your app has the permission.

## Explain why your app needs the permission

The permissions dialog shown by the system when you call
[`requestPermissions()`](https://developer.android.com/reference/androidx/core/app/ActivityCompat#requestPermissions(android.app.Activity,%20java.lang.String%5B%5D,%20int)) says what permission your app wants, but doesn't
say why. In some cases, the user might find that puzzling. It is a good idea to
explain to the user why your app wants the permissions before you call
`requestPermissions()`.

Research shows that users are much more comfortable with permissions requests if
they know why the app needs them, such as whether the permission is needed to
support a core feature of the app or for advertising. As a result, if you are
only using a fraction of the API calls that fall under a permission group,
explicitly list which of those permissions you are using and why. For example,
if you are only using coarse location, let the user know this in your app
description or in help articles about your app.

Under certain conditions, it is also helpful to let users know about sensitive
data access in real time. For example, if you are accessing the camera or
microphone, consider letting the user know by using a notification icon
somewhere in your app, or in the notification tray (if the application is
running in the background), so it doesn't seem like you are collecting data
surreptitiously.

> [!NOTE]
> **Note:** Starting in Android 12 (API level 31), [privacy indicators](https://developer.android.com/training/permissions/explaining-access#indicators) notify the user whenever applications access the microphone or camera.

If you need to request a permission for a feature to work, but the reason is
not clear to the user, find a way to explain why you need sensitive permissions.

If the `ContextCompat.checkSelfPermission()` method returns `PERMISSION_DENIED`,
call [`shouldShowRequestPermissionRationale()`](https://developer.android.com/reference/androidx/core/app/ActivityCompat#shouldShowRequestPermissionRationale(android.app.Activity,%20java.lang.String)). If this method returns
`true`, show an educational UI to the user. In this UI, describe why
the feature that the user wants to enable needs a particular permission.

Additionally, if your app requests a permission related to location, microphone,
or camera, consider [explaining why your app needs access](https://developer.android.com/training/permissions/explaining-access) to this
information.

## Request permissions

After the user views an educational UI, or the return value of
`shouldShowRequestPermissionRationale()` indicates that you don't need to show
an educational UI, request the permission. Users see a system permission
dialog, where they can choose whether to grant a particular permission to your
app.

To do this, use the [`RequestPermission`](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.RequestPermission) contract, included in an AndroidX
library, where you [allow the system to manage the permission request code](https://developer.android.com/training/permissions/requesting#allow-system-manage-request-code)
for you. Because using the `RequestPermission` contract simplifies your logic,
it is the recommended solution when possible. However, if needed you can also
[manage a request code yourself](https://developer.android.com/training/permissions/requesting#manage-request-code-yourself) as part of the permission request and
include this request code in your permission callback logic.

### Allow the system to manage the permission request code

To allow the system to manage the request code that is associated with a
permissions request, add dependencies on the following libraries in your
module's `build.gradle` file:

- [`androidx.activity`](https://developer.android.com/jetpack/androidx/releases/activity#declaring_dependencies), version 1.2.0 or later
- [`androidx.fragment`](https://developer.android.com/jetpack/androidx/releases/fragment#declaring_dependencies), version 1.3.0 or later

You can then use one of the following classes:

- To request a single permission, use [`RequestPermission`](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.RequestPermission).
- To request multiple permissions at the same time, use [`RequestMultiplePermissions`](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.RequestMultiplePermissions).

The following steps show how to use the `RequestPermission` contract. The
process is nearly the same for the `RequestMultiplePermissions` contract.

1. In your activity or fragment's initialization logic, pass in an
   implementation of [`ActivityResultCallback`](https://developer.android.com/reference/androidx/activity/result/ActivityResultCallback) into a call to
   [`registerForActivityResult()`](https://developer.android.com/reference/androidx/activity/result/ActivityResultCaller#registerForActivityResult(androidx.activity.result.contract.ActivityResultContract%3CI,%20O%3E,%20androidx.activity.result.ActivityResultCallback%3CO%3E)). The `ActivityResultCallback` defines
   how your app handles the user's response to the permission request.

   Keep a reference to the return value of `registerForActivityResult()`, which
   is of type [`ActivityResultLauncher`](https://developer.android.com/reference/androidx/activity/result/ActivityResultLauncher).
2. To display the system permissions dialog when necessary, call the
   [`launch()`](https://developer.android.com/reference/androidx/activity/result/ActivityResultLauncher#launch(I)) method on the instance of `ActivityResultLauncher` that
   you saved in the previous step.

   After `launch()` is called, the system permissions dialog appears. When the
   user makes a choice, the system asynchronously invokes your implementation
   of `ActivityResultCallback`, which you defined in the previous step.
   **Note:** Your app *cannot* customize the dialog that appears
   when you call `launch()`. To provide more information or
   context to the user, change your app's UI so that it's easier for users to
   understand why a feature in your app needs a particular permission. For
   example, you might change the text in the button that enables the
   feature.

   Also, the text in the system permission dialog references the
   [permission
   group](https://developer.android.com/guide/topics/permissions/overview#group) associated with the permission that you requested. This
   permission grouping is designed for system ease-of-use, and your app
   shouldn't rely on permissions being within or outside of a specific
   permission group.

The following code snippet shows how to handle the permissions response:

### Kotlin

```kotlin
when {
    ContextCompat.checkSelfPermission(
            CONTEXT,
            Manifest.permission.REQUESTED_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED -> {
        // You can use the API that requires the permission.
        performAction(...)
    }
    ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.REQUESTED_PERMISSION) -> {
        // In an educational UI, explain to the user why your app requires this
        // permission for a specific feature to behave as expected, and what
        // features are disabled if it's declined. In this UI, include a
        // "cancel" or "no thanks" button that lets the user continue
        // using your app without granting the permission.
        showInContextUI(...)
    }
    else -> {
        // You can directly ask for the permission.
        requestPermissions(CONTEXT,
                arrayOf(Manifest.permission.REQUESTED_PERMISSION),
                REQUEST_CODE)
    }
}
```

### Java

```java
if (ContextCompat.checkSelfPermission(
        CONTEXT, Manifest.permission.REQUESTED_PERMISSION) ==
        PackageManager.PERMISSION_GRANTED) {
    // You can use the API that requires the permission.
    performAction(...);
} else if (ActivityCompat.shouldShowRequestPermissionRationale(
        this, Manifest.permission.REQUESTED_PERMISSION)) {
    // In an educational UI, explain to the user why your app requires this
    // permission for a specific feature to behave as expected, and what
    // features are disabled if it's declined. In this UI, include a
    // "cancel" or "no thanks" button that lets the user continue
    // using your app without granting the permission.
    showInContextUI(...);
} else {
    // You can directly ask for the permission.
    requestPermissions(CONTEXT,
            new String[] { Manifest.permission.REQUESTED_PERMISSION },
            REQUEST_CODE);
}
```

And this code snippet demonstrates the recommended process to check for
a permission and to request a permission from the user when necessary:

### Kotlin

```kotlin
when {
    ContextCompat.checkSelfPermission(
            CONTEXT,
            Manifest.permission.REQUESTED_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED -> {
        // You can use the API that requires the permission.
    }
    ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.REQUESTED_PERMISSION) -> {
        // In an educational UI, explain to the user why your app requires this
        // permission for a specific feature to behave as expected, and what
        // features are disabled if it's declined. In this UI, include a
        // "cancel" or "no thanks" button that lets the user continue
        // using your app without granting the permission.
        showInContextUI(...)
    }
    else -> {
        // You can directly ask for the permission.
        // The registered ActivityResultCallback gets the result of this request.
        requestPermissionLauncher.launch(
                Manifest.permission.REQUESTED_PERMISSION)
    }
}
```

### Java

```java
if (ContextCompat.checkSelfPermission(
        CONTEXT, Manifest.permission.REQUESTED_PERMISSION) ==
        PackageManager.PERMISSION_GRANTED) {
    // You can use the API that requires the permission.
    performAction(...);
} else if (ActivityCompat.shouldShowRequestPermissionRationale(
        this, Manifest.permission.REQUESTED_PERMISSION)) {
    // In an educational UI, explain to the user why your app requires this
    // permission for a specific feature to behave as expected, and what
    // features are disabled if it's declined. In this UI, include a
    // "cancel" or "no thanks" button that lets the user continue
    // using your app without granting the permission.
    showInContextUI(...);
} else {
    // You can directly ask for the permission.
    // The registered ActivityResultCallback gets the result of this request.
    requestPermissionLauncher.launch(
            Manifest.permission.REQUESTED_PERMISSION);
}
```

### Manage the permission request code yourself

As an alternative to [allowing the system to manage the permission request
code](https://developer.android.com/training/permissions/requesting#allow-system-manage-request-code), you can manage the permission request code yourself. To do so,
include the request code in a call to [`requestPermissions()`](https://developer.android.com/reference/androidx/core/app/ActivityCompat#requestPermissions(android.app.Activity,%20java.lang.String%5B%5D,%20int)).

> [!NOTE]
> **Note:** Your app cannot customize the dialog that appears when you call `requestPermissions()`. The text in the system permission dialog references a [permission group](https://developer.android.com/guide/topics/permissions/overview#perm-groups), but this permission grouping is designed for system ease-of-use. Your app must not rely on permissions being within or outside of a specific permission group.

The following code snippet demonstrates how to request a permission using a
request code:

### Kotlin

```kotlin
when {
    ContextCompat.checkSelfPermission(
            CONTEXT,
            Manifest.permission.REQUESTED_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED -> {
        // You can use the API that requires the permission.
        performAction(...)
    }
    ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.REQUESTED_PERMISSION) -> {
        // In an educational UI, explain to the user why your app requires this
        // permission for a specific feature to behave as expected, and what
        // features are disabled if it's declined. In this UI, include a
        // "cancel" or "no thanks" button that lets the user continue
        // using your app without granting the permission.
        showInContextUI(...)
    }
    else -> {
        // You can directly ask for the permission.
        requestPermissions(CONTEXT,
                arrayOf(Manifest.permission.REQUESTED_PERMISSION),
                REQUEST_CODE)
    }
}
```

### Java

```java
if (ContextCompat.checkSelfPermission(
        CONTEXT, Manifest.permission.REQUESTED_PERMISSION) ==
        PackageManager.PERMISSION_GRANTED) {
    // You can use the API that requires the permission.
    performAction(...);
} else if (ActivityCompat.shouldShowRequestPermissionRationale(
        this, Manifest.permission.REQUESTED_PERMISSION)) {
    // In an educational UI, explain to the user why your app requires this
    // permission for a specific feature to behave as expected, and what
    // features are disabled if it's declined. In this UI, include a
    // "cancel" or "no thanks" button that lets the user continue
    // using your app without granting the permission.
    showInContextUI(...);
} else {
    // You can directly ask for the permission.
    requestPermissions(CONTEXT,
            new String[] { Manifest.permission.REQUESTED_PERMISSION },
            REQUEST_CODE);
}
```

After the user responds to the system permissions dialog, the system then
invokes your app's implementation of [`onRequestPermissionsResult()`](https://developer.android.com/reference/androidx/core/app/ActivityCompat.OnRequestPermissionsResultCallback#onRequestPermissionsResult(int,%20java.lang.String%5B%5D,%20int%5B%5D)). The
system passes in the user response to the permission dialog, as well as the
request code that you defined, as shown in the following code snippet:

### Kotlin

```kotlin
override fun onRequestPermissionsResult(requestCode: Int,
        permissions: Array<String>, grantResults: IntArray) {
    when (requestCode) {
        PERMISSION_REQUEST_CODE -> {
            // If request is cancelled, the result arrays are empty.
            if ((grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission is granted. Continue the action or workflow
                // in your app.
            } else {
                // Explain to the user that the feature is unavailable because
                // the feature requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
            }
            return
        }

        // Add other 'when' lines to check for other
        // permissions this app might request.
        else -> {
            // Ignore all other requests.
        }
    }
}
```

### Java

```java
@Override
public void onRequestPermissionsResult(int requestCode, String[] permissions,
        int[] grantResults) {
    switch (requestCode) {
        case PERMISSION_REQUEST_CODE:
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted. Continue the action or workflow
                // in your app.
            }  else {
                // Explain to the user that the feature is unavailable because
                // the feature requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change
                // their decision.
            }
            return;
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }
}
```

### Request location permissions

When you request location permissions, follow the same best practices as
for any other [runtime permission](https://developer.android.com/training/permissions/requesting). One important difference when it comes
to location permissions is that the system includes multiple permissions related
to location. Which permissions you request, and how you request them, depend on
the location requirements for your app's use case.

#### Foreground location

If your app contains a feature that shares or receives location information
only once, or for a defined amount of time, then that feature requires
foreground location access. Some examples include the following:

- Within a navigation app, a feature lets users get turn-by-turn directions.
- Within a messaging app, a feature lets users share their current location with another user.

The system considers your app to be using foreground location if a feature of
your app accesses the device's current location in one of the following
situations:

- An activity that belongs to your app is visible.
- Your app is running a foreground service. When a foreground service is
  running, the system raises user awareness by showing a persistent
  notification. Your app retains access when it is placed in the background,
  such as when the user presses the **Home** button on their device or turns
  their device's display off.

  On Android 10 (API level 29) and higher, you must declare a [foreground
  service type](https://developer.android.com/guide/topics/manifest/service-element#foregroundservicetype) of `location`, as shown in the following code snippet. On
  earlier versions of Android, we recommend that you declare this foreground
  service type.

  ```xml
  <!-- Recommended for Android 9 (API level 28) and lower. -->
  <!-- Required for Android 10 (API level 29) and higher. -->
  <service
      android:name="MyNavigationService"
      android:foregroundServiceType="location" ... >
      <!-- Any inner elements go here. -->
  </service>
  ```

You declare a need for foreground location when your app requests either the
[`ACCESS_COARSE_LOCATION`](https://developer.android.com/reference/android/Manifest.permission#ACCESS_COARSE_LOCATION) permission or the
[`ACCESS_FINE_LOCATION`](https://developer.android.com/reference/android/Manifest.permission#ACCESS_FINE_LOCATION) permission, as shown in the following snippet:

```xml
<manifest ... >
  <!-- Include this permission any time your app needs location information. -->
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

  <!-- Include only if your app benefits from precise location access. -->
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
</manifest>
```

#### Background location

An app requires background location access if a feature within the app
constantly shares location with other users or uses the [Geofencing API](https://developer.android.com/training/location/geofencing).
Several examples include the following:

- Within a family location sharing app, a feature lets users continuously share location with family members.
- Within an IoT app, a feature lets users configure their home devices such that they turn off when the user leaves their home and turn back on when the user returns home.

The system considers your app to be using background location if it accesses
the device's current location in any situation other than the ones described in
the [foreground location](https://developer.android.com/training/permissions/requesting#foreground-location) section. The background
location accuracy is the same as the [foreground location accuracy](https://developer.android.com/training/location/permissions#accuracy), which
depends on the location permissions that your app declares.

On Android 10 (API level 29) and higher, you must declare the
[`ACCESS_BACKGROUND_LOCATION`](https://developer.android.com/reference/android/Manifest.permission#ACCESS_BACKGROUND_LOCATION) permission in your app's manifest to request
background location access at runtime. On earlier versions of Android, when your
app receives foreground location access, it automatically receives background
location access as well.

    <manifest ... >
      <!-- Required only when requesting background location access on
           Android 10 (API level 29) and higher. -->
      <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
    </manifest>

> [!NOTE]
> **Note:** The Google Play Store has a [location policy](https://support.google.com/googleplay/android-developer/answer/9799150) concerning device location, restricting background location access to apps that need it for their core functionality and meet related policy requirements.

## Handle permission denial

If the user denies a permission request, your app must help them understand the implications of denying the permission. In particular, your app must make users aware of the features that don't work because of the missing permission. When you do so, keep the following best practices in mind:

- **Minimize functionality loss.** Users should be able to access the app to whatever extent is possible without the requested permissions. If functionality works at least to some extent with the data available, even if it is degraded compared to what would have been possible with the permission, keeping it on is the best option.
- **Guide the user's attention.** Highlight a specific part of your app's UI where there is limited functionality because your app doesn't have the necessary permission. Examples of what you can do include the following:

  - Show a message where the feature's results or data would have appeared.
  - Display a different button that contains an error icon and color.
- **Be specific.** Don't display a generic message. Instead, make clear which features are unavailable because your app doesn't have the necessary permission.

- **Don't block the user interface.** In other words, don't display a full-screen warning message that prevents users from continuing to use your app at all.

- **Accept the user's preference:** While it is okay to communicate to users how their functionality is being impacted by lack of access to the requested permissions, they should not feel pressured to change their mind. Letting a user know that they aren't getting full feature access when it is appropriate (such as when attempting to use a part of the app blocked without the permission) is important, but persistent nagging to reconsider is not respectful of their choice.

Following these guidelines can help ensure that users feel empowered to manage access to their private data in a way that aligns with their values. Failing to gracefully accept highly visible user privacy decisions damages the trust and reputation not only of your app, but of the ecosystem as a whole.

> [!TIP]
> **Tip:** We recommend that your app encourages the best user experience possible, even after permission denials. For example, if microphone access is denied, you must still promote full usability of text functionality.

Additionally, if the user taps Deny for a specific permission more than once during your app's lifetime of installation on a device, the user will no longer see the system permissions dialog if your app requests that permission again. The user's action implies "don't ask again," and is considered a permanent denial. It is very important to only prompt users for permissions when they need access to a specific feature; otherwise, you might inadvertently lose the ability to re-request permissions.
In certain situations, the permission might be denied automatically, without the user taking any action. (A permission might be granted automatically as well.) It is important to not assume anything about automatic behavior. Each time your app needs to access functionality that requires a permission, check that your app is still granted that permission.
To provide the best user experience when asking for app permissions, also see [App permissions best practices](https://developer.android.com/training/permissions/usage-notes).

### Inspect denial status when testing and debugging

To identify whether an app has been permanently denied permissions (for
debugging and testing purposes), use the following command:

```
adb shell dumpsys package PACKAGE_NAME
```

Where <var translate="no">PACKAGE_NAME</var> is the name of the package to inspect.

The output of the command contains sections that look like this:

```
...
runtime permissions:
  android.permission.POST_NOTIFICATIONS: granted=false, flags=[ USER_SENSITIVE_WHEN_GRANTED|USER_SENSITIVE_WHEN_DENIED]
  android.permission.ACCESS_FINE_LOCATION: granted=false, flags=[ USER_SET|USER_FIXED|USER_SENSITIVE_WHEN_GRANTED|USER_SENSITIVE_WHEN_DENIED]
  android.permission.BLUETOOTH_CONNECT: granted=false, flags=[ USER_SENSITIVE_WHEN_GRANTED|USER_SENSITIVE_WHEN_DENIED]
...
```

Permissions that have been denied once by the user are flagged by `USER_SET`.
Permissions that have been denied permanently by selecting **Deny** twice are
flagged by `USER_FIXED`.

To make sure that testers see the request dialog during testing, reset these
flags when you're done debugging your app. To do this, use the command:

```
adb shell pm clear-permission-flags PACKAGE_NAME PERMISSION_NAME user-set user-fixed
```

<var translate="no">PERMISSION_NAME</var> is the name of the permission you want to
reset.

`PERMISSION_NAME` is the name of the permission you want to reset.

To view a complete list of Android app permissions, visit the [permissions API
reference page](https://developer.android.com/reference/android/Manifest.permission#constants_1).

## One-time permissions

![The option called 'Only this time' is the second of three buttons in
the dialog.](https://developer.android.com/static/images/training/permissions/one-time-prompt.svg) **Figure 2.** System dialog that appears when an app requests a one-time permission.

Starting in Android 11 (API level 30), whenever your app requests a permission
related to location, microphone, or camera, the user-facing permissions dialog
contains an option called **Only this time** , as shown in figure 2. If the user
selects this option in the dialog, your app is granted a temporary *one-time
permission*.

Your app can then access the related data for a period of time that depends on
your app's behavior and the user's actions:

- While your app's activity is visible, your app can access the data.
- If the user sends your app to the background, your app can continue to access the data for a short period of time.
- If you launch a foreground service while the activity is visible, and the user then moves your app to the background, your app can continue to access the data until the foreground service stops.

### App process terminates when permission revoked

If the user revokes the one-time permission, such as in system settings, your
app cannot access the data, regardless of whether you launched a foreground
service. As with any permission, if the user revokes your app's one-time
permission, your app's process terminates.

When the user next opens your app and a feature in your app requests access to
location, microphone, or camera, the user is prompted for the permission again.

> [!NOTE]
> **Note:** If your app already follows [best practices](https://developer.android.com/training/permissions/usage-notes) when it requests runtime permissions, you don't need to add or change any logic in your app to support one-time permissions.

## Reset unused permissions

Android provides several ways to reset unused runtime permissions to their
default, denied state:

- An API where you can proactively [remove your app's access](https://developer.android.com/training/permissions/requesting#remove-app-access) to an unused runtime permission.
- A system mechanism that automatically [resets the permissions of unused
  apps](https://developer.android.com/training/permissions/requesting#auto-reset-permissions-unused-apps).

### Remove app access

On Android 13 (API level 33) and higher, you can remove your app's access to
runtime permissions that your app no longer requires. When you update your app,
perform this step so that users are more likely to understand why your app
continues to request specific permissions. This knowledge helps build user
trust in your app.

To remove access to a runtime permission, pass the name of that permission into
[`revokeSelfPermissionOnKill()`](https://developer.android.com/reference/android/content/Context#revokeSelfPermissionOnKill(java.lang.String)). To remove access to a group of runtime
permissions at the same time, pass a collection of permission names into
[`revokeSelfPermissionsOnKill()`](https://developer.android.com/reference/android/content/Context#revokeSelfPermissionsOnKill(java.util.Collection%3Cjava.lang.String%3E)). The permission removal process happens
asynchronously and kills all processes associated with your app's UID.

> [!NOTE]
> **Note:** For system settings to show that your app doesn't access data in a particular [permission group](https://developer.android.com/reference/android/Manifest.permission_group), you must remove access to **all** permissions in that permission group. In this case, it can be helpful to call `revokeSelfPermissionsOnKill()` and pass in multiple permissions within the permission group.

For the system to remove your app's access to the permissions, all processes
tied to your app must be killed. When you call the API, the system determines
when it is safe to kill these processes. Usually, the system waits until your
app spends an extended period of time running in the background instead of the
foreground.

To inform the user that your app no longer requires access to specific runtime
permissions, show a dialog the next time the user launches your app. This
dialog can include the list of permissions.

### Auto-reset permissions of unused apps

If your app targets Android 11 (API level 30) or higher and is not used for a
few months, the system protects user data by automatically resetting the
sensitive runtime permissions that the user had granted your app. Learn more in
the guide about [app hibernation](https://developer.android.com/topic/performance/app-hibernation).

## Request to become the default handler if necessary

Some apps depend on access to sensitive user information related to call logs
and SMS messages. If you want to request the permissions specific to call logs
and SMS messages and publish your app to the Play Store, you must prompt the
user to set your app as the *default handler* for a core system function before
requesting these runtime permissions.

For more information on default handlers, including guidance on showing a
default handler prompt to users, [see the guide about permissions used only in
default handlers](https://developer.android.com/guide/topics/permissions/default-handlers).

## Grant all runtime permissions for testing purposes

To grant all runtime permissions automatically when you install an app on an
emulator or test device, use the `-g` option for the `adb shell install`
command, as demonstrated in the following code snippet:

    adb shell install -g PATH_TO_APK_FILE

## Additional resources

For additional information about permissions, read these articles:

- [Permissions overview](https://developer.android.com/guide/topics/permissions/overview)
- [App permissions best practices](https://developer.android.com/training/permissions/usage-notes)

To learn more about requesting permissions, review the [permissions samples](https://github.com/android/platform-samples/tree/main/samples/privacy/permissions).

You can also complete this [codelab that demonstrates privacy best
practices](https://developer.android.com/codelabs/android-privacy-codelab).