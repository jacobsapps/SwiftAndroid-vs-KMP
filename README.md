# Swift for Android vs KMP

Monorepo containing a Swift-powered REST API, an Android app (Jetpack Compose + Swift JNI), and a shared Swift/Kotlin Multiplatform iOS client.

## Prerequisites
- Swift toolchain via [Swiftly](https://swiftlang.github.io/swiftly/) (matching the Android SDK snapshot)
- Android SDK/NDK + `adb`
- JDK 21 (newer versions break the SwiftJava toolchain)
- Node.js 18+

## Getting Started
1. **API** – `cd server && npm install && npm start`
2. **Swift package** – `cd Android+Swift/Coasters && ./gradlew :swiftcore:copyJniLibs`
3. **CLI** – in `Android+Swift/Core` run `swift run CoasterCLI` (server must be running)
4. **Android app** – from `Android+Swift/Coasters` run `./gradlew :app:assembleDebug` (or open in Android Studio)
5. **iOS app** – open `iOS+KMP/iosApp/iosApp.xcodeproj` and run from Xcode

## SwiftAndroid Notes

- Source of truth for the Swift domain model lives in `Android+Swift/Core/Sources/Core`. Before building the Android app you must copy any changes into `Android+Swift/Coasters/swiftcore/Sources/RollerCoasterCore` (the vendored module Android consumes).
- After every Swift edit run the regeneration pipeline from `Android+Swift/Coasters`:
  ```bash
  ./gradlew :swiftcore:copyJniLibs   # rebuilds Swift, refreshes JNI libs + bindings
  ./gradlew clean assembleDebug      # or use Android Studio’s Clean/Rebuild
  ```
  Skipping these steps leaves Android running stale `.so` files or generated Java stubs.

### Troubleshooting
- **Gradle can’t find `jni.h` / `JAVA_HOME`** – export JDK 21 in every shell: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`.
- **`:swiftcore:copyJniLibs` fails** – double-check the Swift toolchain (Swiftly snapshot) and Swift SDK path; rerun the task after copying the updated Swift sources.
- **Android UI shows empty results** – the app just wraps the Swift JSON feed; make sure the Express server (`server/`) is running and the emulator can reach `http://10.0.2.2:3000`.
