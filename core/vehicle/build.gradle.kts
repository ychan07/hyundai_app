plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.storydrive.core.vehicle"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    // AAOS CarPropertyManager(PERF_VEHICLE_SPEED), CarUxRestrictionsManager 사용
    useLibrary("android.car")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":core:domain"))
    api(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)

    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navihelper)

    testImplementation(libs.junit)
}
