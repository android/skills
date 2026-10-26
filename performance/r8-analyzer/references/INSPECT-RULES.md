This document outlines common "bad" or redundant keep rules for standard Android
development and popular libraries. Current Android toolchains and libraries
include their own consumer keep rules embedded in their AAR or JAR files, making
many manual configurations unnecessary or even harmful to code optimization.

## Case: Global keep rules

**Common mistakes**:

    -dontshrink
    -dontobfuscate
    -dontoptimize
    -keep class !com.example.** { *; }

**The fix** : Global disable flags (`-dontshrink`, `-dontobfuscate`,
`-dontoptimize`) and negated keep rules using the `!` inversion operator (for
example, `-keep class !com.example.ui.** { *; }`, which matches every class in
the app and libraries *except* the specified package) disable R8 optimizations
across the entire codebase. They must be removed from the codebase (or replaced
with narrow keep rules for specific classes).

## Case: Android components

Keep rules for Android components like Activity, Fragment, ViewModel, Views,
Services, or Broadcast receivers are usually redundant. AAPT2 only generates
keep rules for components declared in the `AndroidManifest.xml` and classes
referenced from XML resources (layouts, menus, navigation graphs). ViewModels,
and Fragments or Views created only in code, aren't covered by AAPT2, but they
usually don't need manual rules either: the AndroidX libraries ship the rules
they need for reflection.

**Common mistakes**:

    -keep public class * extends android.app.Activity
    -keep public class * extends android.app.Service
    -keep public class * extends android.view.View
    -keepclassmembers class * extends android.app.Fragment { public void *(android.view.View); }

**The fix**: Delete these manual rules. AAPT2 covers manifest components and
classes referenced from XML; other classes need a rule only if something
reflects on them.

## Case: Official Android and Kotlin libraries

Keep rules targeting official library packages like AndroidX, Kotlin, and
Kotlinx are redundant as they are bundled within the libraries themselves.
Manual rules are often broader than what is strictly needed.

**Common mistakes**:

    -keep class androidx.** { *; }
    -keep class kotlinx.** { *; }
    -keep class kotlin.** { *; }

**The fix**: Delete these manual rules. Rely on the consumer keep rules packaged
within these dependencies.

## Case: Gson

### Overly broad data model rules

The most common mistake is keeping entire packages of data models (POJOs or
DTOs). Package-wide model rules are usually unnecessary on Gson 2.11 and higher
when the serialized fields use `@SerializedName`, because Gson's bundled keep
rules (from 2.11) keep those fields. On Gson versions lower than 2.11, upgrade
first, then remove them. If a class has no no-args constructor, keep a narrow
rule for it.

    -keep class com.example.app.models.** { *; }
    -keep class com.example.app.package.models.* { *; }

### Redundant interface and adapter rules

These rules added for `TypeAdapter` are unnecessary, are already covered by the
library, and prevent R8 from effectively shrinking and optimizing custom
adapters. R8 determines if the adapter implementations are used. Keeping them
globally prevents the removal of unused adapter implementations.

    -keep class * extends com.google.gson.TypeAdapter
    -keep class * implements com.google.gson.TypeAdapterFactory
    -keep class * implements com.google.gson.JsonSerializer
    -keep class * implements com.google.gson.JsonDeserializer

### Unnecessary TypeToken rules

There is no need to handle generic type erasure; Gson's own rules handle the
necessary `TypeToken` preservation.

    -keep class com.google.gson.reflect.TypeToken { *; }
    -keep class * extends com.google.gson.reflect.TypeToken
    -keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken

### Internal and example packages

Keeping internal library logic prevents the compiler from stripping away dead
code within the library.

    -keep class com.google.gson.internal.** { *; }
    -keep class com.google.gson.internal.reflect.** { *; }
    -keep class com.google.gson.internal.UnsafeAllocator { *; }
    -keep class com.google.gson.stream.** { *; }

- **Keeps unused code**: Prevents R8 from removing models that are never actually used in the code.
- **Prevents method stripping** : Keeps all getters, setters, `toString`, `equals`, and `hashCode` methods, even if they are never called.
- **Blocks obfuscation** : Prevents the class names from being obfuscated, which is unnecessary for Gson if you use `@SerializedName`.

**The fix**:

