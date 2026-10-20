package com.chughtai.criterion.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * `criterion.android.application.flavors` (doc 04 §11.1): `distribution` {play, foss} × `tier` {personal, public}
 * with `playPersonal*` disabled; the BuildConfig fields that select content rules; version and signing inputs from
 * Gradle properties (doc 10 §12.5); and the explicit `:app` variants of the doc 10 §11.3 aggregates.
 */
class AndroidApplicationFlavorsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("criterion-android-application"))
            pluginManager.apply(libs.pluginId("criterion-content-eligibility"))

            val release = providers.gradleProperty("criterion.release").orNull // null | "production"
            require(release == null || release == "production") { "criterion.release must be 'production' or unset" }
            val production = release == "production"
            val monetizedValue = providers.gradleProperty("criterion.monetized").orNull
            require(monetizedValue == null || monetizedValue == "true" || monetizedValue == "false") {
                "criterion.monetized must be 'true', 'false' or unset"
            }
            val monetized = monetizedValue == "true"
            val cdnBaseUrl = providers.gradleProperty("criterion.cdn").getOrElse("https://cdn.example.invalid")
            // Empty until the owner's two Ed25519 keys exist (E-23-S01); E-23-S02 fails a release built without keys.
            val trustedKeys = providers.gradleProperty("criterion.trustedKeys").getOrElse("")
            // Both are pasted into Java string literals.
            require(listOf(cdnBaseUrl, trustedKeys).none { value -> value.any { it == '"' || it == '\\' } }) {
                "criterion.cdn and criterion.trustedKeys must not contain quotes or backslashes"
            }

            extensions.configure<ApplicationExtension> {
                buildFeatures.buildConfig = true
                flavorDimensions += listOf("distribution", "tier")
                productFlavors.create("play") {
                    dimension = "distribution"
                    buildConfigField("String", "DISTRIBUTION", "\"play\"")
                }
                productFlavors.create("foss") {
                    dimension = "distribution"
                    buildConfigField("String", "DISTRIBUTION", "\"foss\"")
                }
                productFlavors.create("public") {
                    dimension = "tier"
                    buildConfigField("String", "CONTENT_TIER", if (production) "\"PUBLIC\"" else "\"BETA\"")
                    buildConfigField("String", "REMOTE_CHANNEL", if (production) "\"public\"" else "\"beta\"")
                }
                productFlavors.create("personal") {
                    dimension = "tier"
                    applicationIdSuffix = ".personal"
                    versionNameSuffix = "-personal"
                    buildConfigField("String", "CONTENT_TIER", "\"PERSONAL\"")
                    buildConfigField("String", "REMOTE_CHANNEL", "\"beta\"")
                }
                buildTypes.getByName("debug").applicationIdSuffix = ".debug"
                defaultConfig.buildConfigField("boolean", "MONETIZED", monetized.toString())
                defaultConfig.buildConfigField("String", "CDN_BASE_URL", "\"$cdnBaseUrl\"")
                defaultConfig.buildConfigField("String", "TRUSTED_KEYS", "\"$trustedKeys\"")
                // Release CI passes these (doc 10 §12.5): versionCode = MAJOR*1e6 + MINOR*1e4 + PATCH*100 + n.
                // Module build files must not set versionCode/versionName, or they would override the properties.
                defaultConfig.versionCode = providers.gradleProperty("criterion.versionCode").orNull?.toInt() ?: 1
                defaultConfig.versionName = providers.gradleProperty("criterion.versionName").getOrElse("0.0.0-dev")
                configureSigning(this@with, this, propertyPrefix = "criterionSigning", flavor = "play")
                configureSigning(this@with, this, propertyPrefix = "criterionPersonalSigning", flavor = "personal")
            }

            extensions.configure<ApplicationAndroidComponentsExtension> {
                beforeVariants(selector().withFlavor("distribution" to "play").withFlavor("tier" to "personal")) {
                    it.enable = false
                }
            }

            // Doc 10 §11.3: tests and lint on PlayPublic + FossPersonal; assembly and eligibility on the public tier
            // only, because personal-tier assets are never in the repository.
            tasks.named(ModuleChecks.QUICK) { dependsOn("testPlayPublicDebugUnitTest", "testFossPersonalDebugUnitTest") }
            tasks.named(ModuleChecks.PR) {
                dependsOn(
                    "lintPlayPublicDebug",
                    "lintFossPersonalDebug",
                    "assemblePlayPublicDebug",
                    "assembleFossPublicDebug",
                    "verifyContentEligibilityPlayPublicRelease",
                    "verifyContentEligibilityFossPublicRelease",
                )
            }
            // Owner's machine only (personal packs are never in git): copyPersonalAssets joins with E-10-S07.
            tasks.register("personalCheck") {
                group = "verification"
                description = "Eligibility check and R8 build of the owner's fossPersonalRelease."
                dependsOn("verifyContentEligibilityFossPersonalRelease", "assembleFossPersonalRelease")
            }
            // BuildTierTest compares BuildConfig against the properties the build was configured with.
            tasks.withType<Test>().configureEach {
                systemProperty("criterion.release", release.orEmpty())
                systemProperty("criterion.monetized", monetized.toString())
            }
        }
    }
}

/**
 * Signs one flavor's release builds when all four `<propertyPrefix>*` properties are set (doc 10 §12.5): `play` with
 * the Play upload key (CI's `release` environment, doc 10 §12.6) and `personal` with the owner's sideload key (the
 * owner's `~/.gradle/gradle.properties` only). With none set the release stays unsigned; a partial set fails. Debug
 * keeps the debug key because the build type's signing config wins over the flavor's. A relative store file resolves
 * against the repository root.
 */
private fun configureSigning(
    project: Project,
    android: ApplicationExtension,
    propertyPrefix: String,
    flavor: String,
) {
    val names = listOf("StoreFile", "StorePassword", "KeyAlias", "KeyPassword").map { "$propertyPrefix$it" }
    val values = names.map { project.providers.gradleProperty(it).orNull }
    if (values.all { it == null }) return
    val missing = names.filterIndexed { i, _ -> values[i] == null }
    require(missing.isEmpty()) { "Signing the $flavor flavor needs all of $names; missing $missing" }
    val (storeFile, storePassword, keyAlias, keyPassword) = values.requireNoNulls()
    val config =
        android.signingConfigs.create(flavor) {
            this.storeFile = project.rootProject.file(storeFile)
            this.storePassword = storePassword
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
        }
    android.productFlavors.getByName(flavor).signingConfig = config
}
