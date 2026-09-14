# iosApp

This directory holds the Swift/SwiftUI wrapper that hosts the shared Compose
Multiplatform UI on iOS, in the shape produced by the JetBrains KMP wizard
(kmp.jetbrains.com, template "Compose Multiplatform (UI shared)"):

```
iosApp/
  iosApp/
    iOSApp.swift        // @main App entry point
    ContentView.swift    // UIViewControllerRepresentable wrapping MainViewController()
    Info.plist
```

**Concern — no `iosApp.xcodeproj` is checked in.** This sandbox has only the
Xcode Command Line Tools installed (no full Xcode: `xcodebuild` is not
available — `xcode-select -p` resolves to
`/Library/Developer/CommandLineTools`), and an Xcode `.xcodeproj/project.pbxproj`
is not something that can be hand-authored reliably without a real Xcode
install to generate and validate it against. Rather than check in a
project file nobody has verified opens or builds, this task deliberately
stopped at the Swift source + Info.plist shape above and flags the gap here,
per the task brief's own guidance for this exact situation.

## Regenerating `iosApp.xcodeproj` on a machine with Xcode installed

The `composeApp` module already builds the `ComposeApp.framework` these
Swift files import (`import ComposeApp`, calling
`MainViewControllerKt.MainViewController()`). Two ways to get a working
Xcode project on top of the existing Swift sources:

1. **Re-run the JetBrains KMP wizard** (https://kmp.jetbrains.com) with
   project name `CalmColoring`, package `com.calmcoloring.app`, targets
   Android + iOS, template "Compose Multiplatform (UI shared)". Download
   the zip and copy only its generated `iosApp/iosApp.xcodeproj` (and, if
   different, `iosApp/iosApp/Assets.xcassets`) into this directory — the
   `iOSApp.swift` / `ContentView.swift` / `Info.plist` here already match
   what the wizard would generate for this package name, so keep the ones
   in this repo.
2. **Create a new Xcode project by hand**: File > New > Project > iOS App
   (SwiftUI, Swift), product name `iosApp`, bundle identifier
   `com.calmcoloring.app`, save it into this directory (replacing the
   generated `ContentView.swift`/`iOSApp.swift` with the ones already
   here). Then add a Run Script build phase before "Compile Sources" that
   invokes the Kotlin/Native `embedAndSignAppleFrameworkForXcode` Gradle
   task from `composeApp`, per the standard KMP "Compose Multiplatform
   (UI shared)" wizard output:

   ```
   cd "$SRCROOT/.."
   ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
   ```

   and add `$(SRCROOT)/../composeApp/build/xcode-frameworks` (or wherever
   that task stages `ComposeApp.framework` for the active configuration
   and platform) to Framework Search Paths.

## What was verified in this sandbox

- `./gradlew :composeApp:compileKotlinIosSimulatorArm64` — **succeeds**.
  Kotlin/Native downloaded its own LLVM/sysroot bundle and compiled
  `iosMain`'s `MainViewController.kt` (and all of `commonMain`) against
  the `iosSimulatorArm64` target with no Xcode involvement.
- `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` — **fails**,
  because linking a Kotlin/Native binary against the iOS SDK invokes
  `xcrun xcodebuild -version` to locate the SDK, which needs full Xcode,
  not just the Command Line Tools. This is the point past which a full
  Xcode install is required — see the task report for the exact error.
