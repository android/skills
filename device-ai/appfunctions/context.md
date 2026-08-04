Defines the ubiquitous language for the Android AppFunctions skill suite.

## Architecture and versioning

`AppFunctionServiceEntryPoint` API:
This compile-time AppFunctions architecture was introduced in version
1.0.0-alpha10. You annotate a wrapper service extending
`AppFunctionService` with `@AppFunctionServiceEntryPoint`. KSP then generates
XML metadata and routes services at compile time.

Legacy manual provider API:
This deprecated AppFunctions architecture applies to version
1.0.0-alpha09 and earlier. Applications implement
`AppFunctionConfiguration.Provider` on the `Application` class. Methods
require `AppFunctionContext` as the first parameter. Projects depend on
a standalone `appfunctions-service` library.

## Patterns

Service entry point pattern:
Version 1.0.0-alpha10 introduced this architectural pattern. You
declare `@AppFunction` methods directly inside an abstract class extending
`AppFunctionService`. This class uses `@AppFunctionServiceEntryPoint` and
`@AndroidEntryPoint` annotations. These annotations let you inject data
sources or repositories directly, without an intermediate business logic
delegation layer.