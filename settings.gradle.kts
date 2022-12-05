rootProject.name = "fastmc-graphics"

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://files.minecraftforge.net/maven/")
        gradlePluginPortal()
        maven("https://raw.githubusercontent.com/Luna5ama/JarOptimizer/maven-repo")
    }
}

includeBuild("../mod-setup")

include("shared")
include("shared:java8")
include("forge-1.12.2")
include("architectury-1.16.5", "architectury-1.16.5:common", "architectury-1.16.5:fabric", "architectury-1.16.5:forge")

//include("shared:java16")

include("shared:java17")
include("architectury-1.18.2", "architectury-1.18.2:common", "architectury-1.18.2:fabric", "architectury-1.18.2:forge")
include("architectury-1.19.2", "architectury-1.19.2:common", "architectury-1.19.2:fabric", "architectury-1.19.2:forge")