package com.chughtai.criterion.buildlogic

import java.io.Serializable

/** What `checkModuleBoundaries` knows about one Gradle project: applied plugins and declared dependencies. */
data class ModuleInfo(
    val path: String,
    val hasBuildFile: Boolean,
    /** True when any Android Gradle plugin is applied (the project has an `android` extension). */
    val appliesAndroid: Boolean,
    /** Applied `criterion.*` convention plugin IDs. */
    val conventionPluginIds: Set<String>,
    /** Paths of declared project dependencies, e.g. `:core:domain`. */
    val projectDependencies: Set<String>,
    /** Declared external modules as `group:name`. */
    val externalModules: Set<String>,
    /** External modules declared in an `api` configuration (`api`, `debugApi`, …), as `group:name`. */
    val apiExternalModules: Set<String>,
) : Serializable

/**
 * Doc 04 §3.4 rules 1–3 and 5 over declared dependencies (doc 09 E-01-S08). Rule 4 (cycles) is Gradle's own; the
 * graph-diff print belongs to CI (E-02-S01).
 */
object ModuleBoundaryRules {
    /** Doc 04 §3.4 rule 1's implementation modules; extend it when doc 04 adds one (e.g. P3 `:core:sync`). */
    private val coreImplementationModules =
        setOf(":core:data", ":core:content", ":core:database", ":core:remote", ":core:audio", ":core:prayer")
    private val pureModules = setOf(":core:model", ":core:domain")
    private val roomGroups = setOf("androidx.room3", "androidx.room")
    private val diGroups = setOf("javax.inject", "jakarta.inject", "com.google.dagger", "androidx.hilt")
    private const val FEATURE_AUDIO_PLAYER_UI = "androidx.media3:media3-ui-compose-material3"

    fun violations(
        modules: Collection<ModuleInfo>,
        baseConventionPluginIds: Set<String>,
    ): List<String> =
        modules
            .filter { it.hasBuildFile }
            .sortedBy { it.path }
            .flatMap { module ->
                buildList {
                    if (module.path.startsWith(":feature:")) addAll(featureViolations(module))
                    if (module.path in pureModules) addAll(pureModuleViolations(module))
                    addAll(apiLeakViolations(module))
                    if (module.conventionPluginIds.none { it in baseConventionPluginIds }) {
                        add("${module.path} applies none of $baseConventionPluginIds (doc 04 §3.4 rule 5)")
                    }
                }
            }

    private fun featureViolations(module: ModuleInfo): List<String> =
        buildList {
            module.projectDependencies.sorted().forEach { dependency ->
                if (dependency.startsWith(":feature:") && dependency != module.path) {
                    add("${module.path} depends on $dependency: features never depend on features (doc 04 §3.4 rule 1)")
                }
                if (dependency in coreImplementationModules) {
                    add("${module.path} depends on $dependency: features use :core:domain interfaces only (doc 04 §3.4 rule 1)")
                }
            }
            module.externalModules.sorted().forEach { coordinates ->
                val group = coordinates.substringBefore(':')
                val audioPlayerUi = module.path == ":feature:audio" && coordinates == FEATURE_AUDIO_PLAYER_UI
                val forbidden =
                    when {
                        group in roomGroups -> "Room"
                        group == "com.squareup.okhttp3" -> "OkHttp"
                        group == "androidx.media3" && !audioPlayerUi -> "Media3"
                        else -> null
                    }
                if (forbidden != null) add("${module.path} depends on $coordinates: no $forbidden in features (doc 04 §3.4 rule 1)")
            }
        }

    private fun pureModuleViolations(module: ModuleInfo): List<String> =
        buildList {
            if (module.appliesAndroid) {
                add("${module.path} applies an Android plugin: :core:model and :core:domain are pure Kotlin (doc 04 §3.4 rule 2)")
            }
            module.externalModules.sorted().forEach { coordinates ->
                val group = coordinates.substringBefore(':')
                if (group in diGroups || group in roomGroups) {
                    add("${module.path} depends on $coordinates: no DI or Room in :core:model and :core:domain (doc 04 §3.4 rule 2)")
                }
            }
        }

    private fun apiLeakViolations(module: ModuleInfo): List<String> =
        module.apiExternalModules
            .filter { it.substringBefore(':') in roomGroups }
            .sorted()
            .map { "${module.path} exposes $it through api: Room types stay internal (doc 04 §3.4 rule 3)" }
}
