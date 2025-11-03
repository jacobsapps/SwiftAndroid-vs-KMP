// swift-tools-version: 6.2

import CompilerPluginSupport
import PackageDescription
import class Foundation.FileManager
import class Foundation.ProcessInfo

let javaHome: String = {
    if let envHome = ProcessInfo.processInfo.environment["JAVA_HOME"], !envHome.isEmpty {
        return envHome
    }
    let candidates = [
        "/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home",
        "/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home",
        "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home"
    ]
    for path in candidates where FileManager.default.fileExists(atPath: path) {
        return path
    }
    fatalError("JAVA_HOME is not set and default JDK path was not found. Install a JDK or export JAVA_HOME.")
}()
let javaIncludePath = "\(javaHome)/include"
#if os(Linux)
let javaPlatformIncludePath = "\(javaIncludePath)/linux"
#elseif os(macOS)
let javaPlatformIncludePath = "\(javaIncludePath)/darwin"
#else
#error("Unsupported platform")
#endif

let package = Package(
    name: "RollerCoasterCore",
    platforms: [
        .macOS(.v15)
    ],
    products: [
        .library(
            name: "RollerCoasterCore",
            type: .dynamic,
            targets: ["RollerCoasterCore"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/swiftlang/swift-java", branch: "main")
    ],
    targets: [
        .target(
            name: "RollerCoasterCore",
            dependencies: [
                .product(name: "SwiftJava", package: "swift-java"),
                .product(name: "CSwiftJavaJNI", package: "swift-java"),
                .product(name: "SwiftJavaRuntimeSupport", package: "swift-java")
            ],
            swiftSettings: [
                .unsafeFlags(["-I\(javaIncludePath)", "-I\(javaPlatformIncludePath)"], .when(platforms: [.macOS])),
                .unsafeFlags(["-Xfrontend", "-warn-concurrency"]),
                .unsafeFlags(["-Xfrontend", "-strict-concurrency=minimal"])
            ],
            plugins: [
                .plugin(name: "JExtractSwiftPlugin", package: "swift-java")
            ]
        ),
        .testTarget(
            name: "RollerCoasterCoreTests",
            dependencies: ["RollerCoasterCore"]
        )
    ]
)
