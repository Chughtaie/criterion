package com.chughtai.criterion.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** `criterion.android.application` (doc 04 §16.2): SDK levels, Java 17, desugaring, R8 for release, lint wiring. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply(libs.pluginId("criterion-android-lint"))
            extensions.configure<ApplicationExtension> {
                configureAndroidCommon(this)
                defaultConfig.targetSdk = CriterionSdk.TARGET
                buildTypes.getByName("release").optimization { enable = true } // AGP 9.3+ DSL; R8 full mode
            }
            registerModuleChecks(quickCheckTasks = emptyList(), prCheckTasks = emptyList())
            extensions.configure<ApplicationAndroidComponentsExtension> {
                // The coroutines debug-probes agent is only for local debugging; keep it out of release builds (D-02).
                onVariants(selector().withBuildType("release")) { variant ->
                    variant.packaging.resources.excludes
                        .add("DebugProbesKt.bin")
                }
                // Without flavors the debug variant feeds the aggregates; criterion.android.application.flavors
                // replaces this with doc 10 §11.3's explicit variant lists.
                onVariants(selector().withBuildType("debug")) { variant ->
                    if (variant.productFlavors.isNotEmpty()) return@onVariants
                    val name = variant.name.replaceFirstChar(Char::titlecase)
                    tasks.named(ModuleChecks.QUICK) { dependsOn("test${name}UnitTest") }
                    tasks.named(ModuleChecks.PR) { dependsOn("lint$name", "assemble$name") }
                }
            }
        }
    }
}
