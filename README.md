# androidzoom

The text-size setting shared by mikelward's Android apps: a multiplier on top
of the system font scale, adjustable with a two-finger pinch anywhere in the
app and by whatever control the app puts in Settings. `SPEC.md` is what it
promises; `AGENTS.md` is how to work on it.

## Modules

- **`zoom-core`** -- plain Kotlin, no Android. The range (80%-160%), the
  default, the clamp, and the pinch math (slop, gain, overshoot). Testable on
  the JVM, and kept that way by `:zoom-core:verifyNoAndroid`.
- **`zoom-compose`** -- Android, Compose. `FontSizeState` (what is on screen,
  including mid-gesture), `rememberFontSizeState`, `ProvideFontSize` (the
  scaled density plus the pinch), and the window helpers for dialogs and
  sheets. Depends on `zoom-core` with `api`.

Storage and the Settings controls are each app's own.

## Consuming it

By coordinate. This repository publishes a Maven repository into its own
`maven` branch, which raw.githubusercontent.com serves.

`settings.gradle.kts`, inside `dependencyResolutionManagement { repositories { … } }`:

```kotlin
maven {
    name = "androidzoom"
    url = uri("https://raw.githubusercontent.com/mikelward/androidzoom/maven")
    content { includeGroup("com.mikelward.androidzoom") }
}
```

`gradle/libs.versions.toml`:

```toml
[versions]
androidzoom = "1.0.N"

[libraries]
androidzoom-compose = { group = "com.mikelward.androidzoom", name = "zoom-compose", version.ref = "androidzoom" }
```

And the consumer's `gradle-update.yml`, so the weekly batch can see a release:

```yaml
with:
  extra-repositories: |
    https://raw.githubusercontent.com/mikelward/androidzoom/maven
  no-cooldown-for: |
    com.mikelward.androidzoom:zoom-core
    com.mikelward.androidzoom:zoom-compose
```

## Using it

```kotlin
// Wherever the app's theme wraps its screens.
val fontSize = rememberFontSizeState(
    persisted = settings.fontSize,               // the app's stored FontSizeSettings, from memory
    onScaleSettled = { settingsStore.setFontScale(it) },
    onPinchEnabledChange = { settingsStore.setPinchFontSize(it) },
)
MaterialTheme(colorScheme = colors) {
    ProvideFontSize(fontSize) {
        AppContent()
    }
}
```

A Settings slider drives the same state: `preview(scale)` while dragging,
`commit(scale)` on release, hosted in `StableInputDensity { }` so its own drag
is not restarted by the density it is changing. The pinch switch calls
`choosePinch(enabled)`. A dialog or sheet wraps its content in
`FontSizeWindow { }` (text only) or `FontSizePinchWindow { }` (text and pinch).

## Building

```sh
./gradlew check                          # tests, verifyNoAndroid
scripts/verify-version-derivation.sh     # the version and the staged Maven tree
cd zoom-core && ../gradlew test          # the core alone, no AGP needed
```
