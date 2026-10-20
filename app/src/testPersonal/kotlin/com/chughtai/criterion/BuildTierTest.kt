package com.chughtai.criterion

import com.chughtai.criterion.core.model.ContentTier
import com.chughtai.criterion.core.model.RemoteChannel
import com.chughtai.criterion.di.BuildInfoModule
import org.junit.Assert.assertEquals
import org.junit.Test

/** Personal-tier variants (fossPersonal*) apply PERSONAL content rules and read the beta channel (doc 04 §11.1). */
class BuildTierTest {
    @Test
    fun personalTierIsPersonal() {
        assertEquals("PERSONAL", BuildConfig.CONTENT_TIER)
        val info = BuildInfoModule.buildInfo()
        assertEquals(ContentTier.PERSONAL, info.contentTier)
        assertEquals(RemoteChannel.BETA, info.channel)
        assertEquals(System.getProperty("criterion.monetized") == "true", info.monetized)
    }
}
