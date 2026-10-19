plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
}

android {
    namespace = "com.chughtai.criterion.core.content"
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests { isIncludeAndroidResources = true } }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":core:model"))
    api(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}

// --- Robolectric + BundledSQLiteDriver: the Android AAR only has device .so files, so load the
// host build from sqlite-bundled-jvm via the loader's documented system properties.
val sqliteNative: Configuration = configurations.create("sqliteNative") { isTransitive = false }
dependencies { sqliteNative(libs.androidx.sqlite.bundled.jvm) }
val hostNativeDir = run {
    val os = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch").let { it == "aarch64" || it == "arm64" }
    when {
        os.contains("mac") -> if (arm) "osx_arm64" else "osx_x64"
        os.contains("win") -> "windows_x64"
        else -> if (arm) "linux_arm64" else "linux_x64"
    }
}
val hostNativeName = when {
    hostNativeDir.startsWith("osx") -> "libsqliteJni.dylib"
    hostNativeDir.startsWith("windows") -> "sqliteJni.dll"
    else -> "libsqliteJni.so"
}
val extractSqliteNative = tasks.register<Sync>("extractSqliteNative") {
    from({ zipTree(sqliteNative.singleFile) }) {
        include("natives/$hostNativeDir/**")
        eachFile { path = name }
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir("sqlite-native"))
}
tasks.withType<Test>().configureEach {
    dependsOn(extractSqliteNative)
    val dir = layout.buildDirectory.dir("sqlite-native").get().asFile.absolutePath
    systemProperty("androidx.sqlite.driver.bundled.path", dir)
    systemProperty("androidx.sqlite.driver.bundled.name", hostNativeName)
    // Robolectric 4.17 + SDK 36 on JDK 21: FileDescriptorInterceptor needs jdk.internal.access
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED")
}
