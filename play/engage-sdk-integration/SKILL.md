---
name: engage-sdk-integration
description: Helps developers integrate, debug, and resolve Play Engage SDK implementation
  issues. Use when adding Engage SDK support, generating publishing code, mapping
  data classes to entities, or fixing SDK-related errors.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-07-18'
  keywords:
  - android
  - engage
  - engage sdk
  - play engage library
  - google play
---

This skill guides you through integrating the Play Engage SDK into an Android
app. It ensures that the code follows the mandatory structure and uses the
required Engage entities for each vertical.

## Workflow

Follow these steps to assist the developer:

1. **Identify vertical and cluster:**

   - Ask the developer which vertical their app belongs to based on **[references/schemas/](references/schemas)**.
   - Check if the integration is for TV or mobile. If the integration is for TV, read the TV-specific sections in [patterns.md](references/patterns.md) as well.
   - Use `{VERTICAL}.md` in the **[references/schemas/](references/schemas)** directory to identify the corresponding Engage entities and the `client` class name. The `client` field in the JSON provides the full class name. For example, `com.google.android.engage.food.service.AppEngageFoodClient`.
   - **Note:** Initializing the client class requires a `Context` parameter. For example, `AppEngageFoodClient(context)`.
   - Always refer to [common.md](references/common.md) for common entities.
   - Ask which cluster type they want to publish from the supported cluster types for that vertical.
   - Find the method to call from `{VERTICAL}.md` in the **[references/schemas/](references/schemas)** directory for the specified cluster. Each method will specify the request it expects.
   - Get the request structure from [requests.md](references/requests.md) and clusters from [clusters.md](references/clusters.md). Then suggest and use sources to fill the fields in the request structure correctly, along with the required entities and clusters.
2. **Generate structured boilerplate code:**

   - Create a new directory for all Engage-related code. Name the directory to match the naming convention of the existing codebase.
   - Generate the following classes using templates in [patterns.md](references/patterns.md):
     - `Constants`: holds constant values such as attempt counts and publish types.
     - `ItemToEntityConverter`: converts the app's local models to Engage's Entity models.
     - `ClusterRequestFactory`: constructs the publish requests.
     - `EngageWorker`: handles the actual publishing and publish errors using WorkManager.
     - `EngagePublisher`: orchestrates periodic and one-time jobs.
     - `EngageBroadcastReceiver`: listens for AppEngageService intents and starts a one-time publish job from `EngagePublisher`. **Important** : Implement both **static registration** and **dynamic registration** patterns, including the companion object `register` method inside the `EngageBroadcastReceiver` class.
3. **Suggest entity mapping:**

   - Ask the developer to provide their local model schema (for example, a data class or a JSON snippet).
   - If they haven't provided one, share entities from `{VERTICAL}.md` in the **[references/schemas/](references/schemas)** directory as a guide.
   - Once the local model is identified, suggest a mapping to the corresponding Engage entity.
   - Generate the conversion logic using the `ItemToEntityConverter` pattern in [patterns.md](references/patterns.md) and add it to the generated `{ENGAGE_CODE_DIR}/ItemToEntityConverter`.
4. **Suggest data source:**

   - Ask the developer to provide the source of actual data you'll publish.
   - Once you identify the data source, use it to fetch the data in the app's local model schema.
   - Use `{ENGAGE_CODE_DIR}/ItemToEntityConverter` to convert this data to an Engage entity.
   - Use obtained Engage entity model data with `{ENGAGE_CODE_DIR}/
     ClusterRequestFactory` to get cluster requests.
   - Call corresponding cluster publishing method obtained from `{VERTICAL}.md` in the **[references/schemas/](references/schemas)** directory with the obtained request in previous step in `{ENGAGE_CODE_DIR}/EngageWorker`.
5. **Gradle and manifest updates:**

   - Suggest updates to `build.gradle` and `AndroidManifest.xml`.
   - For mobile apps, use [patterns.md](references/patterns.md).
   - For TV apps, use the TV-specific sections in [patterns.md](references/patterns.md).
   - Provide the necessary `implementation` dependencies for `build.gradle` or `build.gradle.kts` from [patterns.md](references/patterns.md).
   - Provide the `<receiver>` and `<service>` declarations for `AndroidManifest.xml`.
   - Note: Except for TV, there aren't any vertical-specific imports. For all other verticals, `com.google.android.engage:engage-core:1.6.0` is sufficient.
