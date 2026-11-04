pluginManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        google(); mavenCentral(); gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        exclusiveContent {
            forRepository { maven(providers.gradleProperty("storageRepository").orElse("https://jitpack.io").get()) }
            filter { includeGroup("com.github.gycrosskit.storage") }
        }
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        google(); mavenCentral()
    }
}
rootProject.name = "storage-consumer"
