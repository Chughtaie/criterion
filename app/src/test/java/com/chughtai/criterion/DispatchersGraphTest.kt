package com.chughtai.criterion

import com.chughtai.criterion.core.domain.AppDispatchers
import com.chughtai.criterion.core.domain.AppScope
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject
import kotlin.coroutines.ContinuationInterceptor
import kotlin.time.Clock

/** The app's real SingletonComponent provides doc 04 §9's DispatchersModule bindings. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [36])
class DispatchersGraphTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var dispatchers: AppDispatchers

    @Inject
    lateinit var appScope: AppScope

    @Inject
    lateinit var clock: Clock

    @Test
    fun singletonGraphProvidesDispatchersScopeAndClock() {
        hiltRule.inject()
        assertThat(dispatchers.io).isSameInstanceAs(Dispatchers.IO)
        assertThat(dispatchers.default).isSameInstanceAs(Dispatchers.Default)
        assertThat(dispatchers.main).isSameInstanceAs(Dispatchers.Main.immediate)
        assertThat(appScope.scope.coroutineContext[ContinuationInterceptor]).isSameInstanceAs(Dispatchers.Default)
        assertThat(appScope.scope.coroutineContext[Job]?.isActive).isTrue()
        assertThat(clock).isSameInstanceAs(Clock.System)
    }
}
