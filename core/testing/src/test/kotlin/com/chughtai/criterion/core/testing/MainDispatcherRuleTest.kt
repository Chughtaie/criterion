package com.chughtai.criterion.core.testing

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MainDispatcherRuleTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun runTestSharesTheRulesScheduler() =
        runTest {
            assertThat(testScheduler).isSameInstanceAs(mainDispatcherRule.testDispatcher.scheduler)
        }

    @Test
    fun mainRunsEagerlyOnTheTestDispatcher() =
        runTest {
            var ran = false
            launch(Dispatchers.Main) { ran = true }
            assertThat(ran).isTrue()
            assertThat(withContext(Dispatchers.Main) { Thread.currentThread() }).isSameInstanceAs(Thread.currentThread())
        }

    @Test
    fun appDispatchersAllUseTheTestDispatcher() {
        val dispatchers = mainDispatcherRule.appDispatchers
        assertThat(listOf(dispatchers.io, dispatchers.default, dispatchers.main)).containsExactly(
            mainDispatcherRule.testDispatcher,
            mainDispatcherRule.testDispatcher,
            mainDispatcherRule.testDispatcher,
        )
    }

    @Test
    fun mainIsResetAfterTheRule() {
        val inner = MainDispatcherRule()
        val noOp =
            object : Statement() {
                override fun evaluate() = Unit
            }
        inner.apply(noOp, Description.EMPTY).evaluate()
        // The outer rule's Main was replaced and then reset by the inner rule, so Main is the missing dispatcher.
        assertThrows(IllegalStateException::class.java) { Dispatchers.Main.isDispatchNeeded(kotlin.coroutines.EmptyCoroutineContext) }
    }
}
