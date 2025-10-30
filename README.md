# SwiftAndroid Project

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
