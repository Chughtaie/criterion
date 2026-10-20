plugins {
    alias(libs.plugins.criterion.jvm.library)
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
}
