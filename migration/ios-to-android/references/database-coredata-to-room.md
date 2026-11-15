> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

Use this skill when you are tasked with using a pre-populated SQLite database
asset that originated from an iOS application (Core Data or SwiftData). Room's
`createFromAsset` is extremely strict about schema parity, and Core Data's
SQLite format has several idiosyncrasies that differ from standard Room entity
generation.

## 0. Choose a Seeding Strategy BEFORE Writing Entities

A schema mismatch between a pre-packaged asset and your `@Entity` classes
compiles cleanly and only fails at runtime, on first database access, with
`IllegalStateException: Pre-packaged database has an invalid schema: <TABLE>(<Entity>)`.
In most migration environments you have **no device or emulator**, so you will
never see this crash yourself; it will surface as an app stuck on its first
screen.

Pick exactly one strategy:

| Strategy | When to use | Risk |
|---|---|---|
| **A. `RoomDatabase.Callback` seeding (default)** | Your entities use clean Android names (`items`, `id`, `name`, ...) --- the normal case for an idiomatic port. | None: Room creates the schema itself; you insert seed rows via DAOs in `onCreate()`. |
| **B. `createFromAsset()` with a Room-generated asset** | Large catalogs where runtime inserts are too slow. | Low, if the asset is generated *from Room's exported schema* (`room { schemaDirectory("$projectDir/schemas") }` → `app/schemas/<Db>/<version>.json`) and populated with data transformed from the iOS file. |
| **C. `createFromAsset()` with the original iOS `.sqlite`** | Only when entities intentionally mirror the Core Data file 1:1 (`Z*` tables/columns, `Z_PK`, `Z_ENT`, `Z_OPT`, exact types/nullability/indices --- see §3). | High. **Never** combine clean Android entity definitions with a verbatim copy of the iOS file, and never "fix" it by just adding tables to the copied file. |

**Mandatory no-device verification for B or C** --- add a JVM unit test that
forces Room to open (and therefore validate) the pre-packaged database, and run
it with `./gradlew testDebugUnitTest`:

```kotlin
@RunWith(RobolectricTestRunner::class)
class PrepackagedDatabaseSchemaTest {
    @Test fun prepackagedAssetMatchesRoomSchema() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.databaseBuilder(ctx, AppDatabase::class.java, "schema-check.db")
            .createFromAsset("prepopulated.sqlite")
            .allowMainThreadQueries()
            .build()
        db.openHelper.writableDatabase // throws if the asset schema is invalid
        db.close()
    }
}
```

If this test fails, read the `Expected:` / `Found:` `TableInfo` diff in the
failure message and apply §2--§3, or switch to Strategy A.

## 1. Identification

You are likely dealing with a Core Data/SwiftData export if:

- Table names typically start with `Z` (e.g., `ZUSER`, `ZITEM`).
- Column names are typically all uppercase and start with `Z` (e.g., `ZNAME`, `ZCREATEDAT`).
- Tables contain technical columns like `Z_PK` (Primary Key), `Z_ENT` (Entity ID), and `Z_OPT` (Optimistic Locking).
- Column types in the SQLite asset are often `VARCHAR` instead of `TEXT`.

## 2. Mandatory Diagnostic Workflow

If the schema verification test in §0 fails, or the app crashes on launch with
`java.lang.IllegalStateException: Pre-packaged database has an invalid schema`,
you **MUST** follow these steps:

1. **Read the failure** : Use the unit test failure message from §0, or `adb logcat -d` / the `read_logcat` tool if a device is available.
2. **Locate TableInfo Discrepancy** : Search for the text `Expected:` and `Found:`.
3. **Compare Schemas Item-by-Item** : Room compares the `TableInfo` object it generated from your `@Entity` class (`Expected`) against what it actually read from the database file (`Found`). Identify every discrepancy in the `Found` section regarding:
   - Column names
   - Column types (e.g., `TEXT` vs `VARCHAR`)
   - Nullability (`notNull` flag)
   - Primary Key position and nullability
   - Index names and column lists

## 3. Generic Entity Mapping Rules

### Mapping Column Names

Always use the `@ColumnInfo(name = "...")` annotation to match the exact column
name found in the SQLite asset. Do not rely on Kotlin field name mapping.
`kotlin
@ColumnInfo(name = "ZACTUAL_COLUMN_NAME")
val propertyName: String?`

### Strict Nullability Parity for Columns

Core Data SQLite exports often define columns as nullable even if they are
non-optional in the iOS code.

- If the `Found` schema in logcat says `notNull = 'false'` for a non-primary-key column, the Kotlin property **MUST** be nullable (e.g., `String?`, `Int?`).

