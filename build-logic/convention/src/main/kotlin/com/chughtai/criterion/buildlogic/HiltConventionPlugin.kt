package com.chughtai.criterion.buildlogic

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * `criterion.hilt` (doc 04 §16.2): KSP + Hilt (no kapt) for an Android application or library module. The
 * androidx.hilt compiler for `@HiltWorker` arrives with the first worker (E-10-S02).
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val androidPlugins = listOf("com.android.application", "com.android.library")
            androidPlugins.forEach { androidPlugin ->
                pluginManager.withPlugin(androidPlugin) {
                    pluginManager.apply(libs.pluginId("ksp"))
                    pluginManager.apply(libs.pluginId("hilt"))
                    dependencies {
                        add("implementation", libs.library("hilt-android"))
                        add("ksp", libs.library("hilt-compiler"))
                        // Robolectric @HiltAndroidTest tests (doc 04 §9; the spike's pattern).
                        add("testImplementation", libs.library("hilt-android-testing"))
                        add("kspTest", libs.library("hilt-compiler"))
                    }
                }
            }
            afterEvaluate {
                if (androidPlugins.none { pluginManager.hasPlugin(it) }) {
                    throw GradleException("$path applies criterion.hilt without an Android application or library plugin")
                }
            }
        }
    }
}
