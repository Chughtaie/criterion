package com.chughtai.criterion.buildlogic

/**
 * The checks `verifyContentEligibility<Variant>` can make before any pack exists (doc 04 §11.3). The full
 * per-item ContentPolicy, sha256 and G1/G5 gates arrive with the first packs (E-02-S04).
 */
object ContentEligibility {
    /** Asset folders whose every file must be listed in `content/bundled.json` (doc 04 §11.3). */
    val listedFolders = listOf("packs/", "audio/timing/")

    fun problems(
        tier: String,
        distribution: String,
        release: Boolean,
        production: Boolean,
        monetized: Boolean,
        listedPaths: Set<String>,
        assetFiles: Set<String>,
    ): List<String> =
        buildList {
            // Misuse checks apply to what ships (release variants), so tests under release flags still run.
            if (release && tier == "personal" && production) {
                add("a personal-tier variant is never a production build (-Pcriterion.release=production)")
            }
            if (release && monetized && !(distribution == "play" && tier == "public")) {
                add("-Pcriterion.monetized=true is allowed only for playPublic (F-PL-08)")
            }
            assetFiles
                .filter { file -> listedFolders.any { file.startsWith(it) } }
                .filterNot { it in listedPaths }
                .sorted()
                .forEach { add("$it is in assets but not listed in content/bundled.json") }
        }
}
