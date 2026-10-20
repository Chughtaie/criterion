package com.chughtai.criterion.buildlogic

import org.gradle.api.Project

/**
 * Per-module halves of the root `quickCheck` / `prCheck` aggregates (doc 10 §11.3). Each convention plugin
 * decides which of its module's tasks belong in them; `criterion.root` depends on these two tasks in every
 * module, so a module's tests cannot silently drop out of CI.
 */
internal object ModuleChecks {
    const val QUICK = "moduleQuickCheck"
    const val PR = "modulePrCheck"
}

internal fun Project.registerModuleChecks(
    quickCheckTasks: List<String>,
    prCheckTasks: List<String>,
) {
    tasks.register(ModuleChecks.QUICK) {
        group = "verification"
        description = "This module's part of the root quickCheck aggregate."
        dependsOn(quickCheckTasks)
    }
    tasks.register(ModuleChecks.PR) {
        group = "verification"
        description = "This module's part of the root prCheck aggregate."
        dependsOn(ModuleChecks.QUICK)
        dependsOn(prCheckTasks)
    }
}
