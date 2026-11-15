To integrate the Ink API, add the dependencies to your app.

The Ink API library is available from the Google Maven repository.

### Kotlin

```kotlin
dependencies {
    val ink_version = "1.0.0"

    implementation "androidx.ink:ink-nativeloader:$ink_version"
    implementation "androidx.ink:ink-rendering:$ink_version"
    implementation "androidx.ink:ink-strokes:$ink_version"
    implementation "androidx.ink:ink-authoring-compose:$ink_version"
    implementation "androidx.ink:ink-brush-compose:$ink_version"
    implementation "androidx.ink:ink-geometry-compose:$ink_version"
    implementation "androidx.ink:ink-storage:$ink_version"
}
```

### Groovy

```groovy
dependencies {
    def ink_version = "1.0.0"

    implementation("androidx.ink:ink-nativeloader:$ink_version")
    implementation("androidx.ink:ink-rendering:$ink_version")
    implementation("androidx.ink:ink-strokes:$ink_version")
    implementation("androidx.ink:ink-authoring-compose:$ink_version")
    implementation("androidx.ink:ink-brush-compose:$ink_version")
    implementation("androidx.ink:ink-geometry-compose:$ink_version")
    implementation("androidx.ink:ink-storage:$ink_version")
}
```