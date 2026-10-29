Defines the ubiquitous language for the Android AppFunctions skill suite.

## Architecture and versioning

`AppFunctionServiceEntryPoint` API:
This compile-time AppFunctions architecture was introduced in version
1.0.0-alpha10. You annotate a wrapper service extending
`AppFunctionService` with `@AppFunctionServiceEntryPoint`. KSP then generates
XML metadata and routes services at compile time.

Legacy manual provider API:
This AppFunctions architecture applies to version
1.0.0-alpha09 and earlier. Applications implement
`AppFunctionConfiguration.Provider` on the `Application` class. Methods
require `AppFunctionContext` as the first parameter. Projects depend on
a standalone `appfunctions-service` library. Version 1.0.0-alpha12 removed
`AppFunctionConfiguration` and `AppFunctionContext`.

App description:
The app-level text that agents read to understand the app's AppFunctions.
It names the app's primary domain, lists its tasks, and states call order
across functions. Write it in the `appfn:instructions` attribute of
`app_metadata.xml`. Before 1.0.0-beta01, this attribute was
`appfn:description`.

## Patterns

Service entry point pattern:
Version 1.0.0-alpha10 introduced this architectural pattern. You
declare `@AppFunctionDeclaration` methods directly inside an abstract class
extending `AppFunctionService`. Before 1.0.0-alpha13, this annotation was
`@AppFunction`. This class uses `@AppFunctionServiceEntryPoint`, plus
`@AndroidEntryPoint` when the app uses Hilt. These annotations let you
inject data sources or repositories directly, without an intermediate
business logic delegation layer.