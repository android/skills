---
name: android-cli
description: Provides instructions for installing and using the `android` CLI. The
  `android` command-line tool is a critical tool for Android development and helps
  you create new Android projects, install APKs onto devices and emulators and launch
  and run apps on them, manage and interact with Android virtual devices (including
  screenshots and UI inspection), manage Android SDK components, look up official
  Android documentation, and discover and install official Android skills.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-25'
  keywords:
  - android-cli
  - sdk
  - emulator
  - skills
  - docs
  - knowledge base
  - project creation
  - screenshots
---

This skill provides instructions for using the `android` CLI tool. The tool
includes various commands for creating projects, running applications,
interacting with devices, and managing the CLI environment.

## Installation

If the android tool is not in the path, install it. To install run the following
command:

- Linux x86-64: curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh \| bash
- Linux ARM64: curl -fsSL https://dl.google.com/android/cli/latest/linux_arm64/install.sh \| bash
- Mac Arm: curl -fsSL https://dl.google.com/android/cli/latest/darwin_arm64/install.sh \| bash
- Mac Intel: curl -fsSL https://dl.google.com/android/cli/latest/darwin_x86_64/install.sh \| bash
- Windows: curl -fsSL https://dl.google.com/android/cli/latest/windows_x86_64/install.cmd -o "%TEMP%\\i.cmd" \&\& "%TEMP%\\i.cmd"

## SDK management

To manage the installation of Android SDKs and tools, use the `sdk` command.
For example:

- `android sdk install <package>[@<version>]...`: Install specific packages. Multiple packages can be specified, separated by spaces. `<version>` defaults to latest. For example: `android sdk install platforms/android-30@2
  platforms/android-34`
- `android sdk update [<pkg-name>]`: Update a specific package or all packages to the latest version.
- `android sdk remove <pkg-name>`: Remove a package from the local SDK.
- `android sdk list --all`: List installed and available SDK packages.

## Project creation

Create projects from templates using the `create` command.

For example: `android create empty-activity --name="My App" --output=./my-app`

## Interacting with Android devices

Use the `android layout` command to inspect the UI layout of an Android
application in JSON format. Use the `android screen` command to visually inspect
the UI and obtain bounding coordinates for visual regions.
`android layout` dumps the UI as JSON. `android screen capture -o <file path>` saves a PNG.
Both auto-select the device when only one is connected; with more than one, pass `--device=<serial>` (use `adb devices` to list serials).

### UI Dump

`android layout` returns a JSON tree of the UI elements on screen.

Each JSON object represents a UI element in the Android app. The following properties may be present:
- `text` - any literal text the element contains
- `resource-id` - the Android resource id used to refer to the element
- `content-desc` - a description of a UI element for use by accessibility tools
- `interactions` - the set of user interactions the element supports. May contain one or more of: `CHECKABLE`, `CLICKABLE`, `FOCUSABLE`, `SCROLLABLE`, `LONG-CLICKABLE`, `PASSWORD`
- `state` - the set of states the element is in. May contain one or more of `CHECKED`, `FOCUSED`, `SELECTED`
- `bounds` - the screen coordinates of the bounding rectangle of the element, in the format `[min X,min Y][max X, max Y]`
- `center` - the screen coordinates of the center of the element, in the format `[x,y]`
- `off-screen` - if true, the element is in the UI hierarchy but not within the bounds of the screen; it may require scrolling to view.
- `hidden` - if true, the element is in the UI hierarchy but not visible or interactive on screen.

Use `layout` as a primary means of examining an Android app.

By default, `layout` only returns elements that have content or are interactive:
- elements with non-empty `text` or `content-desc` properties
- elements with non-empty `interactions` or `state`
To return all elements, use the `--full` flag

If `layout` continues to fail, use `android screen capture --annotate` to inspect the app.

### Screenshot

`android screen capture -o <file path>` (or `--output=<file path>`) saves a PNG of the current device screen to `<file path>`.

