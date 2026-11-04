import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { kotlin("multiplatform"); kotlin("plugin.serialization"); id("com.android.library"); `maven-publish` }
kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    iosArm64(); iosSimulatorArm64(); iosX64(); jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }; ohosArm64()
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2-1.0.0")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.1-1.0.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2-1.0.0")
        }
    }
}
android {
    namespace = "io.github.gycrosskit.storage"; compileSdk = 36; defaultConfig { minSdk = 24 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
}
publishing { repositories { maven { name = "staging"; url = uri(rootProject.layout.buildDirectory.dir("maven")) } } }
