package com.chughtai.criterion.core.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/** Injected as a type, so pure modules need no qualifier annotations (doc 04 §4.4). */
data class AppDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
    val main: CoroutineDispatcher,
)

/** The process-lifetime scope: SupervisorJob() + dispatchers.default (doc 04 §4.4). */
class AppScope(
    val scope: CoroutineScope,
)
