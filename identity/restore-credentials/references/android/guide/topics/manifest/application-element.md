syntax:
:

    ```xml
    <application android:allowTaskReparenting=["true" | "false"]
                 android:allowBackup=["true" | "false"]
                 android:allowClearUserData=["true" | "false"]
                 android:allowCrossUidActivitySwitchFromBelow=["true" | "false"]
                 android:allowNativeHeapPointerTagging=["true" | "false"]
                 android:appCategory=["accessibility" | "audio" | "game" |
                 "image" | "maps" | "news" | "productivity" | "social" | "video"]
                 android:backupAgent="string"
                 android:backupInForeground=["true" | "false"]
                 android:banner="drawable resource"
                 android:dataExtractionRules="string resource"
                 android:debuggable=["true" | "false"]
                 android:description="string resource"
                 android:enabled=["true" | "false"]
                 android:enableOnBackInvokedCallback=["true" | "false"]
                 android:extractNativeLibs=["true" | "false"]
                 android:forceQueryable=["true" | "false"]
                 android:fullBackupContent="string"
                 android:fullBackupOnly=["true" | "false"]
                 android:gwpAsanMode=["always" | "never"]
                 android:hasCode=["true" | "false"]
                 android:hasFragileUserData=["true" | "false"]
                 android:hardwareAccelerated=["true" | "false"]
                 android:icon="drawable resource"
                 android:intentMatchingFlags=["none" | "enforceIntentFilter" | "allowNullAction"]
                 android:isGame=["true" | "false"]
                 android:isMonitoringTool=["parental_control" | "enterprise_management" |
                 "other"]
                 android:killAfterRestore=["true" | "false"]
                 android:largeHeap=["true" | "false"]
                 android:label="string resource"
                 android:localeConfig="xml resource"
                 android:logo="drawable resource"
                 android:manageSpaceActivity="string"
                 android:name="string"
                 android:networkSecurityConfig="xml resource"
                 android:pageSizeCompat=["true" | "false"]
                 android:permission="string"
                 android:persistent=["true" | "false"]
                 android:process="string"
                 android:restoreAnyVersion=["true" | "false"]
                 android:requestLegacyExternalStorage=["true" | "false"]
                 android:requiredAccountType="string"
                 android:resizeableActivity=["true" | "false"]
                 android:restrictedAccountType="string"
                 android:supportsRtl=["true" | "false"]
                 android:taskAffinity="string"
                 android:testOnly=["true" | "false"]
                 android:theme="resource or theme"
                 android:uiOptions=["none" | "splitActionBarWhenNarrow"]
                 android:usesCleartextTraffic=["true" | "false"]
                 android:vmSafeMode=["true" | "false"] >
        . . .
    </application>
    ```

contained in:
:   `<manifest>`

can contain:
:   `<activity>`

    `<activity-alias>`

    `<meta-data>`

    `<service>`

    `<receiver>`

    `<profileable>`

    `<provider>`

    `<uses-library>`

    `<uses-native-library>`

    `<property>`

description:

:   The declaration of the application. This element contains subelements
    that declare each of the application's components and has attributes
    that can affect all the components.

    Many of these attributes, such as
    `icon`, `label`, `permission`, `process`,
    `taskAffinity`, and `allowTaskReparenting`, set default values
    for corresponding attributes of the component elements. Others, such as
    `debuggable`, `enabled`, `description`, and
    `allowClearUserData`, set values for the application as a whole and
    aren't overridden by the components.

