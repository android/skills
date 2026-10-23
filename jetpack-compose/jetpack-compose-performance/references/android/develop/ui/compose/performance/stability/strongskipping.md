Strong Skipping is a mode available in the Compose compiler. When enabled, it
changes the compiler's behavior in two ways:

- Composables with unstable parameters become [skippable](https://developer.android.com/develop/ui/compose/performance/stability#functions)
- Lambdas with unstable captures are [remembered](https://developer.android.com/develop/ui/compose/state#state-in-composables)

> [!NOTE]
> **Note:** Strong Skipping is enabled by default in Kotlin 2.0.20. The full list of options are available in the [Compose Compiler Gradle Plugin reference](https://kotlinlang.org/docs/compose-compiler-options.html).

## Enable strong skipping mode

Strong skipping is enabled by default in Kotlin 2.0.20.

To enable strong skipping for a Gradle module in a release prior to 2.0.20,
include the following option in
the `composeCompiler` block of your Gradle configuration:

    android { ... }

    composeCompiler {
       enableStrongSkippingMode = true
    }

## Composable skippability

Strong skipping mode relaxes some of the [stability](https://developer.android.com/develop/ui/compose/performance/stability) rules normally applied
by the Compose compiler when it comes to skipping and composable functions. By
default, the Compose compiler marks a composable function as skippable if all of
its arguments have stable values. Strong skipping mode changes this.

With strong skipping enabled, all restartable composable functions become
skippable. This applies whether or not they have unstable parameters.
Non-restartable composable functions remain unskippable.

### When to skip

To determine whether to skip a composable during recomposition, Compose compares
the value of each parameter with their previous values. The type of comparison
depends on the [stability](https://developer.android.com/develop/ui/compose/performance/stability) of the parameter.

- Unstable parameters are compared using instance equality (`===`)
- Stable parameters are compared using object equality (`Object.equals()`)

If all parameters meet these requirements, Compose skips the composable during
recomposition.

You might want a composable to opt out of strong skipping. That is, you might
want a restartable but non-skippable composable. In this case, use the
`@NonSkippableComposable` annotation.

    @NonSkippableComposable
    @Composable
    fun MyNonSkippableComposable {}

### Annotate classes as stable

If you want an object to be compared using object equality (`Object.equals()`)
instead of instance equality (`===`), continue to annotate the given class with
`@Stable` or `@Immutable`.

Keep the following trade-offs in mind before adding `@Stable` or `@Immutable`:

- **Classes with stable properties in Compose modules are already stable** : A class with `val` properties of stable types in a module compiled with the Compose compiler is automatically inferred as stable and uses `.equals()` without needing `@Immutable` or `@Stable`.
- **Only annotate classes with cheap `.equals()` checks** : Add `@Stable` or `@Immutable` when the compiler can't infer stability (such as classes from non-Compose modules) and new instances with identical values are frequently allocated, provided the class's `.equals()` check is inexpensive.
- **Be cautious when annotating models with collections** : Adding `@Stable` or `@Immutable` to models containing collections (or replacing `List` with `ImmutableList`) causes Compose to compare every element in the collection with `.equals()` on each recomposition pass. For large collections or models used in lazy layouts (`LazyColumn`, `LazyRow`, `LazyGrid`), comparing every element can be slower than using instance equality (`===`) or recomposing directly.

## Lambda memoization

Strong skipping mode also enables more [memoization](https://en.wikipedia.org/wiki/Memoization) of lambdas
inside composables. With strong skipping enabled, every lambda inside a
composable function will be automatically remembered.

### Examples

To achieve memoization of lambdas inside composables when using strong skipping,
the compiler wraps your lambda with a `remember` call. It is keyed with the
captures of the lambda.

Consider a case where you have a lambda as in the following example:

    @Composable
    fun MyComposable(unstableObject: Unstable, stableObject: Stable) {
        val lambda = {
            use(unstableObject)
            use(stableObject)
        }
    }

With strong skipping enabled, the compiler memoizes the lambda by wrapping it in
a `remember` call:

    @Composable
    fun MyComposable(unstableObject: Unstable, stableObject: Stable) {
        val lambda = remember(unstableObject, stableObject) {
            {
                use(unstableObject)
                use(stableObject)
            }
        }
    }

The keys follow the same comparison rules as composable functions. The runtime
compares unstable keys using instance equality. It compares stable keys using
object equality.

> [!NOTE]
> **Note:** This is slightly different to a typical `remember` call where the Compose runtime compares all keys using object equality.

### Memoization and recomposition

This optimization greatly increases the number of composables that the runtime
skips during recomposition. Without memoization, the runtime is much more likely
to allocate a new lambda to any composable that takes a lambda parameter during
recomposition. As a result, the new lambda has parameters that are not equal to
the last composition. This results in recomposition.

> [!NOTE]
> **Note:** A common misconception is that lambdas with unstable captures are themselves unstable objects. This is not true. The Compose compiler always considers lambdas to be stable. However, with Strong skipping disabled, the compiler does not memoize them and the runtime allocates them during recomposition. As such, lambdas with unstable captures cause Compose not to skip composables because they have unequal parameters.

### Avoid memoization

If you have a lambda that you don't want to memoize, use the `@DontMemoize`
annotation.

    val lambda = @DontMemoize {
        ...
    }

## APK Size

When compiled, skippable Composables result in more generated code than
composables that are not skippable. With strong skipping enabled, the compiler
marks nearly all composables as skippable and wraps all lambdas in a
`remember{...}`. Due to this, enabling strong skipping mode has a very small
impact on the APK size of your application.

Enabling strong skipping in [Now In Android](https://github.com/android/nowinandroid) increased the APK
size by 4kB. The difference in size largely depends on the number of previously
unskippable composables that were present in the given app but should be
relatively minor.