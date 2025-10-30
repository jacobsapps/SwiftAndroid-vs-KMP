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

## SwiftAndroid Quick Start

The official documentation is quite early, and I had to troubleshoot for a couple of days to get anywhere with this. 
This guide should help you get through the teething issues with setup. Follow these steps on macOS to run every part of the project (API, Swift core, Android, iOS, CLI).

### 1. Install prerequisites
Follow the [official Swift SDK for Android quick start](https://www.swift.org/documentation/articles/swift-sdk-for-android-getting-started.html) up to **Hello World on Android – Step 3** (the host `swift build` run). Stop before the cross-compilation step and return here to use the project-specific workflow below.

You should now have:
- [Swiftly](https://swiftlang.github.io/swiftly/) matching toolchain `swift-DEVELOPMENT-SNAPSHOT-2025-10-16-a`.
- Xcode 16+ (provides the macOS SDK and command-line tools).
- Android Studio (or command-line Android SDK + NDK r26+, platform-tools `adb`).
- Homebrew JDK 21 (`brew install openjdk@21`) and export once per shell:  
  ```bash
  export JAVA_HOME=$(/usr/libexec/java_home -v 21)
  ```
- Node.js 18 or newer.

### 2. Clone & open a shell
```bash
git clone <repo-url>
cd SwiftAndroid
```

### 3. Start the REST API
```bash
cd server
npm install
npm start
```
Leave this running (default port `http://localhost:3000`).

### 4. Prepare Swift → Android artifacts
```bash
cd ../Android+Swift/Coasters
./gradlew :swiftcore:copyJniLibs
```
This task cross-compiles the Swift `Core` library for all Android ABIs and copies JNI-ready `.so` files plus required Swift runtime libraries into `app/src/main/jniLibs/`.

### 5. Build the Android app
```bash
./gradlew :app:assembleDebug
```
The generated APK expects the server from step 3. Install on an emulator/device with `adb install app/build/outputs/apk/debug/app-debug.apk`.

### 6. Run the Swift CLI (optional smoke test)
```bash
cd ../Core
swift run CoasterCLI
```
The CLI fetches data from the same local API; ensure step 3 is active.

### 7. Build & run the iOS app
```bash
cd ../../iOS+KMP
open iosApp/iosApp.xcodeproj
```
Select a simulator target in Xcode and press Run. Xcode will trigger the KMP gradle sync automatically.

### 8. Troubleshooting
- **“JAVA_HOME is not set”** – run `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` in every shell before Gradle or Swift commands.
- **Swift build fails on Android link step** – rerun step 4; it wires in OpenSSL/zlib automatically.
- **App returns empty data** – confirm the Express server is running and the Android device/emulator can reach `http://10.0.2.2:3000` (emulator) or your host IP/device bridge.
- **Large build folders staged in git** – remove them with `git rm -r --cached Android+Swift/Coasters/swiftcore/.build-*`; `.gitignore` already blocks them on future commits.

You now have the server, Swift core library, Android app, iOS app, and CLI all working from one consistent set of commands.
