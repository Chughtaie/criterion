// Convention plugins for the Criterion build (doc 04 §16.2). Included from the root settings via
// pluginManagement { includeBuild("build-logic") }.
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
