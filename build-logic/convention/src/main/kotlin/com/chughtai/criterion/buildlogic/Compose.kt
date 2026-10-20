package com.chughtai.criterion.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/** `criterion.android.application.compose`: `criterion.android.application` plus Compose. */
class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("criterion-android-application"))
            configureCompose(extensions.getByType<ApplicationExtension>())
        }
    }
}

/** `criterion.android.library.compose`: `criterion.android.library` plus Compose. */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("criterion-android-library"))
            configureCompose(extensions.getByType<LibraryExtension>())
        }
    }
}

/**
 * Compose compiler, BOM and tooling; `:core:model` types are stable. `-PcomposeReports` writes compiler reports and
 * metrics for compile tasks that run (add `--rerun-tasks` when they are up to date).
 */
private fun Project.configureCompose(extension: CommonExtension) {
    pluginManager.apply(libs.pluginId("kotlin-compose"))
    extension.buildFeatures.compose = true
    dependencies {
        val bom = libs.library("androidx-compose-bom")
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))
        add("implementation", libs.library("androidx-compose-ui-tooling-preview"))
        add("debugImplementation", libs.library("androidx-compose-ui-tooling"))
    }
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("compose_compiler_config.conf"))
        if (providers.gradleProperty("composeReports").isPresent) {
            reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
            metricsDestination.set(layout.buildDirectory.dir("compose-metrics"))
        }
    }
}
