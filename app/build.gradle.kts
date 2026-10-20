plugins {
    alias(libs.plugins.criterion.android.application)
    alias(libs.plugins.criterion.android.application.compose)
}

android {
    namespace = "com.chughtai.criterion"

    defaultConfig {
        applicationId = "com.chughtai.criterion"
        versionCode = 1
        versionName = "1.0"
    }

    androidResources { generateLocaleConfig = true } // + res/resources.properties unqualifiedResLocale=en-US
    bundle { language { enableSplit = false } } // in-app language switch (D-21)
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
