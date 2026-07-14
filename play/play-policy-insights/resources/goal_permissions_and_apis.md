{{#IF_ALL requested_permissions.exact_alarm, exact_alarm}}{{ACTIVATE_GOAL}}{{/IF_ALL}}
{{#IF_ALL requested_permissions.foreground_service, foreground_service}}{{ACTIVATE_GOAL}}{{/IF_ALL}}
{{#IF_ALL requested_permissions.accessibility, accessibility}}{{ACTIVATE_GOAL}}{{/IF_ALL}}
{{#IF_ALL requested_permissions.APPS_ON_DEVICE, data_sources.APPS_ON_DEVICE}}{{ACTIVATE_GOAL}}{{/IF_ALL}}
{{#IF_ALL requested_permissions.SMS_CALL_LOG, data_sources.SMS_CALL_LOG}}{{ACTIVATE_GOAL}}{{/IF_ALL}}
{{#IF_ALL requested_permissions.MEDIA, data_sources.MEDIA}}{{ACTIVATE_GOAL}}{{/IF_ALL}}
{{#IF_ALL requested_permissions.AUDIO, data_sources.AUDIO}}{{ACTIVATE_GOAL}}{{/IF_ALL}}
{{#IF_ALL requested_permissions.ALL_FILES, data_sources.FILES_AND_DOCS}}{{ACTIVATE_GOAL}}{{/IF_ALL}}
{{#IF_ALL requested_permissions.LOCATION, data_sources.PRECISE_LOCATION}}{{ACTIVATE_GOAL}}{{/IF_ALL}}
{{#IF_ALL requested_permissions.LOCATION, data_sources.APPROX_LOCATION}}{{ACTIVATE_GOAL}}{{/IF_ALL}}
{{#IF_ALL requested_permissions.CONTACTS, data_sources.CONTACTS}}{{ACTIVATE_GOAL}}{{/IF_ALL}}

## Permissions and APIs Audit

### Hint: Deducing Core Functionality

Since you must determine if certain permissions are justified by the app's "core
purpose", use these fast heuristics:
1. **The "Broken" Test**: Is the feature essential to the app's primary purpose?
   If the app would still be functional and useful without the feature, it is
   NOT core functionality.
2. **Manifest Intent**: Review `AndroidManifest.xml`. The name of the `LAUNCHER`
   Activity and specialized `<intent-filter>` declarations (like default SMS
   handlers) strongly indicate the app's main purpose.
3. **Naming**: The package name (`{{PACKAGE_NAME}}`) and app label
   (`{{APP_NAME}}`) often describe the app's purpose explicitly.
4. **Execution Context**: Usage in classes like `BackupManager` suggest core
   functionality, whereas usage in `AdHelper`, `CrashReporter`, or
   `AnalyticsManager` indicates secondary features.
5. **Mandatory Rule**: Secondary features like **advertising, analytics, or
   social sharing never justify** restricted permissions like Background
   Location, All Files Access, or Broad Media Access.

---

### Policies to Verify

{{#IF_ALL requested_permissions.MEDIA, data_sources.MEDIA}}
#### Photo and Video Access Policy (Policy ID: photo_video_access_policy)

- **Goal**: Evaluate if the app's core functionality justifies broad access to
  photos or if it should migrate to the Android Photo Picker.
- **The Policy Spirit**: User privacy is paramount. Apps should only request
  broad media storage permissions if they are dedicated media managers (like
  Gallery or Backup apps). For standard tasks like profile picture uploads,
  custom sharing, or attaching media, developers must use scoped APIs to prevent
  security risks.
- **Evidence**:
  {{#EACH data_sources.MEDIA}}
  - `{{ITEM}}`
  {{/EACH}}
  **Relevant Permissions Requested**:
  {{#EACH requested_permissions.MEDIA}}
  - `{{ITEM}}`
  {{/EACH}}
  {{#IF TARGET_SDK}}
  **Target SDK**: `{{TARGET_SDK}}`
  {{/IF}}
- **Common Evaluation Matrix**:
  | Target SDK | Broad Media Permission Requested? | Condition / Context Checked | Severity | Direct Actionable Recommendation |
  | :--- | :--- | :--- | :--- | :--- |
  | **33 or higher** | Yes | App requests broad media access (e.g. `READ_MEDIA_IMAGES`), but features only require user-selected media. | `IMPORTANT` | Migrate to the **Android Photo Picker** (`MediaStore.ACTION_PICK_IMAGES`) which does not require any permission prompt. |
  | **Any** | Yes | App requests broad storage/media permissions but is not a dedicated media manager (e.g., a social or utility app). | `IMPORTANT` | Migrate to the **Android Photo Picker** for single-item or multi-item media selection. |

- **Domain-Specific Heuristics (Strictly Bounded)**:
  Look beyond standard native file pickers. Extrapolate using this heuristic:
  1. **User-Selected Media Heuristic**: Analyze where the code handles selected
     files. If images or videos are loaded strictly via a user-facing button
     click (e.g., "Upload Avatar", "Share Photo", "Attach File") and processed
     one-at-a-time, broad filesystem media permissions are structurally
     unnecessary. Flag an `IMPORTANT` violation and recommend Photo Picker
     migration.
{{/IF_ALL}}

{{#IF_ALL requested_permissions.ALL_FILES, data_sources.FILES_AND_DOCS}}
#### All Files Access Policy (Policy ID: all_files_access_policy)

- **Goal**: Evaluate if the app's core purpose justifies the high-risk
  `MANAGE_EXTERNAL_STORAGE` permission.
- **The Policy Spirit**: Full filesystem access is a restricted privilege. The
  Play Store strictly limits `MANAGE_EXTERNAL_STORAGE` to apps where full disk
  reads/writes are critical to the core purpose (e.g., file managers, antivirus
  scanner, backup tools). Non-compliant apps must utilize Scoped Storage or
  Storage Access Framework (SAF).
- **Evidence**:
  {{#EACH data_sources.FILES_AND_DOCS}}
  - `{{ITEM}}`
  {{/EACH}}
  **Relevant Permissions Requested**:
  {{#EACH requested_permissions.ALL_FILES}}
  - `{{ITEM}}`
  {{/EACH}}
- **Common Evaluation Matrix**:
  | Core App Purpose | Is Permission Justified? | Severity | Direct Actionable Recommendation |
  | :--- | :--- | :--- | :--- |
  | **File Manager, Antivirus, Backup/Restore, or Document Manager** | Yes (Compliant) | None | No action needed; core purpose justifies the restricted permission. |
  | **Standard Utility, Social Media, Game, or Productivity App** | No (Violation) | `CRITICAL` | Remove `MANAGE_EXTERNAL_STORAGE` from the Manifest. For document picking or local file saving, migrate to the **Storage Access Framework (SAF)**. |

- **Domain-Specific Heuristics (Strictly Bounded)**:
  Surgically audit how the filesystem is queried:
  1. **Scoped Storage Sufficiency**: Check the files using the storage APIs. If
     the app is using filesystem pathways solely to log diagnostics, store
     custom caches, or save simple download artifacts, broad access is not
     justified. Recommend using app-specific directories
     (`Context.getExternalFilesDir()`) which require zero permissions.
{{/IF_ALL}}

{{#IF requested_permissions.LOCATION}}
{{#IF_ANY data_sources.PRECISE_LOCATION, data_sources.APPROX_LOCATION}}
#### Location Access Policy (Policy ID: location_access_policy)

- **Goal**: Verify that location access is essential, uses minimum scope, and is
  properly disclosed.
- **The Policy Spirit**: User tracking is extremely sensitive. Apps must collect
  the minimum scope of location required (approximate vs precise), provide clear
  prominent disclosures, and must never utilize background tracking unless it is
  essential for safety, navigation, or physical fitness features.
- **Evidence**:
  {{#IF data_sources.PRECISE_LOCATION}}
  - **Precise Location usage**:
    {{#EACH data_sources.PRECISE_LOCATION}}
    - `{{ITEM}}`
    {{/EACH}}
  {{/IF}}
  {{#IF data_sources.APPROX_LOCATION}}
  - **Approximate Location usage**:
    {{#EACH data_sources.APPROX_LOCATION}}
    - `{{ITEM}}`
    {{/EACH}}
  {{/IF}}
  **Relevant Permissions Requested**:
  {{#EACH requested_permissions.LOCATION}}
  - `{{ITEM}}`
  {{/EACH}}
  {{#IF TARGET_SDK}}
  **Target SDK**: `{{TARGET_SDK}}`
  {{/IF}}
- **Common Evaluation Matrix**:
  | Scope | Target SDK | Finding / Condition Checked | Severity | Direct Actionable Recommendation |
  | :--- | :--- | :--- | :--- | :--- |
  | **Foreground** | Any | Requests `ACCESS_FINE_LOCATION` but features only require city-level or approximate weather/search features. | `IMPORTANT` | Downgrade Manifest to `ACCESS_COARSE_LOCATION` to respect the minimum scope mandate. |
  | **Foreground** | Any | App purpose (`{{APP_NAME}}`) does not imply location, yet foreground tracking is used, and prominent disclosure alert code is missing. | `IMPORTANT` | Implement a **Prominent In-App Disclosure** dialog explaining what location data is collected *before* requesting foreground permission. |
  | **Background** | Any | Requests `ACCESS_BACKGROUND_LOCATION` solely for advertising, marketing, or general analytics. | `CRITICAL` | **High-Risk Violation**: Completely remove background location collection from the codebase. |
  | **Background** | Any | Requests background location, but features could operate with foreground location access. | `IMPORTANT` | Downgrade the feature to foreground-only location tracking and remove background permission. |
  | **Background** | Any | Background location is legitimate, but prominent disclosure does not mention "location" and "when the app is closed or not in use." | `IMPORTANT` | Update the Prominent Disclosure text to explicitly state "location" and "when closed or not in use". |
  | **Foreground** | **37 or higher** | Precise location is requested on Android 17+. | `SUGGESTION` | Migrate to the **Location Button** API as the minimum scope mechanism for precise foreground location. |

- **Domain-Specific Heuristics (Strictly Bounded)**:
  Actively trace custom background workers and disclosures:
  1. **The Foreground Sufficiency Test**: Analyze background threads,
     `WorkManager` tasks, or background services triggering location updates. If
     the background process performs syncing or location calculations that could
     be deferred to when the user is actively viewing the app, flag an
     `IMPORTANT` violation.
  2. **The Reasonable Expectation Test**: If the app label (`{{APP_NAME}}`) or
     packages suggest a utility that shouldn't logically track location,
     evaluate any indirect geo-tracking (such as geo-lookup of network IP
     addresses, or sending local Wi-Fi SSID logs off-device). If found, flag an
     `IMPORTANT` missing prominent disclosure.
{{/IF_ANY}}
{{/IF}}

{{#IF_ALL requested_permissions.CONTACTS, data_sources.CONTACTS}}
#### Contacts Access Policy (Policy ID: contacts_access_policy)

- **Goal**: Evaluate if broad contacts access (`READ_CONTACTS`) is justified or
  if the Android Contact Picker should be used.
- **The Policy Spirit**: Address books contain sensitive personal details. The
  Play Store mandates that apps use scoped contact access unless broad,
  continuous contacts synchronization is critical (such as in social network
  friend-matching or full contact managers).
- **Evidence**:
  {{#EACH data_sources.CONTACTS}}
  - `{{ITEM}}`
  {{/EACH}}
  **Relevant Permissions Requested**:
  {{#EACH requested_permissions.CONTACTS}}
  - `{{ITEM}}`
  {{/EACH}}
  {{#IF TARGET_SDK}}
  **Target SDK**: `{{TARGET_SDK}}`
  {{/IF}}
- **Common Evaluation Matrix**:
  | Target SDK | Access Model | Condition / Finding Checked | Severity | Direct Actionable Recommendation |
  | :--- | :--- | :--- | :--- | :--- |
  | **Any** | Broad (`READ_CONTACTS`) | Broad access is requested for one-time transactions, file sharing, referrals, or simple forms. | `IMPORTANT` | Migrate to the **Android Contact Picker** (`Intent.ACTION_PICK_CONTACTS`) to query contacts securely. |
  | **37 or higher** | Broad (`READ_CONTACTS`) | Target SDK is 37+ (effective Oct 2026), and broad access is used for secondary or standard tasks. | `IMPORTANT` | Migrate to the **Android Contact Picker** as broad contacts access is restricted. |

- **Domain-Specific Heuristics (Strictly Bounded)**:
  Evaluate the depth of contact queries:
  1. **Single-Item Verification**: Search for database cursors reading contact
     tables (`ContactsContract.CommonDataKinds`). If the cursors are used solely
     to let a user select a single friend, mobile phone number, or email
     address, broad contacts access is a violation. Recommend Contact Picker.
{{/IF_ALL}}

{{#IF_ALL requested_permissions.exact_alarm, exact_alarm}}
#### Exact Alarm Policy (Policy ID: exact_alarm_policy)

- **Goal**: Evaluate if the app's core functionality justifies the
  `USE_EXACT_ALARM` permission.
- **The Policy Spirit**: Exact alarms degrade system performance and battery
  life. The Play Store strictly limits the high-risk `USE_EXACT_ALARM`
  permission to alarm clocks, timers, and calendar apps where precise,
  down-to-the-second timing is critical.
- **Evidence**:
  {{#EACH exact_alarm}}
  - `{{ITEM}}`
  {{/EACH}}
  **Relevant Permissions Requested**:
  {{#EACH requested_permissions.exact_alarm}}
  - `{{ITEM}}`
  {{/EACH}}
- **Common Evaluation Matrix**:
  | Core App Purpose | Permission Requested | Justified? | Severity | Direct Actionable Recommendation |
  | :--- | :--- | :--- | :--- | :--- |
  | **Alarm Clock, Timer, or Calendar App** | `USE_EXACT_ALARM` | Yes (Compliant) | None | No action needed. |
  | **Standard Utility, Game, Sync, or Productivity App** | `USE_EXACT_ALARM` | No (Violation) | `IMPORTANT` | Switch to `SCHEDULE_EXACT_ALARM` which respects system battery constraints, or use standard `AlarmManager` inexact scheduling. |

- **Domain-Specific Heuristics (Strictly Bounded)**:
  Examine alarm trigger targets:
  1. **Time-Insensitive Syncing**: Verify the files scheduling alarms. If alarms
     are used to fetch network updates, clean cache files, trigger analytics
     uploads, or post local daily notifications, exact timing is not justified.
     Recommend using `WorkManager` for background tasks instead of
     `AlarmManager`.
{{/IF_ALL}}

{{#IF_ALL requested_permissions.foreground_service, foreground_service}}
#### Foreground Services (Policy ID: foreground_services_policy)

- **Goal**: Verify the declaration and justification of foreground services.
- **The Policy Spirit**: Foreground services keep processes alive in the
  background and must be highly visible to users. Every declared service must
  have an appropriate `foregroundServiceType` defined in the Manifest, and
  special types like `specialUse` require specific tag property justifications.
- **Evidence**:
  {{#EACH foreground_service}}
  - `{{ITEM}}`
  {{/EACH}}
  **Relevant Permissions Requested**:
  {{#EACH requested_permissions.foreground_service}}
  - `{{ITEM}}`
  {{/EACH}}
  {{#IF TARGET_SDK}}
  **Target SDK**: `{{TARGET_SDK}}`
  {{/IF}}
- **Common Evaluation Matrix**:
  | Service Configuration | Justification Check | Severity | Direct Actionable Recommendation |
  | :--- | :--- | :--- | :--- |
  | **Missing type tag** | Foreground service is declared but lacks a `foregroundServiceType` attribute. | `CRITICAL` | Add the appropriate `android:foregroundServiceType` attribute to the service declaration in the Manifest. |
  | **Lacks specialUse property** | Service type is `specialUse`, but Manifest lacks the required `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" ...>` tag. | `CRITICAL` | Add the `<property>` tag inside the service block with a valid subtype string. |
  | **Type Misalignment** | Declared FGS type does not logically align with the app's core purpose. | `IMPORTANT` | Re-align FGS type to match app features, or migrate background operations to **WorkManager** if user-visible foreground presence is not justified. |
  | **Declaration Reminder** | Foreground service is declared (even if type is correct). | `SUGGESTION` | **Play Console Declaration Required**: For apps targeting Android 14+, you must complete a Foreground Service declaration in the Play Console (App content section) for each type used, providing a functional description, user impact video, and a specific use case selection. |

- **Domain-Specific Heuristics (Strictly Bounded)**:
  Critique specialUse justifications and service behavior:
  1. **Justification String Audit**: Read the text of the `<property>` tag for
     `specialUse`. If the text contains weak, boilerplate, or placeholder
     justifications (e.g., "requires background process for app to run"), flag
     an `IMPORTANT` violation warning the developer that Google Play reviewers
     will reject this service.
  2. **Notification Integrity**: Verify if the FGS implementation creates a
     valid user-facing notification. If no `startForeground()` or notification
     builder logic is associated with the service initiation, flag an
     `IMPORTANT` violation.
  3. **Play Console Declaration Confirmation**: If any foreground service is
     used, flag a `SUGGESTION` to remind the developer that a specialized
     declaration form in the Play Console is mandatory, requiring a video
     demonstration of the feature.
{{/IF_ALL}}

{{#IF_ALL requested_permissions.accessibility, accessibility}}
#### Accessibility API Policy (Policy ID: accessibility_api_policy)

- **Goal**: Verify the configuration and justification of Accessibility
  Services.
- **The Policy Spirit**: Accessibility APIs provide deep system access to assist
  users with disabilities. Using these APIs for non-accessibility tasks (like UI
  automation, screen scraping, background monitoring, or ad blocking) is
  strictly prohibited by Google Play and causes immediate app rejection.
- **Evidence**:
  {{#EACH accessibility}}
  - `{{ITEM}}`
  {{/EACH}}
  **Relevant Permissions Requested**:
  {{#EACH requested_permissions.accessibility}}
  - `{{ITEM}}`
  {{/EACH}}
- **Common Evaluation Matrix**:
  | Service Configuration | Real Code Usage | Justified? | Severity | Direct Actionable Recommendation |
  | :--- | :--- | :--- | :--- | :--- |
  | **`isAccessibilityTool="true"`** | Code performs screen scraping, ad blocking, or automated click routines for standard users. | No (Violation) | `CRITICAL` | **High Risk of Play Store Rejection**: Remove accessibility helper configs. Migrate UI automation to standard Android testing libraries. |
  | **`isAccessibilityTool` false/missing** | Code implements an accessibility listener but is not a dedicated helper app. | No prominent disclosure | `CRITICAL` | **High Risk of Play Store Rejection**: Because you request accessibility permission for a standard utility, you MUST implement an in-app **Prominent Disclosure and Affirmative Consent screen** before asking the user to enable the service, otherwise your app will be rejected. |

- **Domain-Specific Heuristics (Strictly Bounded)**:
  Trace the ingestion of accessibility events:
  1. **Telemetry Siphoning**: Scan the Accessibility Service methods
     (`onAccessibilityEvent`). If the service captures on-screen text,
     notifications, or keystrokes and routes them to local caches, shared
     preferences, or off-device network logging endpoints, flag a `CRITICAL`
     violation under the Accessibility API policy.
{{/IF_ALL}}

{{#IF_ALL requested_permissions.APPS_ON_DEVICE, data_sources.APPS_ON_DEVICE}}
#### Package Visibility (Policy ID: package_visibility_policy)

- **Goal**: Evaluate if the app's core functionality justifies broad visibility
  into installed apps (`QUERY_ALL_PACKAGES` or related package APIs).
- **The Policy Spirit**: The list of installed apps reveals sensitive user
  habits. The Play Store strictly restricts the `QUERY_ALL_PACKAGES` permission
  to apps that directly manage device safety (Antivirus, File Managers, Device
  Search).
- **Evidence**:
  {{#EACH data_sources.APPS_ON_DEVICE}}
  - `{{ITEM}}`
  {{/EACH}}
  **Relevant Permissions Requested**:
  {{#EACH requested_permissions.APPS_ON_DEVICE}}
  - `{{ITEM}}`
  {{/EACH}}
- **Common Evaluation Matrix**:
  | Core App Purpose | Permission Requested | Justified? | Severity | Direct Actionable Recommendation |
  | :--- | :--- | :--- | :--- | :--- |
  | **Antivirus, File Manager, or Device Search** | `QUERY_ALL_PACKAGES` | Yes (Compliant) | None | No action needed. |
  | **Any App** | `QUERY_ALL_PACKAGES` | No, used for ads, analytics, or secondary marketing. | `IMPORTANT` | **High-Risk Violation**: Remove the permission from the Manifest. Use specific `<queries>` intent declarations if package checks are strictly required for sharing/integration. |

- **Domain-Specific Heuristics (Strictly Bounded)**:
  Examine indirect queries:
  1. **Indirect Package Searching**: Check if the code queries package details
     dynamically using `PackageManager.getInstalledPackages()` or checks intents
     in loops. If the intent lists are used to deduce if competitor apps or
     advertising profiles exist, flag an `IMPORTANT` violation of package
     visibility guidelines.
{{/IF_ALL}}

{{#IF_ALL requested_permissions.SMS_CALL_LOG, data_sources.SMS_CALL_LOG}}
#### SMS and Call Log Permissions (Policy ID: sms_call_log_policy)

- **Goal**: Evaluate if the app's core functionality justifies access to
  sensitive SMS or Call Log data.
- **The Policy Spirit**: SMS and Call details are high-risk. Google Play
  restricts access to default phone handlers and default SMS handlers. Standard
  utility or shopping apps must use non-privileged APIs for SMS-based
  verification.
- **Evidence**:
  {{#EACH data_sources.SMS_CALL_LOG}}
  - `{{ITEM}}`
  {{/EACH}}
  **Relevant Permissions Requested**:
  {{#EACH requested_permissions.SMS_CALL_LOG}}
  - `{{ITEM}}`
  {{/EACH}}
- **Common Evaluation Matrix**:
  | Code Context | Finding / Trigger Checked | Severity | Direct Actionable Recommendation |
  | :--- | :--- | :--- | :--- |
  | **OTP/Account Verification** | App requests SMS permissions to automatically read login verification codes (OTPs). | `IMPORTANT` | Migrate to the **SMS Retriever API** or **SMS User Consent API** which require zero permissions. |
  | **Utility or Secondary Feature** | App requests SMS/Call log access for non-core dashboard or notification features. | `CRITICAL` | **High-Risk Violation**: Completely remove SMS and Call Log permissions from the Manifest. |
  | **Default SMS Handler** | App appears to be a legitimate handler (e.g. implements required intent filters). | `SUGGESTION` | **Play Console Declaration**: You must submit a **Permissions Declaration Form** in the Play Console and may be required to provide a video demonstration of this core functionality. |

- **Domain-Specific Heuristics (Strictly Bounded)**:
  Audit background receivers:
  1. **Background Incoming SMS Listeners**: Check if a `BroadcastReceiver`
     listens to `android.provider.Telephony.SMS_RECEIVED`. If this receiver
     parses text in the background without being a designated default SMS
     handler, flag a `CRITICAL` violation.
  2. **Default Handler Confirmation**: If the app requests SMS/Call Log
     permissions and correctly implements the system-mandated intent filters for
     a Default Handler, flag a `SUGGESTION` to remind the developer about the
     mandatory Play Console declaration form.
{{/IF_ALL}}

{{#IF_ALL requested_permissions.AUDIO, data_sources.AUDIO}}
#### Audio Recording Policy (Policy ID: audio_recording_policy)

- **Goal**: Evaluate if the app's core functionality justifies broad access to
  audio recording.
- **The Policy Spirit**: Unprompted audio recording is a severe privacy breach.
  Apps should request microphone access strictly for user-visible, time-bounded
  actions. Target SDK 34+ encourages using the system-managed Microphone Button
  for temporary needs.
- **Evidence**:
  {{#EACH data_sources.AUDIO}}
  - `{{ITEM}}`
  {{/EACH}}
  **Relevant Permissions Requested**:
  {{#EACH requested_permissions.AUDIO}}
  - `{{ITEM}}`
  {{/EACH}}
  {{#IF TARGET_SDK}}
  **Target SDK**: `{{TARGET_SDK}}`
  {{/IF}}
- **Common Evaluation Matrix**:
  | Target SDK | Audio Recording Trigger Context | Severity | Direct Actionable Recommendation |
  | :--- | :--- | :--- | :--- |
  | **34 or higher** | App requests broad `RECORD_AUDIO` permission for occasional, user-initiated vocal input or short recording. | `IMPORTANT` | Migrate to the **Android Microphone Button** API to process temporary audio securely. |
  | **Any** | App captures audio for secondary features (analytics, user-agent details, etc.) without explicit user control. | `IMPORTANT` | Remove microphone permissions. For general searches, integrate standard Android Speech Recognizer intents. |

- **Domain-Specific Heuristics (Strictly Bounded)**:
  Verify recording indicators and threads:
  1. **Continuous Capture**: Scan for active recorder threads (`AudioRecord` or
     `MediaRecorder`). If recording loops can be active when the app is
     minimized or without a visible user-facing indicator, flag this as a
     `CRITICAL` violation.
{{/IF_ALL}}

{{#IF_ALL requested_permissions.FILES_AND_DOCS, data_sources.FILES_AND_DOCS}}
#### Files and Docs Access Policy (Policy ID: files_and_docs_policy)

- **Goal**: Evaluate if broad file access (non-media) is justified or if the
  Storage Access Framework should be used.
- **The Policy Spirit**: Storage isolation (Scoped Storage) is mandatory on
  modern Android versions. Broad access to shared files is heavily restricted.
  Standard files, documents, and download folders should be navigated using
  scoped contracts to prevent global filesystem snooping.
- **Evidence**:
  {{#EACH data_sources.FILES_AND_DOCS}}
  - `{{ITEM}}`
  {{/EACH}}
  **Relevant Permissions Requested**:
  {{#EACH requested_permissions.FILES_AND_DOCS}}
  - `{{ITEM}}`
  {{/EACH}}
  {{#IF TARGET_SDK}}
  **Target SDK**: `{{TARGET_SDK}}`
  {{/IF}}
- **Common Evaluation Matrix**:
  | Target SDK | Storage Configuration | Justified? | Severity | Direct Actionable Recommendation |
  | :--- | :--- | :--- | :--- | :--- |
  | **30 or higher** | App requests broad `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE` for simple document selection. | No (Violation) | `IMPORTANT` | **Scoped Storage Mandate**: Migrate your document/file selections to the **Storage Access Framework (SAF)** (`Intent.ACTION_OPEN_DOCUMENT`). |

- **Domain-Specific Heuristics (Strictly Bounded)**:
  Critique external directory creation:
  1. **Manual File Sync Heuristic**: Check if the code creates custom root-level
     folders on external storage (e.g.
     `Environment.getExternalStorageDirectory() + "/my_folder"`). If directories
     are created for standard document outputs or logging, flag an `IMPORTANT`
     violation. Direct the developer to utilize scoped storage paths.
{{/IF_ALL}}

## Output schema

Save final JSON output to `{{TEMP_DIR}}/worker_{{GOAL_NAME}}.json`.

```json
{
  "domain": "Permissions and APIs",
  "findings": [
    {
      "policy_id": "STRING_VALUE (The exact Policy ID, e.g., photo_video_access_policy)",
      "issue_summary": "STRING_VALUE",
      "severity": "CRITICAL | IMPORTANT | SUGGESTION",
      "files_involved": ["STRING_VALUE"],
      "evidence": "STRING_VALUE",
      "recommendation": "STRING_VALUE"
    }
  ]
}
```
