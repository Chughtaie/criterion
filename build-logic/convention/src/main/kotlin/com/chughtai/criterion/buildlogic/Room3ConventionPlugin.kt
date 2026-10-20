package com.chughtai.criterion.buildlogic

import androidx.room3.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * `criterion.room3` (doc 04 §16.2): KSP + Room 3 with exported schemas and the bundled SQLite driver.
 * Room stays `implementation` so its types never leak through `api` (doc 04 §3.4 rule 3). The `user.db`
 * migration-completeness check arrives with user.db itself (E-07-S01).
 */
class Room3ConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("ksp"))
            pluginManager.apply(libs.pluginId("room3"))
            // The Room 3 plugin fails without a schema directory (g04 §2.3).
            extensions.configure<RoomExtension> { schemaDirectory("$projectDir/schemas") }
            // Dependencies wait for the module's base plugin, so plugin order in the module does not matter.
            listOf("com.android.library", "com.android.application", "org.jetbrains.kotlin.jvm").forEach { base ->
                pluginManager.withPlugin(base) {
                    dependencies {
                        add("implementation", libs.library("androidx-room3-runtime"))
                        add("implementation", libs.library("androidx-sqlite-bundled"))
                        add("ksp", libs.library("androidx-room3-compiler"))
                        add("testImplementation", libs.library("androidx-room3-testing"))
                    }
                }
            }
        }
    }
}