Use `screen capture` as a secondary means of examining an Android app
Examples:
- Understanding the content of an on-screen image
- Looking at a `WebView` (web content does not always appear in the ui dump)
- Trying to find a UI element by its visual appearance

**IMPORTANT** : Always *VISUALLY* examine the PNG image returned from any `android screen` command BEFORE doing anything else.

#### Annotated Screenshot

`android screen capture --annotate -o <file path>` (or `-a -o <file path>`)
`android screen resolve --screenshot <path> --string <string>`

The `--annotate` command adds numerical labels and bounding boxes around UI elements. Use this command to locate UI elements that cannot
be located in the `layout` output.

To refer to these labels in input commands, use `screen resolve` to convert labels into coordinates:

`android screen resolve --screenshot <file path> --string "#3"` returns `<x coord of region 3> <y coord of region 3>`

To save turns, you can combine shell commands:

`adb shell input $(android screen resolve --screenshot screen.png --string "tap #34")`

This command taps on region #34 from `screen.png`

### Input

Use `adb shell input` for interacting with Android devices.
Refer to the `"interactions"` property of an element for what interactions can be performed on a particular element.

Interact with UI elements with their `center` coordinate or their `bounds` coordinates:
`json
{
"key": -248568265,
"class": "android.widget.Button",
"bounds": "[138,9][167,38]",
"center": "[152,23]"
}`
To tap on this button, you would execute `adb shell input tap 152 23`. This taps the center.

    {
      "key": 12487234,
      "class": "com.example.ui.ScrollableList",
      "bounds": "[100,200][400,600]",
      "center": "[250,400]"
    }

To scroll down on this list, you would execute `adb shell input swipe 250 400 250 200 500`. This swipes from the center to the top over 500ms.

#### Text Input

To enter text, ensure the field is focused and execute `adb shell input text "<text>"`.

- **Spaces (`%s`):** Replace spaces with `%s` (Android ignores text after raw spaces): `adb shell input text "Hello%sworld"` (enters `"Hello world"`).
- **Special characters:** Escape shell metacharacters (`&`, `$`, `(`, `)`, `!`): `adb shell input text "AT\&T"` or `adb shell input text 'Price:\$10'`.
- **Submit / Enter:** `adb shell input keyevent 66`

### Android Interaction Rules

1. Always ensure text input fields have `"FOCUSED"` in their `"state"` list before entering text.
2. If an element has `"SCROLLABLE"` in its `"interactions"` list, try scrolling it when looking for missing UI elements.
3. Always scroll slowly when executing scroll inputs. The 5th argument to `adb shell input swipe` controls scroll duration.
4. Content may take time to load; if a `layout` is missing information after you take an action, wait a few seconds, then perform `layout` again to see if anything changes.

## Running journey tests

Journey tests involve device interaction; use the `android layout` and `android
screen` commands to evaluate Journeys. The instructions for device interaction
apply here as well.

A journey is an XML-specified test of an Android app's behavior. It consists of a list of `<action>` elements. For example:
`xml
<journey name="My Journey">
<description>
A sample journey to illustrate the format
</description>
<actions>
<action>
Tap the "Home" icon
</action>
<action>
Verify that the app is on its Home screen
</action>
</actions>
</journey>`

Evaluate a journey by proceeding through the `<actions>` list in sequential order. Evaluate each `<action>` block individually.
A journey succeeds if all elements in the `<actions>` list succeed.

A journey is a test case for an app. The journey XML is the source of truth; if the app disagrees with the journey, the app has failed.
Additionally, if the app exits, crashes, or freezes, journey evaluation stops and the journey fails.

**IMPORTANT** - Execute each step EXACTLY as written, and independently of other steps! If an action says to `"tap the first search result"`,
you MUST find the search results and tap the first one. Do this even if you believe you know the intent behind the action.

### Taking Actions

