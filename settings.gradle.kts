pluginManagement {
    val kotlinVersion: String by settings

    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
    }
    plugins {
        id("fabric-loom").version("0.12-SNAPSHOT")
        id("org.jetbrains.kotlin.jvm").version(kotlinVersion)
    }
}

rootProject.name = "FastMinecraft"

include("shared-src", "shared-java8", "shared-java17")
val version = System.getenv("VERSION")
if (version == null) {
    include("forge-1.12.2", "fabric-1.16.5")
} else {
    include(version)
}