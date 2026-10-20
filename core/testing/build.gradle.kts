plugins {
    alias(libs.plugins.criterion.android.library)
}

dependencies {
    api(project(":core:domain"))
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
}