Some `<action>` elements specify UI interactions to perform on the running Android app. Perform the interaction and verify that the app does
not crash or behave in an unexpected manner. This is the *only* verification you should perform for an `<action>`.

If the interaction cannot be performed as specified, the journey fails.
Example:
`<action>Click the red button</action>`
If you determine a red button is not present in the UI, the journey fails.

If the text of an `<action>` specifies a list of actions, break it into sub-actions and evaluate them individually:
Example:
`<action>Search for soda and add the first result to the cart</action>`
This should be evaluated as:
`<action>Search for soda</action>
<action>Add the first result to the cart</action>`

If an `<action>` contains something that is not a specification for a UI interaction, alert the user that the journey is malformed and exit
early, specifying the error in question.

### Verifying Expectations

`<action>` elements that begin with "check" or "verify" specify expectations for the current state of the Android app. Determine the current
state of the app and check if the expectations are met.

Determine the current state of the app by inspecting the current screen of the device without interacting with it.
Example:
`<action>Check if "Switch 2" is visible on the screen</action>`
This requires only inspecting the current screen, not scrolling or interacting. If "Switch 2" is not currently visible, the action fails.

If the expectations are not met, mark the `<action>` as a failure and the journey evaluation ends. A single `<action>` may contain
multiple expectations.
Example:
`<action>Verify that the app is on the Home screen, the Home icon is blue, and the temperature is displayed</action>`
This `<action>` fails if ANY of the following are false:
- The app is on the Home screen
- There is a Home icon, and it is blue
- A temperature is displayed

### Handling failure

When running a journey, evaluate it as a test. Failure is acceptable, and often expected. Proper reporting of failures is the priority.

Keep debugging and troubleshooting to a minimum; assume that tools are showing you the correct output every time. The goal is to determine
if the *current* Android app can correctly handle the *current* steps outlined in the journey. Suggestions for bug fixes, clarification, or
other improvements should be kept to journey evaluation summary at the end.

### Summarizing

After evaluating the whole journey, output a markdown (.md) file describing the results.

The file should have the following format:

    # Journey: <Name of the journey>

    ## Results

      <!-- Append "✅" to the action header if the instruction was evaluated or "❌" if the instruction could not be evaluated -->
    ### Action: Click the blue button ✅
    - **Commands**:
      <!-- A list of the ADB commands executed while evaluating the instruction -->
      - `adb shell input swipe 490 200 500 500 500`
      - `adb shell input tap 45 920`

      <!-- Failure reasons, feedback, or other useful information -->
    - **Comment**: The journey step doesn't specify that the button requires scrolling to see

    ### Action: Check if "Switch 2" is visible on the screen ✅
    - **Screenshot**: [switch_2_screenshot.png](path/to/screenshot)
    - **Comment**: Confirmed both in layout and visually

    ### Action: The home screen is shown ❌
    - **Comment**: The settings page was shown

    <!-- Append nothing to the action header if the action was not evaluated because the journey execution ended early -->
    ### Action: Swipe down to view active notifications

## Doc searching

The `docs` command searches authoritative, high-quality Android developer
documentation in the Android Knowledge Base. By providing a few keywords, this
tool will return high quality articles that contain examples or guidance on how
to use Android APIs or libraries. Use this tool to obtain additional information
on how to achieve Android-specific tasks or to know more about Android APIs,
surfaces, libraries, or devices.

Always use this tool to get the most up-to-date information about Android
concepts. Typical good use cases are:
- Finding migration guides for APIs.
- Finding examples for APIs.
- Finding up-to-date information about Android APIs.
- Finding best practices for Android concepts.

## Running APKs

Use the `run` command to run Android apps.

## Managing emulators

Manage Android Virtual Devices (AVDs) using the `android emulator` command

## Managing skills

Manage agent skills for Android using the `android skills` command.

## Updating the CLI

Update the Android CLI using the `android update` command.

