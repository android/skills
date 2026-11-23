JNI is the Java Native Interface. It defines a way for the bytecode that Android compiles from
managed code (written in the Java or Kotlin programming languages) to interact with native code
(written in C/C++). JNI is vendor-neutral, has support for loading code from dynamic shared
libraries, and while cumbersome at times is reasonably efficient.

**Note:** Because Android compiles Kotlin to ART-friendly bytecode in
a similar manner as the Java programming language, you can apply the guidance on this page to both
the Kotlin and Java programming languages in terms of JNI architecture and its associated costs.
To learn more, see
[Kotlin and Android](https://developer.android.com/kotlin).

If you're not already familiar with it, read through the
[Java Native Interface Specification](http://docs.oracle.com/javase/7/docs/technotes/guides/jni/spec/jniTOC.html)
to get a sense for how JNI works and what features are available. Some
aspects of the interface aren't immediately obvious on
first reading, so you may find the next few sections handy.

To browse global JNI references and see where global JNI references are created and deleted, use
the **JNI heap** view in the [Memory Profiler](https://developer.android.com/studio/profile/memory-profiler#jni-references)
in Android Studio 3.2 and higher.


## General tips

<br />

Try to minimize the footprint of your JNI layer. There are several dimensions to consider here.
Your JNI solution should try to follow these guidelines (listed below by order of importance,
beginning with the most important):

- **Minimize marshalling of resources across the JNI layer.** Marshalling across the JNI layer has non-trivial costs. Try to design an interface that minimizes the amount of data you need to marshall and the frequency with which you must marshall data.
- **Avoid asynchronous communication between code written in a managed programming
  language and code written in C++ when possible**. This will keep your JNI interface easier to maintain. You can typically simplify asynchronous UI updates by keeping the async update in the same language as the UI. For example, instead of invoking a C++ function from the UI thread in the Java code via JNI, it's better to do a callback between two threads in the Java programming language, with one of them making a blocking C++ call and then notifying the UI thread when the blocking call is complete.
- **Minimize the number of threads that need to touch or be touched by JNI.** If you do need to utilize thread pools in both the Java and C++ languages, try to keep JNI communication between the pool owners rather than between individual worker threads.
- **Keep your interface code in a low number of easily identified C++ and Java source
  locations to facilitate future refactors.** Consider using a JNI auto-generation library as appropriate.

## JavaVM and JNIEnv

JNI defines two key data structures, "JavaVM" and "JNIEnv". Both of these are essentially
pointers to pointers to function tables. (In the C++ version, they're classes with a
pointer to a function table and a member function for each JNI function that indirects through
the table.) The JavaVM provides the "invocation interface" functions,
which allow you to create and destroy a JavaVM. In theory you can have multiple JavaVMs per process,
but Android only allows one.

The JNIEnv provides most of the JNI functions. Your native functions all receive a JNIEnv as
the first argument, except for `@CriticalNative` methods,
see [faster native calls](https://developer.android.com/ndk/guides/jni-tips#fast_critical_native).

The JNIEnv is used for thread-local storage. For this reason, **you cannot share a JNIEnv between threads** .
If a piece of code has no other way to get its JNIEnv, you should share
the JavaVM, and use `GetEnv` to discover the thread's JNIEnv. (Assuming it has one; see `AttachCurrentThread` below.)

The C declarations of JNIEnv and JavaVM are different from the C++
declarations. The `"jni.h"` include file provides different typedefs
depending on whether it's included into C or C++. For this reason it's a bad idea to
include JNIEnv arguments in header files included by both languages. (Put another way: if your
header file requires `#ifdef __cplusplus`, you may have to do some extra work if anything in
that header refers to JNIEnv.)


## Threads

<br />

All threads are Linux threads, scheduled by the kernel. They're usually
started from managed code (using `Thread.start()`),
but they can also be created elsewhere and then attached to the `JavaVM`. For
example, a thread started with `pthread_create()` or `std::thread`
can be attached using the `AttachCurrentThread()` or
`AttachCurrentThreadAsDaemon()` functions. Until a thread is
attached, it has no JNIEnv, and **cannot make JNI calls**.

It's usually best to use `Thread.start()` to create any thread that needs to
call in to Java code. Doing so will ensure that you have sufficient stack space, that you're
in the correct `ThreadGroup`, and that you're using the same `ClassLoader`
as your Java code. It's also easier to set the thread's name for debugging in Java than from
native code (see `pthread_setname_np()` if you have a `pthread_t` or
`thread_t`, and `std::thread::native_handle()` if you have a
`std::thread` and want a `pthread_t`).

Attaching a natively-created thread causes a `java.lang.Thread`
object to be constructed and added to the "main" `ThreadGroup`,
making it visible to the debugger. Calling `AttachCurrentThread()`
on an already-attached thread is a no-op.

Android does not suspend threads executing native code. If
garbage collection is in progress, or the debugger has issued a suspend
request, Android will pause the thread the next time it makes a JNI call.

Threads attached through JNI **must call
`DetachCurrentThread()` before they exit** .
If coding this directly is awkward, in Android 2.0 (Eclair) and higher you
can use `pthread_key_create()` to define a destructor
function that will be called before the thread exits, and
call `DetachCurrentThread()` from there. (Use that
key with `pthread_setspecific()` to store the JNIEnv in
thread-local-storage; that way it'll be passed into your destructor as
the argument.)


## jclass, jmethodID, and jfieldID

<br />

If you want to access an object's field from native code, you would do the following:

- Get the class object reference for the class with `FindClass`
- Get the field ID for the field with `GetFieldID`
- Get the contents of the field with something appropriate, such as `GetIntField`

Similarly, to call a method, you'd first get a class object reference and then a method ID. The IDs are often just
pointers to internal runtime data structures. Looking them up may require several string
comparisons, but once you have them the actual call to get the field or invoke the method
is very quick.

If performance is important, it's useful to look the values up once and cache the results
in your native code. Because there is a limit of one JavaVM per process, it's reasonable
to store this data in a static local structure.

The class references, field IDs, and method IDs are guaranteed valid until the class is unloaded. Classes
are only unloaded if all classes associated with a ClassLoader can be garbage collected,
which is rare but will not be impossible in Android. Note however that
the `jclass`
is a class reference and **must be protected** with a call
to `NewGlobalRef` (see the next section).

If you would like to cache the IDs when a class is loaded, and automatically re-cache them
if the class is ever unloaded and reloaded, the correct way to initialize
the IDs is to add a piece of code that looks like this to the appropriate class:

### Kotlin

```kotlin
companion object {
    /*
     * We use a static class initializer to allow the native code to cache some
     * field offsets. This native function looks up and caches interesting
     * class/field/method IDs. Throws on failure.
     */
    private external fun nativeInit()

    init {
        nativeInit()
    }
}
```

### Java

```java
    /*
     * We use a class initializer to allow the native code to cache some
     * field offsets. This native function looks up and caches interesting
     * class/field/method IDs. Throws on failure.
     */
    private static native void nativeInit();

    static {
        nativeInit();
    }
```

Create a `nativeClassInit` method in your C/C++ code that performs the ID lookups. The code
will be executed once, when the class is initialized. If the class is ever unloaded and
then reloaded, it will be executed again.


## Local and global references

<br />

Every argument passed to a native method, and almost every object returned
by a JNI function is a "local reference". This means that it's valid for the
duration of the current native method in the current thread.
**Even if the object itself continues to live on after the native method
returns, the reference is not valid.**

This applies to all sub-classes of `jobject`, including
`jclass`, `jstring`, and `jarray`.
(The runtime will warn you about most reference mis-uses when extended JNI
checks are enabled.)

The only way to get non-local references is via the functions
`NewGlobalRef` and `NewWeakGlobalRef`.

If you want to hold on to a reference for a longer period, you must use
a "global" reference. The `NewGlobalRef` function takes the
local reference as an argument and returns a global one.
The global reference is guaranteed to be valid until you call
`DeleteGlobalRef`.

This pattern is commonly used when caching a jclass returned
from `FindClass`, e.g.:

```
jclass localClass = env->FindClass("MyClass");
jclass globalClass = reinterpret_cast<jclass>(env->NewGlobalRef(localClass));
```

All JNI methods accept both local and global references as arguments.
It's possible for references to the same object to have different values.
For example, the return values from consecutive calls to
`NewGlobalRef` on the same object may be different.
**To see if two references refer to the same object,
you must use the `IsSameObject` function.** Never compare
references with `==` in native code.

One consequence of this is that you
**must not assume object references are constant or unique**
in native code. The value representing an object may be different
from one invocation of a method to the next, and it's possible that two
different objects could have the same value on consecutive calls. Do not use
`jobject` values as keys.

Programmers are required to "not excessively allocate" local references. In practical terms this means
that if you're creating large numbers of local references, perhaps while running through an array of
objects, you should free them manually with
`DeleteLocalRef` instead of letting JNI do it for you. The
implementation is only required to reserve slots for
16 local references, so if you need more than that you should either delete as you go or use
`EnsureLocalCapacity`/`PushLocalFrame` to reserve more.

Note that `jfieldID`s and `jmethodID`s are opaque
types, not object references, and should not be passed to
`NewGlobalRef`. The raw data
pointers returned by functions like `GetStringUTFChars`
and `GetByteArrayElements` are also not objects. (They may be passed
between threads, and are valid until the matching Release call.)

One unusual case deserves separate mention. If you attach a native
thread with `AttachCurrentThread`, the code you are running will
never automatically free local references until the thread detaches. Any local
references you create will have to be deleted manually. In general, any native
code that creates local references in a loop probably needs to do some manual
deletion.

Be careful using global references. Global references can be unavoidable, but they are difficult
to debug and can cause difficult-to-diagnose memory (mis)behaviors. All else being equal, a
solution with fewer global references is probably better.


## UTF-8 and UTF-16 strings

<br />

The Java programming language uses UTF-16. For convenience, JNI provides methods that work with
[Modified UTF-8](https://en.wikipedia.org/wiki/UTF-8#Modified_UTF-8) as well. The
modified encoding is useful for C code because it encodes \\u0000 as 0xc0 0x80 instead of 0x00.
The nice thing about this is that you can count on having C-style zero-terminated strings,
suitable for use with standard libc string functions. The down side is that you cannot pass
arbitrary UTF-8 data to JNI and expect it to work correctly.

To get the UTF-16 representation of a `String`, use `GetStringChars`.
Note that **UTF-16 strings are not zero-terminated**, and \\u0000 is allowed,
so you need to hang on to the string length as well as the jchar pointer.

**Don't forget to `Release` the strings you `Get`** . The
string functions return `jchar*` or `jbyte*`, which
are C-style pointers to primitive data rather than local references. They
are guaranteed valid until `Release` is called, which means they are not
released when the native method returns.

**Data passed to NewStringUTF must be in Modified UTF-8 format** . A
common mistake is reading character data from a file or network stream
and handing it to `NewStringUTF` without filtering it.
Unless you know the data is valid MUTF-8 (or 7-bit ASCII, which is a compatible subset),
you need to strip out invalid characters or convert them to proper Modified UTF-8 form.
If you don't, the UTF-16 conversion is likely to provide unexpected results.
CheckJNI---which is on by default for emulators---scans strings
and aborts the VM if it receives invalid input.

Before Android 8, it was usually faster to operate with UTF-16 strings as Android
did not require a copy in `GetStringChars`, whereas
`GetStringUTFChars` required an allocation and a conversion to UTF-8.
Android 8 changed the `String` representation to use 8 bits per character
for ASCII strings (to save memory) and started to use a
[moving
garbage collector](https://source.android.com/docs/core/runtime/improvements#concurrent-compacting-gc). These features greatly reduce the number of cases where ART
can provide a pointer to the `String` data without making a copy, even
for `GetStringCritical`. However, if most strings processed by the code
are short, it is possible to avoid the allocation and deallocation in most cases by
using a stack-allocated buffer and `GetStringRegion` or
`GetStringUTFRegion`. For example:

```c++
    constexpr size_t kStackBufferSize = 64u;
    jchar stack_buffer[kStackBufferSize];
    std::unique_ptr<jchar[]> heap_buffer;
    jchar* buffer = stack_buffer;
    jsize length = env->GetStringLength(str);
    if (length > kStackBufferSize) {
      heap_buffer.reset(new jchar[length]);
      buffer = heap_buffer.get();
    }
    env->GetStringRegion(str, 0, length, buffer);
    process_data(buffer, length);
```


## Primitive arrays

<br />

JNI provides functions for accessing the contents of array objects.
While arrays of objects must be accessed one entry at a time, arrays of
primitives can be read and written directly as if they were declared in C.

To make the interface as efficient as possible without constraining
the VM implementation, the `Get<PrimitiveType>ArrayElements`
family of calls allows the runtime to either return a pointer to the actual elements, or
allocate some memory and make a copy. Either way, the raw pointer returned
is guaranteed to be valid until the corresponding `Release` call
is issued (which implies that, if the data wasn't copied, the array object
will be pinned down and can't be relocated as part of compacting the heap).
**You must `Release` every array you `Get`.** Also, if the `Get`
call fails, you must ensure that your code doesn't try to `Release` a NULL
pointer later.

You can determine whether or not the data was copied by passing in a
non-NULL pointer for the `isCopy` argument. This is rarely
useful.

The `Release` call takes a `mode` argument that can
have one of three values. The actions performed by the runtime depend upon
whether it returned a pointer to the actual data or a copy of it:

- `0`
  - Actual: the array object is un-pinned.
  - Copy: data is copied back. The buffer with the copy is freed.
- `JNI_COMMIT`
  - Actual: does nothing.
  - Copy: data is copied back. The buffer with the copy **is not freed**.
- `JNI_ABORT`
  - Actual: the array object is un-pinned. Earlier writes are **not** aborted.
  - Copy: the buffer with the copy is freed; any changes to it are lost.

One reason for checking the `isCopy` flag is to know if
you need to call `Release` with `JNI_COMMIT`
after making changes to an array --- if you're alternating between making
changes and executing code that uses the contents of the array, you may be
able to
skip the no-op commit. Another possible reason for checking the flag is for
efficient handling of `JNI_ABORT`. For example, you might want
to get an array, modify it in place, pass pieces to other functions, and
then discard the changes. If you know that JNI is making a new copy for
you, there's no need to create another "editable" copy. If JNI is passing
you the original, then you do need to make your own copy.

It is a common mistake (repeated in example code) to assume that you can skip the `Release` call if
`*isCopy` is false. This is not the case. If no copy buffer was
allocated, then the original memory must be pinned down and can't be moved by
the garbage collector.

Also note that the `JNI_COMMIT` flag does **not** release the array,
and you will need to call `Release` again with a different flag
eventually.


## Region calls

<br />

There is an alternative to calls like `Get<Type>ArrayElements`
and `GetStringChars` that may be very helpful when all you want
to do is copy data in or out. Consider the following:

```c++
    jbyte* data = env->GetByteArrayElements(array, NULL);
    if (data != NULL) {
        memcpy(buffer, data, len);
        env->ReleaseByteArrayElements(array, data, JNI_ABORT);
    }
```

This grabs the array, copies the first `len` byte
elements out of it, and then releases the array. Depending upon the
implementation, the `Get` call will either pin or copy the array
contents.
The code copies the data (for perhaps a second time), then calls `Release`; in this case
`JNI_ABORT` ensures there's no chance of a third copy.

One can accomplish the same thing more simply:

```c++
    env->GetByteArrayRegion(array, 0, len, buffer);
```

This has several advantages:

- Requires one JNI call instead of 2, reducing overhead.
- Doesn't require pinning or extra data copies.
- Reduces the risk of programmer error --- no risk of forgetting to call `Release` after something fails.

Similarly, you can use the `Set<Type>ArrayRegion` call
to copy data into an array, and `GetStringRegion` or
`GetStringUTFRegion` to copy characters out of a
`String`.


## Exceptions

<br />

**You must not call most JNI functions while an exception is pending.**
Your code is expected to notice the exception (via the function's return value,
`ExceptionCheck`, or `ExceptionOccurred`) and return,
or clear the exception and handle it.

The only JNI functions that you are allowed to call while an exception is
pending are:

- `DeleteGlobalRef`
- `DeleteLocalRef`
- `DeleteWeakGlobalRef`
- `ExceptionCheck`
- `ExceptionClear`
- `ExceptionDescribe`
- `ExceptionOccurred`
- `MonitorExit`
- `PopLocalFrame`
- `PushLocalFrame`
- `Release<PrimitiveType>ArrayElements`
- `ReleasePrimitiveArrayCritical`
- `ReleaseStringChars`
- `ReleaseStringCritical`
- `ReleaseStringUTFChars`

Many JNI calls can throw an exception, but often provide a simpler way
of checking for failure. For example, if `NewString` returns
a non-NULL value, you don't need to check for an exception. However, if
you call a method (using a function like `CallObjectMethod`),
you must always check for an exception, because the return value is not
going to be valid if an exception was thrown.

Note that exceptions thrown by managed code do not unwind native stack
frames. (And C++ exceptions, generally discouraged on Android, must not be
thrown across the JNI transition boundary from C++ code to managed code.)
The JNI `Throw` and `ThrowNew` instructions just
set an exception pointer in the current thread. Upon returning to managed
from native code, the exception will be noted and handled appropriately.

Native code can "catch" an exception by calling `ExceptionCheck` or
`ExceptionOccurred`, and clear it with
`ExceptionClear`. As usual,
discarding exceptions without handling them can lead to problems.

There are no built-in functions for manipulating the `Throwable` object
itself, so if you want to (say) get the exception string you will need to
find the `Throwable` class, look up the method ID for
`getMessage "()Ljava/lang/String;"`, invoke it, and if the result
is non-NULL use `GetStringUTFChars` to get something you can
hand to `printf(3)` or equivalent.


## Extended checking

<br />

JNI does very little error checking. Errors usually result in a crash. Android also offers a mode called CheckJNI, where the JavaVM and JNIEnv function table pointers are switched to tables of functions that perform an extended series of checks before calling the standard implementation.

The additional checks include:

- Arrays: attempting to allocate a negative-sized array.
- Bad pointers: passing a bad jarray/jclass/jobject/jstring to a JNI call, or passing a NULL pointer to a JNI call with a non-nullable argument.
- Class names: passing anything but the "java/lang/String" style of class name to a JNI call.
- Critical calls: making a JNI call between a "critical" get and its corresponding release.
- Direct ByteBuffers: passing bad arguments to `NewDirectByteBuffer`.
- Exceptions: making a JNI call while there's an exception pending.
- JNIEnv\*s: using a JNIEnv\* from the wrong thread.
- jfieldIDs: using a NULL jfieldID, or using a jfieldID to set a field to a value of the wrong type (trying to assign a StringBuilder to a String field, say), or using a jfieldID for a static field to set an instance field or vice versa, or using a jfieldID from one class with instances of another class.
- jmethodIDs: using the wrong kind of jmethodID when making a `Call*Method` JNI call: incorrect return type, static/non-static mismatch, wrong type for 'this' (for non-static calls) or wrong class (for static calls).
- References: using `DeleteGlobalRef`/`DeleteLocalRef` on the wrong kind of reference.
- Release modes: passing a bad release mode to a release call (something other than `0`, `JNI_ABORT`, or `JNI_COMMIT`).
- Type safety: returning an incompatible type from your native method (returning a StringBuilder from a method declared to return a String, say).
- UTF-8: passing an invalid [Modified UTF-8](https://en.wikipedia.org/wiki/UTF-8#Modified_UTF-8) byte sequence to a JNI call.

(Accessibility of methods and fields is still not checked: access restrictions don't apply to native code.)

There are several ways to enable CheckJNI.

If you're using the emulator, CheckJNI is on by default.

If you have a rooted device, you can use the following sequence of commands to restart the runtime with CheckJNI enabled:

```bash
adb shell stop
adb shell setprop dalvik.vm.checkjni true
adb shell start
```

In either of these cases, you'll see something like this in your logcat output when the runtime starts:

```
D AndroidRuntime: CheckJNI is ON
```

If you have a regular device, you can use the following command:

```bash
adb shell setprop debug.checkjni 1
```

This won't affect already-running apps, but any app launched from that point on will have CheckJNI enabled. (Change the property to any other value or simply rebooting will disable CheckJNI again.) In this case, you'll see something like this in your logcat output the next time an app starts:

```
D Late-enabling CheckJNI
```

You can also set the `android:debuggable` attribute in your application's manifest to
turn on CheckJNI just for your app. Note that the Android build tools will do this automatically for
certain build types.


## Native libraries

<br />

You can load native code from shared libraries with the standard
[`System.loadLibrary`](https://developer.android.com/reference/java/lang/System#loadLibrary(java.lang.String)).

In practice, older versions of Android had bugs in PackageManager that caused installation and
update of native libraries to be unreliable. The [ReLinker](https://github.com/KeepSafe/ReLinker)
project offers workarounds for this and other native library loading problems.

Call `System.loadLibrary` (or `ReLinker.loadLibrary`) from a static class
initializer. The argument is the "undecorated" library name,
so to load `libfubar.so` you would pass in `"fubar"`.

If you have only one class with native methods, it makes sense for the call to
`System.loadLibrary` to be in a static initializer for that class. Otherwise you might
want to make the call from `Application` so you know that the library is always loaded,
and always loaded early.

There are two ways that the runtime can find your native methods. You can either
explicitly register them with `RegisterNatives()`, or you can let the runtime
look them up dynamically with `dlsym()`. Most apps should call
`RegisterNatives()` from `JNI_OnLoad()`. This performs up-front registration of
native methods with an explicit mapping. It takes a little bit more
up-front work to write that code, but has the following advantages:

1. Mistakes are caught early. An incorrectly named function will cause an error during library load.
2. The only symbol that typically must be exported is `JNI_OnLoad()`. Some use cases may additionally need `ANativeActivity_OnCreate()` or similar.
3. Android Studio can warn you and auto-fix common mistakes in class names and function signatures.

In contrast, the advantage of letting the runtime discover your functions via
`dlsym()` is that it's slightly less code to write, and cheaper in the case
where your JNI functions are usually never called.

To use `RegisterNatives`:

- Provide a `JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved)` function.
- In your `JNI_OnLoad`, register all of your native methods using `RegisterNatives`.
- Build with a [version script](https://developer.android.com/ndk/guides/symbol-visibility) (preferred) or use `-fvisibility=hidden` so that only your `JNI_OnLoad` is exported from your library. This produces faster and smaller code, and avoids potential collisions with other libraries loaded into your app (but it creates less useful stack traces if your app crashes in native code).

The static initializer should look like this:

### Kotlin

```kotlin
companion object {
    init {
        System.loadLibrary("fubar")
    }
}
```

### Java

```java
static {
    System.loadLibrary("fubar");
}
```

The `JNI_OnLoad` function should look something like this if
written in C++:

```c++
JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    // Find your class. JNI_OnLoad is called from the correct class loader context for this to work.
    jclass c = env->FindClass("com/example/app/package/MyClass");
    if (c == nullptr) return JNI_ERR;

    // Register your class' native methods.
    static const JNINativeMethod methods[] = {
        {"nativeFoo", "()V", reinterpret_cast<void*>(nativeFoo)},
        {"nativeBar", "(Ljava/lang/String;I)Z", reinterpret_cast<void*>(nativeBar)},
    };
    int rc = env->RegisterNatives(c, methods, sizeof(methods)/sizeof(JNINativeMethod));
    if (rc != JNI_OK) return rc;

    return JNI_VERSION_1_6;
}
```

To instead use "discovery" of native methods, you need to name them in a specific way (see
[the JNI spec](http://java.sun.com/javase/6/docs/technotes/guides/jni/spec/design.html#wp615)
for details). This means that if a method signature is wrong, you won't know about it until the
first time the method is actually invoked.

Any `FindClass` calls made from `JNI_OnLoad` will resolve classes in the
context of the class loader that was used to load the shared library. When called from other
contexts, `FindClass` uses the class loader associated with the method at the top of the
Java stack, or if there isn't one (because the call is from a native thread that was just attached)
it uses the "system" class loader. The system class loader does not know about your application's
classes, so you won't be able to look up your own classes with `FindClass` in that
context. This makes `JNI_OnLoad` a convenient place to look up and cache classes: once
you have a valid `jclass` [global reference](https://developer.android.com/ndk/guides/jni-tips#local_and_global_references)
you can use it from any attached thread.


## Faster native calls with `@FastNative` and `@CriticalNative`

<br />

Native methods can be annotated with
[`@FastNative`](https://developer.android.com/reference/dalvik/annotation/optimization/FastNative) or
[`@CriticalNative`](https://developer.android.com/reference/dalvik/annotation/optimization/CriticalNative)
(but not both) to speed up transitions between managed and native code. However, these annotations
come with certain changes in behavior that need to be carefully considered before use. While we
briefly mention these changes below, please refer to the documentation for the details.


The `@CriticalNative` annotation can be applied only to native methods that do not
use managed objects (in parameters or return values, or as an implicit `this`), and this
annotation changes the JNI transition ABI. The native implementation must exclude the
`JNIEnv` and `jclass` parameters from its function signature.

While executing a `@FastNative` or `@CriticalNative` method, the garbage
collection cannot suspend the thread for essential work and may become blocked. Do not use these
annotations for long-running methods, including usually-fast, but generally unbounded, methods.
In particular, the code should not perform significant I/O operations or acquire native locks that
can be held for a long time.


These annotations were implemented for system use since
[Android 8](https://source.android.com/docs/core/dalvik/improvements#faster-native-methods)
and became CTS-tested public
API in Android 14. These optimizations are likely to work also on Android 8-13 devices (albeit
without the strong CTS guarantees) but the dynamic lookup of native methods is supported only on
Android 12+, the explicit registration with JNI `RegisterNatives` is strictly required
for running on Android versions 8-11. These annotations are ignored on Android 7-, the ABI mismatch
for `@CriticalNative` would lead to wrong argument marshalling and likely crashes.

For performance critical methods that need these annotations, it is strongly recommended to
explicitly register the method(s) with JNI `RegisterNatives` instead of relying on the
name-based "discovery" of native methods. To get optimal app startup performance, it is recommended
to include callers of `@FastNative` or `@CriticalNative` methods in the
[baseline profile](https://developer.android.com/topic/performance/baselineprofiles/overview). Since Android 12,
a call to a `@CriticalNative` native method from a compiled managed method is almost as
cheap as a non-inline call in C/C++ as long as all arguments fit into registers (for example up to
8 integral and up to 8 floating point arguments on arm64).

Sometimes it can be preferable to split a native method into two, a very fast method that can
fail and another one that handles the slow cases. For example:

### Kotlin

```kotlin
fun writeInt(nativeHandle: Long, value: Int) {
    // A fast buffered write with a `@CriticalNative` method should succeed most of the time.
    if (!nativeTryBufferedWriteInt(nativeHandle, value)) {
        // If the buffered write failed, we need to use the slow path that can perform
        // significant I/O and can even throw an `IOException`.
        nativeWriteInt(nativeHandle, value)
    }
}

@CriticalNative
external fun nativeTryBufferedWriteInt(nativeHandle: Long, value: Int): Boolean

external fun nativeWriteInt(nativeHandle: Long, value: Int)
```

### Java

```java
void writeInt(long nativeHandle, int value) {
    // A fast buffered write with a `@CriticalNative` method should succeed most of the time.
    if (!nativeTryBufferedWriteInt(nativeHandle, value)) {
        // If the buffered write failed, we need to use the slow path that can perform
        // significant I/O and can even throw an `IOException`.
        nativeWriteInt(nativeHandle, value);
    }
}

@CriticalNative
static native boolean nativeTryBufferedWriteInt(long nativeHandle, int value);

static native void nativeWriteInt(long nativeHandle, int value);
```


## 64-bit considerations

<br />

To support architectures that use 64-bit pointers, use a `long` field rather than an
`int` when storing a pointer to a native structure in a Java field.


## Unsupported features/backwards compatibility

<br />

All JNI 1.6 features are supported, with the following exception:

- `DefineClass` is not implemented. Android does not use Java bytecodes or class files, so passing in binary class data doesn't work.

For backward compatibility with older Android releases, you may need to
be aware of:

- **Dynamic lookup of native functions**

  Until Android 2.0 (Eclair), the '$' character was not properly
  converted to "_00024" during searches for method names. Working
  around this requires using explicit registration or moving the
  native methods out of inner classes.
- **Detaching threads**

  Until Android 2.0 (Eclair), it was not possible to use a `pthread_key_create`
  destructor function to avoid the "thread must be detached before
  exit" check. (The runtime also uses a pthread key destructor function,
  so it'd be a race to see which gets called first.)
- **Weak global references**

  Until Android 2.2 (Froyo), weak global references were not implemented.
  Older versions will vigorously reject attempts to use them. You can use
  the Android platform version constants to test for support.

  Until Android 4.0 (Ice Cream Sandwich), weak global references could only
  be passed to `NewLocalRef`, `NewGlobalRef`, and
  `DeleteWeakGlobalRef`. (The spec strongly encourages
  programmers to create hard references to weak globals before doing
  anything with them, so this should not be at all limiting.)

  From Android 4.0 (Ice Cream Sandwich) on, weak global references can be
  used like any other JNI references.
- **Local references**

  Until Android 4.0 (Ice Cream Sandwich), local references were
  actually direct pointers. Ice Cream Sandwich added the indirection
  necessary to support better garbage collectors, but this means that lots
  of JNI bugs are undetectable on older releases. See
  [JNI Local Reference Changes in ICS](http://android-developers.blogspot.com/2011/11/jni-local-reference-changes-in-ics.html) for more details.

  In Android versions prior to [Android 8.0](https://developer.android.com/about/versions/oreo), the
  number of local references is capped at a version-specific limit. Beginning in Android 8.0,
  Android supports unlimited local references.
- **Determining reference type with `GetObjectRefType`**

  Until Android 4.0 (Ice Cream Sandwich), as a consequence of the use of
  direct pointers (see above), it was impossible to implement
  `GetObjectRefType` correctly. Instead we used a heuristic
  that looked through the weak globals table, the arguments, the locals
  table, and the globals table in that order. The first time it found your
  direct pointer, it would report that your reference was of the type it
  happened to be examining. This meant, for example, that if
  you called `GetObjectRefType` on a global jclass that happened
  to be the same as the jclass passed as an implicit argument to your static
  native method, you'd get `JNILocalRefType` rather than
  `JNIGlobalRefType`.
- **`@FastNative` and `@CriticalNative`**

  Up to Android 7, these optimization annotations were ignored. The ABI
  mismatch for `@CriticalNative` would lead to wrong argument
  marshalling and likely crashes.

  The dynamic lookup of native functions for `@FastNative` and
  `@CriticalNative` methods was unimplemented in Android 8-10 and
  contains known bugs in Android 11. Using these optimizations without
  explicit registration with JNI `RegisterNatives` is likely to
  lead to crashes on Android 8-11.
- **`FindClass` throws `ClassNotFoundException`**

  For backward compatibility, Android throws `ClassNotFoundException`
  instead of `NoClassDefFoundError` when a class isn't found by
  `FindClass`. This behavior is consistent with the Java reflection API
  `Class.forName(name)`.


## FAQ: Why do I get `UnsatisfiedLinkError`?

<br />

When working on native code it's not uncommon to see a failure like this:

```
java.lang.UnsatisfiedLinkError: Library foo not found
```

In some cases it means what it says --- the library wasn't found. In
other cases the library exists but couldn't be opened by `dlopen(3)`, and
the details of the failure can be found in the exception's detail message.

Common reasons why you might encounter "library not found" exceptions:

- The library doesn't exist or isn't accessible to the app. Use `adb shell ls -l <path>` to check its presence and permissions.
- The library wasn't built with the NDK. This can result in dependencies on functions or libraries that don't exist on the device.

Another class of `UnsatisfiedLinkError` failures looks like:

```
java.lang.UnsatisfiedLinkError: myfunc
        at Foo.myfunc(Native Method)
        at Foo.main(Foo.java:10)
```

In logcat, you'll see:

```
W/dalvikvm(  880): No implementation found for native LFoo;.myfunc ()V
```

This means that the runtime tried to find a matching method but was
unsuccessful. Some common reasons for this are:

- The library isn't getting loaded. Check the logcat output for messages about library loading.
- The method isn't being found due to a name or signature mismatch. This is commonly caused by:
  - For lazy method lookup, failing to declare C++ functions with `extern "C"` and appropriate visibility (`JNIEXPORT`). Note that prior to Ice Cream Sandwich, the JNIEXPORT macro was incorrect, so using a new GCC with an old `jni.h` won't work. You can use `arm-eabi-nm` to see the symbols as they appear in the library; if they look mangled (something like `_Z15Java_Foo_myfuncP7_JNIEnvP7_jclass` rather than `Java_Foo_myfunc`), or if the symbol type is a lowercase 't' rather than an uppercase 'T', then you need to adjust the declaration.
  - For explicit registration, minor errors when entering the method signature. Make sure that what you're passing to the registration call matches the signature in the log file. Remember that 'B' is `byte` and 'Z' is `boolean`. Class name components in signatures start with 'L', end with ';', use '/' to separate package/class names, and use '$' to separate inner-class names (`Ljava/util/Map$Entry;`, say).

Using `javah` to automatically generate JNI headers may help
avoid some problems.


## FAQ: Why didn't `FindClass` find my class?

(Most of this advice applies equally well to failures to find methods
with `GetMethodID` or `GetStaticMethodID`, or fields
with `GetFieldID` or `GetStaticFieldID`.)

Make sure that the class name string has the correct format. JNI class
names start with the package name and are separated with slashes,
such as `java/lang/String`. If you're looking up an array class,
you need to start with the appropriate number of square brackets and
must also wrap the class with 'L' and ';', so a one-dimensional array of
`String` would be `[Ljava/lang/String;`.
If you're looking up an inner class, use '$' rather than '.'. In general,
using `javap` on the .class file is a good way to find out the
internal name of your class.

If you enable code shrinking, make sure that you
[configure which code to keep](https://developer.android.com/studio/build/shrink-code#keep-code). Configuring
proper keep rules is important because the code shrinker might otherwise remove classes, methods,
or fields that are only used from JNI.

If the class name looks right, you could be running into a class loader
issue. `FindClass` wants to start the class search in the
class loader associated with your code. It examines the call stack,
which will look something like:

```
    Foo.myfunc(Native Method)
    Foo.main(Foo.java:10)
```

The topmost method is `Foo.myfunc`. `FindClass`
finds the `ClassLoader` object associated with the `Foo`
class and uses that.

This usually does what you want. You can get into trouble if you
create a thread yourself (perhaps by calling `pthread_create`
and then attaching it with `AttachCurrentThread`). Now there
are no stack frames from your application.
If you call `FindClass` from this thread, the
JavaVM will start in the "system" class loader instead of the one associated
with your application, so attempts to find app-specific classes will fail.

There are a few ways to work around this:

- Do your `FindClass` lookups once, in `JNI_OnLoad`, and cache the class references for later use. Any `FindClass` calls made as part of executing `JNI_OnLoad` will use the class loader associated with the function that called `System.loadLibrary` (this is a special rule, provided to make library initialization more convenient). If your app code is loading the library, `FindClass` will use the correct class loader.
- Pass an instance of the class into the functions that need it, by declaring your native method to take a Class argument and then passing `Foo.class` in.
- Cache a reference to the `ClassLoader` object somewhere handy, and issue `loadClass` calls directly. This requires some effort.


## FAQ: How do I share raw data with native code?

<br />

You may find yourself in a situation where you need to access a large
buffer of raw data from both managed and native code. Common examples
include manipulation of bitmaps or sound samples. There are two
basic approaches.

You can store the data in a `byte[]`. This allows very fast
access from managed code. On the native side, however, you're
not guaranteed to be able to access the data without having to copy it. In
some implementations, `GetByteArrayElements` and
`GetPrimitiveArrayCritical` will return actual pointers to the
raw data in the managed heap, but in others it will allocate a buffer
on the native heap and copy the data over.

The alternative is to store the data in a direct byte buffer. These
can be created with `java.nio.ByteBuffer.allocateDirect`, or
the JNI `NewDirectByteBuffer` function. Unlike regular
byte buffers, the storage is not allocated on the managed heap, and can
always be accessed directly from native code (get the address
with `GetDirectBufferAddress`). Depending on how direct
byte buffer access is implemented, accessing the data from managed code
can be very slow.

The choice of which to use depends on two factors:

1. Will most of the data accesses happen from code written in Java or in C/C++?
2. If the data is eventually being passed to a system API, what form must it be in? (For example, if the data is eventually passed to a function that takes a byte\[\], doing processing in a direct `ByteBuffer` might be unwise.)

If there's no clear winner, use a direct byte buffer. Support for them
is built directly into JNI, and performance should improve in future releases.