package com.chughtai.criterion.buildlogic

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ContentEligibilityTest {
    private fun problems(
        tier: String = "public",
        distribution: String = "play",
        release: Boolean = true,
        production: Boolean = false,
        monetized: Boolean = false,
        listed: Set<String> = emptySet(),
        files: Set<String> = emptySet(),
    ) = ContentEligibility.problems(tier, distribution, release, production, monetized, listed, files)

    @Test
    fun `empty manifests pass for every variant`() {
        listOf("play" to "public", "foss" to "public", "foss" to "personal").forEach { (distribution, tier) ->
            assertThat(problems(tier = tier, distribution = distribution)).isEmpty()
        }
        assertThat(problems(production = true)).isEmpty()
    }

    @Test
    fun `a personal release variant is never a production build`() {
        assertThat(problems(tier = "personal", distribution = "foss", production = true)).containsExactly(
            "a personal-tier variant is never a production build (-Pcriterion.release=production)",
        )
        // Debug variants (unit tests under release flags) are not shipped, so they are not misuse.
        assertThat(problems(tier = "personal", distribution = "foss", release = false, production = true)).isEmpty()
    }

    @Test
    fun `monetized is allowed only for playPublic release builds`() {
        assertThat(problems(monetized = true)).isEmpty()
        listOf("foss" to "public", "foss" to "personal").forEach { (distribution, tier) ->
            assertThat(problems(tier = tier, distribution = distribution, monetized = true)).containsExactly(
                "-Pcriterion.monetized=true is allowed only for playPublic (F-PL-08)",
            )
            assertThat(problems(tier = tier, distribution = distribution, release = false, monetized = true)).isEmpty()
        }
    }

    @Test
    fun `every pack and timing file must be listed in bundled json`() {
        val listed = setOf("packs/translation/ur.junagarhi.qe@1.db")
        val files =
            listed +
                setOf(
                    "packs/mushaf_layout/mushaf.indopak16_taj.dk@3.db",
                    "audio/timing/alafasy.murattal.gapless.db",
                    "fonts/indopak.ttf",
                    "content/bundled.json",
                )
        assertThat(problems(listed = listed, files = files))
            .containsExactly(
                "audio/timing/alafasy.murattal.gapless.db is in assets but not listed in content/bundled.json",
                "packs/mushaf_layout/mushaf.indopak16_taj.dk@3.db is in assets but not listed in content/bundled.json",
            ).inOrder()
        assertThat(problems(listed = listed, files = listed)).isEmpty()
    }
}
