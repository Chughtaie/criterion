package com.chughtai.criterion

import com.chughtai.criterion.core.model.ContentTier
import com.chughtai.criterion.core.model.RemoteChannel
import com.chughtai.criterion.di.BuildInfoModule
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Public-tier variants are BETA (closed testing) unless built with -Pcriterion.release=production, which makes them
 * PUBLIC (doc 04 §11.1). The flavors convention passes that property to the test JVM as `criterion.release`.
 */
class BuildTierTest {
    @Test
    fun publicTierIsBetaUnlessProduction() {
        val production = System.getProperty("criterion.release") == "production"
        assertEquals(if (production) "PUBLIC" else "BETA", BuildConfig.CONTENT_TIER)
        val info = BuildInfoModule.buildInfo()
        assertEquals(if (production) ContentTier.PUBLIC else ContentTier.BETA, info.contentTier)
        assertEquals(if (production) RemoteChannel.PUBLIC else RemoteChannel.BETA, info.channel)
        assertEquals(System.getProperty("criterion.monetized") == "true", info.monetized)
    }
}
