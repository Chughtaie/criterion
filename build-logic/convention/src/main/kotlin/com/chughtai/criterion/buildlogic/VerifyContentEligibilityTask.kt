package com.chughtai.criterion.buildlogic

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Fails a variant whose bundled content is not eligible for its tier (doc 04 §11.3; stub until E-02-S04). It has no
 * outputs, so it runs every time and reads its inputs directly.
 */
@DisableCachingByDefault(because = "A cheap check with no outputs")
abstract class VerifyContentEligibilityTask : DefaultTask() {
    @get:Input
    abstract val tier: Property<String>

    @get:Input
    abstract val distribution: Property<String>

    @get:Input
    abstract val release: Property<Boolean>

    @get:Input
    abstract val production: Property<Boolean>

    @get:Input
    abstract val monetized: Property<Boolean>

    /** The tier source set's `content/bundled.json`; a missing file lists nothing. */
    @get:Internal
    abstract val bundledJson: RegularFileProperty

    /** Every asset folder that merges into the variant (main, flavors, build type, tier). */
    @get:Internal
    abstract val assetDirs: ListProperty<Directory>

    @TaskAction
    fun verify() {
        val manifest = bundledJson.get().asFile
        val listed =
            if (manifest.exists()) {
                val json = JsonSlurper().parse(manifest) as Map<*, *>
                (json["items"] as List<*>).map { (it as Map<*, *>)["path"].toString() }.toSet()
            } else {
                emptySet()
            }
        val files =
            assetDirs.get().map { it.asFile }.filter { it.isDirectory }.flatMap { root ->
                root
                    .walkTopDown()
                    .filter { it.isFile && !it.name.startsWith(".") }
                    .map { it.relativeTo(root).invariantSeparatorsPath }
                    .toList()
            }
        val problems =
            ContentEligibility.problems(
                tier.get(),
                distribution.get(),
                release.get(),
                production.get(),
                monetized.get(),
                listed,
                files.toSet(),
            )
        if (problems.isNotEmpty()) {
            throw GradleException("Content not eligible for this variant (doc 04 §11.3):\n" + problems.joinToString("\n") { "  - $it" })
        }
        logger.lifecycle("$name: ${listed.size} bundled item(s), no problems")
    }
}
