package com.chughtai.criterion.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Fails the build when the KGP lift or the KSP version is lost (D-02, g04 rec 1). */
@DisableCachingByDefault(because = "A cheap check with no outputs")
abstract class VerifyKotlinGradlePluginTask : DefaultTask() {
    @get:Input
    abstract val edges: ListProperty<ClasspathEdge>

    @get:Input
    abstract val expectedKotlin: Property<String>

    @get:Input
    abstract val expectedKsp: Property<String>

    @TaskAction
    fun verify() {
        val classpath = edges.get()
        KotlinLift.lifts(classpath).forEach { logger.lifecycle("verifyKotlinGradlePlugin: $it") }
        val problems = KotlinLift.problems(classpath, expectedKotlin.get(), expectedKsp.get())
        if (problems.isNotEmpty()) {
            throw GradleException("Kotlin Gradle plugin lift lost (D-02):\n" + problems.joinToString("\n") { "  - $it" })
        }
    }
}
