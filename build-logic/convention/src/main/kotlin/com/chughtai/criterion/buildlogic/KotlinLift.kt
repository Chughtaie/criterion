package com.chughtai.criterion.buildlogic

import java.io.Serializable

/** One edge of the root buildscript classpath: a requested module version and the version Gradle selected. */
data class ClasspathEdge(
    val group: String,
    val module: String,
    val requested: String,
    val selected: String,
) : Serializable

/**
 * The KGP lift (D-02, g04 rec 1): AGP 9.4.1 bundles kotlin-gradle-plugin 2.2.10, and the root `kotlin-jvm`
 * `apply false` must lift it to the catalogue's Kotlin version; KSP must resolve to the catalogue's KSP version.
 */
object KotlinLift {
    private const val KOTLIN_GROUP = "org.jetbrains.kotlin"
    private const val KGP = "kotlin-gradle-plugin"
    private const val KSP_GROUP = "com.google.devtools.ksp"
    private const val KSP = "symbol-processing-gradle-plugin"

    fun problems(
        edges: Collection<ClasspathEdge>,
        expectedKotlin: String,
        expectedKsp: String,
    ): List<String> = check(edges, KOTLIN_GROUP, KGP, expectedKotlin) + check(edges, KSP_GROUP, KSP, expectedKsp)

    /** Human-readable `module:requested -> selected` lines for the lifted plugins, as `buildEnvironment` prints them. */
    fun lifts(edges: Collection<ClasspathEdge>): List<String> =
        edges
            .filter { (it.group == KOTLIN_GROUP && it.module == KGP) || (it.group == KSP_GROUP && it.module == KSP) }
            .filter { it.requested != it.selected }
            .map { "${it.group}:${it.module}:${it.requested} -> ${it.selected}" }
            .distinct()
            .sorted()

    private fun check(
        edges: Collection<ClasspathEdge>,
        group: String,
        module: String,
        expected: String,
    ): List<String> {
        val selected = edges.filter { it.group == group && it.module == module }.map { it.selected }.toSortedSet()
        return when {
            selected.isEmpty() -> listOf("$group:$module is not on the root buildscript classpath")
            selected != sortedSetOf(expected) -> listOf("$group:$module resolves to $selected; D-02 requires $expected")
            else -> emptyList()
        }
    }
}
