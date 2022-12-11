rootProject.name = "fastmc-graphics"

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://files.minecraftforge.net/maven/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://maven.fastmc.dev")
    }
}

//includeBuild("../mod-setup")
//includeBuild("../fastmc-common") {
//    dependencySubstitution {
//        substitute(module("dev.fastmc:fastmc-common")).using(project(":"))
//        substitute(module("dev.fastmc:fastmc-common-java8")).using(project(":java8"))
//        substitute(module("dev.fastmc:fastmc-common-java17")).using(project(":java17"))
//    }
//}

include("shared")
include("shared:java8")
include("forge-1.12.2")
include("architectury-1.16.5", "architectury-1.16.5:common", "architectury-1.16.5:fabric", "architectury-1.16.5:forge")

//include("shared:java16")

include("shared:java17")
include("architectury-1.18.2", "architectury-1.18.2:common", "architectury-1.18.2:fabric", "architectury-1.18.2:forge")
include("architectury-1.19.2", "architectury-1.19.2:common", "architectury-1.19.2:fabric", "architectury-1.19.2:forge")