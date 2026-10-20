package com.chughtai.criterion.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider

private const val TESTING_MODULE = ":core:testing"

/**
 * `criterion.android.robolectric` (doc 10 §5.1, g04 rec 8): Robolectric test dependencies, the JDK 21 flags
 * SDK 36 needs, the host `libsqliteJni` for `BundledSQLiteDriver`, and one-SDK-per-JVM NATIVE graphics.
 */
class AndroidRobolectricConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> { testOptions.unitTests.isIncludeAndroidResources = true }
                addTestDependencies()
            }
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> { testOptions.unitTests.isIncludeAndroidResources = true }
                addTestDependencies()
            }

            // Host libsqliteJni for BundledSQLiteDriver (g04 §2.3 #4). sqlite-bundled-jvm 2.7.1 ships
            // linux_x64, linux_arm64, osx_arm64 and windows_x64 only.
            val sqliteNative = configurations.create("sqliteNative") { isTransitive = false }
            dependencies.add(sqliteNative.name, libs.library("androidx-sqlite-bundled-jvm"))
            val os = System.getProperty("os.name").lowercase()
            val arch = System.getProperty("os.arch")
            val arm = arch in setOf("aarch64", "arm64")
            val nativeDir =
                when {
                    "mac" in os -> if (arm) "osx_arm64" else null
                    "win" in os -> if (arm) null else "windows_x64"
                    else -> if (arm) "linux_arm64" else "linux_x64"
                }
            val nativeLib =
                when {
                    "mac" in os -> "libsqliteJni.dylib"
                    "win" in os -> "sqliteJni.dll"
                    else -> "libsqliteJni.so"
                }
            val unsupported = if (nativeDir == null) "sqlite-bundled-jvm has no host library for $os/$arch" else null
            val nativeOut = layout.buildDirectory.dir("sqlite-native")
            val extract =
                tasks.register<Sync>("extractSqliteNative") {
                    doFirst { if (unsupported != null) throw GradleException(unsupported) }
                    from({ zipTree(sqliteNative.singleFile) }) {
                        include("natives/$nativeDir/**")
                        eachFile { path = name }
                        includeEmptyDirs = false
                    }
                    into(nativeOut)
                }

            val roboSdks = providers.gradleProperty("roboSdks")
            tasks.withType<Test>().configureEach {
                dependsOn(extract)
                jvmArgumentProviders.add(SqliteNativeArgs(nativeOut, nativeLib))
                // SDK 36 ApplicationSharedMemory + JDK 21 strong encapsulation (g04 §2.3 #3).
                jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED")
                maxHeapSize = "3g"
                // NATIVE graphics are process-global: two SDKs in one JVM abort it, so those suites run only with
                // -ProboSdks=N (one SDK per JVM) and the golden task (doc 10 §7.4). A module whose only tests are
                // goldens must still pass its default test task.
                if (roboSdks.isPresent) {
                    systemProperty("robolectric.enabledSdks", roboSdks.get())
                } else {
                    filter {
                        isFailOnNoMatchingTests = false
                        excludeTestsMatching("*NativeGraphics*")
                        excludeTestsMatching("*Golden*")
                    }
                }
            }
        }
    }
}

private fun Project.addTestDependencies() {
    dependencies {
        add("testImplementation", libs.library("robolectric"))
        add("testImplementation", libs.library("androidx-test-core"))
        add("testImplementation", libs.library("truth"))
        add("testImplementation", libs.library("turbine"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        if (path != TESTING_MODULE && findProject(TESTING_MODULE) != null) {
            add("testImplementation", project(TESTING_MODULE))
        }
    }
}

/** The extracted library's absolute path is machine-specific, so it is not a test input (keeps test caching relocatable). */
private class SqliteNativeArgs(
    @get:Internal val directory: Provider<Directory>,
    @get:Input val library: String,
) : CommandLineArgumentProvider {
    override fun asArguments(): List<String> =
        listOf(
            "-Dandroidx.sqlite.driver.bundled.path=${directory.get().asFile.absolutePath}",
            "-Dandroidx.sqlite.driver.bundled.name=$library",
        )
}