attributes
:

    `android:allowTaskReparenting`
    :   Whether activities that the application defines can move from
        the task that started them to the task they have an affinity for when that task
        is next brought to the front. It's `"true"` if they can move, and
        `"false"` if they must remain with the task where they started.
        The default value is `"false"`.


        The
        `<activity>`
        element has its own
        `allowTaskReparenting`
        attribute that can override the value set here.

    `android:allowBackup`

    :   Whether to let the application participate in the backup
        and restore infrastructure. If this attribute is set to `"false"`, no
        backup or restore of the application is ever performed, disabling all cloud backups
        and device-to-device (D2D) transfers, even by a full-system backup that otherwise
        causes all application data to save using `adb`. The default value of this
        attribute is `"true"`. If your app processes sensitive data, you can keep
        this attribute set to `"true"` and configure
        [`android:dataExtractionRules`](https://developer.android.com/guide/topics/manifest/application-element#dataExtractionRules) to safely
        exclude sensitive keys or temporary caches while preserving user preferences and
        continuity.

        **Note:**For apps targeting Android 12 (API level 31) or higher, this behavior
        varies. On devices from some device manufacturers, you can't disable device-to-device migration
        of your app's files.

        However, you can disable cloud-based backup and restore of your app's files by setting
        this attribute to `"false"`, even if your app targets Android 12 (API level 31) or
        higher.


        For more information, see the
        [backup and restore](https://developer.android.com/about/versions/12/behavior-changes-12#backup-restore) section
        of the page that describes behavior changes for apps targeting Android 12 (API level 31) or
        higher.

    `android:allowClearUserData`

    :   Whether to let the application reset user data. This data includes
        flags, such as whether the user has seen introductory tooltips, as
        well as user-customizable settings and preferences. The default value of this
        attribute is `"true"`.

        **Note:** Only apps that are part of the system image can
        declare this attribute explicitly. Third-party apps can't include this
        attribute in their manifest files.

        For more information, see [Data backup overview](https://developer.android.com/guide/topics/data/backup).

    `android:allowCrossUidActivitySwitchFromBelow`

    :   Specifies whether activities from other applications in the same task
        can be launched on top of this application. If set to `"false"`,
        activity launches that would replace this application in the user's view
        are blocked.

        The default value is `"true"`.

        For more information, see the guide to
        [Secure Background Activity Launches](https://developer.android.com/guide/components/activities/secure-bal).

    `android:allowNativeHeapPointerTagging`

    :   Whether the app enables the Heap pointer tagging feature. The default value of
        this attribute is `"true"`.

        **Note:** Disabling this feature **doesn't** address the
        underlying code health issue. Future hardware devices might not support this manifest tag.

        For more information, see [Tagged Pointers](https://source.android.com/devices/tech/debug/tagged-pointers).

    `android:appCategory`

    :   Declares the category of this app. Categories are used to cluster multiple apps
        together into meaningful groups, such as when summarizing battery, network, or
        disk usage. Only define this value for apps that fit well into one of
        the specific categories.

        Must be one of the following constant values.

        | Value | Description |
        |---|---|
        | `accessibility` | Apps that are primarily accessibility apps, such as screen-readers. |
        | `audio` | Apps that primarily work with audio or music, such as music players. |
        | `game` | Apps that are primarily games. |
        | `image` | Apps that primarily work with images or photos, such as camera or gallery apps. |
        | `maps` | Apps that are primarily map apps, such as navigation apps. |
        | `news` | Apps that are primarily news apps, such as newspapers, magazines, or sports apps. |
        | `productivity` | Apps that are primarily productivity apps, such as cloud storage or workplace apps. |
        | `social` | Apps that are primarily social apps, such as messaging, communication, email, or social network apps. |
        | `video` | Apps that primarily work with video or movies, such as streaming video apps. |


    `android:backupAgent`
    :   The name of the class that implements the application's backup agent,
        a subclass of `BackupAgent`. The attribute value is
        a fully qualified class name, such as `"com.example.project.MyBackupAgent"`.
        However, as a shorthand, if the first character of the name is a period,
        for example, `".MyBackupAgent"`, it is appended to the
        package name specified in the
        `<manifest>`
        element.


        There is no default. The name must be specific.

    `android:backupInForeground`
    :   Indicates that
        [Auto Backup](https://developer.android.com/guide/topics/data/autobackup) operations
        can be performed on this app even if the app is in a foreground-equivalent
        state. The system shuts down an app during auto backup operation,
        so use this attribute with caution. Setting this flag to `"true"` can impact app
        behavior while the app is active.

        The default value is `"false"`, which means that the OS avoids
        backing up the app while it's running in the foreground, such as a music app
        that is actively playing music using a service in the
        `startForeground()` state.

    `android:banner`
    :   A [drawable resource](https://developer.android.com/guide/topics/resources/drawable-resource)
        providing an extended graphical banner for its associated item. Use with the
        `<application>` tag to supply a default banner for all application activities or with the
        [`<activity>`](https://developer.android.com/guide/topics/manifest/activity-element)
        tag to supply a banner for a specific activity.

        The system uses the banner to represent an app in
        the Android TV home screen. Therefore, only specify this for applications with an activity that handles the
        `CATEGORY_LEANBACK_LAUNCHER` intent.


        This attribute is set as a reference to a drawable resource containing
        the image, for example `"@drawable/banner"`. There is no default banner.


        For more information, see [Provide a home screen banner](https://developer.android.com/training/tv/start/start#banner).

    `android:dataExtractionRules`

    :
        Applications can set this attribute to an XML resource where they specify the
        rules determining which files and directories can be copied from the device as part of backup or
        transfer operations.


        For information about the format of the XML file, see [Backup and restore](https://developer.android.com/about/versions/12/behavior-changes-12#backup-restore).

    `android:debuggable`
    :   Whether the application can be debugged, even when running
        on a device in user mode. It's `"true"` if it can be and `"false"`
        if not. The default value is `"false"`.

    `android:description`
    :   User-readable text about the application, which is longer and more descriptive than the application label.
        The value is set as a reference to a string resource. Unlike the label, it can't be a raw string.
        There is no default value.

    `android:enabled`
    :   Whether the Android system can instantiate components of
        the application. It's `"true"` if it can and `"false"`
        if not. If the value is `"true"`, each component's
        `enabled` attribute determines whether that component is enabled.
        If the value is `"false"`, it overrides the
        component-specific values, and all components are disabled.


        The default value is `"true"`.

    `android:enableOnBackInvokedCallback`

    :   This flag lets you opt out of predictive system animations at the app level.


        Set `android:enableOnBackInvokedCallback=false` to turn off predictive back animations at the app level
        and instruct the system to ignore calls to the `OnBackInvokedCallback` platform API.

    `android:extractNativeLibs`
    :   This attribute indicates whether the package installer extracts native libraries from the
        APK to the file system. If set to `"false"`, your native libraries are stored
        uncompressed in the APK. Although your APK might be larger, your application loads faster
        because the libraries load directly from the APK at runtime.


        The default value of `extractNativeLibs` depends on `minSdkVersion` and the
        version of AGP you're using. In most cases, the default behavior is probably what you want, and
        you don't have to set this attribute explicitly.

    `android:forceQueryable`

    :   Specifies whether this application is visible to all other applications
        on the device, regardless of what those other applications declare in the
        `<queries>` tags in their manifests.

        The default value is `"false"`.

        For more information, see the guide on
        [automatic package visibility filtering](https://developer.android.com/training/package-visibility/automatic).

    `android:fullBackupContent`
    :   This attribute points to an XML file that contains full backup rules for
        [Auto Backup](https://developer.android.com/guide/topics/data/autobackup).
        These rules determine what files get backed up. For more information, see the
        [XML config syntax](https://developer.android.com/guide/topics/data/autobackup#XMLSyntax)
        for Auto Backup.


        This attribute is optional. If it is not specified, by default, Auto Backup
        includes most of your app's files. For more information, see
        [Files that are backed up](https://developer.android.com/guide/topics/data/autobackup#Files).

    `android:fullBackupOnly`
    :   This attribute indicates whether to use
        [Auto Backup](https://developer.android.com/guide/topics/data/autobackup) on devices
        where it is available. If set to `"true"`, then your app performs
        Auto Backup when installed on a device running Android 6.0 (API level 23) or
        higher. On older devices, your app ignores this attribute and performs
        [key/value backups](https://developer.android.com/guide/topics/data/keyvaluebackup).


        The default value is `"false"`.

    `android:gwpAsanMode`
    :   This attribute indicates whether to use
        [GWP-ASan](https://developer.android.com/ndk/guides/gwp-asan), a native memory allocator feature that helps
        find use-after-free and heap-buffer-overflow bugs.


        The default value is `"never"`.

    `android:hasCode`
    :   Whether the application contains any DEX code---that is, code using the
        Kotlin or Java programming language.
        It's `"true"` if it does and `"false"` if not. When the
        value is `"false"`, the system doesn't try to load any application
        code when launching components. The default value is `"true"`.


        If the application includes native (C/C++) code, but no DEX code, this should
        be set to `"false"`. If set to `"true"` when the APK
        contains no DEX code, the app may fail to load.


        **This property must account for code included in the application by
        dependencies.** If the application depends on an AAR that uses
        Java/Kotlin code, or directly on a JAR, `app:hasCode` must be
        `"true"`, or omitted as that is the default.


        For example, your app might support
        [Play feature delivery](https://developer.android.com/platform/technology/app-bundle) and include feature
        modules that don't generate any DEX files, which is bytecode optimized for the Android
        platform. If so, you need to set this property to `"false"` in the module's manifest
        file to avoid runtime errors.

    `android:hasFragileUserData`
    :   Whether to show the user a prompt to
        keep the app's data when the user uninstalls the app. The default value is `"false"`.

    `android:hardwareAccelerated`
    :   Whether hardware-accelerated rendering is enabled for all
        activities and views in this application. It's `"true"` if it is
        enabled and `"false"` if not. The default value is `"true"` if you set
        either [`minSdkVersion`](https://developer.android.com/guide/topics/manifest/uses-sdk-element#min)
        or [`targetSdkVersion`](https://developer.android.com/guide/topics/manifest/uses-sdk-element#target)
        to `"14"` or higher. Otherwise, it's `"false"`.

        Starting from Android 3.0 (API level 11), a hardware-accelerated OpenGL renderer is
        available to applications to improve performance for many common 2D graphics
        operations. When the hardware-accelerated renderer is enabled, most operations
        in Canvas, Paint, Xfermode, ColorFilter, Shader, and Camera are accelerated.


        This results in smoother animations, smoother scrolling, and improved
        responsiveness overall, even for applications that don't explicitly make use
        the framework's OpenGL libraries.


        Not all of the OpenGL 2D operations are accelerated. If you enable
        the hardware-accelerated renderer, test your application so that it can
        make use of the renderer without errors.


        For more information, read the
        [Hardware acceleration](https://developer.android.com/guide/topics/graphics/hardware-accel)
        guide.

    `android:icon`
    :   An icon for the application as whole and the default icon for
        each of the application's components. See the individual
        `icon` attributes for the
        `<activity>`,
        `<activity-alias>`,
        `<service>`,
        `<receiver>`, and
        `<provider>` elements.


        This attribute is set as a reference to a drawable resource containing
        the image, such as `"@drawable/icon"`. There is no default icon.

    `android:intentMatchingFlags`

    :
        Use this attribute to fine-tune how the system matches incoming intents to app
        components. By default, no special matching rules are applied.


        This attribute can be specified on the `<application>` tag
        as well as on component tags, including `<activity>`,
        `<activity-alias>`, `<receiver>`,
        `<service>`, and `<provider>`. The value
        set on a component overrides the value set on the
        `<application>` tag.


        The value must be one or more of the following flags, separated by '`|`':

        | Flag | Description |
        |---|---|
        | `none` | Disables all special matching rules for incoming intents. When specifying multiple flags, conflicting values are resolved by giving precedence to the `none` flag. |
        | `enforceIntentFilter` | Enforces stricter matching for incoming intents: - Explicit intents must match the target component's intent filter. - Intents without an action don't match any intent filter. |
        | `allowNullAction` | Relaxes the matching rules to allow intents without an action to match. This flag is used in conjunction with `enforceIntentFilter` to achieve the following behavior: - Explicit intents must match the target component's intent filter. - Intents without an action are allowed to match any intent filter. |

        For more information, see the
        [Safer Intents](https://developer.android.com/about/versions/16/behavior-changes-16#safer-intents)
        section in the Android 16 (API level 36) behavior changes.

    `android:isGame`
    :   Whether the application is a game. The system might group together applications classified
        as games or display them separately from other applications. The default is `"false"`.

    `android:isMonitoringTool`

    :   Indicates that this application is designed to monitor other individuals.

        **Note:** If an app declares this attribute in its manifest, the developer must
        follow the
        [Stalkerware](https://support.google.com/googleplay/android-developer/answer/9888380#commercial-spyware)
        policy to publish the app to Google Play.

        There is no default value. The developer must specify one of the following values:

        | Value | Description |
        |---|---|
        | `"parental_control"` | App caters to parental control and is specifically targeted at parents who want to keep their kids safe. |
        | `"enterprise_management"` | App caters to enterprises that want to manage and track devices given to employees. |
        | `"other"` | App caters to a use case not otherwise specified in this table. |

    `android:killAfterRestore`

    :   Whether the application terminates after its
        settings have been restored during a full-system restore operation.
        Single-package restore operations never cause the application to
        shut down. Full-system restore operations typically only occur once,
        when the phone is first set up. Third-party applications don't normally
        need to use this attribute.

        The default is `"true"`, which means that after the application
        finishes processing its data during a full-system restore, it terminates.

    `android:largeHeap`

    :   Whether the application's processes are created with a large Dalvik heap. This applies to
        all processes created for the application. It only applies to the first application loaded into a
        process. If you're using a shared user ID to let multiple applications use a process, they all
        must use this option consistently to avoid unpredictable results.

        Most apps don't need this and instead focus on reducing their overall memory usage for
        improved performance. Enabling this also doesn't guarantee a fixed increase in available memory,
        because some devices are constrained by their total available memory.

        To query the available memory size at runtime, use the methods `getMemoryClass()` or `getLargeMemoryClass()`.

    `android:label`
    :   A user-readable label for the application as a whole and a default
        label for each of the application's components. See the individual
        `label` attributes for the
        `<activity>`,
        `<activity-alias>`,
        `<service>`,
        `<receiver>`, and
        `<provider>` elements.


        The label is set as a reference to a string resource, so that
        it can be localized like other strings in the user interface.
        However, as a convenience while you're developing the application,
        it can also be set as a raw string.

    `android:localeConfig`

    :   A reference to an XML resource that specifies the list of locales
        supported by the application. This is used by the system to support per-app
        language preferences.

        For more information, see the guide on
        [per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages).

    `android:logo`
    :   A logo for the application as whole and the default logo for activities.
        This attribute is set as a reference to a drawable resource containing
        the image, such as `"@drawable/logo"`. There is no default logo.

    `android:manageSpaceActivity`
    :   The fully qualified name of an `Activity` subclass that the system
        launches to let users manage the memory occupied by the application
        on the device. The activity is also declared with an
        `<activity>` element.

    `android:name`
    :   The fully qualified name of an `Application`
        subclass implemented for the application. When the application process
        is started, this class is instantiated before any of the application's
        components.


        The subclass is optional. Most applications don't need one.
        In the absence of a subclass, Android uses an instance of the base
        `Application` class.

    `android:networkSecurityConfig`

    :   Specifies the name of the XML file that contains your application's
        [Network security
        configuration](https://developer.android.com/training/articles/security-config). The value is a reference to the XML resource file
        containing the configuration.

        This attribute was added in API level 24.

    `android:pageSizeCompat`

    :   Overrides the user or platform compatibility settings for 16 KB page
        sizes, which lets you force page-agnostic compatibility mode on or off for
        this application.

        For more information, see the guide on
        [supporting 16 KB page sizes](https://developer.android.com/guide/practices/page-sizes).

    `android:permission`
    :   The name of a permission that clients need in order to interact
        with the application. This attribute is a convenient way to set a
        permission that applies to all of the application's components. It is
        overwritten by setting the `permission` attributes of individual
        components.


        For more information about permissions, see the
        [Permissions](https://developer.android.com/guide/topics/manifest/manifest-intro#perms)
        section in the app manifest overview and
        [Security tips](https://developer.android.com/guide/topics/security/security).

    `android:persistent`
    :   Whether the application remains running at all times. It's
        `"true"` if it does and `"false"` if not. The default value
        is `"false"`. Applications don't normally set this flag.
        Persistence mode is intended only for certain system applications.

    `android:process`
    :   The name of a process where all components of the application run.
        Each component can override this default by setting its own `process`
        attribute.


        By default, Android creates a process for an application when the first
        of its components needs to run. All components then run in that process.
        The name of the default process matches the package name set by the
        `<manifest>` element.


        By setting this attribute to a process name that's shared with another
        application, you can arrange for components of both applications to run in
        the same process, but only if the two applications also share a
        user ID and are signed with the same certificate.


        If the name assigned to this attribute begins with a colon (`:`), a new
        process, private to the application, is created when it's needed.
        If the process name begins with a lowercase character, a global process
        of that name is created. A global process can be shared with other
        applications, reducing resource usage.

    `android:restoreAnyVersion`
    :   Indicates that the application is prepared to attempt a restore of any
        backed-up data set, even if the backup was stored by a newer version
        of the application than is currently installed on the device. Setting
        this attribute to `"true"` lets the Backup Manager
        - `$1`
        attempt a restore even when a version mismatch suggests that the data is incompatible. *Use with caution!*
        - The default value of this attribute is `"false"`.

    `android:requestLegacyExternalStorage`

    :   Whether the application wants to opt out of
        [scoped storage](https://developer.android.com/training/data-storage/files/external-scoped).

        **Note:** Depending on changes related to policy or app
        compatibility, the system might not honor this opt-out request.

    `android:requiredAccountType`
    :   Specifies the account type required by the application to function.
        If your app requires an `Account`, the value for this attribute must
        correspond to the account authenticator
        type used by your app, as defined by `AuthenticatorDescription`,
        such as `"com.google"`.

        The default value is null and indicates that the application
        can work *without* any accounts.


        Because restricted profiles
        can't add accounts, specifying this attribute makes your app
        unavailable from a restricted profile unless you also declare
        [`android:restrictedAccountType`](https://developer.android.com/guide/topics/manifest/application-element#restrictedAccountType) with
        the same value.


        **Caution:**
        If the account data might reveal personally identifiable information, it's important
        that you declare this attribute and leave [`android:restrictedAccountType`](https://developer.android.com/guide/topics/manifest/application-element#restrictedAccountType) null, so that restricted profiles cannot use
        your app to access personal information that belongs to the owner user.


        This attribute was added in API level 18.

    `android:resizeableActivity`

    :
        Specifies whether the app supports [multi-window mode](https://developer.android.com/guide/topics/ui/multi-window).

        > [!WARNING]
        > **Warning:** To improve the layout of apps on form factors with smallest width \>= 600dp, the system ignores this attribute for apps that target Android 16 (API level 36). Your app can opt out of the Android 16 behavior, but the opt out will be eliminated in a future release. See [Device compatibility mode](https://developer.android.com/guide/practices/device-compatibility-mode#android_16).


        You can set this attribute in either the [`<activity>`](https://developer.android.com/guide/topics/manifest/activity-element)
        or `<application>` element.


        If you set this attribute to `"true"`, the user can launch the activity in
        split-screen and free-form modes. If you set the attribute to `"false"`, the app
        can't be tested or optimized for a multi-window environment. The system can
        still put the activity in multi-window mode with compatibility mode applied.


        Setting this attribute to `"true"` doesn't guarantee that there are no
        other apps in multi-window mode visible on screen, such as picture-in-picture, or
        on other displays. Therefore, setting this flag doesn't mean that your app
        has exclusive resource access.


        For apps targeting API level 24 or higher, the default is `"true"`.


        If your app targets API level 31 or higher, this attribute works differently on small and large
        screens:

        - **Large screens (sw \>= 600dp):** all apps support multi-window mode. The attribute indicates whether an app can be resized, not whether the app supports multi-window mode. If `resizeableActivity="false"`, the app is put into compatibility mode when necessary to conform to display dimensions.
        - **Small screens (sw \< 600dp):** if `resizeableActivity="true"` and the minimum width and minimum height of the main activity are within the multi-window requirements, the app supports multi-window mode. If `resizeableActivity="false"`, the app doesn't support multi-window mode regardless of the activity minimum width and height.

        > [!NOTE]
        > **Note:**
        >
        > - Device manufacturers can override the API level 31 behavior to improve the layout of apps.
        > - On devices with Android 16 (API level 36) or higher installed, virtual device owners (select trusted and privileged apps) can configure devices they manage to override (ignore) this attribute to improve app layout. See also [Companion app
        >   streaming](https://source.android.com/docs/core/permissions/app-streaming).
        >
        > See [Device compatibility mode](https://developer.android.com/guide/practices/device-compatibility-mode).


        A task's root activity value is applied to all additional activities launched in the task. That is, if the root
        activity of a task is resizable, then the system treats all other activities in the task as resizable. If the root
        activity isn't resizable, the other activities in the task aren't resizable.


        This attribute was added in API level 24.

    `android:restrictedAccountType`
    :   Specifies the account type required by this application and indicates that restricted profiles
        can access such accounts that belong to the owner user. If your app requires an
        `Account` and restricted profiles *can*
        access the primary user's accounts, the value for this attribute must
        correspond to the account authenticator type used by your app, as
        defined by `AuthenticatorDescription`, such as `"com.google"`.

        The default value is null and indicates that the application can work *without* any
        accounts.


        **Caution:**
        Specifying this attribute lets restricted profiles use your
        app with accounts that belong to the owner user, which might reveal personally identifiable
        information. If the account might reveal personal details, *don't*
        use this attribute. Instead, declare the [`android:requiredAccountType`](https://developer.android.com/guide/topics/manifest/application-element#requiredAccountType) attribute
        to make your app unavailable to restricted profiles.


        This attribute was added in API level 18.

    `android:supportsRtl`

    :   Declares whether your application is willing to support right-to-left (RTL) layouts.

        If this is set to `"true"` and
        [`targetSdkVersion`](https://developer.android.com/guide/topics/manifest/uses-sdk-element#target)
        is set to 17 or higher, various RTL APIs are
        activated and used by the system so your app can display RTL layouts.
        If this is set to `"false"` or if `targetSdkVersion` is set to 16 or lower, the RTL APIs are ignored
        or have no effect, and your app behaves the same regardless of the layout
        direction associated to the user's locale choice. That is, your layouts are always left-to-right.

        The default value of this attribute is `"false"`.

        This attribute was added in API level 17.

    `android:taskAffinity`
    :   An affinity name that applies to all activities within the application,
        except for those that set a different affinity with their own
        `taskAffinity`
        attributes. See that attribute for more information.


        By default, all activities within an application share the same
        affinity. The name of that affinity is the same as the package name
        set by the
        `<manifest>` element.

    `android:testOnly`
    :   Indicates whether this application is only for testing purposes. For example,
        it might expose functionality or data outside of itself that can cause a security
        hole, but be useful for testing. This kind of APK only installs
        through `adb`. You can't publish it to Google Play.

        Android Studio automatically adds this attribute when you click **Run**
        ![](https://developer.android.com/static/studio/images/buttons/toolbar-run.png).

    `android:theme`
    :   A reference to a style resource defining a default theme for all
        activities in the application. Individual activities can override
        the default by setting their own `theme`
        attributes. For more information, see [Styles and themes](https://developer.android.com/guide/topics/ui/themes).

    `android:uiOptions`
    :   Extra options for an activity's UI. Must be one of the following values:

        | Value | Description |
        |---|---|
        | `"none"` | No extra UI options. This is the default. |
        | `"splitActionBarWhenNarrow"` | Adds a bar at the bottom of the screen to display action items in the *app bar* , also known as the *action bar*, when constrained for horizontal space, such as when in portrait mode on a handset. Instead of a small number of action items appearing in the app bar at the top of the screen, the app bar splits into the top navigation section and the bottom bar for action items. This means a reasonable amount of space is available for the action items and for the navigation and title elements at the top. Menu items aren't split across the two bars. They always appear together. |


        For more information about the app bar, see [Add the app bar](https://developer.android.com/training/appbar).

        This attribute was added in API level 14.

    `android:usesCleartextTraffic`
    :   Indicates whether the app intends to use cleartext network traffic, such as cleartext HTTP.
        The default value for apps that target API level 27 or lower is `"true"`. Apps that
        target API level 28 or higher default to `"false"`.

        **Note:**
        This attribute is getting deprecated and will be ignored for apps targeting API levels 38 and above.
        Specify a [Network Security Configuration](https://developer.android.com/training/articles/security-config#CleartextTraffic) to control cleartext traffic for API levels 24 and above.
        If your app targets API levels 23 and below, you must specify `android:usesCleartextTraffic` in addition to a Network Security Config.


        When the attribute is set to `"false"`, platform components, for example, HTTP and FTP
        stacks, `DownloadManager`, and
        `MediaPlayer`, refuse the
        app's requests to use cleartext traffic.

        Third-party libraries are strongly encouraged to honor this
        setting as well. The key reason for avoiding cleartext traffic is the lack of confidentiality,
        authenticity, and protections against tampering. A network attacker can eavesdrop on transmitted
        data and also modify it without being detected.


        This flag is honored on a best-effort basis because it's impossible to prevent all cleartext
        traffic from Android applications given the level of access provided to them. For example, there's
        no expectation that the `Socket` API honors
        this flag, because it can't determine whether its traffic is in cleartext.

        However, most
        network traffic from applications is handled by higher-level network stacks and components, which can
        honor this flag by either reading it from
        `ApplicationInfo.flags`
        or
        `NetworkSecurityPolicy.isCleartextTrafficPermitted()`.


        **Note:**
        `WebView` honors this attribute for
        applications targeting API level 26 and higher.


        During app development, StrictMode can be used to identify any cleartext traffic from the app.
        For more information, see
        `StrictMode.VmPolicy.Builder.detectCleartextNetwork()`.


        This attribute was added in API level 23.


        This flag is ignored on Android 7.0 (API level 24) and above if an Android Network Security
        Config is present.

    `android:vmSafeMode`
    :   Indicates whether the app wants the virtual machine (VM) to operate
        in safe mode. The default value is `"false"`.

        This attribute was added in API level 8, where a value of `"true"`
        disabled the Dalvik just-in-time (JIT) compiler.


        This attribute was adapted in API level 22, where a value of `"true"`
        disabled the ART ahead-of-time (AOT) compiler.

introduced in:
:   API level 1

see also:
:   `<activity>`

    `<service>`

    `<receiver>`

    `<provider>`