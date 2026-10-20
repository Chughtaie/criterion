package com.chughtai.criterion.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** SDK levels and Java target from D-02. */
internal object CriterionSdk {
    const val COMPILE = 37
    const val TARGET = 36
    const val MIN = 24
}

/** Settings every Android module shares (doc 04 §16.2): SDK levels, Java 17 and core library desugaring. */
internal fun Project.configureAndroidCommon(extension: CommonExtension) {
    extension.apply {
        compileSdk { version = release(CriterionSdk.COMPILE) }
        defaultConfig.minSdk = CriterionSdk.MIN
        defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17
        compileOptions.isCoreLibraryDesugaringEnabled = true
    }
    dependencies {
        add("coreLibraryDesugaring", libs.library("android-desugarJdkLibs"))
    }
}

/** `:core:mushaf-render` → `com.chughtai.criterion.core.mushafrender`. */
internal fun Project.defaultNamespace(): String = "com.chughtai.criterion" + path.replace("-", "").replace(':', '.')
