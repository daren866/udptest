plugins {
    alias(libs.plugins.example.android.compose.library)
}

android {
    namespace = "com.mjc.feature.camera"
}

dependencies {
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.video)
    implementation(libs.camerax.compose)
}
