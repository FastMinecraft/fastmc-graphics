rootProject.name = "FastMinecraft"

pluginManagement {
    val kotlinVersion: String by settings

    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://files.minecraftforge.net/maven/")
        gradlePluginPortal()
    }

    plugins {
        id("dev.architectury.loom").version("0.12-SNAPSHOT")
        id("architectury-plugin").version("3.4-SNAPSHOT")

        id("fabric-loom").version("0.12-SNAPSHOT")
        id("org.jetbrains.kotlin.jvm").version(kotlinVersion)
    }
}

include("shared", "shared:java8", "shared:java16", "shared:java17")
include("forge-1.12.2", "fabric-1.16.5")