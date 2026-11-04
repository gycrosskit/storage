import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { kotlin("multiplatform"); id("com.android.library"); `maven-publish` }
kotlin {
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    sourceSets.androidMain.dependencies {
        api(project(":storage-core"))
        implementation("com.tencent:mmkv:2.4.3")
    }
    sourceSets.androidUnitTest.dependencies {
        implementation(kotlin("test"))
        implementation("org.robolectric:robolectric:4.16")
    }
}
android {
    namespace = "io.github.gycrosskit.storage.mmkv"; compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }
    testOptions { unitTests.isIncludeAndroidResources = true }
}
publishing { repositories { maven { name = "staging"; url = uri(rootProject.layout.buildDirectory.dir("maven")) } } }
