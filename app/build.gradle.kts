plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.gamelingo.persian"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.gamelingo.persian"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
}
dependencies {
    implementation("com.google.mlkit:translate:17.0.3")
}