1. Use `@SerializedName` on fields in your data classes that are used for serialization and deserialization.
2. Gson (**2.11.0 and higher** ) bundles its own rules ([View Gson's embedded
   ProGuard rules](https://github.com/google/gson/blob/main/gson/src/main/resources/META-INF/proguard/gson.pro)). The bundled keep rules retain the `@SerializedName` annotated fields. If you are on a version lower than 2.11, upgrade to Gson 2.11 first, because it has the necessary keep rules, and then delete the keep rules that target the classes used for Gson serialization and deserialization. If a class has no no-args constructor, keep a narrow rule for it.

## Case: Retrofit

Retrofit has shipped with its own consumer keep rules from 2.10.0 and higher
(2.9.0 already covers the `@GET`/`@POST` interface rule), so any keep rules for
the library or classes depending on Retrofit are detrimental to the
optimization process.

### Blanket library preservation

This is the most harmful Retrofit rule as it disables any shrinking for the
entire library.

    -keep class retrofit2.** { *; }
    -keep class retrofit2.api.** { *; }
    -keep class com.package.example.retrofit.api.** { *; }

### Manual annotation keeps

Retrofit's consumer rules automatically keep the interfaces annotated with
`@GET`, `@POST`, `@DELETE`, `@PUT`, `@HEAD`, `@OPTIONS`, and `@PATCH`, making
these manual rules obsolete.

    -keepclasseswithmembers class * { @retrofit2.http.* <methods>; }

### Redundant network response and adapter rules

Network responses and third-party adapter wrappers (like RxJava) are often
overly preserved by developers out of caution.

    -keep,allowobfuscation,allowshrinking class retrofit2.Response
    -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
    -keep class retrofit2.adapter.rxjava2.Result { *; }

**The fix** : Verify you are using Retrofit 2.10.0 and higher. Retrofit from
2.9.0 bundles rules that detect its own HTTP annotations (`@GET`, `@POST`)
([View Retrofit's embedded ProGuard rules](https://github.com/square/retrofit/blob/master/retrofit/src/main/resources/META-INF/proguard/retrofit2.pro)), and from 2.10.0 it also keeps
`retrofit2.Response` and `kotlin.coroutines.Continuation`. It automatically
keeps the method signatures it needs to work.

## Case: Kotlin coroutines

Kotlin Coroutines comes heavily optimized out of the box with embedded R8 rules
(`kotlinx-coroutines-core` and `kotlinx-coroutines-android` include their own
rules).

### Blanket coroutine library rules

Keeping everything under `kotlinx.coroutines` is extremely detrimental to app
size, as coroutines contain a vast amount of internal APIs that aren't used.

    -keepclassmembers class kotlinx.coroutines.** { *; }

### Redundant internal continuations

These low-level coroutine elements are preserved safely by the library's own
consumer rules. Manually adding these prevents R8 from performing internal
optimizations (such as removing unused continuations or inlining).

    -keepclassmembers class kotlin.coroutines.SafeContinuation { *; }

### Dispatcher and exception handler rules

Sometimes developers notice crashes related to missing classes on older Android
versions and add these rules, but if you are using an up-to-date version of
Kotlin Coroutines, these are handled automatically or are not an issue.
`kotlinx-coroutines-core` keeps `MainDispatcherFactory` and
`CoroutineExceptionHandler`, and `kotlinx-coroutines-android` keeps
`AndroidExceptionPreHandler` and `AndroidDispatcherFactory`.

    -keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
    -keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
    -keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
    -keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

**The fix** : Remove any broad `kotlinx` keep rules. Kotlin Coroutines (**1.7.0
and higher** ) bundles the necessary keep rules ([View Coroutines' embedded
ProGuard rules](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro)).

## Case: Parcelable and enums

**Common mistakes** : Legacy projects often contain manual `Parcelable` or `enum`
keep rules:

    -keep class * implements android.os.Parcelable { public static final android.os.Parcelable$Creator *; }
    -keepclassmembers enum * { *; }

**The fix**:

1. The default ProGuard file `proguard-android-optimize.txt` already includes the necessary keep rules for all `Parcelable` `CREATOR` fields and `enum` methods (`values` and `valueOf`).
2. **Delete all manual Parcelable and enum rules**: Delete these manual keep rules without proposing any replacement rules.
3. *(Optional for Parcelable)* : Use the `kotlin-parcelize` plugin and `@Parcelize` annotation to avoid manual `writeToParcel` boilerplate.
4. **Ideal rule** : **None.** Delete all manual `Parcelable` and `enum` keeps.

## Case: Room database

**Common mistakes** : Keeping DAO interfaces or the generated `_Impl` classes
manually.

    -keep class * extends androidx.room.RoomDatabase
    -keep class *_*Impl { *; }

**The fix**: Room generates its own ProGuard rules for the code it creates.
Manual rules are redundant and prevent R8 from optimizing the database access
layers.

- **Ideal rule** : **None.** Delete all manual Room or DAO keeps.

## Summary

If you have updated your libraries to the versions mentioned, your
`proguard-rules.pro` must not contain any keep rules for the libraries
mentioned here.