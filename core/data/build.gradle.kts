plugins {
    alias(libs.plugins.criterion.android.library)
    alias(libs.plugins.criterion.hilt)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:content"))
    implementation(project(":core:database"))
    implementation(libs.kotlinx.coroutines.android)
}
