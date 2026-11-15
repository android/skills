> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill provides a comprehensive mapping for migrating common iOS third-party
libraries and framework extensions to their Android counterparts. It covers
dependency translation, async structures, data persistence, and testing
patterns.

**Official Reference Links:**

- [Retrofit](https://square.github.io/retrofit/)
- [Save data in a local database using Room \| Android Developers](https://developer.android.com/training/data-storage/room)
- [Dependency injection with Hilt \| Android Developers](https://developer.android.com/training/dependency-injection/hilt-android)
- [Guide to coroutines \| Kotlin](https://kotlinlang.org/docs/coroutines-guide.html)
- [Compose \| Coil](https://coil-kt.github.io/coil/compose/)

## Prerequisites

Before using this skill, you **Must** ensure the following prerequisites are
met:

- **Source Context:** Access to the existing iOS codebase and its dependency configuration (e.g., `Podfile`, `Package.swift`) is **Mandatory**.
- **Software Requirements:** Gradle 7.4+ (minimum required for stable Gradle Version Catalogs), Android Gradle Plugin (AGP) 8.0+, Kotlin 1.8+, and Gradle Version Catalogs (`gradle/libs.versions.toml`) are **Mandatory**.
- **Language:** Proficiency in Kotlin and understanding of coroutines is **Mandatory**.

## Limitations

This skill has the following limitations:

- **Library Versions:** Examples assume modern versions (Retrofit 2.9+, Room 2.5+, Hilt 2.44+). Older versions may require different syntax.
- **Custom Libraries:** This skill covers common industry-standard libraries. Proprietary or niche iOS libraries will require manual mapping.

## Glossary

| iOS Library/Framework | Android Equivalent | Definition |
|---|---|---|
| **Alamofire** | **Retrofit** | Type-safe HTTP client for networking. |
| **URLSession** | **OkHttp** | The underlying engine for HTTP requests. |
| **Realm / CoreData** | **Room** | Local database and ORM for persistence. |
| **Swinject** | **Hilt** | Dependency Injection framework. |
| **Combine / RxSwift** | **Kotlin Flow** | Reactive streams for asynchronous data. |
| **Kingfisher** | **Coil** | Image loading and caching library. |
| **XCTest** | **JUnit / MockK** | Testing frameworks for unit and instrumentation tests. |
| **Lottie** | **Lottie-Compose** | Vector animation rendering library. |

## Setup \& Dependencies

You **Must** evaluate the project requirements and add the necessary
dependencies to your `build.gradle.kts` file.

```kotlin
dependencies {
   // Networking: Retrofit
   val retrofitVersion = "2.9.0"
   implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
   implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion")

   // Database: Room
   val roomVersion = "2.5.2"
   implementation("androidx.room:room-runtime:$roomVersion")
   implementation("androidx.room:room-ktx:$roomVersion")
   kapt("androidx.room:room-compiler:$roomVersion")

   // DI: Hilt
   val hiltVersion = "2.48"
   implementation("com.google.dagger:hilt-android:2.48")
   kapt("com.google.dagger:hilt-android-compiler:2.48")

   // Image Loading: Coil
   implementation("io.coil-kt:coil-compose:2.4.0")

   // Testing: MockK & JUnit
   testImplementation("junit:junit:4.13.2")
   testImplementation("io.mockk:mockk:1.13.5")
}
```

## Instructions: Mapping Concepts

### 1. Networking: Alamofire to Retrofit

When migrating from Alamofire to Retrofit, you **Must** define a type-safe
interface for your API endpoints.

- **Input:** A target URL or endpoint and the expected response model.
- **Output:** A type-safe Kotlin interface and a repository implementation that returns a `Result` containing the model.

#### iOS (Alamofire)

```swift
AF.request("https://api.example.com/users/1").responseDecodable(of: User.self) { response in
   switch response.result {
   case .success(let user): print(user.name)
   case .failure(let error): print(error)
   }
}
```

#### Android (Retrofit)

You **Must** use `suspend` functions in the interface to leverage Coroutines.

```kotlin
// 1. Define Data Class
@JsonClass(generateAdapter = true)
data class User(val id: String, val name: String)


// 2. Define Interface
interface ApiService {
   @GET("users/{id}")
   suspend fun getUser(@Path("id") id: String): User
}


// 3. Usage in Repository
class UserRepository @Inject constructor(private val apiService: ApiService) {
   suspend fun fetchUser(id: String): Result<User> {
       return try {
           Result.success(apiService.getUser(id))
       } catch (e: Exception) {
           Result.failure(e)
       }
   }
}
```

#### Adding Interceptors (OkHttp)

To handle authentication tokens or logging, you **Must** use an OkHttp
Interceptor.

```kotlin
class AuthInterceptor(private val tokenProvider: TokenProvider) : Interceptor {
   override fun intercept(chain: Interceptor.Chain): Response {
       val request = chain.request().newBuilder()
           .addHeader("Authorization", "Bearer ${tokenProvider.getToken()}")
           .build()
       return chain.proceed(request)
   }
}
```

#### Logic: Handling Legacy RxJava

To determine the correct Retrofit return type, you **Must** follow this logic:

- **IF** the project has an existing, extensive RxJava implementation that is out of scope for migration, **THEN** you **Must** use Retrofit's `RxJava3CallAdapterFactory`.
- **ELSE** , you **Must** use `suspend` functions for all new networking code.

### 2. Data Persistence: CoreData/Realm to Room

Android's recommended ORM is Room. You **Must** map iOS models to Room entities
and use DAOs (Data Access Objects) for database operations.

- **Input:** iOS CoreData or Realm models.
- **Output:** Room entities, DAOs, and a Database class providing type-safe local persistence.

#### iOS (CoreData Example)

```swift
// CoreData usage typically involves NSManagedObjectContext
let context = persistentContainer.viewContext
let user = User(context: context)
user.name = "John"
try? context.save()
```

#### Android (Room)

```kotlin
// 1. Define Entity
@Entity(tableName = "users")
data class User(
   @PrimaryKey val id: String,
   val name: String
)


// 2. Define DAO
@Dao
interface UserDao {
   @Query("SELECT * FROM users WHERE id = :id")
   suspend fun getUserById(id: String): User?


   @Insert(onConflict = OnConflictStrategy.REPLACE)
   suspend fun insertUser(user: User)
}


// 3. Define Database
@Database(entities = [User::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
   abstract fun userDao(): UserDao
}
```

#### Database Migrations

When upgrading the database schema, you **Must** provide a `Migration` object.

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
   override fun migrate(database: SupportSQLiteDatabase) {
       database.execSQL("ALTER TABLE users ADD COLUMN age INTEGER DEFAULT 0 NOT NULL")
   }
}


// Usage in Database builder
Room.databaseBuilder(context, AppDatabase::class.java, "database-name")
   .addMigrations(MIGRATION_1_2)
   .build()
```

### 3. Dependency Injection: Swinject to Hilt

Hilt is the standard DI framework for Android. You **Must** use constructor
injection where possible.

#### iOS (Swinject Example)

```swift
let container = Container()
container.register(UserService.self) { _ in UserServiceImpl() }
let userService = container.resolve(UserService.self)!
```

#### Android (Hilt)

```kotlin
// 1. Application Class
@HiltAndroidApp
class MyApplication : Application()


// 2. Provide Dependencies via Module
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
   @Provides
   @Singleton
   fun provideApiService(): ApiService {
       return Retrofit.Builder()
           .baseUrl("https://api.example.com/")
           .addConverterFactory(MoshiConverterFactory.create())
           .build()
           .create(ApiService::class.java)
   }
}


// 3. Inject in Activity or ViewModel
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
   @Inject lateinit var apiService: ApiService
   // ...
}
```

#### Hilt Scopes

You **Must** use the correct scope annotation to control the lifecycle of your
dependencies from the following exhaustive list for common components:

- `@Singleton`: Single instance for the application's entire lifecycle.
- `@ActivityRetainedScoped`: Lives across configuration changes; shared by all ViewModels of an Activity.
- `@ViewModelScoped`: Lives for the lifetime of a specific ViewModel.
- `@ActivityScoped`: Lives for the lifetime of a specific Activity.
- `@FragmentScoped`: Lives for the lifetime of a specific Fragment.

```kotlin
@ViewModelScoped
class MyViewModelDependency @Inject constructor() {
   // ...
}
```

### 4. Concurrency: Combine/RxSwift to Flow

You **Must** map reactive operators from Combine/RxSwift to Kotlin Flow.

#### Mapping Table: Operators

| Combine Operator | Kotlin Flow Equivalent |
|---|---|
| `map` | `map` |
| `filter` | `filter` |
| `flatMap` | `flatMapMerge` / `flatMapConcat` |
| `sink` | `collect` |
| `@Published` | `StateFlow` / `MutableStateFlow` |

#### Example Mapping

**iOS (Combine):**
`swift
@Published var userData: User?
// ...
userService.fetchUser()
.receive(on: DispatchQueue.main)
.sink { completion in ... } receiveValue: { user in
self.userData = user
}`

**Android (StateFlow \& Coroutines):**
\`\`\`kotlin
private val _userData = MutableStateFlow(null) val userData: StateFlow = _userData.asStateFlow()

fun loadUser() {
viewModelScope.launch {
try {
val user = userRepository.fetchUser("1").getOrThrow()
_userData.value = user
} catch (e: Exception) {
// Handle error
}
}
}
\`\`\`

#### Combining Flows

To merge data from multiple sources, you **Must** use the `combine` operator.

```kotlin
val userFlow = userRepository.getUserFlow()
val settingsFlow = settingsRepository.getSettingsFlow()


val combinedFlow = userFlow.combine(settingsFlow) { user, settings ->
   UserProfile(user, settings)
}
```

#### Handling Events (PassthroughSubject to SharedFlow)

For one-off events (like navigation or showing a snackbar), you **Must** use
`SharedFlow` instead of `StateFlow`.

**iOS (Combine):**
`swift
let eventSubject = PassthroughSubject<String, Never>()
// ...
eventSubject.send("NavigateToDetail")`

#### Android (SharedFlow)

```kotlin
private val _events = MutableSharedFlow<String>()
val events: SharedFlow<String> = _events.asSharedFlow()


fun triggerEvent() {
   viewModelScope.launch {
       _events.emit("NavigateToDetail")
   }
}
```

### 5. Image Loading: Kingfisher to Coil

Coil is the recommended image loading library for Jetpack Compose.

#### iOS (Kingfisher)

```swift
let url = URL(string: "https://example.com/image.png")
imageView.kf.setImage(with: url)
```

#### Android (Coil in Compose)

```kotlin
import coil.compose.AsyncImage            // Coil 2.x (use coil3.compose.AsyncImage for Coil 3)

AsyncImage(
    model = item.imageUrl,
    contentDescription = item.name,
    contentScale = ContentScale.Crop,
    // Mirror the iOS fallback (e.g. a gradient/placeholder view when the URL is nil or fails)
    placeholder = painterResource(R.drawable.ph_item_image),
    error = painterResource(R.drawable.ph_item_image),
    fallback = painterResource(R.drawable.ph_item_image), // model == null
    modifier = Modifier.fillMaxWidth().height(170.dp)
)
```

> [!IMPORTANT]
> **Important:** Always provide `placeholder`, `error`, and `fallback` for remote images. Remote images are unavailable offline, while loading, and in JVM tests --- without a placeholder the UI renders a blank white box. If the iOS view shows a fallback (e.g. a `LinearGradient` rectangle when the image URL is nil), port it as the placeholder (e.g. a `ph_` gradient drawable per `resources-and-assets.md`).

### 6. Testing: XCTest to MockK

Use MockK for mocking in Kotlin unit tests.

#### iOS (XCTest + Stubbing)

```swift
class UserServiceTests: XCTestCase {
   func testFetchUser() {
       let mockRepo = MockUserRepository()
       mockRepo.stubbedUser = User(name: "John")
       let service = UserService(repo: mockRepo)
       // Assertions...
   }
}
```

#### Android (JUnit + MockK)

```kotlin
class UserServiceTest {
   private val mockRepo = mockk<UserRepository>()
   private val userService = UserService(mockRepo)


   @Test
   fun `test fetch user success`() = runBlocking {
       // Arrange
       val expectedUser = User("1", "John")
       coEvery { mockRepo.getUserById("1") } returns expectedUser


       // Act
       val result = userService.getUser("1")


       // Assert
       assertEquals(expectedUser, result)
   }
}
```

### 7. Animations: Lottie to Lottie-Compose

When migrating Lottie animations, you **Must** use `LottieAnimation` in Compose.

- **Input:** A Lottie JSON file or URL.
- **Output:** A Compose-compatible animation rendering.

#### iOS (Lottie)

```swift
let animationView = LottieAnimationView(name: "animation")
view.addSubview(animationView)
animationView.play()
```

#### Android (Lottie-Compose)

```kotlin
@Composable
fun MyAnimation() {
   val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animation))
   LottieAnimation(composition = composition)
}
```

## Tooling Integration Example

If a future tool is built from this skill to automate dependency checking, it
**Must** follow this structure to comply with benchmark standards:

### Verification Command \& Agnostic Agent Instructions

To verify that your Android dependencies are correctly configured:

1. **CLI Verification Command:** `bash
   ./gradlew dependencies --configuration releaseRuntimeClasspath`
2. **Agent Audit Instruction:** *Read `build.gradle.kts` and `gradle/libs.versions.toml` to verify that each
   mapped iOS library from `Podfile` / `Package.swift` has its corresponding
   Android Jetpack or third-party dependency declared.*

## Antipatterns

### DO

- **DO** use `suspend` functions for all asynchronous operations in Retrofit and Room.
- **DO** use `StateFlow` or `SharedFlow` to expose state from ViewModels to the UI.
- **DO** use constructor injection with `@Inject` in Hilt.
- **DO** use `AsyncImage` from Coil for loading images in Compose, always with `placeholder` / `error` / `fallback` painters.

### DON'T

- **DON'T** block the main thread with networking or database operations. Use `withContext(Dispatchers.IO)`.
- **DON'T** use heavy ORMs if a lightweight wrapper like Room is sufficient.
- **DON'T** manually manage dependency graphs if Hilt can be used.

### NEVER

- **NEVER** use `RxJava` in new greenfield projects unless explicitly required by legacy constraints. Use Coroutines and Flow.
- **NEVER** expose `MutableStateFlow` directly to the UI; always expose as read-only `StateFlow`.