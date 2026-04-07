plugins {
    alias(libs.plugins.example.android.library)
}

android {
    namespace = "com.mjc.core.mlkit"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
}
