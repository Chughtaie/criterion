plugins {
    alias(libs.plugins.criterion.android.library)
    alias(libs.plugins.criterion.room3)
    alias(libs.plugins.criterion.hilt)
}

dependencies {
    implementation(project(":core:model"))
}
