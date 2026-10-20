package com.chughtai.criterion.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `criterion.android.library` (doc 04 §16.2): the application's SDK, Java and desugaring setup, plus a resource prefix. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply(libs.pluginId("criterion-android-lint"))
            extensions.configure<LibraryExtension> {
                configureAndroidCommon(this)
                namespace = defaultNamespace()
                resourcePrefix = path.removePrefix(":").replace(':', '_').replace('-', '_') + "_"
            }
            registerModuleChecks(quickCheckTasks = listOf("testDebugUnitTest"), prCheckTasks = listOf("lintDebug"))
        }
    }
}
