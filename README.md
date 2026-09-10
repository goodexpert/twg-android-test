# TWG Android Test

A product lookup app for The Warehouse Group: search products by keyword, scan a barcode/QR
code, and view product details with price and any applicable specials. Originally a
Java/Android app; the deliverable is the Kotlin Multiplatform (Compose Multiplatform) port
under `:composeApp`. The legacy Java module is retained as `:app` for reference only.

An iOS target is included as a bonus — the spec asks for a native Android app.

## What's implemented

Everything the brief asks for lives in `:composeApp`, wired end-to-end on both Android and iOS:

- **Guest login** on cold start — `Login.json` with `Authorization: Guest`, `X-TWL-Device`
  from the platform. The returned `X-TWL-Token` is kept in a secure store
  (EncryptedSharedPreferences on Android, Keychain-backed settings on iOS) and injected on
  every subsequent request.
- **Search** — `Search.json`, submitted on enter or from a persisted recent-searches list
  for quick reuse.
- **Results list** — grid and list layouts (toggle persisted), skeleton loading, empty and
  error states with retry, offset-based pagination that prefetches before the user hits the
  end.
- **Product details** — `Product.json` with image, name, description, formatted price, and a
  Special/Clearance badge derived from the response flags.
- **Barcode / QR scanning** — cross-platform camera preview and decoder via the
  `network.chaintech:qr-kit` multiplatform library, with the same reader on Android and iOS
  and a re-arm gate so a single code cannot fire twice while the camera holds frame.
  Runtime camera permission is handled by a first-class permission manager (an
  `expect`/`actual` split over Android's `ActivityResultContracts` and iOS's
  `AVCaptureDevice`) with a denied-state fallback that offers a retry or a jump to system
  settings.
- **Deep links** — cold-start and warm-open URIs on `nz.co.thewarehousegroup.app.kmp://`
  resolve to typed destinations (`productList?query=…`, `productDetails?productId=…`). The
  same parser powers QR-scan payloads, so a QR sharing a `productDetails` URL round-trips
  through the scanner.
- **`minSdk = 28` (Android 9 Pie)** as the brief requires.
- **Unit tests** across the repositories, view models, storage layers, and deep-link parser.

## Module layout

| Module | What it is |
| --- | --- |
| `:composeApp` | The deliverable. Kotlin Multiplatform module holding all shared logic and Compose UI, targeting Android and iOS. |
| `iosApp/` | Xcode project. A SwiftUI shell that hosts `:composeApp` through `ComposeUIViewController`. |
| `:app` | The original Java app, kept as a reference so reviewers can compare the port line-by-line. Untouched apart from build-script tweaks needed to keep it compiling under AGP 9. |

`:app` and `:composeApp` install side by side — `:composeApp` uses the applicationId
`nz.co.warehouseandroidtest.kmp` so both can sit on one device during review. The suffix
comes off when `:app` is removed.

## Architecture at a glance

- **Presentation — MVI.** Each screen has a `Contract` (State/Intent/Effect), a
  `ViewModel : BaseViewModel<State, Intent, Effect>`, and a `Screen` that renders state and
  emits intents. One-off navigation and toasts travel as `Effect`s so recomposition never
  re-fires them. See `feature/*` and `ui/BaseViewModel.kt`.
- **Dependency injection — `AppContainer`.** A single process-scoped container assembles the
  HTTP client, API, repositories, and stores lazily. Screens receive the one dependency they
  need through their composables rather than reaching into the container (constructor
  injection over service locator). See `AppContainer.kt`.
- **Networking — Ktor 3 + `kotlinx.serialization`.** One `HttpClient` for API calls with an
  auth plugin that adds `Ocp-Apim-Subscription-Key`, `X-TWL-Device`, and the session token.
  A separate Ktor client backs Coil's image fetcher so the APIM subscription key never leaks
  to image hosts.
- **Persistence — `multiplatform-settings`.** Recent-searches and product-list preferences
  use the ordinary store; the session token uses the secure store
  (EncryptedSharedPreferences / Keychain) via an `expect`/`actual` factory.
- **Image loading — Coil 3** with the Ktor 3 network fetcher, so Android and iOS share the
  same HTTP stack for images.
- **Navigation — Navigation Compose 2.9** with type-safe `@Serializable` routes in
  `ui/Routes.kt`. Deep-link URIs are parsed into the same route objects, so navigation from
  taps, deep links, and QR scans all flow through `NavController.navigate(route)`.
- **Deep links.** Only the URL scheme is pinned in `AndroidManifest.xml` and `Info.plist`.
  Supported hosts live entirely in `deeplink/DeepLinks.kt`, so adding a new deep-linked
  destination is a Kotlin-only change with no platform-manifest touch and no
  Android/iOS drift.
