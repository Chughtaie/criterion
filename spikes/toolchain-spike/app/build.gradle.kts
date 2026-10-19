plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.chughtai.criterion"
    compileSdk { version = release(37) }

    defaultConfig {
        applicationId = "com.chughtai.criterion"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization { enable = true }
            signingConfig = signingConfigs.getByName("debug") // spike only: makes the release APK installable
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures { compose = true }
    testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:content"))
    coreLibraryDesugaring(libs.android.desugarJdkLibs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.ui.compose.material3)
    implementation(libs.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.glance.appwidget)

    implementation(libs.adhan2)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.junit4)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
}

// Same host-native SQLite + JDK 21 flags as :core:content (move to a convention plugin in the real build)
val sqliteNative: Configuration = configurations.create("sqliteNative") { isTransitive = false }
dependencies { sqliteNative(libs.androidx.sqlite.bundled.jvm) }
val extractSqliteNative = tasks.register<Sync>("extractSqliteNative") {
    from({ zipTree(sqliteNative.singleFile) }) { include("natives/linux_x64/**"); eachFile { path = name }; includeEmptyDirs = false }
    into(layout.buildDirectory.dir("sqlite-native"))
}
tasks.withType<Test>().configureEach {
    dependsOn(extractSqliteNative)
    systemProperty("androidx.sqlite.driver.bundled.path", layout.buildDirectory.dir("sqlite-native").get().asFile.absolutePath)
    systemProperty("androidx.sqlite.driver.bundled.name", "libsqliteJni.so")
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED")
}

// Spike helper: -ProboSdks=33 restricts Robolectric to one SDK per JVM (native graphics is process-global)
val roboSdks = providers.gradleProperty("roboSdks")
tasks.withType<Test>().configureEach {
    if (roboSdks.isPresent) systemProperty("robolectric.enabledSdks", roboSdks.get())
    else filter { // NATIVE graphics is process-global: run these one SDK per JVM with -ProboSdks=N
        excludeTestsMatching("*KashidaNativeGraphicsTest*"); excludeTestsMatching("*NativeGraphicsProbeTest*")
    }
}
