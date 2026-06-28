# Tsundoku AI Agent Guide

Doc: Tsundoku architecture, patterns, guidelines for AI agents.

## 1. Project Overview
- **Tech Stack:** Kotlin Multiplatform (KMP), Compose Multiplatform (CMP).
- **Architecture:** Clean Architecture + MVVM.
- **Dependency Injection:** Koin.
- **Navigation:** Navigation 3 (Nav3).
- **Networking:** Ktor with ContentNegotiation (Serialization).
- **Database:** Room (KMP).
- **Targets:** Android, iOS, Desktop (JVM), Web (WasmJS).
- **Package Root:** `uk.tsundokus`.

---

## 2. Module Structure & Hierarchy

Project modularized by feature and layer. Use **typesafe project accessors** (e.g., `projects.core.domain`).

### Core Modules (`:core:*`)
- **`:core:domain`**: Pure Kotlin. Business models, standard `Result<D, E>` type, `Error` interfaces, global loggers.
- **`:core:data`**: Shared networking setup (`HttpClientFactory`), standard Ktor configs, common data sources.
- **`:core:presentation`**: Shared UI logic, `UiText` for localized strings, `ObserveAsEvents` for one-time events.
- **`:core:designsystem`**: Shared Compose tokens (Color, Type, Shape), reusable atomic components (Buttons, TextFields, etc.).

### Feature Modules (`:features:*:*`)
Each feature split into:
- **`:domain`**: Interfaces (`Service`, `Repository`), business models, validators.
- **`:data`**: Domain interface impls, DTOs, mappers, API services.
- **`:presentation`**: Compose UI (Screens, Components), ViewModels, navigation routes.
- **`:database`** (Optional): Room entity definitions and DAOs.

### Application Modules
- **`:composeApp`**: Main entry point for shared UI. Aggregates all features, wires Navigation/DI.
- **`:androidApp`**: Android-specific config, depends only on `:composeApp`.

---

## 3. Architecture Layers

### Domain Layer (Pure Kotlin)
- Define `interface AuthService` or `interface GroupRepository`.
- Use `uk.tsundokus.core.domain.util.Result<D, E>` for all operation outcomes.
- Models: data classes, ideally immutable.

### Data Layer
- Implement domain interfaces.
- Use `Ktor` for networking.
- Use `Mappers` to convert DTOs to domain models.
- **Convention:** DTOs suffix with `Request` or `Response`.

### Presentation Layer (Compose Multiplatform)
- **ViewModels:**
    - Use `StateFlow` for UI state (e.g., `state: StateFlow<LoginState>`).
    - Use `Channel` and `Flow` for one-time events (e.g., `events: Flow<LoginEvent>`).
    - Inherit from `androidx.lifecycle.ViewModel`.
- **UI Components:**
    - `Root` composables (e.g., `LoginRoot`) handle ViewModel interaction and event observation.
    - Screen composables (e.g., `LoginScreen`) stateless, take data/callbacks.
    - Use `ObserveAsEvents` to handle ViewModel events (Snackbars, Navigation).

---

## 4. Navigation (Navigation 3)

- **Routes:** `@Serializable` data classes/objects in feature's `presentation` module (e.g., `data object Home : NavKey`).
- **Graphs:** Features define `EntryProviderScope<NavKey>.featureGraph` extension.
- **Wiring:** All feature graphs aggregated in `composeApp/App.kt` via `NavDisplay`.
- **Top-level Tabs:** Implement `TopLevelTab` and `LoggedIn` interfaces for consistent bottom bar behavior.

---

## 5. Dependency Injection (Koin)

- Each module defines `val moduleName = module { ... }`.
- Feature modules aggregated in `composeApp/src/commonMain/kotlin/uk/tsundokus/composeapp/App.kt`.
- Use `koinViewModel()` in Composables.

---

## 6. Design System & Theming

- **Theme:** `TsundokuTheme` (built on Material3).
- **Tokens:** In `:core:designsystem`. Use `MaterialTheme.colorScheme` or custom `TsundokuTheme` properties.
- **Resources:** Use `Res.string.key` or `Res.drawable.key` via Compose Resources.
- **Localization:** Managed in `composeResources/values/strings.xml` within each module.

---

## 7. Compose Previews

Use multi-preview annotations from `:core:designsystem` for consistent testing across themes and devices.

### Multi-preview Annotations
- **`@PreviewThemes`**: Light and Dark mode previews. **Preferred for most components.**
- **`@PreviewPhones`**: Portrait and Landscape previews for phones.
- **`@PreviewScreenSizes`**: Phone, Foldable, Tablet, Desktop, Web previews.
- **`@PreviewAll`**: Every theme+screen combination (14 previews). Use sparingly.

### Pattern
Wrap previewed component in `TsundokuTheme` and `Surface` (if needed for background).
```kotlin
@PreviewThemes
@Composable
private fun MyComponentPreview() {
    TsundokuTheme {
        Surface {
            MyComponent()
        }
    }
}
```

---

## 8. Convention Plugins (build-logic)

**NEVER** configure KMP manually. Use:
- `tsundoku.convention.kmp.library`: Standard KMP library.
- `tsundoku.convention.cmp.library`: CMP library with Compose dependencies.
- `tsundoku.convention.cmp.feature`: Feature presentation module (includes VM, Lifecycle, Core Presentation).
- `tsundoku.convention.room`: Enables Room with KSP.
- `tsundoku.convention.buildkonfig`: BuildConfig-like constants.

---

## 9. Source Set Hierarchy & Platform Code

Custom hierarchy template in `build-logic` (`HierarchyTemplate.kt`):
```
common
├── mobile  (android + ios)
├── web     (wasmJs)
├── native  (ios + macos)
│   └── apple → ios, macos
└── desktop (jvm)
```
- **Ktor engines:** `okhttp` (android), `darwin` (native), `js` (web), `apache5` (desktop).
- **Expect/Actual:** Use sparingly. Prefer interfaces in `commonMain`, platform-specific impls via DI.

## 10. Coding Guidelines

### Naming
- **Composables:** PascalCase. Root composables end in `Root`.
- **ViewModels:** `FeatureViewModel`.
- **DI Modules:** `featureDataModule`, `featurePresentationModule`.

### Patterns
- **Error Handling:** Always use `Result` type. Convert `DataError` to `UiText` via `asUiText()`.
- **Async Work:** `viewModelScope.launch` in ViewModels. `Dispatchers.IO` for heavy/blocking calls (Ktor/Room non-blocking).
- **Immutability:** Prefer `val` and `data class` with `copy()`.

### Testing
- **commonMain:** Use **Mokkery** for mocking interfaces.
- **androidMain / jvmMain:** Use **Mockk**.
- Target unit tests for domain logic and ViewModels.

---

## 11. Critical Workflows
- **Sync:** `./gradlew help` (triggers sync).
- **Format:** `./gradlew ktlintFormat`.
- **Build:** `./gradlew assembleDebug`.
- **Local Config:** `local.properties` must have `API_KEY`.