- **Static analysis — detekt 1.23.8** with `detekt-formatting`. `./gradlew :composeApp:detekt`
  reports zero code smells on the current tree.

## Requirements

- JDK 17. The Gradle daemon is pinned to it via `gradle/gradle-daemon-jvm.properties`, since
  Gradle 9.7.1 does not run on newer JDKs.
- Android SDK platform 37.
- Xcode 15 or newer, for the iOS target only.

The API needs an Azure APIM subscription key. Put it in `local.properties` as
`twg.subscriptionKey`, or set `TWG_SUBSCRIPTION_KEY` in the environment. Both `:app` and
`:composeApp` read it at build time; without it the app compiles but every request returns
401.

## Building and running

Android:

```bash
./gradlew :composeApp:assembleDebug   # the deliverable
./gradlew :app:assembleDebug          # legacy Java app, for reference
```

iOS — open `iosApp/iosApp.xcodeproj`, pick a simulator and run. The Kotlin framework is built
by a run-script phase, so no separate Gradle step is needed. From the command line:

```bash
xcrun simctl boot "iPhone 17 Pro" && open -a Simulator

cd iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath /tmp/dd build

xcrun simctl install booted /tmp/dd/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch booted nz.co.warehouseandroidtest.kmp
```

To see a Kotlin exception from a crash on launch, use
`xcrun simctl launch --console-pty booted nz.co.warehouseandroidtest.kmp`.

### Trying a deep link

Once installed, either scheme can open the app directly:

```bash
# Android
adb shell am start -a android.intent.action.VIEW \
  -d 'nz.co.thewarehousegroup.app.kmp://productDetails?productId=R2450827'

# iOS Simulator
xcrun simctl openurl booted \
  'nz.co.thewarehousegroup.app.kmp://productList?query=drill'
```

## Tests

```bash
./gradlew :composeApp:testDebugUnitTest        # common + Android unit tests
./gradlew :composeApp:iosSimulatorArm64Test    # common tests on the iOS target
./gradlew :composeApp:detekt                   # static analysis
```

Test coverage spans the repositories (session, product, search), view models (search,
product list, product details, QR scanner), storage layers (session store, recent searches,
list preferences), and the deep-link parser and handler.

## Toolchain notes

**SDK levels.** `compileSdk 37`, `minSdk 28` (Android 9 Pie, per the brief), `targetSdk 35`.
`targetSdk` is deliberately behind `compileSdk`: raising it opts into new runtime behaviour
and is a separate decision.

**No `iosX64` target.** Compose Multiplatform no longer publishes artifacts for the Intel-Mac
simulator. The Xcode project sets `EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64` to match,
so building for a generic simulator destination resolves to arm64 only.

**Three flags in `gradle.properties` exist only for the duration of the migration:**

| Flag | Why | Goes away when |
| --- | --- | --- |
| `android.builtInKotlin=false` | AGP 9 refuses to apply `com.android.application` alongside the Kotlin Multiplatform plugin. Google documents this pair as the temporary bypass. | `:composeApp` is split into a `com.android.kotlin.multiplatform.library` module plus a thin app module. |
| `android.newDsl=false` | Same cause. Re-enables the legacy variant API, which the Kotlin Gradle plugin still calls. AGP states this API **is removed in AGP 10.0** — that is the real deadline. | Same as above. |
| `android.uniquePackageNames=false` | AGP 9 turned duplicate library namespaces into a fatal manifest merger error. `com.android.support:animated-vector-drawable` and `:support-vector-drawable` both declare `android.support.graphics.drawable`, so `:app` cannot merge its manifest without this. | `:app` is deleted. |

Both remedies land naturally at the end of the migration, so all three are intended to
disappear together. Until then, `:app` builds only because of the third flag — worth knowing
before anyone tries to remove it.

**AndroidX.** `android.useAndroidX=true` is set project-wide for `:composeApp`. `:app` stays
on the pre-AndroidX support library; the two modules never share a classpath and Jetifier is
off, so nothing rewrites the legacy dependencies.

## Repository history

The commits are grouped by feature and kept in order, matching the brief's guidance ("NO
GIANT INITIAL COMMIT"). Each PR in the log — build/toolchain migration, session, network,
list screen, details and QR, permissions, detekt, deep links — is a self-contained slice of
the port that compiles, tests, and runs on its own. `git log --oneline` is intended as
review reading.

## What isn't here (and why)

Things a reviewer might reasonably expect to see at this size — deliberately left out, with
the reasoning behind each so the omission is a decision, not a gap:

- **DI framework (Koin, Hilt, Kodein).** The dependency graph is one `HttpClient`, one API,
  three repositories, three stores, one deep-link handler. `AppContainer` reads
  top-to-bottom in one file and gives compile errors when a wire is missing. Koin's payoff
  (feature-module ownership, dynamic overrides) doesn't kick in at this scale, and its cost
  (runtime `NoBeanDefFound` in place of a compile error) is a regression here. Hilt is
  Android-only, which rules it out for a KMP module. Revisit when the project grows real
  feature modules.
