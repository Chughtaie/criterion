package com.chughtai.criterion.buildlogic

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KotlinLiftTest {
    private val lifted =
        listOf(
            ClasspathEdge("org.jetbrains.kotlin", "kotlin-gradle-plugin", "2.2.10", "2.4.21"),
            ClasspathEdge("org.jetbrains.kotlin", "kotlin-gradle-plugin", "2.4.21", "2.4.21"),
            ClasspathEdge("com.google.devtools.ksp", "symbol-processing-gradle-plugin", "2.2.10-2.0.2", "2.3.12"),
            ClasspathEdge("com.android.tools.build", "gradle", "9.4.1", "9.4.1"),
        )

    @Test
    fun `the lifted classpath passes and reports both lifts`() {
        assertThat(KotlinLift.problems(lifted, expectedKotlin = "2.4.21", expectedKsp = "2.3.12")).isEmpty()
        assertThat(KotlinLift.lifts(lifted))
            .containsExactly(
                "com.google.devtools.ksp:symbol-processing-gradle-plugin:2.2.10-2.0.2 -> 2.3.12",
                "org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10 -> 2.4.21",
            ).inOrder()
    }

    @Test
    fun `losing the lift fails`() {
        val unlifted =
            listOf(
                ClasspathEdge("org.jetbrains.kotlin", "kotlin-gradle-plugin", "2.2.10", "2.2.10"),
                ClasspathEdge("com.google.devtools.ksp", "symbol-processing-gradle-plugin", "2.2.10-2.0.2", "2.3.12"),
            )
        assertThat(KotlinLift.problems(unlifted, expectedKotlin = "2.4.21", expectedKsp = "2.3.12")).containsExactly(
            "org.jetbrains.kotlin:kotlin-gradle-plugin resolves to [2.2.10]; D-02 requires 2.4.21",
        )
    }

    @Test
    fun `a missing plugin fails`() {
        val problems = KotlinLift.problems(emptyList(), expectedKotlin = "2.4.21", expectedKsp = "2.3.12")
        assertThat(problems).containsExactly(
            "org.jetbrains.kotlin:kotlin-gradle-plugin is not on the root buildscript classpath",
            "com.google.devtools.ksp:symbol-processing-gradle-plugin is not on the root buildscript classpath",
        )
    }
}
