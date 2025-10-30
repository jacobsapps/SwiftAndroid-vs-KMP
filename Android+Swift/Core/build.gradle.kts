import java.nio.file.Files
import java.nio.file.Path

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.jacob.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
    }

    sourceSets.getByName("main").apply {
        java.srcDir(layout.buildDirectory.dir("generated/java"))
        jniLibs.srcDir(layout.projectDirectory.dir("build/android"))
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("org.swift.swiftkit:swiftkit-core:1.0-SNAPSHOT")
}

val swiftlyPath: String by lazy {
    val configured = project.findProperty("swiftly.path") ?: System.getenv("SWIFTLY_PATH")
    if (configured != null && configured.toString().isNotBlank()) return@lazy configured.toString()
    val candidates = listOf(
        "${System.getProperty("user.home")}/.swiftly/bin/swiftly",
        "${System.getProperty("user.home")}/.local/share/swiftly/bin/swiftly",
        "${System.getProperty("user.home")}/.local/bin/swiftly",
        "/usr/local/bin/swiftly",
        "/opt/homebrew/bin/swiftly"
    )
    candidates.firstOrNull { file(it).exists() }
        ?: error("swiftly not found. Set swiftly.path or SWIFTLY_PATH.")
}

val swiftSdkRoot: Path by lazy {
    val configured = project.findProperty("swift.sdk.path") ?: System.getenv("SWIFT_SDK_PATH")
    if (configured != null && configured.toString().isNotBlank()) return@lazy file(configured.toString()).toPath()
    val candidates = listOf(
        "${System.getProperty("user.home")}/Library/org.swift.swiftpm/swift-sdks",
        "${System.getProperty("user.home")}/.config/swiftpm/swift-sdks"
    )
    val path = candidates.map { file(it).toPath() }.firstOrNull { Files.exists(it) }
        ?: error("Swift SDK path not found. Set swift.sdk.path or SWIFT_SDK_PATH.")
    path
}

val swiftSdkBundle = "swift-DEVELOPMENT-SNAPSHOT-2025-10-16-a-android-0.1.artifactbundle"

val abis = mapOf(
    "arm64-v8a" to mapOf(
        "triple" to "aarch64-unknown-linux-android28",
        "runtimeDir" to "swift-aarch64",
        "ndkDir" to "aarch64-linux-android"
    ),
    "x86_64" to mapOf(
        "triple" to "x86_64-unknown-linux-android28",
        "runtimeDir" to "swift-x86_64",
        "ndkDir" to "x86_64-linux-android"
    )
)

val generatedJniLibs = layout.buildDirectory.dir("android")
val generatedJava = layout.buildDirectory.dir("generated/java")

val buildSwiftAll = tasks.register("buildSwiftAll") {
    group = "swift"
    description = "Build Swift library for all ABIs"

    dependsOn(abis.keys.map { "buildSwift${it.replace('-', ' ').replace(" ", "").capitalize()}" })
}

abis.forEach { (abi, info) ->
    val taskName = "buildSwift${abi.replace("-", "").replaceFirstChar { it.uppercase() }}"
    tasks.register(taskName, Exec::class) {
        group = "swift"
        description = "Build Swift for $abi"

        workingDir = layout.projectDirectory.asFile
        executable = swiftlyPath
        args("run", "swift", "build", "--product", "Core", "--swift-sdk", info.getValue("triple"))

        environment["JAVA_HOME"] = System.getenv("JAVA_HOME")
        environment["SWIFT_ANDROID_NDK_INCLUDE"] = swiftSdkRoot.resolve(swiftSdkBundle).resolve("swift-android/ndk-sysroot/usr/include").toString()
        environment["ENABLE_SWIFT_JAVA_PLUGIN"] = "1"
    }
}

val copyArtifacts = tasks.register("copyArtifacts") {
    group = "swift"
    description = "Copy Swift artifacts into jniLibs and generated Java sources"
    dependsOn(buildSwiftAll)

    doLast {
        val sdkPath = swiftSdkRoot.resolve(swiftSdkBundle)
        val swiftAndroidDir = sdkPath.resolve("swift-android")
        val runtimeBase = swiftAndroidDir.resolve("swift-resources/usr/lib")

        abis.forEach { (abi, info) ->
            val triple = info.getValue("triple")
            val runtime = runtimeBase.resolve(info.getValue("runtimeDir")).resolve("android")
            val outputDir = generatedJniLibs.get().dir(abi).asFile
            outputDir.mkdirs()

            val buildDir = layout.projectDirectory.dir(".build/$triple/debug").asFile
            buildDir.listFiles { _, name -> name.startsWith("lib") && name.endsWith(".so") }?.forEach { file ->
                file.copyTo(File(outputDir, file.name), overwrite = true)
            }

            listOf("libc++_shared.so").forEach { name ->
                swiftAndroidDir.resolve("ndk-sysroot/usr/lib/${info.getValue("ndkDir")}/$name").toFile()
                    .copyTo(File(outputDir, name), overwrite = true)
            }

            val runtimeLibs = listOf(
                "libswiftCore.so",
                "libswift_Concurrency.so",
                "libswift_StringProcessing.so",
                "libswift_RegexParser.so",
                "libswiftSwiftOnoneSupport.so",
                "libswiftDispatch.so",
                "libswiftSynchronization.so",
                "libFoundation.so",
                "libFoundationEssentials.so",
                "libFoundationInternationalization.so",
                "libFoundationNetworking.so",
                "lib_FoundationICU.so",
                "libBlocksRuntime.so",
                "libdispatch.so"
            )

            runtimeLibs.forEach { name ->
                val file = runtime.resolve(name).toFile()
                if (file.exists()) {
                    file.copyTo(File(outputDir, name), overwrite = true)
                }
            }
        }

        val generatedSourceDir = layout.projectDirectory.dir(".build/plugins/outputs/core/Core/destination/JExtractSwiftPlugin/src/generated/java").asFile
        if (generatedSourceDir.exists()) {
            generatedSourceDir.copyRecursively(generatedJava.get().asFile.apply { mkdirs() }, overwrite = true)
        }
    }
}

preBuild.dependsOn(copyArtifacts)