6. **Debugging:**

   - Perform a Gradle sync.
   - If errors occur, follow this resolution order:
     - Fix import errors. For package `com.google.android.engage` or classes starting with `AppEngage`, verify the package name in the `{VERTICAL}.md` in **[references/schemas/](references/schemas)** directory or [common.md](references/common.md).
     - Fix any other errors.
   - Execute a full Gradle build and resolve any remaining compilation issues. Repeat this step until the Gradle build is successful.
7. **User checklist:** At the end of code generation, notify the user to go
   through this checklist to verify that the integration is complete and as
   intended:

   - \[ \] Verify that all the Engage-related files are created in `{ENGAGE_CODE_DIR}/`:
     - `Constants`
     - `ItemToEntityConverter`
     - `ClusterRequestFactory`
     - `EngageWorker`
     - `{cluster_type}Publisher`
     - `EngageBroadcastReceiver`
   - \[ \] Verify that app's local model is converted to Engage entity by populating the fields correctly in the model in `{ENGAGE_CODE_DIR}/ItemToEntityConverter`.
   - \[ \] Verify that all image URIs in `ItemToEntityConverter` point to images matching the strict aspect ratio requirements of the vertical (for example, 16:9, 1:1, 2:3).
   - \[ \] Verify that `{ENGAGE_CODE_DIR}/EngageWorker` uses the data source identified in Step 4.
   - \[ \] Verify that `EngageBroadcastReceiver.register(context)` is called within the `Application` class or `MainActivity` to register the receiver dynamically.
   - \[ \] Verify that `AndroidManifest.xml` contains the static `<receiver>` declaration for `EngageBroadcastReceiver` with the necessary intent actions.
   - **Important** : Explicitly instruct the developer to call `EngageBroadcastReceiver.register(context)` inside their custom `Application` class `onCreate()` (or their main activity `onCreate()`) to dynamically register the receiver. Stress that **both** static and dynamic registrations are required for the integration to function.

## Reference materials

- **FAQ:** [Engage FAQ](references/android/guide/playcore/engage/faq.md) - Refer to this document for answers to frequently
  asked questions from developers.

- **Vertical-specific guides:**

  - [Food Vertical](references/android/guide/playcore/engage/food.md)
  - [Watch Vertical](references/android/guide/playcore/engage/watch.md)
  - [Listen Vertical](references/android/guide/playcore/engage/listen.md)
  - [Read Vertical](references/android/guide/playcore/engage/read.md)
  - [Shopping Vertical](references/android/guide/playcore/engage/shopping.md)
  - [Social Vertical](https://developer.android.com/guide/playcore/engage/social)
  - [Travel Vertical](references/android/guide/playcore/engage/travel.md)
  - [Health and Fitness Vertical](references/android/guide/playcore/engage/healthandfitness.md)
  - [Other Verticals](references/android/guide/playcore/engage/otherverticals.md)
  - [TV Getting Started](references/android/guide/playcore/engage/tv/getting-started.md)
  - [TV Recommendations](references/android/guide/playcore/engage/tv/recommendations.md)
  - [TV Continue Watching](references/android/guide/playcore/engage/tv/continue-watching/index.md)
  - [TV Entitlements](references/android/guide/playcore/engage/tv/entitlements.md)
- **Vertical-specific schemas:**

  - [Food Schema](references/schemas/food.md)
  - [Watch Schema](references/schemas/watch.md)
  - [Listen Schema](references/schemas/listen.md)
  - [Read Schema](references/schemas/read.md)
  - [Shopping Schema](references/schemas/shopping.md)
  - [Social Schema](references/schemas/social.md)
  - [Travel Schema](references/schemas/travel.md)
  - [TV Schema](references/schemas/tv.md)
  - [Other Schema](references/schemas/other.md)
