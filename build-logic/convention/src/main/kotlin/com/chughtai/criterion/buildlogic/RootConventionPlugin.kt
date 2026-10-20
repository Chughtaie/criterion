package com.chughtai.criterion.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.kotlin.dsl.register

/**
 * `criterion.root` (doc 04 §16.2, doc 10 §11.3): the guard tasks `checkModuleBoundaries` and `verifyKotlinGradlePlugin`,
 * and the aggregates `quickCheck` (local loop) and `prCheck` (what CI's `ci/check` runs). Each module's convention plugin
 * contributes its own `moduleQuickCheck` / `modulePrCheck`, so no module's tests or lint can drop out of the aggregates.
 */
class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            check(this == rootProject) { "criterion.root is applied once, in the root project" }
            // A module with a build file must apply one of these, so its tests and lint reach the aggregates.
            val moduleConventionIds =
                listOf("criterion-android-application", "criterion-android-library", "criterion-jvm-library")
                    .map { libs.pluginId(it) }
            val conventionPluginIds = libs.pluginAliases.filter { it.startsWith("criterion.") }.map { libs.pluginId(it) }
            val boundaries =
                tasks.register<CheckModuleBoundariesTask>("checkModuleBoundaries") {
                    group = "verification"
                    description = "Fails on any doc 04 §3.4 module-boundary violation."
                    violations.set(
                        provider {
                            ModuleBoundaryRules.violations(
                                subprojects.map { it.moduleInfo(conventionPluginIds) },
                                moduleConventionIds.toSet(),
                            )
                        },
                    )
                }
            val kotlinLift =
                tasks.register<VerifyKotlinGradlePluginTask>("verifyKotlinGradlePlugin") {
                    group = "verification"
                    description = "Fails unless the root classpath lifts KGP and KSP to the catalogue versions (D-02)."
                    edges.set(provider { buildscriptClasspathEdges() })
                    expectedKotlin.set(libs.version("kotlin"))
                    expectedKsp.set(libs.version("ksp"))
                }
            val quickCheck =
                tasks.register("quickCheck") {
                    group = "verification"
                    description = "Local loop: formatting, build-logic tests and every module's unit tests (doc 10 §11.3)."
                    dependsOn(gradle.includedBuild("build-logic").task(":convention:test"))
                }
            val prCheck =
                tasks.register("prCheck") {
                    group = "verification"
                    description = "What CI's ci/check runs: quickCheck + lint + guards + debug assembly (doc 10 §11.3)."
                    dependsOn(quickCheck, boundaries, kotlinLift)
                }
            pluginManager.withPlugin(libs.pluginId("spotless")) {
                quickCheck.configure { dependsOn("spotlessCheck") }
            }
            subprojects {
                val module = this
                moduleConventionIds.forEach { id ->
                    module.pluginManager.withPlugin(id) {
                        quickCheck.configure { dependsOn("${module.path}:${ModuleChecks.QUICK}") }
                        prCheck.configure { dependsOn("${module.path}:${ModuleChecks.PR}") }
                    }
                }
            }
        }
    }
}

private fun Project.moduleInfo(conventionPluginIds: List<String>): ModuleInfo {
    val declared = configurations.flatMap { it.dependencies }
    val declaredApi = configurations.filter { it.name == "api" || it.name.endsWith("Api") }.flatMap { it.dependencies }
    return ModuleInfo(
        path = path,
        hasBuildFile = buildFile.exists(),
        appliesAndroid = extensions.findByName("android") != null,
        conventionPluginIds = conventionPluginIds.filter { pluginManager.hasPlugin(it) }.toSet(),
        projectDependencies = declared.filterIsInstance<ProjectDependency>().map { it.path }.toSet(),
        externalModules = declared.filterIsInstance<ExternalModuleDependency>().map { "${it.group}:${it.name}" }.toSet(),
        apiExternalModules = declaredApi.filterIsInstance<ExternalModuleDependency>().map { "${it.group}:${it.name}" }.toSet(),
    )
}

private fun Project.buildscriptClasspathEdges(): List<ClasspathEdge> =
    buildscript.configurations
        .getByName("classpath")
        .incoming.resolutionResult.allDependencies
        .filterIsInstance<ResolvedDependencyResult>()
        .mapNotNull { dependency ->
            val requested = dependency.requested as? ModuleComponentSelector ?: return@mapNotNull null
            val selected = dependency.selected.moduleVersion ?: return@mapNotNull null
            ClasspathEdge(requested.group, requested.module, requested.version, selected.version)
        }.distinct()
