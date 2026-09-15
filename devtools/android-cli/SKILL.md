---
name: android-cli
description: Provides instructions for installing and using the `android` CLI. The
  `android` command-line tool is a critical tool for Android development and helps
  you create new Android projects, run Android apps on devices, manage and interact
  with Android virtual devices (including screenshots and UI inspection), manage Android
  SDK components, look up official Android documentation, and discover and install
  official Android skills.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-09-12'
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

- Linux: curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/install.sh \| bash
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

**IMPORTANT:** Before using `android layout` or `android screen`, you MUST read
this reference: [interact.md](references/interact.md) The reference file
[interact.md](references/interact.md) contains instructions for properly
interacting with Android devices. Follow its instructions exactly.

### Running journey tests

Journey tests involve device interaction; use the `android layout` and `android
screen` commands to evaluate Journeys. The instructions for device interaction
apply here as well.

**IMPORTANT:** Before evaluating a journey, you MUST read this reference:
[journeys.md](references/journeys.md) The reference file
[journeys.md](references/journeys.md) contains instructions for correctly
evaluating an Android journey. Follow its instructions exactly.

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

## Capturing screenshots

Capture an image of the current screen of a connected Android device and output
it to a file using the `android screen` command.

## Managing skills

Manage agent skills for Android using the `android skills` command.

## Updating the CLI

Update the Android CLI using the `android update` command.

