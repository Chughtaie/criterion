package com.chughtai.criterion.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Fails the build on any doc 04 §3.4 module-boundary violation. */
@DisableCachingByDefault(because = "A cheap check with no outputs")
abstract class CheckModuleBoundariesTask : DefaultTask() {
    @get:Input
    abstract val violations: ListProperty<String>

    @TaskAction
    fun check() {
        val found = violations.get()
        if (found.isNotEmpty()) {
            throw GradleException("Module boundary violations (doc 04 §3.4):\n" + found.joinToString("\n") { "  - $it" })
        }
        logger.lifecycle("checkModuleBoundaries: no violations")
    }
}
