plugins { kotlin("multiplatform"); kotlin("plugin.serialization"); id("com.android.library"); `maven-publish` }
kotlin {
    androidTarget { publishLibraryVariants("release") }
    iosArm64(); iosSimulatorArm64(); iosX64(); jvm(); ohosArm64()
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
android { namespace = "io.github.gycrosskit.storage"; compileSdk = 36; defaultConfig { minSdk = 24 } }
publishing { repositories { maven { name = "staging"; url = uri(providers.environmentVariable("STORAGE_STAGING_DIR").orElse(rootProject.layout.buildDirectory.dir("maven").map { it.asFile.absolutePath })) } } }
