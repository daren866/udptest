plugins {
    alias(libs.plugins.example.android.compose.library)
}

android {
    namespace = "com.mjc.feature.videoplayer"
}

dependencies {
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.smoothstreaming)
    implementation(libs.media3.ui)
    implementation(libs.media3.ui.compose)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)
}
