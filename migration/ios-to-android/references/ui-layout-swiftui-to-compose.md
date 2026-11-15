> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This guide provides a comprehensive, production-grade mapping from iOS
**SwiftUI** declarative layouts and view hierarchies to modern **Jetpack
Compose** (Material 3).

*** ** * ** ***

## 1. Core Layout Container Primitives

| SwiftUI Primitive | Jetpack Compose Equivalent | Android Import | Key Parameters / Notes |
|---|---|---|---|
| `VStack(alignment:spacing:)` | `Column(verticalArrangement:, horizontalAlignment:)` | `androidx.compose.foundation.layout.Column` | Arranges children vertically. |
| `HStack(alignment:spacing:)` | `Row(horizontalArrangement:, verticalAlignment:)` | `androidx.compose.foundation.layout.Row` | Arranges children horizontally. |
| `ZStack(alignment:)` | `Box(contentAlignment:)` | `androidx.compose.foundation.layout.Box` | Stacks children in Z-order. |
| `Spacer()` | `Spacer(modifier = Modifier.weight(1f))` | `androidx.compose.foundation.layout.Spacer` | Expands to fill available space inside Column/Row. |
| `Divider()` | `HorizontalDivider()` | `androidx.compose.material3.HorizontalDivider` | Visual separator line. |
| `GeometryReader` | `BoxWithConstraints` | `androidx.compose.foundation.layout.BoxWithConstraints` | Exposes `maxWidth`, `maxHeight` constraints. |

> [!CAUTION]
> **Caution:** **Compose Layout Anti-Squeeze Invariant (Preventing Vertical Character
> Wrapping)**:

**THE INVARIANT --- apply to every `Row` you write, without exception** : Whenever
a horizontal `Row` contains **flexible content** (a title/description `Text` or
`Column`) beside one or more **fixed-intent elements** (a `Button`, price
`Text`, metric badge, streak pill, `FilterChip`, `SegmentedButton`,
`IconButton`, or `Switch`), apply `Modifier.weight(1f)` to the flexible side
**and** `maxLines = 1` to every fixed-intent element's `Text`. Without an
explicit weight, Compose gives the flexible child its full intrinsic width and
squeezes the fixed-intent sibling to its minimum, wrapping text one character
per line (`B / u / y`) on a typical phone width (roughly 360--411dp).

**Detection heuristic** : A `Row` with two or more children where **none**
carries `Modifier.weight(...)` is unsafe --- assign the weight before moving on.
`Arrangement.SpaceBetween` does **NOT** substitute for a weight.

1. **Trailing action buttons** : A banner or card-header `Column` beside a `Button` (e.g. `"Continue"`, `"Upgrade"`, `"Retry"`) must give the `Column` `Modifier.weight(1f)` and the button label `maxLines = 1`.
2. **Trailing prices \& badges** : In any `Row` containing a title/subtitle `Column` on the left and a price/metric `Text` (such as a price, distance, rating or timestamp) or count badge (`"3 new"`) on the right, give the left `Column` `Modifier.weight(1f)` and the right `Text` `softWrap = false, maxLines = 1`.
3. **Full-Width Segmented Controls \& Multi-Option Chips** : When porting a SwiftUI `Picker(..., .segmented)` or a preference row (e.g., a theme, sort-order or map-type selector with three or more options), **NEVER** place a `Row` of `FilterChip`s or `SegmentedButton`s on the same horizontal `Row` beside a section label. Always stack the section title above in a `Column`, and place the selector below in its own full-width `Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp))` where each chip/button uses `Modifier.weight(1f)` and `Text(..., maxLines = 1, overflow = TextOverflow.Ellipsis)`.

*** ** * ** ***

## 2. Scrollable Containers \& Dynamic Lists

### A. Dynamic Lists (`List` / `ForEach` ➡️ `LazyColumn`)

In SwiftUI, `List` provides lazy recycling of rows. In Android Jetpack Compose,
you **MUST** use `LazyColumn` with typed `items()`:

```kotlin
// Android Jetpack Compose
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ItemListScreen(
    items: List<ItemModel>,
    onItemClick: (ItemModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = items,
            key = { item -> item.id }
        ) { item ->
            ItemCard(item = item, onClick = { onItemClick(item) })
        }
    }
}
```

### B. Vertical Scrollable Screen (`ScrollView` ➡️ `verticalScroll`)

For static content that needs vertical scrolling (forms, detail pages):

```kotlin
// Android Jetpack Compose
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column

@Composable
fun DetailOverviewScreen(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scrollable content
    }
}
```

### C. Horizontal Carousels (`ScrollView(.horizontal)` ➡️ `LazyRow`)

```kotlin
// Android Jetpack Compose
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(horizontal = 16.dp)
) {
    items(carouselItems, key = { it.id }) { item ->
        ItemChip(item = item)
    }
}
```

*** ** * ** ***

## 3. Structural App Components (Material 3)

| SwiftUI Component | Jetpack Compose (Material 3) |
|---|---|
| `TabView` / `tabItem` | `Scaffold` + `NavigationBar` + `NavigationBarItem` |
| `NavigationView` / `NavigationStack` | `NavHost` / `rememberNavController()` |
| `NavigationView` Toolbar / Title | `TopAppBar` / `CenterAlignedTopAppBar` / `MediumTopAppBar` |
| `Card` / rounded visual box | `Card` / `OutlinedCard` / `ElevatedCard` |
| `Button` | `Button`, `OutlinedButton`, `FilledTonalButton`, `TextButton`, `IconButton` |
| `Text` with styling | `Text(stringResource(R.string.title), style = MaterialTheme.typography.titleLarge)` |
| `Text("Literal")`, `Button("Literal")`, `Label("Literal", systemImage:)`, `.navigationTitle("...")`, `.alert("...")`, `TextField("Placeholder", ...)` | `stringResource(R.string.key)` for the text / label / title / placeholder. SwiftUI string literals are implicit `LocalizedStringKey` lookups; Compose literals are not, so never port them as `Text("Literal")`. |
| `String(localized:)` / `NSLocalizedString` / generated accessors (SwiftGen `L10n.key`, R.swift) | `stringResource(R.string.key)` in composables; `context.getString(R.string.key)` outside composition |
| `Text("Hello \(name)")` (interpolation) | `stringResource(R.string.greeting, name)` with `<string name="greeting">Hello %1$s</string>` |
| `.accessibilityLabel("...")` | `Modifier.semantics { contentDescription = ... }` or `contentDescription = stringResource(R.string.key)` |
| `ProgressView` | `CircularProgressIndicator` / `LinearProgressIndicator` |
| `Toggle` / `Toggle(isOn:)` | `Switch(checked = ..., onCheckedChange = ...)` |
| `TextField` | `OutlinedTextField(value = ..., onValueChange = ...)` |

*** ** * ** ***

## 4. State Connection \& Observation in Compose

Always collect state from ViewModels using `collectAsStateWithLifecycle()` to
maintain lifecycle safety:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is FeatureUiState.Loading -> LoadingView()
        is FeatureUiState.Success -> ContentList(items = state.items)
        is FeatureUiState.Error -> ErrorMessage(message = state.message)
    }
}
```

*** ** * ** ***

## 5. Anti-Premature-Exit \& Completeness Mandate

- **Never stub layouts with empty placeholders** : Author the complete composables (`LazyColumn`, `Cards`, metrics, buttons) representing the source SwiftUI screens.
- **Never yield on background compilations** : If `./gradlew assembleDebug` runs in the background, wait for completion synchronously (`WaitMsBeforeAsync: 10000`) or monitor until finished. Do not output text-only waiting messages before authoring target files.