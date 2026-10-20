package com.chughtai.criterion.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.spotless.LineEnding
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `criterion.spotless` (doc 10 §11.1), applied once in the root project: ktlint (official style) with compose-rules
 * for Kotlin and Gradle Kotlin scripts; trailing whitespace and final newlines for Android XML and the plan's
 * Markdown. XML and Markdown are allow-listed because data and generated files must never be rewritten (CLAUDE.md
 * #5, #6), and `spotlessApply` runs outside the agent edit guard.
 */
class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            check(this == rootProject) { "criterion.spotless is applied once, in the root project" }
            pluginManager.apply(libs.pluginId("spotless"))
            val ktlintVersion = libs.version("ktlint")
            val composeRules = libs.library("compose-rules-ktlint").get().let { "${it.module}:${it.versionConstraint.requiredVersion}" }
            // The ktlint rules live here rather than in .editorconfig, so changing them invalidates Spotless's caches.
            val ktlintRules =
                mapOf(
                    "ktlint_code_style" to "ktlint_official",
                    "max_line_length" to "140",
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                )
            val excluded =
                listOf(
                    "**/build/**",
                    "**/.gradle/**",
                    "**/.kotlin/**",
                    "**/.venv/**",
                    ".idea/**",
                    ".claude/worktrees/**",
                    "spikes/**",
                    "docs/research/**",
                    "out/**",
                    "data/**",
                    "tools/**",
                    "**/src/personal/**",
                    ".git/**",
                )

            // Excludes go on the file tree itself, so Gradle never descends into them. Spotless's targetExclude would
            // instead enumerate every build directory to subtract it, racing the tasks that write there.
            fun sources(vararg includes: String) = fileTree(rootDir) { include(*includes).exclude(excluded) }
            extensions.configure<SpotlessExtension> {
                // LF everywhere (.editorconfig). The default GIT_ATTRIBUTES mode reads git metadata while configuring,
                // which defeats configuration-cache reuse.
                lineEndings = LineEnding.UNIX
                kotlin {
                    target(sources("**/*.kt"))
                    ktlint(ktlintVersion).customRuleSets(listOf(composeRules)).editorConfigOverride(ktlintRules)
                }
                kotlinGradle {
                    target(sources("**/*.gradle.kts"))
                    ktlint(ktlintVersion).editorConfigOverride(ktlintRules)
                }
                format("xml") {
                    target(sources("**/src/*/res/**/*.xml", "**/src/*/AndroidManifest.xml"))
                    trimTrailingWhitespace()
                    endWithNewline()
                }
                format("markdown") {
                    target(sources("*.md", "docs/plan/**/*.md", ".github/**/*.md", ".claude/**/*.md"))
                    trimTrailingWhitespace()
                    endWithNewline()
                }
            }
        }
    }
}