## `android help` output

    Usage: android [-Vhv] [--sdk=PARAM] [COMMAND]
      -h, --help       Shows the help message for the specified command
          --sdk=PARAM  Path to the Android SDK
      -v, --verbose    Enable verbose output for troubleshooting
      -V, --version    Print version information and exit
    Commands:
      analyze     Analyzes the file(s) (AAB, APK, JAR, AAR) at the given location(s)
                  and reports back all available details
      auth        Authentication commands. Log in to and out of Google services for
                  Android CLI
      build       Build android artifacts
      completion  Installs shell autocomplete configuration for Android CLI in the
                  current user profile
      create      Creates a new Android project from available templates. You can
                  specify the project name, output directory, `minSdk` value, and
                  dry-run execution
      describe    Analyzes an Android project to generate descriptive metadata. This
                  command identifies and outputs the paths to JSON files that detail
                  the project's structure, including build targets and their
                  corresponding output artifact locations (such as APKs). This
                  information enables other tools and commands to locate build
                  artifacts efficiently
      device      Manage Android devices. Mirror connected devices or emulators in a
                  desktop window and manage remote physical device reservations
      docs        Searches and fetches developer documentation from the official
                  Android Knowledge Base
      emulator    Manages Android Virtual Devices (AVDs). Includes commands to
                  start, stop, list, and view details about emulators
      help        Shows the help information for a specified command
      info        Prints environment information including SDK location and
                  configuration variables
      init        Initializes the environment for Android CLI. Sets up required
                  configurations, directories, and default skills
      input       Interact with Android apps by performing gestures and key events
                  on UI elements
      install     Installs an Android app (one or more APKs) on a connected device
                  or emulator without activating any components, using incremental
                  optimizations for faster deployment than `adb`
      layout      Returns the layout tree of an app
      preview     Renders Jetpack Compose @Preview functions to PNG images and
                  optionally exports component tree JSON files. Runs without a
                  device or an IDE
      profiler    Profiles Android apps, games, and devices
      run         Deploys and launches an Android app on a connected device or
                  emulator
      screen      Captures and inspects the screen of a connected Android device or
                  emulator
      sdk         Manages the Android SDK installation. Includes commands to
                  install, update, remove, and list available and installed SDK
                  packages
      signals     Queries app stability and field telemetry from Crashlytics
      skills      Manages Android CLI skills. Includes commands to install, remove,
                  list, and search for skills by keyword
      studio      Connects Android CLI to a running Android Studio instance to
                  analyze files, find declarations and usages, render Compose
                  previews, and look up library versions
      update      Updates Android CLI to the latest version

    analyze
              Usage: android analyze [-h] [--analysis=PARAM] [--format=PARAM] <path>
              Analyzes the file(s) (AAB, APK, JAR, AAR) at the given location(s) and
              reports back all available details
              
              Options:
                    --analysis=PARAM  Analysis category to filter output (basic,
                                      optimization). Supports comma-separated values
                                      (default: all)
                -f, --format=PARAM    Output format: json, textproto. If omitted,
                                      output is formatted for human readability
                -h, --help            Shows the help message for the specified
                                      command
              
              android options:
                    --sdk=PARAM       Path to the Android SDK
                -v, --verbose         Enable verbose output for troubleshooting
                -V, --version         Print version information and exit
              
              Positional Parameters:
                <path>  Path to the target file(s) to analyze (AAB, APK, JAR, AAR)

    auth
              Usage: android auth [-h] [COMMAND]
              Authentication commands. Log in to and out of Google services for
              Android CLI
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                login   Log in to Google services for Android CLI
                logout  Log out from Google services for Android CLI
                status  Prints the currently signed-in account

    build
              Usage: android build [-h] [<reference>] [-- <build_args>]
              Build android artifacts
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              
              Positional Parameters:
                <reference>      Build reference ([system://][path]?[target])
                -- <build_args>  Arguments passed verbatim to the build system. Use
                                 -- to separate from other arguments

    completion
              Usage: android completion [-h] [<shell>]
              Installs shell autocomplete configuration for Android CLI in the
              current user profile
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              
              Positional Parameters:
                <shell>  If provided, prints the completion configuration script for
                         the specified shell without installing it. Supports Bash
                         and Zsh

    create
              Usage: android create [-h] [--application-id=PARAM] [--force] [--list]
                                    [--min-sdk=PARAM] [--name=PARAM] [--namespace=PARAM]
                                    [--output=PARAM] [<template-name>]
              Creates a new Android project from available templates. You can
              specify the project name, output directory, `minSdk` value, and
              dry-run execution
              
              Options:
                    --application-id=PARAM  The app ID for the app, for example
                                            `com.example.myapp`
                    --force                 Allow creating the project in a
                                            non-empty directory, overwriting
                                            existing files
                -h, --help                  Shows the help message for the specified
                                            command
                    --list                  List all available templates
                    --min-sdk=PARAM         The `minSdk` value supported by the app
                                            (the default value is defined in the
                                            template)
                    --name=PARAM            The name of the app, for example `My
                                            Application`
                    --namespace=PARAM       The namespace for resources and package
                                            name for Kotlin source files
                -o, --output=PARAM          The destination project directory path
                                            (the default value is `.`)
              
              android options:
                    --sdk=PARAM             Path to the Android SDK
                -v, --verbose               Enable verbose output for
                                            troubleshooting
                -V, --version               Print version information and exit
              
              Positional Parameters:
                <template-name>  The template name

    describe
              Usage: android describe [-h] [--project_dir=PARAM]
              Analyzes an Android project to generate descriptive metadata. This
              command identifies and outputs the paths to JSON files that detail the
              project's structure, including build targets and their corresponding
              output artifact locations (such as APKs). This information enables
              other tools and commands to locate build artifacts efficiently
              
              Options:
                -h, --help               Shows the help message for the specified
                                         command
                    --project_dir=PARAM  The project directory to describe
              
              android options:
                    --sdk=PARAM          Path to the Android SDK
                -v, --verbose            Enable verbose output for troubleshooting
                -V, --version            Print version information and exit

    device
              Usage: android device [-h] [COMMAND]
              Manage Android devices. Mirror connected devices or emulators in a
              desktop window and manage remote physical device reservations
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                hub     Launches a desktop window for real-time screen mirroring and
                        interactive control of a connected Android device or
                        emulator
                remote  Manage remote physical devices. Includes commands to list,
                        reserve, connect to, and manage remote device reservations

    docs
              Usage: android docs [-h] [COMMAND]
              Searches and fetches developer documentation from the official Android
              Knowledge Base
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                fetch   Fetches an Android documentation article from a URL starting
                        with `kb://`
                search  Searches Android documentation. Enclose keywords in quotes

    emulator
              Usage: android emulator [-h] [COMMAND]
              Manages Android Virtual Devices (AVDs). Includes commands to start,
              stop, list, and view details about emulators
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                create  Creates a virtual device
                list    Lists available virtual devices
                remove  Deletes a virtual device
                start   Launches the specified virtual device. This command returns
                        when the emulator is fully started and ready to use
                stop    Stops the specified virtual device

    help
              Usage: android help [-h] [COMMAND]
              Shows the help information for a specified command
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              
              Positional Parameters:
                COMMAND  The command to show help for

    info
              Usage: android info [-h] [<field>]
              Prints environment information including SDK location and
              configuration variables
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              
              Positional Parameters:
                <field>  The specific field to print the value of (sdk, version,
                         launcher_version). If omitted, prints all fields

    init
              Usage: android init [-h]
              Initializes the environment for Android CLI. Sets up required
              configurations, directories, and default skills
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit

    input
              Usage: android input [-h] [--device=PARAM] [COMMAND]
              Interact with Android apps by performing gestures and key events on UI
              elements
              
              Options:
                    --device=PARAM  Device serial number or emulator ID (as reported
                                    by 'adb devices')
                -h, --help          Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM     Path to the Android SDK
                -v, --verbose       Enable verbose output for troubleshooting
                -V, --version       Print version information and exit
              Commands:
                key     Press a hardware button, navigation key, or Android keycode
                scroll  Scroll a container element, optionally until a target
                        element appears
                swipe   Swipe from a UI element matching a selector in a given
                        direction
                tap     Tap on a UI element matching a selector
                text    Type text into a UI element matching a selector

    install
              Usage: android install [-h] [--apks=PARAM] [--device=PARAM] [--install-options=PARAM]
                                     [--use-delta-install] [<apks_or_reference>] [--
                                     <build_args>]
              Installs an Android app (one or more APKs) on a connected device or
              emulator without activating any components, using incremental
              optimizations for faster deployment than `adb`
              
              Options:
                    --apks=PARAM             The paths to the APKs, comma separated
                    --device=PARAM           The device serial number
                -h, --help                   Shows the help message for the
                                             specified command
                    --install-options=PARAM  Additional options or flags to pass to
                                             package manager install (for example,
                                             `-g`, `-d`)
                    --use-delta-install      Use fast delta install (speeds up
                                             incremental updates by transferring
                                             only modified code and resources;
                                             default: `true`)
              
              android options:
                    --sdk=PARAM              Path to the Android SDK
                -v, --verbose                Enable verbose output for
                                             troubleshooting
                -V, --version                Print version information and exit
              
              Positional Parameters:
                <apks_or_reference>  APK file path(s) or a build reference
                                     ([system://][path]?[target])
                -- <build_args>      Arguments passed verbatim to the build system.
                                     Use -- to separate from other arguments

    layout
              Usage: android layout [-dhp] [--device=PARAM] [--flat] [--full] [--no-idle]
                                    [--output=PARAM]
              Returns the layout tree of an app
              
              Options:
                    --device=PARAM  The device serial number
                -d, --diff          Deprecated; no-op flag. Will be removed in a
                                    future release
                    --flat          Returns a flat list instead of a tree
                    --full          Returns the full tree, including non-interactive
                                    and hidden elements
                -h, --help          Shows the help message for the specified command
                    --no-idle       Don't wait for layout idle state when fetching
                                    layout
                -o, --output=PARAM  Writes the layout to the specified file or
                                    directory. If omitted, prints to standard output
                -p, --pretty        Pretty-prints the returned JSON
              
              android options:
                    --sdk=PARAM     Path to the Android SDK
                -v, --verbose       Enable verbose output for troubleshooting
                -V, --version       Print version information and exit

    preview
              Usage: android preview [-h] [COMMAND]
              Renders Jetpack Compose @Preview functions to PNG images and
              optionally exports component tree JSON files. Runs without a device or
              an IDE
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                list    Lists available Jetpack Compose @Preview functions in a
                        target
                render  Builds a target and renders its Jetpack Compose @Preview
                        functions to PNG images and optional component tree JSON
                        files

    profiler
              Usage: android profiler [-h] [COMMAND]
              Profiles Android apps, games, and devices
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                heapdump     Capture Android JVM heap dump snapshots (requires API
                             29+)
                session      Manage profiling sessions of any type: list them, query
                             their status, or terminate them
                systemtrace  System tracing tools. Captures CPU scheduling, thread
                             slices, frame lifecycle, GPU counters, and Vulkan
                             timing into a Perfetto trace file

    run
              Usage: android run [-h] [--activity=PARAM] [--apks=PARAM] [--debug] [--device=PARAM]
                                 [--install-options=PARAM] [--type=PARAM] [--use-delta-install]
                                 [<apks_or_reference>] [-- <build_args>]
              Deploys and launches an Android app on a connected device or emulator
              
              Options:
                    --activity=PARAM         The activity name
                    --apks=PARAM             The paths to the APKs, comma separated
                    --debug                  Run in debug mode
                    --device=PARAM           The device serial number
                -h, --help                   Shows the help message for the
                                             specified command
                    --install-options=PARAM  Additional options or flags to pass to
                                             package manager install (for example,
                                             `-g`, `-d`)
                    --type=PARAM             The component type (`ACTIVITY`,
                                             `WATCH_FACE`, `TILE`, `COMPLICATION`,
                                             `DECLARATIVE_WATCH_FACE`,
                                             `WEAR_WIDGET`)
                    --use-delta-install      Use fast delta install (speeds up
                                             incremental updates by transferring
                                             only modified code and resources;
                                             default: `true`)
              
              android options:
                    --sdk=PARAM              Path to the Android SDK
                -v, --verbose                Enable verbose output for
                                             troubleshooting
                -V, --version                Print version information and exit
              
              Positional Parameters:
                <apks_or_reference>  APK file path(s) or a build reference
                                     ([system://][path]?[target])
                -- <build_args>      Arguments passed verbatim to the build system.
                                     Use -- to separate from other arguments

    screen
              Usage: android screen [-h] [COMMAND]
              Captures and inspects the screen of a connected Android device or
              emulator
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                capture  Outputs the device screen to a PNG
                resolve  Targets UI elements visually. Substitutes bounding box
                         coordinates from an annotated screenshot into a string,
                         replacing all instances of `#N` with the center coordinates
                         of the bounding box labeled `N`

    sdk
              Usage: android sdk [-h] [--ignore-outdated-xmls] [--platform=PARAM]
                                 [COMMAND]
              Manages the Android SDK installation. Includes commands to install,
              update, remove, and list available and installed SDK packages
              
              Options:
                -h, --help                  Shows the help message for the specified
                                            command
                    --ignore-outdated-xmls  When installing or updating packages, do
                                            not update other packages' XML files
                    --platform=PARAM        Target platform `<os>_<arch>` (for
                                            example, `linux_x86_64`, `mac_arm64`, or
                                            `windows_x86`); defaults to the current
                                            host
              
              android options:
                    --sdk=PARAM             Path to the Android SDK
                -v, --verbose               Enable verbose output for
                                            troubleshooting
                -V, --version               Print version information and exit
              Commands:
                install  Installs SDK packages
                list     Lists installed and available SDK packages
                remove   Removes packages from the SDK
                update   Updates one or all packages to the latest version

    signals
              Usage: android signals [-h] [COMMAND]
              Queries app stability and field telemetry from Crashlytics
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                context  Lists accessible Crashlytics projects and apps
                crashes  Lists, describes, and analyzes field-reported crash, ANR,
                         and non-fatal issues
                heaps    Lists, describes, and downloads heap dumps collected on
                         low-memory events

    skills
              Usage: android skills [-h] [COMMAND]
              Manages Android CLI skills. Includes commands to install, remove,
              list, and search for skills by keyword
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                add     Installs a specific skill by its ID to your environment
                find    Searches for available skills in the repository matching a
                        keyword
                list    Lists installed and available skills
                remove  Removes an installed skill by its ID
                update  Updates installed skills

    studio
              Usage: android studio [-h] [COMMAND]
              Connects Android CLI to a running Android Studio instance to analyze
              files, find declarations and usages, render Compose previews, and look
              up library versions
              
              Options:
                -h, --help       Shows the help message for the specified command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                analyze-file            Analyzes a file in Android Studio
                check                   Checks the status of running Android Studio
                                        instances
                find-declaration        Finds the declaration of a symbol
                find-usages             Finds usages of a symbol
                open-file               Opens a file in Android Studio
                render-compose-preview  Renders a Compose preview in Android Studio
                version-lookup          Looks up the latest available versions of
                                        Maven artifacts, Android versions, and SDK
                                        tools

    update
              Usage: android update [-h] [--url=PARAM]
              Updates Android CLI to the latest version
              
              Options:
                -h, --help       Shows the help message for the specified command
                    --url=PARAM  The URL to download the update from
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
