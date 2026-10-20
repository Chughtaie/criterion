// Root: load every plugin once with `apply false` (doc 04 §16.1). kotlin-jvm (like the other Kotlin plugins) carries
// KGP 2.4.21 and lifts AGP 9's bundled KGP 2.2.10 (D-02; g04 §1.2); verifyKotlinGradlePlugin guards it.
// criterion.root adds the guards and the quickCheck/prCheck aggregates; criterion.spotless formats the repository.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.dependency.analysis) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
    alias(libs.plugins.criterion.root)
    alias(libs.plugins.criterion.spotless)
}
