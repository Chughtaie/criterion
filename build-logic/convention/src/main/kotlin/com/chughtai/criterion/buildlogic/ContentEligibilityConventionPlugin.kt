package com.chughtai.criterion.buildlogic

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * `criterion.content.eligibility` (doc 04 §11.3): registers `verifyContentEligibility<Variant>` for every flavored
 * variant and runs it before the variant merges its assets.
 */
class ContentEligibilityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val production = providers.gradleProperty("criterion.release").orNull == "production"
            val monetized = providers.gradleProperty("criterion.monetized").orNull == "true"
            extensions.configure<ApplicationAndroidComponentsExtension> {
                onVariants { variant ->
                    val flavors = variant.productFlavors.toMap()
                    val tier = flavors["tier"] ?: return@onVariants
                    val name = variant.name.replaceFirstChar(Char::titlecase)
                    val verify =
                        tasks.register<VerifyContentEligibilityTask>("verifyContentEligibility$name") {
                            group = "verification"
                            description = "Checks the content bundled into ${variant.name} (doc 04 §11.3)."
                            this.tier.set(tier)
                            distribution.set(flavors.getValue("distribution"))
                            release.set(variant.buildType == "release")
                            this.production.set(production)
                            this.monetized.set(monetized)
                            bundledJson.set(layout.projectDirectory.file("src/$tier/assets/content/bundled.json"))
                            variant.sources.assets?.let { assets -> assetDirs.set(assets.all.map { it.flatten() }) }
                        }
                    tasks.configureEach { if (this.name == "merge${name}Assets") dependsOn(verify) }
                }
            }
        }
    }
}
