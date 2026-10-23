<br />

**OWASP category:** [MASVS-CODE: Code Quality](https://mas.owasp.org/MASVS/10-MASVS-CODE)

## Overview

Using [`PendingIntent.getCreator*()`](https://developer.android.com/reference/android/app/PendingIntent#public-methods_1) or [`PendingIntent.getTarget*()`](https://developer.android.com/reference/android/app/PendingIntent#public-methods_1) to determine whether to trust a PendingIntent's sender creates an
exploitation risk.

[`PendingIntent.getCreator*()`](https://developer.android.com/reference/android/app/PendingIntent#public-methods_1) or
[`PendingIntent.getTarget*()`](https://developer.android.com/reference/android/app/PendingIntent#public-methods_1) returns the PendingIntent's
creator, which does not always match its sender. The creator may be trusted, but
the sender should **never** be trusted, as the sender might be a malicious app
that acquired another app's PendingIntent using a variety of mechanisms, for
example:

- from [`NotificationListenerService`](https://developer.android.com/reference/android/service/notification/NotificationListenerService)
- legitimate use cases that are part of the vulnerable app.

An example of a legitimate use of [`PendingIntent.getCreator*()`](https://developer.android.com/reference/android/app/PendingIntent#public-methods_1) or [`PendingIntent.getTarget*()`](https://developer.android.com/reference/android/app/PendingIntent#public-methods_1) would be to show
the icon of the app that will be started by the PendingIntent.

## Impact

Trusting a PendingIntent's sender because you queried (and trust) the creator
can lead to vulnerabilities. If an app trusts the PendingIntent's sender based
on its creator, and then shares its authentication or authorization logic, then
whenever the PendingIntent's sender is a malicious app, this would lead to an
authentication bypass or potentially even remote code execution based on
invalidated, untrusted input, depending on the implementation of the vulnerable
application's code.

## Mitigations

### Distinguish between sender and creator

Any type of authentication or authorization logic performed when receiving a
PendingIntent must not be based on assumptions regarding the PendingIntent's
creator identified using either [`PendingIntent.getCreator*()`](https://developer.android.com/reference/android/app/PendingIntent#public-methods_1)
or [`PendingIntent.getTarget*()`](https://developer.android.com/reference/android/app/PendingIntent#public-methods_1).

### Use alternative ways to validate callers

If you need to authenticate the caller, instead of using PendingIntent, you
should use a Service or ContentProvider -- both allow fetching the caller UID
with [Binder.getCallingUid()](https://developer.android.com/reference/android/os/Binder#getCallingUid()) when you are in the context of dispatching an
incoming IPC. The UID can be queried later by using
[PackageManager.getPackagesForUid()](https://developer.android.com/reference/android/content/pm/PackageManager#getPackagesForUid(int)).

Another approach, available from API level 34, would be to use
[BroadcastReceiver.getSentFromUid()](https://developer.android.com/reference/android/content/BroadcastReceiver#getSentFromUid()) or
[BroadcastReceiver.getSentFromPackage()](https://developer.android.com/reference/android/content/BroadcastReceiver#getSentFromPackage()) if the sender opted in to sharing
identity during broadcast using [BroadcastOptions.isShareIdentityEnabled()](https://developer.android.com/reference/android/app/BroadcastOptions#isShareIdentityEnabled()).

You should always check if the calling package has the expected signature, as
sideloaded packages can have package names overlapping with ones from the Play
Store.

## Resources

- [Pending Intent](https://developer.android.com/reference/android/app/PendingIntent)