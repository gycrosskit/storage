import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.2.21-1.0.0"
    id("com.android.library") version "8.10.1"
}
val storageVersion = providers.gradleProperty("storageVersion").orElse("1.0.0").get()
kotlin {
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }; iosArm64(); iosX64(); iosSimulatorArm64(); ohosArm64(); jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }
    sourceSets {
        commonMain.dependencies { api("com.github.gycrosskit.storage:storage-core:$storageVersion") }
        androidMain.dependencies { implementation("com.github.gycrosskit.storage:storage-mmkv:$storageVersion") }
    }
}
android {
    namespace = "io.github.gycrosskit.storage.consumer"; compileSdk = 36; defaultConfig { minSdk = 24 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
}
