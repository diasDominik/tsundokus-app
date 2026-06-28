# Tsundoku

Track every manga order from checkout to bookshelf — across Android, iOS, Desktop and Web from a single Kotlin codebase.

![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11-4285F4?logo=jetpackcompose&logoColor=white)
![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20iOS%20%7C%20Desktop%20%7C%20Web-lightgrey)
![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)

Tsundoku is a Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP) app for tracking manga orders — shipping, delays, and what you're reading next, all in one place. The UI, business logic and data layer are shared across all targets; only thin platform shells differ.

## Features

- **Orders** — add, edit and track manga orders from checkout to delivery; see status at a glance.
- **Reading list** — what you're reading and what's next.
- **Delays** — report a delayed order and keep its timeline accurate.
- **Accounts** — register, log in, password reset, and **verify-gated registration**: a new account must verify its email before it can sign in.
- **Deep links** — email verification and password-reset links open straight into the app (Android App Links + custom scheme; Nav3 official deep-link API).
- **Offline-first** — browse cached orders without a connection (local Room cache).
- **Settings** — theme (System / Light / Dark), currency, about, and an open-source licenses screen (AboutLibraries).

There are **no group/expense, push-notification, or guest-account features** — Tsundoku is single-user order tracking, and account email is the only transactional channel.

## Platforms

| Target | Shell |
|--------|-------|
| Android | `:androidApp` |
| iOS | `iosApp` (SwiftUI host) |
| Desktop (JVM) | `:composeApp` desktop entry |
| Web (WasmJS) | `:composeApp` web entry |

## Tech stack

- **Kotlin Multiplatform** + **Compose Multiplatform** UI
- **Clean Architecture** + **MVI** presentation (State / Event / ViewModel / Root + Screen)
- **Koin** (annotations + compiler) for dependency injection
- **Navigation 3** (type-safe routes + official `DeepLinkRequest` / `UriDeepLinkMatcher` deep links)
- **Ktor** client (typed `post`/`get` → `Result<T, DataError.Remote>`) for networking
- **Room** (KMP) for the local orders cache
- **kotlinx.serialization**, **KSafe** (Keystore/Keychain-encrypted session storage), **BuildKonfig** (build-time config)
- **AboutLibraries** for OSS license attribution
- Gradle **convention plugins** in `build-logic`, version catalog in `gradle/libs.versions.toml`

## Architecture

Modularized **by feature and by layer**. Dependencies point inward: `presentation → domain ← data`, with `core` shared by all features.

- **`:core:domain`** — pure Kotlin: models, `Result<D, E>`, error types, logging.
- **`:core:data`** — shared networking (`HttpClientFactory`), encrypted session (`SecureStore` / `KSafeSessionStorage`), preferences.
- **`:core:presentation`** — shared UI utilities (`UiText`, `ObserveAsEvents`, navigation contracts).
- **`:core:designsystem`** — theme, tokens and reusable Compose components.
- **`:features:*`** — each feature split into `domain` (interfaces, models), `data` (implementations, DTOs, mappers), `presentation` (screens, ViewModels, routes), optional `database`/`testing`.
- **`:composeApp`** — shared entry point: wires navigation + DI, hosts Desktop/Web `main`, the iOS `MainViewController`, and the deep-link matchers.
- **`:androidApp`** — Android application shell.

See [`AGENTS.md`](AGENTS.md) for detailed conventions.

## Project structure

```
composeApp/                 Shared app entry (DI + navigation + deep links), desktop & web main, iOS controller
androidApp/                 Android application
iosApp/                     iOS SwiftUI host (Xcode project)
core/
  data/  domain/  presentation/  designsystem/
features/
  authentication/  data · domain · presentation · testing
  orders/          data · domain · presentation · database · sqliteWasmWorker
  settings/        data · domain · presentation
build-logic/                Gradle convention plugins
gradle/libs.versions.toml   Version catalog
```

## Getting started

### Prerequisites

- JDK 21+
- Android Studio (latest stable) / IntelliJ IDEA
- Xcode (for iOS), on macOS

### Configuration

Build-time config is injected via BuildKonfig and **required** to build. Add these to `local.properties` (git-ignored) or provide them as environment variables in CI:

```properties
API_KEY=your-api-key          # must match the server's TSUNDOKU_API_KEY
BASE_URL_HTTP=https://your-backend.example.com
```

`API_KEY` is sent as the `x-api-key` header; `BASE_URL_HTTP` is the server base URL the client talks to. No Firebase/google-services config is needed.

### Run

```bash
# Android — install on a device/emulator (or run :androidApp from the IDE)
./gradlew :androidApp:installDebug

# Desktop (JVM), hot-reload enabled
./gradlew :composeApp:hotRunDesktop

# Web (WasmJS) — dev server
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run, or use the KMP run configuration.

### Build & test

```bash
./gradlew build          # build all targets
./gradlew check          # tests + lint (ktlint)
./gradlew ktlintFormat   # auto-format
```

## License

Licensed under the **GNU General Public License v3.0**. See [`LICENSE`](LICENSE).
