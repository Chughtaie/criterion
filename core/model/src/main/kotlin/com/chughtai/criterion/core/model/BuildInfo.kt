package com.chughtai.criterion.core.model

/** Which content rules a build applies (doc 04 §11.1, D-29). The public flavor is BETA unless built for production. */
enum class ContentTier { PERSONAL, BETA, PUBLIC }

/** The `distribution` flavor (doc 04 §11.1). */
enum class Distribution { PLAY, FOSS }

/** The signed remote channel a build reads (doc 04 §11.2): PUBLIC builds read `public`, everything else `beta`. */
enum class RemoteChannel { BETA, PUBLIC }

/** Build facts, mapped once from BuildConfig by :app's BuildInfoModule (doc 04 §9). */
data class BuildInfo(
    val distribution: Distribution,
    val contentTier: ContentTier,
    val monetized: Boolean,
    val channel: RemoteChannel,
    val versionCode: Int,
    val cdnBaseUrl: String,
    /** Ed25519 public keys (hex) that may sign remote files; empty until the owner's keys exist (E-23-S01). */
    val trustedKeys: List<String>,
)
