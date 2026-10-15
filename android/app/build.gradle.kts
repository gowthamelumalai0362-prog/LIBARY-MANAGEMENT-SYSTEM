plugins {
    id("com.android.application")
}

val backendUrl = (project.findProperty("backendUrl") as String?)
    ?: System.getenv("AURALMS_BACKEND_URL")
    ?: "https://auralms-backend.onrender.com"

android {
    namespace = "com.library.auralms"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.library.auralms"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "BACKEND_URL", "\"${backendUrl.trimEnd('/')}\"")
    }

    buildFeatures {
        buildConfig = true
    }
}
