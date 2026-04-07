plugins {
    alias(libs.plugins.example.android.compose.library)
}

android {
    namespace = "com.mjc.feature.download"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.workmanager)
    implementation(project(":core:download"))
}
