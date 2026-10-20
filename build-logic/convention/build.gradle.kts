import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.chughtai.criterion.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.spotless.gradlePlugin)
    testImplementation(libs.junit4)
    testImplementation(libs.truth)
}

gradlePlugin {
    plugins {
        fun conventionPlugin(
            alias: Provider<PluginDependency>,
            implementationClass: String,
        ) {
            val id = alias.get().pluginId
            register(id) {
                this.id = id
                this.implementationClass = "com.chughtai.criterion.buildlogic.$implementationClass"
            }
        }
        conventionPlugin(
            libs.plugins.criterion.android.application
                .asProvider(),
            "AndroidApplicationConventionPlugin",
        )
        conventionPlugin(libs.plugins.criterion.android.application.compose, "AndroidApplicationComposeConventionPlugin")
        conventionPlugin(
            libs.plugins.criterion.android.library
                .asProvider(),
            "AndroidLibraryConventionPlugin",
        )
        conventionPlugin(libs.plugins.criterion.android.library.compose, "AndroidLibraryComposeConventionPlugin")
        conventionPlugin(libs.plugins.criterion.jvm.library, "JvmLibraryConventionPlugin")
        conventionPlugin(libs.plugins.criterion.android.lint, "AndroidLintConventionPlugin")
        conventionPlugin(libs.plugins.criterion.spotless, "SpotlessConventionPlugin")
        conventionPlugin(libs.plugins.criterion.root, "RootConventionPlugin")
    }
}
