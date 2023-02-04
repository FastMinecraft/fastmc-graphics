rootProject.name = "fastmc-graphics"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fastmc.dev/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://files.minecraftforge.net/maven/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }
}

//includeBuild("../mod-setup")

include("shared")
include("forge-1.12.2")
include("architectury-1.16.5", "architectury-1.16.5:common", "architectury-1.16.5:fabric", "architectury-1.16.5:forge")
include("architectury-1.18.2", "architectury-1.18.2:common", "architectury-1.18.2:fabric", "architectury-1.18.2:forge")
include("architectury-1.19.2", "architectury-1.19.2:common", "architectury-1.19.2:fabric", "architectury-1.19.2:forge")