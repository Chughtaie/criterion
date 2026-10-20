package com.chughtai.criterion.buildlogic

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** `criterion.jvm.library` (doc 04 §16.2): pure Kotlin on JVM 17 for `:core:model` and `:core:domain`; JUnit 4 + Truth + coroutines-test. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("kotlin-jvm"))
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
            }
            dependencies {
                add("testImplementation", libs.library("junit4"))
                add("testImplementation", libs.library("truth"))
                add("testImplementation", libs.library("kotlinx-coroutines-test"))
            }
            registerModuleChecks(quickCheckTasks = listOf("test"), prCheckTasks = emptyList())
        }
    }
}
