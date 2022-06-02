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
        id("org.jetbrains.kotlin.jvm").version(kotlinVersion)
    }
}

include("shared")
include("shared:java8")
include("forge-1.12.2")
include("architectury-1.16.5", "architectury-1.16.5:common", "architectury-1.16.5:fabric")

//include("shared:java16")

include("shared:java17")
include("architectury-1.18.2", "architectury-1.18.2:common", "architectury-1.18.2:fabric")