App permissions help support user privacy by protecting access to the following:

- **Restricted data**, such as system state and users' contact information
- **Restricted actions**, such as connecting to a paired device and recording audio

[Video](https://www.youtube.com/watch?v=zCAx4WZ98rs)

This page provides an overview to how Android permissions work, including a
high-level workflow for using permissions, descriptions of different types of
permissions, and some best practices for using permissions in your app. Other
pages explain how to [minimize your app's requests for
permissions](https://developer.android.com/training/permissions/evaluating), [declare
permissions](https://developer.android.com/training/permissions/declaring), [request runtime
permissions](https://developer.android.com/training/permissions/requesting), and [restrict how other apps can
interact](https://developer.android.com/training/permissions/restrict-interactions) with your app's
components.

To view a complete list of Android app permissions, visit the [permissions API
reference page](https://developer.android.com/reference/android/Manifest.permission).

To view some sample apps that demonstrate the permissions workflow, visit the
[Android permissions samples
repository](https://github.com/android/platform-samples/tree/main/samples/privacy/permissions)
on GitHub.

## Workflow for using permissions

If your app offers functionality that might require access to restricted data or
restricted actions, determine whether you can get the information or perform the
actions [without needing to declare
permissions](https://developer.android.com/training/permissions/evaluating). You can fulfill many use cases
in your app, such as taking photos, pausing media playback, and displaying
relevant ads, without needing to declare any permissions.

If you decide that your app must access restricted data or perform restricted
actions to fulfill a use case, declare the appropriate permissions. Some
permissions, known as [install-time permissions](https://developer.android.com/guide/topics/permissions/overview#install-time), are
automatically granted when your app is installed. Other permissions, known as
[runtime permissions](https://developer.android.com/guide/topics/permissions/overview#runtime), require your app to go a step further and
request the permission at runtime.

Figure 1 illustrates the workflow for using app permissions:
![](https://developer.android.com/static/images/training/permissions/workflow-overview.svg) **Figure 1.** High-level workflow for using permissions on Android.

## Types of permissions

Android categorizes permissions into different types, including install-time
permissions, runtime permissions, and special permissions. Each permission's
type indicates the scope of restricted data that your app can access, and the
scope of restricted actions that your app can perform, when the system grants
your app that permission. The protection level for each permission is based on
its type and is shown on the
[permissions API reference](https://developer.android.com/reference/android/Manifest.permission) page.

### Install-time permissions

![The left image shows a list of an app's install-time permissions. The
right image shows a pop-up dialog that contains 2 options: allow and deny.](https://developer.android.com/static/images/training/permissions/install-time.svg) **Figure 2.** The list of an app's install-time permissions, which appears in an app store.

Install-time permissions give your app limited access to restricted data or
let your app perform restricted actions that minimally affect the
system or other apps. When you declare install-time permissions in your app, an
app store presents an install-time permission notice to the user when
they view an app's details page, as shown in figure 2. The
system automatically grants your app the permissions when the user installs your
app.

Android includes several sub-types of install-time permissions, including
normal permissions and signature permissions.

#### Normal permissions

These permissions allow access to data and actions that extend beyond your app's
sandbox but present very little risk to the user's
privacy and the operation of other apps.

The system assigns the `normal` protection level to normal permissions.

#### Signature permissions

The system grants a signature permission to an app only when the app is
signed by the same certificate as the app or the OS that defines the permission.

Applications that implement privileged services, such as autofill or VPN
services, also make use of signature permissions. These apps require
service-binding signature permissions so that only the system can bind
to the services.

> [!NOTE]
> **Note:** Some signature permissions aren't for use by third-party apps.

The system assigns the `signature` protection level to signature permissions.

### Runtime permissions

![A pop-up dialog that contains 2 options: allow and deny.](https://developer.android.com/static/images/training/permissions/runtime.svg) **Figure 3.** The system permission prompt that appears when your app requests a runtime permission.

Runtime permissions, also known as dangerous permissions, give your app
additional access to restricted data or let your app perform
restricted actions that more substantially affect the system and other apps.
Therefore, you need to [request runtime
permissions](https://developer.android.com/training/permissions/requesting) in your app before you can access
the restricted data or perform restricted actions. Don't assume
that these permissions have been previously granted---check them and,
if needed, request them before each access.

When your app requests a
runtime permission, the system presents a runtime permission prompt, as shown in
figure 3.

Many runtime permissions access *private user data*, a special type of
restricted data that includes potentially sensitive information. Examples of
private user data include location and contact information.

The microphone and camera provide access to particularly sensitive information.
Therefore, the system helps you [explain why your app accesses this
information](https://developer.android.com/training/permissions/explaining-access).

The system assigns the `dangerous` protection level to runtime permissions.

### Special permissions

Special permissions correspond to particular app operations. Only the platform
and OEMs can define special permissions. Additionally, the platform and OEMs
usually define special permissions when they want to protect access to
particularly powerful actions, such as drawing over other apps.

The **Special app access** page in system settings contains a set of
user-toggleable operations. Many of these operations are implemented as special
permissions.

Learn more about how to [request special
permissions](https://developer.android.com/training/permissions/requesting-special).

The system assigns the `appop` protection level to special permissions.

### Permission groups

Permissions can belong to [permission groups](https://developer.android.com/guide/topics/manifest/permission-group-element.html).
Permission groups consist of a set of logically related permissions. For
example, permissions to send and receive SMS messages might belong to the same
group, as they both relate to the application's interaction with SMS.

Permission groups help the system minimize the number of system dialogs that are
presented to the user when an app requests closely related permissions. When a
user is presented with a prompt to grant permissions for an application,
permissions belonging to the same group are presented in the same interface.
However, permissions can change groups without notice, so don't assume that a
particular permission is grouped with any other permission.

## Best practices

App permissions build on [system security
features](https://source.android.com/security/features) and help Android support
the following goals related to user privacy:

- **Control:** The user has control over the data that they share with apps.
- **Transparency:** The user understands what data an app uses and why the app accesses this data.
- **Data minimization:** An app accesses and uses only the data that's required for a specific task or action that the user invokes.

This section presents a set of core best practices for using permissions
effectively in your app. For more details on how you can work with permissions
on Android, visit the [app permissions best
practices](https://developer.android.com/training/permissions/usage-notes) page.

### Request a minimal number of permissions

When the user requests a particular action in your app, your app should request
only the permissions that it needs to complete that action. Depending on how you
are using the permissions, there might be an [alternative way to fulfill your
app's use case](https://developer.android.com/training/permissions/evaluating) without relying on access to
sensitive information.

### Associate runtime permissions with specific actions

Request permissions as late into the flow of your app's use cases as possible.
For example, if your app lets users send audio messages to others, wait
until the user has navigated to the messaging screen and has pressed the **Send
audio message** button. After the user presses the button, your app can then
request access to the microphone.

### Consider your app's dependencies

When you include a library, you also inherit its permission requirements. Be
aware of the permissions that each dependency requires and what those
permissions are used for.

### Be transparent

When you make a permissions request, be clear about what you're accessing,
why, and what functionalities are affected if permissions are denied,
so users can make informed decisions.

### Make system accesses explicit

When you access sensitive data or hardware, such as the camera or microphone,
provide a continuous indication in your app if the system doesn't already
[provide these indicators](https://developer.android.com/training/permissions/explaining-access#indicators).
This reminder helps users understand exactly when your app accesses restricted
data or performs restricted actions.

## Permissions in system components

Permissions aren't only for requesting system functionality. Your app's system
components can restrict which other apps can interact with your app, as
described on the page about how to [restrict interactions with other
apps](https://developer.android.com/training/permissions/restrict-interactions).