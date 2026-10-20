package com.chughtai.criterion.buildlogic

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ModuleBoundaryRulesTest {
    private val library = "criterion.android.library"
    private val jvm = "criterion.jvm.library"
    private val base = setOf(library, jvm, "criterion.android.application")

    private fun module(
        path: String,
        conventions: Set<String> = setOf(library),
        android: Boolean = library in conventions,
        projects: Set<String> = emptySet(),
        externals: Set<String> = emptySet(),
        apiExternals: Set<String> = emptySet(),
        hasBuildFile: Boolean = true,
    ) = ModuleInfo(path, hasBuildFile, android, conventions, projects, externals + apiExternals, apiExternals)

    private fun violations(vararg modules: ModuleInfo) = ModuleBoundaryRules.violations(modules.toList(), base)

    @Test
    fun `a feature may use domain, model, designsystem, navigation and testing`() {
        val feature =
            module(
                ":feature:reader",
                projects = setOf(":core:domain", ":core:model", ":core:designsystem", ":core:navigation", ":core:testing"),
            )
        assertThat(violations(feature)).isEmpty()
    }

    @Test
    fun `a feature depending on another feature fails`() {
        assertThat(violations(module(":feature:a", projects = setOf(":feature:b")), module(":feature:b"))).containsExactly(
            ":feature:a depends on :feature:b: features never depend on features (doc 04 §3.4 rule 1)",
        )
    }

    @Test
    fun `a feature depending on any implementation module fails`() {
        listOf(":core:data", ":core:content", ":core:database", ":core:remote", ":core:audio", ":core:prayer").forEach { core ->
            assertThat(violations(module(":feature:home", projects = setOf(core)))).containsExactly(
                ":feature:home depends on $core: features use :core:domain interfaces only (doc 04 §3.4 rule 1)",
            )
        }
    }

    @Test
    fun `Room, OkHttp and Media3 are not allowed in features`() {
        val feature =
            module(
                ":feature:search",
                externals =
                    setOf(
                        "androidx.room3:room3-runtime",
                        "androidx.room:room-runtime",
                        "com.squareup.okhttp3:okhttp",
                        "androidx.media3:media3-exoplayer",
                    ),
            )
        assertThat(violations(feature))
            .containsExactly(
                ":feature:search depends on androidx.media3:media3-exoplayer: no Media3 in features (doc 04 §3.4 rule 1)",
                ":feature:search depends on androidx.room3:room3-runtime: no Room in features (doc 04 §3.4 rule 1)",
                ":feature:search depends on androidx.room:room-runtime: no Room in features (doc 04 §3.4 rule 1)",
                ":feature:search depends on com.squareup.okhttp3:okhttp: no OkHttp in features (doc 04 §3.4 rule 1)",
            ).inOrder()
    }

    @Test
    fun `only the audio feature may use the Media3 player UI`() {
        val playerUi = setOf("androidx.media3:media3-ui-compose-material3")
        assertThat(violations(module(":feature:audio", externals = playerUi))).isEmpty()
        assertThat(violations(module(":feature:reader", externals = playerUi))).containsExactly(
            ":feature:reader depends on androidx.media3:media3-ui-compose-material3: no Media3 in features (doc 04 §3.4 rule 1)",
        )
        assertThat(violations(module(":feature:audio", externals = setOf("androidx.media3:media3-session")))).containsExactly(
            ":feature:audio depends on androidx.media3:media3-session: no Media3 in features (doc 04 §3.4 rule 1)",
        )
    }

    @Test
    fun `model and domain apply no Android plugin`() {
        listOf(":core:model", ":core:domain").forEach { path ->
            assertThat(violations(module(path, conventions = setOf(jvm), android = true))).containsExactly(
                "$path applies an Android plugin: :core:model and :core:domain are pure Kotlin (doc 04 §3.4 rule 2)",
            )
        }
    }

    @Test
    fun `model and domain use no DI or Room`() {
        val forbidden =
            setOf(
                "javax.inject:javax.inject",
                "jakarta.inject:jakarta.inject-api",
                "com.google.dagger:hilt-core",
                "androidx.room3:room3-common",
            )
        assertThat(violations(module(":core:domain", conventions = setOf(jvm), externals = forbidden)))
            .containsExactly(
                ":core:domain depends on androidx.room3:room3-common: no DI or Room in :core:model and :core:domain (doc 04 §3.4 rule 2)",
                ":core:domain depends on com.google.dagger:hilt-core: no DI or Room in :core:model and :core:domain (doc 04 §3.4 rule 2)",
                ":core:domain depends on jakarta.inject:jakarta.inject-api: no DI or Room in :core:model and :core:domain (doc 04 §3.4 rule 2)",
                ":core:domain depends on javax.inject:javax.inject: no DI or Room in :core:model and :core:domain (doc 04 §3.4 rule 2)",
            ).inOrder()
    }

    @Test
    fun `Room never leaks through api`() {
        val content =
            module(
                ":core:content",
                apiExternals = setOf("androidx.room3:room3-runtime"),
                externals = setOf("androidx.sqlite:sqlite-bundled"),
            )
        assertThat(violations(content)).containsExactly(
            ":core:content exposes androidx.room3:room3-runtime through api: Room types stay internal (doc 04 §3.4 rule 3)",
        )
        assertThat(violations(module(":core:content", externals = setOf("androidx.room3:room3-runtime")))).isEmpty()
    }

    @Test
    fun `every module with a build file applies a base convention plugin`() {
        assertThat(violations(module(":core:remote", conventions = emptySet(), android = true))).containsExactly(
            ":core:remote applies none of [criterion.android.library, criterion.jvm.library, criterion.android.application] " +
                "(doc 04 §3.4 rule 5)",
        )
    }

    @Test
    fun `intermediate projects without a build file are ignored`() {
        assertThat(violations(module(":core", conventions = emptySet(), hasBuildFile = false))).isEmpty()
    }
}
