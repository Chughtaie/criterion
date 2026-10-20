package com.chughtai.criterion.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * `criterion.android.lint` (doc 10 §11.1): abort on error, warnings as errors on CI, a per-module baseline that
 * may only shrink, and Slack's compose-lint checks. Applied by the application and library conventions.
 * Each module lints only its own code against its own baseline (checkDependencies = false): with dependency
 * checking on, `:app`'s lint re-reported library issues that the library's baseline had already recorded.
 */
class AndroidLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> { lint.configureCriterion(this@with) }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> { lint.configureCriterion(this@with) }
            }
            dependencies {
                add("lintChecks", libs.library("slack-compose-lint"))
            }
        }
    }
}

private fun Lint.configureCriterion(project: Project) {
    abortOnError = true
    warningsAsErrors = project.providers.environmentVariable("CI").orNull == "true"
    checkDependencies = false
    // Only modules with recorded pre-existing issues have a baseline (lint aborts the first run of a module whose
    // configured baseline file is missing). To record one: create an empty lint-baseline.xml, then updateLintBaseline.
    val baselineFile = project.file("lint-baseline.xml")
    if (baselineFile.exists()) baseline = baselineFile
    // These checks compare against the newest releases online. The toolchain is frozen by D-02, so their
    // results would change with upstream releases rather than with our code.
    disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
}
