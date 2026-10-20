package com.chughtai.criterion.core.data.di

import com.chughtai.criterion.core.domain.AppDispatchers
import com.chughtai.criterion.core.domain.AppScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * The only place that names concrete dispatchers and the system clock; everything else injects [AppDispatchers],
 * [AppScope] and [Clock] (doc 04 §4.4, §9). Public so tests can replace it with `@TestInstallIn`.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @Singleton
    fun dispatchers(): AppDispatchers =
        AppDispatchers(io = Dispatchers.IO, default = Dispatchers.Default, main = Dispatchers.Main.immediate)

    @Provides
    @Singleton
    fun appScope(dispatchers: AppDispatchers): AppScope = AppScope(CoroutineScope(SupervisorJob() + dispatchers.default))

    @Provides
    @Singleton
    fun clock(): Clock = Clock.System
}
