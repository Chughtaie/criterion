package com.chughtai.criterion.di

import com.chughtai.criterion.BuildConfig
import com.chughtai.criterion.core.model.BuildInfo
import com.chughtai.criterion.core.model.ContentTier
import com.chughtai.criterion.core.model.Distribution
import com.chughtai.criterion.core.model.RemoteChannel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** The one place BuildConfig becomes [BuildInfo] (doc 04 §9, §11.1). */
@Module
@InstallIn(SingletonComponent::class)
internal object BuildInfoModule {
    @Provides
    @Singleton
    fun buildInfo(): BuildInfo =
        BuildInfo(
            distribution = Distribution.valueOf(BuildConfig.DISTRIBUTION.uppercase()),
            contentTier = ContentTier.valueOf(BuildConfig.CONTENT_TIER),
            monetized = BuildConfig.MONETIZED,
            channel = RemoteChannel.valueOf(BuildConfig.REMOTE_CHANNEL.uppercase()),
            versionCode = BuildConfig.VERSION_CODE,
            cdnBaseUrl = BuildConfig.CDN_BASE_URL,
            trustedKeys =
                BuildConfig.TRUSTED_KEYS
                    .split(',')
                    .map(String::trim)
                    .filter(String::isNotEmpty),
        )
}
