plugins {
    id("com.utopia-rise.godot-kotlin-jvm") version "0.13.0-4.4.1"
    // godot-kotlin-jvm uses Kotlin 2.0.21
    kotlin("plugin.serialization") version "2.0.21"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
}

godot {
    // ---------Setup-----------------

    // the script registration which you'll attach to nodes are generated into this directory
    registrationFileBaseDir.set(projectDir.resolve("gdj"))

    // Create .gdj files from all JVM scripts
    isRegistrationFileGenerationEnabled.set(true)

    // enable coroutine support to allow for godot's async functions to be used with kotlin coroutines
    isGodotCoroutinesEnabled.set(true)

    // defines whether the script registration files should be generated hierarchically according to the classes package path or flattened into `registrationFileBaseDir`
    //isRegistrationFileHierarchyEnabled.set(true)

    // defines whether your scripts should be registered with their fqName or their simple name (can help with resolving script name conflicts)
    //isFqNameRegistrationEnabled.set(false)

    // ---------Android----------------

    // NOTE: Make sure you read: https://godot-kotl.in/en/stable/user-guide/exporting/#android as not all jvm libraries are compatible with android!
    // IMPORTANT: Android export should to be considered from the start of development!
    //isAndroidExportEnabled.set(false)
    //d8ToolPath.set(File("${System.getenv("ANDROID_SDK_ROOT")}/build-tools/35.0.0/d8"))
    //androidCompileSdkDir.set(File("${System.getenv("ANDROID_SDK_ROOT")}/platforms/android-35"))

    // --------IOS and Graal------------

    // NOTE: this is an advanced feature! Read: https://godot-kotl.in/en/stable/user-guide/advanced/graal-vm-native-image/
    // IMPORTANT: Graal Native Image needs to be considered from the start of development!
    //isGraalNativeImageExportEnabled.set(false)
    //graalVmDirectory.set(File("${System.getenv("GRAALVM_HOME")}"))
    //windowsDeveloperVCVarsPath.set(File("${System.getenv("VC_VARS_PATH")}"))
    //isIOSExportEnabled.set(false)

    // --------Library authors------------

    // library setup. See: https://godot-kotl.in/en/stable/develop-libraries/
    //classPrefix.set("MyCustomClassPrefix")
    //projectName.set("LibraryProjectName")
    //projectName.set("LibraryProjectName")
}

kotlin.sourceSets.main {
    kotlin.srcDirs("src/main/kuiper")
}