- **MVI framework (Orbit, Ballast, MVIKotlin).** `BaseViewModel<State, Intent, Effect>` +
  a `Contract` per feature is ~40 lines of shared code. A library at this size would be
  more surface area than logic — and it would obscure the very MVI shape the test asks the
  reviewer to look at.
- **Room / SQLDelight.** Nothing in the app is relational. Recent searches and list
  preferences are two small key-value blobs; the session token is one string in a secure
  store. `multiplatform-settings` covers all three without a schema, a migration, or a
  generated DAO layer. If offline product caching or search history sync landed, SQLDelight
  would be the choice (KMP-native, generated typed queries).
- **Paging library (Paging 3 / MP Paging).** Offset pagination is ~15 lines in
  `ProductListViewModel` and testable without a library harness. Paging 3 is Android-only,
  and the MP alternatives don't yet pay for themselves at one paged screen. Reach for it
  when there are multiple paged sources or backed-by-DB sources that need diffing.
- **Mock library (MockK, Mockito).** Tests build concrete fakes (`FakeWarehouseApi`,
  `FakeSessionStore`, etc.). Fakes read like the real thing, refactor with it, and don't
  turn every test into a stub-recording exercise. Mocks earn their keep when the
  collaborator is expensive to fake by hand — none of these are.
- **Turbine for coroutine testing.** `viewModel.effects.take(N).toList()` is enough for
  short deterministic sequences and keeps the test dependency list minimal. Turbine would
  win once tests need timing assertions or long-running flow interaction.
- **UI / end-to-end tests (Compose UI test, XCUITest).** The ViewModels are already tested
  against fake APIs and stores at the "given input, emit these states and effects" level,
  which is most of what an E2E test would prove. A proper Compose Multiplatform UI-test
  setup — `createComposeRule` on Android, XCUITest on iOS, a `MockEngine`-backed
  `HttpClient` so tests aren't tied to the APIM subscription key — is worth building when
  navigation flows get non-trivial (multi-step forms, conditional back-stack behaviour,
  deep-link routing that gates revenue). At five screens with linear flows, the added
  confidence is small against the setup and flake cost.
- **Snapshot / screenshot tests (Paparazzi, Roborazzi, shot).** These earn their keep on a
  real design system with atomic components rendered under multiple themes, locales, RTL,
  and font-scale settings — which is exactly what protects a shared UI kit from silent
  visual regressions. This app has five feature screens with no design tokens or theming
  layer yet, so a baseline captured today would mostly re-lock provisional layout choices,
  and every dp/sp tweak would show up as noisy binary churn in review. Tooling is also
  Android-JVM only (Paparazzi/Roborazzi don't cover the iOS Compose target), so a KMP-wide
  visual-regression story would need a second pipeline anyway. Reach for it once the
  `ui/components/` layer stabilises into a component library — that's the point where the
  regression cost outweighs the maintenance cost.
- **Feature-module split (`:feature-*`, `:data`, `:domain`, `:network`).** With five
  screens and one team, a single `:composeApp` keeps navigation between call sites
  trivial. Splitting now would front-load Gradle configuration cost with none of the
  parallel-build or ownership payoff. The `feature/*` package boundaries already draw the
  seam a future split would follow.
- **Analytics / crash reporting (Firebase, Sentry).** Out of scope for a take-home; both
  would need real keys, real endpoints, and a privacy conversation. The MVI `Effect` layer
  is the natural place to hook one when it comes.
- **CI pipeline (GitHub Actions).** No `.github/workflows/` on this branch. `./gradlew
  :composeApp:testDebugUnitTest` and `./gradlew :composeApp:detekt` are the two commands a
  CI job would run; wiring them into a workflow is a one-file addition and the obvious next
  step once the repository is not being read primarily by humans.

---

## Original brief

> This Repo is the starting point for a test application. You are required to refactor it as
> specified in the brief obtained as part of the interview process.
>
> This isn't just about the functionality. We want to see what control you have over your code and
> how you represent your system and logic, and how elegantly it is done. We want to see your
> thought process - which means - NO GIANT INITIAL COMMIT. No one writes perfect code that adds
> the complete behavior for a new feature on the first time.
>
> If in doubt, impress us!
>
> ### Instructions
>
> Please either fork or clone this repo, and either provide us a link to your repo, or raise a PR
> (which will not be merged and will be declined automatically, but will be used to assess your
> code).
>
> As detailed on the brief, all new code should be in Kotlin. If you can use any of the new Kotlin
> features, do!
>
> All required features should be implemented - which means it both compiles and is to
> specification. Please add unit tests to your project.
