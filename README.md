# TWG Android Test

A product lookup app for The Warehouse Group: scan a barcode or search by keyword, then view
product details. The repository is mid-migration from the original Java/Android app to Kotlin
Multiplatform with Compose Multiplatform, adding an iOS target.

The legacy app still builds and runs. Nothing has been ported to the shared module yet beyond
scaffolding — see [Migration status](#migration-status).

## Module layout

| Module | What it is |
| --- | --- |
| `:app` | The original Java app. Pre-AndroidX support library, Retrofit/Gson/Glide, ZXing, XML layouts, six Activities. Untouched apart from its build script and one manifest attribute. |
| `:composeApp` | Kotlin Multiplatform module holding the shared Compose UI. Targets Android and iOS. Currently a placeholder screen. |
| `iosApp/` | Xcode project. A SwiftUI shell that hosts `:composeApp` through `ComposeUIViewController`. |

`:app` and `:composeApp` install side by side — the new module uses the applicationId
`nz.co.warehouseandroidtest.kmp` so both can sit on one device during the migration. The suffix
comes off when `:app` is removed.

## Migration status

Done:

- Build migrated to Kotlin DSL with a version catalog (`gradle/libs.versions.toml`).
- Toolchain moved to Gradle 9.7.1 / AGP 9.4.0 / Kotlin 2.4.20 / Compose Multiplatform 1.12.0.
- `:composeApp` created with Android and iOS targets, and an `expect`/`actual` `Platform`.
- `iosApp` Xcode project created, building and running on the simulator.

Not started — the legacy app is still the only thing that does any of this:

- Data layer. Retrofit + OkHttp + Gson must become Ktor Client + `kotlinx.serialization`;
  the seven classes under `app/src/main/java/.../data/` need porting to `@Serializable` Kotlin.
- The four real screens (search, results with pagination, product detail, barcode scan).
- Barcode scanning. `cn.yipianfengye.android:zxing-library` is Android-only and unmaintained;
  it needs an `expect`/`actual` split over CameraX/ML Kit and `AVCaptureMetadataOutput`.
- Guest login and the `SharedPreferences` user id, which needs a multiplatform key-value store.
- Image loading (Glide is Android-only), navigation, and DI.

`Platform.deviceHeader` already exists in `commonMain` because the API requires an `X-TWL-Device`
header that the legacy app hardcodes to `"Android"` in its OkHttp interceptor. Nothing consumes
it yet.

## Requirements

- JDK 17. The Gradle daemon is pinned to it via `gradle/gradle-daemon-jvm.properties`, since
  Gradle 9.7.1 does not run on newer JDKs.
- Android SDK platform 37.
- Xcode 15 or newer, for the iOS target only.

The API needs an Azure APIM subscription key. Put it in `local.properties` as
`twg.subscriptionKey`, or set `TWG_SUBSCRIPTION_KEY` in the environment. `:app` reads it at build
time into `BuildConfig`; without it the app builds but every request returns 401.

## Building and running

Android:

```bash
./gradlew :app:assembleDebug          # legacy app
./gradlew :composeApp:assembleDebug   # Compose Multiplatform app
```

Tests:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :composeApp:iosSimulatorArm64Test
```

iOS — open `iosApp/iosApp.xcodeproj`, pick a simulator and run. The Kotlin framework is built by
a run script phase, so no separate Gradle step is needed. From the command line:

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

## Toolchain notes

**SDK levels.** `compileSdk 37`, `minSdk 28`, `targetSdk 35`. `targetSdk` is deliberately behind
`compileSdk`: raising it opts into new runtime behaviour and is a separate decision from the
migration.

**No `iosX64` target.** Compose Multiplatform no longer publishes artifacts for the Intel-Mac
simulator. The Xcode project sets `EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64` to match, so
building for a generic simulator destination resolves to arm64 only.

**Three flags in `gradle.properties` exist only for the duration of the migration:**

| Flag | Why | Goes away when |
| --- | --- | --- |
| `android.builtInKotlin=false` | AGP 9 refuses to apply `com.android.application` alongside the Kotlin Multiplatform plugin. Google documents this pair as the temporary bypass. | `:composeApp` is split into a `com.android.kotlin.multiplatform.library` module plus a thin app module. |
| `android.newDsl=false` | Same cause. Re-enables the legacy variant API, which the Kotlin Gradle plugin still calls. AGP states this API **is removed in AGP 10.0** — that is the real deadline. | Same as above. |
| `android.uniquePackageNames=false` | AGP 9 turned duplicate library namespaces into a fatal manifest merger error. `com.android.support:animated-vector-drawable` and `:support-vector-drawable` both declare `android.support.graphics.drawable`, so `:app` cannot merge its manifest without this. | `:app` is deleted. |

Both remedies land naturally at the end of the migration, so all three are intended to disappear
together. Until then, `:app` builds only because of the third flag — worth knowing before anyone
tries to remove it.

**AndroidX.** `android.useAndroidX=true` is set project-wide for `:composeApp`. `:app` stays on
the pre-AndroidX support library; the two modules never share a classpath and Jetifier is off, so
nothing rewrites the legacy dependencies.

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
