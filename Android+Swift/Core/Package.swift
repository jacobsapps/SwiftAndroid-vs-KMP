// swift-tools-version: 6.2

import CompilerPluginSupport
import PackageDescription
import class Foundation.FileManager
import class Foundation.Process
import class Foundation.ProcessInfo
import class Foundation.Pipe
import struct Foundation.URL

func locateJavaHome() -> String {
    if let envHome = ProcessInfo.processInfo.environment["JAVA_HOME"], !envHome.isEmpty {
        return envHome
    }
    #if os(macOS)
    if let detected = try? detectJavaHomeViaUsrLibexec(version: "21"), !detected.isEmpty {
        return detected
    }
    #endif
    let defaultHome = "/Library/Java/JavaVirtualMachines/openjdk.jdk/Contents/Home"
    if FileManager.default.fileExists(atPath: defaultHome) {
        return defaultHome
    }
    fatalError("JAVA_HOME is not set and default JDK path was not found. Install a JDK or export JAVA_HOME.")
}

#if os(macOS)
func detectJavaHomeViaUsrLibexec(version: String) throws -> String? {
    let process = Process()
    process.executableURL = URL(fileURLWithPath: "/usr/libexec/java_home")
    process.arguments = ["-v", version]
    let pipe = Pipe()
    process.standardOutput = pipe
    process.standardError = Pipe()
    try process.run()
    process.waitUntilExit()
    guard process.terminationStatus == 0 else { return nil }
    let data = pipe.fileHandleForReading.readDataToEndOfFile()
    return String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines)
}
#endif

let javaHome = locateJavaHome()
let javaIncludePath = "\(javaHome)/include"
#if os(Linux)
let javaPlatformIncludePath = "\(javaIncludePath)/linux"
#elseif os(macOS)
let javaPlatformIncludePath = "\(javaIncludePath)/darwin"
#else
#error("Unsupported platform")
#endif

let enableSwiftJavaPlugin = ProcessInfo.processInfo.environment["ENABLE_SWIFT_JAVA_PLUGIN"] == "1"

let corePlugins: [Target.PluginUsage] = enableSwiftJavaPlugin
    ? [.plugin(name: "JExtractSwiftPlugin", package: "swift-java")]
    : []

let package = Package(
    name: "Core",
    platforms: [
        .macOS(.v15)
    ],
    products: [
        .library(
            name: "Core",
            type: .dynamic,
            targets: ["Core"]
        ),
        .executable(
            name: "CoasterCLI",
            targets: ["CoasterCLI"]
        )
    ],
    dependencies: [
        .package(url: "https://github.com/swiftlang/swift-java", branch: "main")
    ],
    targets: [
        .target(
            name: "Core",
            dependencies: [
                .product(name: "SwiftJava", package: "swift-java"),
                .product(name: "CSwiftJavaJNI", package: "swift-java")
            ],
            resources: [
                .copy("swift-java.config")
            ],
            cSettings: [
                .unsafeFlags(["-I\(javaIncludePath)", "-I\(javaPlatformIncludePath)"], .when(platforms: [.macOS]))
            ],
            plugins: corePlugins
        ),
        .executableTarget(
            name: "CoasterCLI",
            dependencies: ["Core"]
        ),
        .testTarget(
            name: "CoreTests",
            dependencies: ["Core"]
        )
    ]
)
