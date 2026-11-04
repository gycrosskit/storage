plugins {
    kotlin("multiplatform") version "2.2.21-1.0.0"
    id("com.android.library") version "8.10.1"
}
val storageVersion = providers.gradleProperty("storageVersion").orElse("0.1.0-rc.1").get()
kotlin {
    androidTarget(); iosArm64(); iosX64(); iosSimulatorArm64(); ohosArm64(); jvm()
    sourceSets {
        commonMain.dependencies { api("com.github.gycrosskit.storage:storage-core:$storageVersion") }
        androidMain.dependencies { implementation("com.github.gycrosskit.storage:storage-mmkv:$storageVersion") }
    }
}
android { namespace = "io.github.gycrosskit.storage.consumer"; compileSdk = 36; defaultConfig { minSdk = 24 } }
