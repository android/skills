The [Restore Credentials](https://developer.android.com/identity/sign-in/restore-credentials) feature within Credential Manager ensures a
frictionless transition for users moving to a new device. While testing the
feature, you can use either of the following approaches:

- **Separate devices**: Use a source device to perform the backup and a separate target device to perform the restore, or use two separate Android Studio emulators.
- **Single device**: Use one physical device or an Android Studio emulator. After the app backs up its data, uninstall and reinstall the app, and then perform the restore.

This guide describes how you can use Android Studio to test Restore Credentials
for a debuggable app on an emulator, simulating both situations of separate
devices or a single device.

## Prerequisites

To use Android Studio's Backup and Restore feature to test Restore Credentials,
verify the following prerequisites:

- [Android Studio Otter \| 2025.2.1](https://developer.android.com/studio/releases#test-backup-restore) or higher.
- A [virtual device](https://developer.android.com/studio/run/managing-avds) or physical device running Android 9 (API level 28) or higher with a **Google Play** system image and Google Play services version `242200000` or higher (verify with `adb shell dumpsys package
  com.google.android.gms | grep versionCode`).
- A Google Account signed in on the test device or emulator, with a PIN, pattern, or password screen lock configured under **Settings \> Security** so cloud end-to-end encryption (E2EE) is active and `E2eeUnavailableException` isn't thrown.
- An app built with `debuggable true` or running in [`debug`](https://developer.android.com/studio/debug) mode. Debugging is enabled by default for emulators.

## Use Android Studio's Backup and Restore feature

To test the Restore Credentials functionality using Android Studio, first back
up data from a device, and then restore app data on another device. After
restoring the app, the authentication state is automatically restored.

### Back up authentication data in Android Studio

To test the backup flow in Android Studio, complete the following steps:

1. Run your app on the emulator.
2. Sign in to the app using any authentication mechanism (for example, username and password, passkeys, or Sign in with Google) so your app calls `createCredential()` with `CreateRestoreCredentialRequest`.
3. Click **Backup App Data** on the device options at the top of the running device window. ![Backup App Data in Android Studio](https://developer.android.com/static/identity/sign-in/images/test-restore-step1.png) **Figure 1:**Backup App Data in Android Studio
4. For **Backup type** , select **Device to Device** or **Cloud**.

   > [!NOTE]
   > **Note:** Google Play services transfers the `RestoreCredential` key pair regardless of `android:allowBackup`. However, Android Studio's **Cloud** backup test action and background restoration in `BackupAgent.onRestoreFinished()` require `android:allowBackup="true"` in `AndroidManifest.xml`. If `android:allowBackup` is `false`, test using the **Device to Device** backup type in Android Studio and verify foreground restoration on your launcher `Activity`'s first launch.

5. Click **OK**.

### Test restored authentication

After the backup is completed, either use the same device to test the restore
step or use a different device.

To test the restore flow in Android Studio, complete the following steps:

1. To test with the same device, uninstall and reinstall the app (`adb
   uninstall <package_name>` followed by `adb install <path_to_apk>`). This
   clears all local app data and the previous restore key on the device. To use
   it on a new device, install the app on the new device.

   > [!NOTE]
   > **Note:** In a production environment, users typically navigate the setup wizard while onboarding. However, Android Studio's restore flow simulates the setup wizard flow, enabling you to test the restoration process without manually performing the initial device setup.

2. To test **Tier-1 background restoration**
   (`BackupAgent.onRestoreFinished()`) without launching the `Activity` first,
   open **Run \> Edit Configurations** in Android Studio and set **Launch
   Options \> Launch** to **Nothing** before installing. If you launch the app
   before running **Restore App Data**, you are redirected to the
   authentication screen because the restore key hasn't been restored to the
   device yet.

3. Click **Restore App data** on the device options of the running device
   window and select the recently created backup.

   ![Restore App Data in Android Studio](https://developer.android.com/static/identity/sign-in/images/test-restore-step2.png) **Figure 2:**Restore App Data in Android Studio

   <br />

4. Inspect Logcat to confirm `BackupAgent.onRestoreFinished()` retrieved the
   `RestoreCredential` in the background, or open the app to verify that the
   user is signed in automatically.

## Test with `adb` or automated JUnit 5 test suites

In addition to Android Studio's UI action, you can test backup and restore flows
from the command line or in continuous integration (CI):

- **Command-line testing with `bmgr`** : On a physical device or headless emulator, enable the backup manager (`adb shell bmgr enable true`), trigger a backup (`adb shell bmgr backupnow <package_name>`), reinstall the app, list available backup sets (`adb shell bmgr list sets`), and trigger a restore (`adb shell bmgr restore <token> <package_name>`). For full details, see [Test backup and restore](https://developer.android.com/identity/data/testingbackup).
- **Automated JUnit 5 host tests (`androidx.test.backup`)** : Write automated JUnit 5 tests using `BackupRestoreController` to exercise both `BackupTransportMode.CLOUD_ENCRYPTED` and `BackupTransportMode.DEVICE_TO_DEVICE` across one or two connected devices. For setup instructions and sample tests, see [Automated backup and restore
  testing](https://developer.android.com/studio/preview/backup-restore-testing).