package com.chughtai.criterion.core.testing

import com.chughtai.criterion.core.domain.AppDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Replaces Dispatchers.Main with [testDispatcher] for one test, so ViewModel code runs under `runTest` without
 * `runBlocking` around `viewModelScope` (CLAUDE.md coding standards; g04 §2.3 #5). Code under test gets
 * [appDispatchers] instead of the production `DispatchersModule`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    val appDispatchers: AppDispatchers = AppDispatchers(io = testDispatcher, default = testDispatcher, main = testDispatcher)

    override fun starting(description: Description) = Dispatchers.setMain(testDispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}
