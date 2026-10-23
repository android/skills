To configure your app's Cast session, provide an [`OptionsProvider`](https://developers.google.com/android/reference/com/google/android/gms/cast/framework/OptionsProvider). Use the
[`CastOptions`](https://developers.google.com/android/reference/com/google/android/gms/cast/framework/CastOptions) object built by the provider to set the receiver application ID,
manage session lifecycles, and customize media playback behavior.

## Use the default options provider

For a basic setup that uses the default Cast receiver application, add the
`DefaultCastOptionsProvider` to your app's `AndroidManifest.xml` file:

    <application>
      ...
      <meta-data
        android:name="com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME"
        android:value="androidx.media3.cast.DefaultCastOptionsProvider" />
      ...
    </application>

## Create a custom options provider

For more advanced configurations, such as setting a custom receiver application
ID, you need to create your own `OptionsProvider`.

### 1. Declare the provider in your manifest

First, declare your custom provider in `AndroidManifest.xml`. Make sure to use
the fully qualified class name.

    <application>
      ...
      <meta-data
        android:name="com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME"
        android:value="path.to.your.class.MyCustomCastOptionsProvider" />
      ...
    </application>

### 2. Implement the OptionsProvider interface

Next, create a class that implements the `OptionsProvider` interface. In this
class, you must override `getCastOptions()` to return a `CastOptions` instance.
The custom `OptionsProvider` class is where you configure your Cast session, for
example, by setting your custom receiver application ID.

For more information, see [CastOptions.Builder](https://developers.google.com/android/reference/com/google/android/gms/cast/framework/CastOptions.Builder).


### Kotlin

```kotlin
class MyCustomCastOptionsProvider : OptionsProvider {

  override fun getCastOptions(context: Context): CastOptions {
    return CastOptions.Builder()
      .setReceiverApplicationId(APP_ID)
      .setRemoteToLocalEnabled(true)
      .build()
  }

  override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? {
    return null
  }

  companion object {
    // Add your receiver app ID in <APP_ID>.
    private const val APP_ID = "<APP_ID>"
  }
}
```

### Java

```java
public static final class MyCustomCastOptionsProvider implements OptionsProvider {

  // Add your receiver app ID in <APP_ID>.
  public static final String APP_ID = "<APP_ID>";

  @Override
  public CastOptions getCastOptions(Context context) {
    return new CastOptions.Builder()
        .setReceiverApplicationId(APP_ID)
        .setRemoteToLocalEnabled(true)
        .build();
  }

  @Override
  public @Nullable List<SessionProvider> getAdditionalSessionProviders(Context context) {
    return null;
  }
}
```

<br />