## `android help` output

    Usage: android [-Vhv] [--sdk=PARAM] [COMMAND]
      -h, --help       Show the help message for this command
          --sdk=PARAM  Path to the Android SDK
      -v, --verbose    Enable verbose output for troubleshooting
      -V, --version    Print version information and exit
    Commands:
      completion  Installs shell autocomplete configuration for the Android CLI in
                  the current user profile
      create      Create a new Android project from available templates. Allows
                  specifying project name, output directory, minSdk, and dry-run
                  execution
      describe    Analyzes an Android project to generate descriptive metadata. This
                  command identifies and outputs the paths to JSON files that detail
                  the project's structure, including build targets and their
                  corresponding output artifact locations (e.g., APKs). This
                  information enables other tools and commands to locate build
                  artifacts efficiently
      docs        Android documentation commands for searching and fetching
                  developer documentation from the official knowledge base
      emulator    Manage Android Virtual Devices (AVDs). Includes commands to start,
                  stop, list, and view details about emulators
      help        Shows the help of all commands
      info        Print environment information including SDK location, connected
                  devices, and configuration variables. Use specific fields to
                  narrow the output
      init        Initializes the environment for the Android CLI. Sets up required
                  configurations, directories, and default skills
      install     Install an Android Application (one or more APKs) to a connected
                  device or emulator without activating any components using
                  incremental optimizations for faster deployment than adb
      layout      Returns the layout tree of an application
      run         Build, deploy, and launch an Android application on a connected
                  device or emulator
      screen      Commands to view the device
      sdk         Manage the Android SDK installation. Includes commands to install,
                  update, remove, and list available and installed SDK packages
      skills      Manage Android CLI skills. Includes commands to install, remove,
                  list, and search for skills by keyword
      studio      Android Studio commands
      update      Update the Android CLI to the latest version

    completion
              Usage: android completion [-h] [<shell>]
              Installs shell autocomplete configuration for the Android CLI in the
              current user profile
              
              Options:
                -h, --help       Show the help message for this command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              
              Positional Parameters:
                <shell>  If provided, print the given shell completion without
                         installing. Supported: bash, zsh

    create
              Usage: android create [-h] [--applicationId=PARAM] [--list] [--minSdk=PARAM]
                                    [--name=PARAM] [--namespace=PARAM] [--output=PARAM]
                                    [<template-name>]
              Create a new Android project from available templates. Allows
              specifying project name, output directory, minSdk, and dry-run
              execution
              
              Options:
                    --applicationId=PARAM  The application ID for the application
                                           (e.g. 'com.example.myapp')
                -h, --help                 Show the help message for this command
                    --list                 List all available templates
                    --minSdk=PARAM         The 'minSdk' supported by the application
                                           (default is defined in the template)
                    --name=PARAM           The name of the application (e.g. 'My
                                           Application')
                    --namespace=PARAM      The namespace for resources and package
                                           name for Kotlin source files
                -o, --output=PARAM         The destination project directory path
                                           (default is '.')
              
              android options:
                    --sdk=PARAM            Path to the Android SDK
                -v, --verbose              Enable verbose output for troubleshooting
                -V, --version              Print version information and exit
              
              Positional Parameters:
                <template-name>  The template name

    describe
              Usage: android describe [-h] [--project_dir=PARAM]
              Analyzes an Android project to generate descriptive metadata. This
              command identifies and outputs the paths to JSON files that detail the
              project's structure, including build targets and their corresponding
              output artifact locations (e.g., APKs). This information enables other
              tools and commands to locate build artifacts efficiently
              
              Options:
                -h, --help               Show the help message for this command
                    --project_dir=PARAM  The project directory to describe
              
              android options:
                    --sdk=PARAM          Path to the Android SDK
                -v, --verbose            Enable verbose output for troubleshooting
                -V, --version            Print version information and exit

    docs
              Usage: android docs [-h] [COMMAND]
              Android documentation commands for searching and fetching developer
              documentation from the official knowledge base
              
              Options:
                -h, --help       Show the help message for this command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                search  Search Android documentation. Enclose keywords in quotes
                fetch   Fetch an Android documentation article from a URL (kb://...)

    emulator
              Usage: android emulator [-h] [COMMAND]
              Manage Android Virtual Devices (AVDs). Includes commands to start,
              stop, list, and view details about emulators
              
              Options:
                -h, --help       Show the help message for this command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                create  Creates a virtual device
                start   Launches the specified virtual device. This command will
                        return when the emulator is fully started and ready to use
                stop    Stops the specified virtual device
                list    Lists available virtual devices
                remove  Delete a virtual device

    help
              Usage: android help [-h] [COMMAND]
              Shows the help of all commands
              
              Options:
                -h, --help       Show the help message for this command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              
              Positional Parameters:
                COMMAND  The command to show help for

    info
              Usage: android info [-h] [<field>]
              Print environment information including SDK location, connected
              devices, and configuration variables. Use specific fields to narrow
              the output
              
              Options:
                -h, --help       Show the help message for this command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              
              Positional Parameters:
                <field>  The specific field to print the value of. If omitted print
                         all

    init
              Usage: android init [-h]
              Initializes the environment for the Android CLI. Sets up required
              configurations, directories, and default skills
              
              Options:
                -h, --help       Show the help message for this command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit

    install
              Usage: android install [-h] [--apks=PARAM] [--device=PARAM] [--install-options=PARAM]
                                     [--use-delta-install]
              Install an Android Application (one or more APKs) to a connected
              device or emulator without activating any components using incremental
              optimizations for faster deployment than adb
              
              Options:
                    --apks=PARAM             The paths to the APKs, comma separated
                    --device=PARAM           The device serial number
                -h, --help                   Show the help message for this command
                    --install-options=PARAM  Additional options/flags to pass to
                                             package manager install (e.g. -g,-d)
                    --use-delta-install      Use fast delta install (speeds up
                                             incremental updates by transferring
                                             only modified code and resources;
                                             default: true)
              
              android options:
                    --sdk=PARAM              Path to the Android SDK
                -v, --verbose                Enable verbose output for
                                             troubleshooting
                -V, --version                Print version information and exit

    layout
              Usage: android layout [-dhp] [--device=PARAM] [--flat] [--full] [--no-idle]
                                    [--output=PARAM]
              Returns the layout tree of an application
              
              Options:
                    --device=PARAM  The device serial number
                -d, --diff          Deprecated; no-op flag. Will be removed in a
                                    future release
                    --flat          Returns a flat list instead of a tree
                    --full          Return the full tree, including non-interactive
                                    and hidden elements
                -h, --help          Show the help message for this command
                    --no-idle       Don't wait for layout idle state when fetching
                                    layout
                -o, --output=PARAM  Writes the layout to the specified file or
                                    directory. If omitted, prints to standard output
                -p, --pretty        Pretty-prints the returned JSON
              
              android options:
                    --sdk=PARAM     Path to the Android SDK
                -v, --verbose       Enable verbose output for troubleshooting
                -V, --version       Print version information and exit

    run
              Usage: android run [-h] [--activity=PARAM] [--apks=PARAM] [--debug] [--device=PARAM]
                                 [--install-options=PARAM] [--type=PARAM] [--use-delta-install]
              Build, deploy, and launch an Android application on a connected device
              or emulator
              
              Options:
                    --activity=PARAM         The activity name
                    --apks=PARAM             The paths to the APKs, comma separated
                    --debug                  Run in debug mode
                    --device=PARAM           The device serial number
                -h, --help                   Show the help message for this command
                    --install-options=PARAM  Additional options/flags to pass to
                                             package manager install (e.g. -g,-d)
                    --type=PARAM             The component type (ACTIVITY,
                                             WATCH_FACE, TILE, COMPLICATION,
                                             DECLARATIVE_WATCH_FACE, WEAR_WIDGET)
                    --use-delta-install      Use fast delta install (speeds up
                                             incremental updates by transferring
                                             only modified code and resources;
                                             default: true)
              
              android options:
                    --sdk=PARAM              Path to the Android SDK
                -v, --verbose                Enable verbose output for
                                             troubleshooting
                -V, --version                Print version information and exit

    screen
              Usage: android screen [-h] [COMMAND]
              Commands to view the device
              
              Options:
                -h, --help       Show the help message for this command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                capture  Outputs the device screen to a PNG
                resolve  Target UI elements visually. Substitutes bounding box
                         coordinates from a annotated screenshot into a string.
                         Replaces all instances of '#N' with the center coordinates
                         of the bounding box labeled 'N'

    sdk
              Usage: android sdk [-h] [--platform=PARAM] [COMMAND]
              Manage the Android SDK installation. Includes commands to install,
              update, remove, and list available and installed SDK packages
              
              Options:
                -h, --help            Show the help message for this command
                    --platform=PARAM  Target platform <os>_<arch> (e.g.
                                      linux_x86_64, mac_arm64, windows_x86),
                                      defaults to current host
              
              android options:
                    --sdk=PARAM       Path to the Android SDK
                -v, --verbose         Enable verbose output for troubleshooting
                -V, --version         Print version information and exit
              Commands:
                install  Install SDK packages
                update   Update one or all packages to the latest version
                remove   Remove packages from the SDK
                list     List installed and available SDK packages

    skills
              Usage: android skills [-h] [COMMAND]
              Manage Android CLI skills. Includes commands to install, remove, list,
              and search for skills by keyword
              
              Options:
                -h, --help       Show the help message for this command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                add     Install a specific skill by its ID to your environment
                remove  Remove an installed skill by its ID
                list    List installed and available skills
                find    Search for available skills in the repository matching a
                        keyword
                update  Update installed skills

    studio
              Usage: android studio [-h] [COMMAND]
              Android Studio commands
              
              Options:
                -h, --help       Show the help message for this command
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
              Commands:
                find-declaration        Find declaration of a symbol
                find-usages             Find usages of a symbol
                open-file               Open a file in Android Studio
                check                   Check the status of running Studio instances
                analyze-file            Analyze a file in Android Studio
                render-compose-preview  Render a Compose preview in Android Studio
                version-lookup          Looks up the latest available versions on
                                        the internet of maven artifacts, Android
                                        versions, and more

    update
              Usage: android update [-h] [--url=PARAM]
              Update the Android CLI to the latest version
              
              Options:
                -h, --help       Show the help message for this command
                    --url=PARAM  The URL to download the update from
              
              android options:
                    --sdk=PARAM  Path to the Android SDK
                -v, --verbose    Enable verbose output for troubleshooting
                -V, --version    Print version information and exit