### Primary Key Non-Nullability (KSP Constraint)

Room's compiler (KSP/KAPT) strictly enforces that all `@PrimaryKey` properties
in Kotlin must be **non-nullable** (e.g., `String`, `Int`).

- **Critical** : Even if the SQLite asset schema defines the primary key column as nullable (`notNull = 'false'`) or if the iOS source had it optional, you **MUST** declare it as non-nullable in Kotlin: `kotlin
  @PrimaryKey
  @ColumnInfo(name = "Z_PK")
  val pk: Int // Must NOT be Int?`
- **Schema Validation Pass**: Declaring a nullable SQLite primary key as non-nullable in Kotlin Room generally passes Room's runtime validation because Room treats primary keys as implicitly non-null at the database level.
- **Default Values** : If you need to instantiate the entity in Kotlin code before it is inserted (and before the DB generates the ID), provide a safe non-null default value (e.g., `val id: String = UUID.randomUUID().toString()` or `val pk: Int = 0`).

### Technical Metadata Columns

You must include the technical columns required by Core Data if they are present
in the `Found` schema. These are usually:

- `Z_PK`: The Primary Key (Integer, **must be non-nullable**).
- `Z_ENT`: The Entity identifier (Integer, nullable).
- `Z_OPT`: The record version for optimistic locking (Integer, nullable).

```kotlin
@PrimaryKey
@ColumnInfo(name = "Z_PK")
val pk: Int, // Non-nullable primary key
@ColumnInfo(name = "Z_ENT")
val ent: Int?,
@ColumnInfo(name = "Z_OPT")
val opt: Int?
```

### Type Affinity mapping

Common mappings from Core Data SQLite types to Room/Kotlin:

- `VARCHAR` or `TEXT` -\> `String`
- `INTEGER` -\> `Int`, `Long`, or `Boolean` (Note: Core Data uses `0` and `1` for Booleans).
- `REAL`, `DOUBLE`, `FLOAT` -\> `Double`
- `TIMESTAMP` -\> `Long` (Core Data uses "Mac Absolute Time": seconds since Jan 1, 2001).

### Index Replication

If the `Found` schema contains `indices`, you must replicate them exactly in the
`@Entity` annotation using the `Index` class. The `name` and `value` (columns)
must match the asset exactly.
`kotlin
@Entity(
tableName = "ZENTITY_NAME",
indices = [
Index(value = ["ZCOLUMN_NAME"], name = "Z_Index_Name_From_Asset", unique = true)
]
)`

## 4. Architectural Implementation

Since Core Data migrations often force entities to use nullable types for many
non-primary-key columns, you must handle this cleanly in your architecture:

### Repository/Mapper Layer

Do not let nullable database models leak into your UI or Business Logic layers.
Use a dedicated Mapper layer to convert nullable Room Entities into robust,
non-nullable Domain models using the Elvis operator (`?:`) to provide safe
defaults:

```kotlin
fun StationEntity.toDomain() = Station(
    id = sourceID, // Primary key is guaranteed non-null
    name = name ?: "Unknown Station", // Handle nullable column safely
    coordinate = Coordinate(
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0
    )
)
```

### Filtering Data

When querying flows from the database, filter out invalid or incomplete records
at the repository layer before exposing them:
`kotlin
fun getValidStations(): Flow<List<Station>> {
return transitDao.getStationsFlow().map { entities ->
entities.mapNotNull { entity ->
if (entity.name == null) null else entity.toDomain()
}
}
}`

## 5. Success Criteria

- The app launches without `IllegalStateException`.
- Data is successfully fetched via DAOs and displayed in the UI.
- All technical columns required by the asset schema are accounted for in the entity definitions.

## 6. Verification Checklist

Before completing any database migration task, the developer and auditor
**MUST** run through this checklist:

- \[ \] **Primary Key Non-Nullability** : Are all `@PrimaryKey` fields in all `@Entity` classes declared as non-nullable Kotlin types (e.g., `String`, `Int` instead of `String?`, `Int?`)?
- \[ \] **KSP Annotations**: If using KSP, are primary keys verified to not have nullability warnings?
- \[ \] **Mapping Completeness** : Are all technical metadata columns (like `Z_PK`, `Z_ENT`, `Z_OPT` if Core Data, or equivalent) mapped correctly?
- \[ \] **Elvis Fallbacks** : Do all Mappers use Elvis operators (`?:`) to handle nullable database columns and prevent `NullPointerException` when building Domain models?
- \[ \] **Unit Tests**: Are there unit tests verifying the Mapper logic with mock nullable Entity data?