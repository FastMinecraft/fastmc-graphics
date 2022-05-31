plugins {
    id("fabric-loom")
}

repositories {
    maven("https://jitpack.io")
    maven("https://maven.fabricmc.net/")
}

val library by configurations

dependencies {
    // Version variables
    val minecraftVersion: String by project
    val yarnMappings: String by project
    val loaderVersion: String by project

    // Dependencies
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings")

    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    library(project(":shared:java8"))
}

loom {
    accessWidenerPath.set(file("src/main/resources/FastMinecraft.accesswidener"))
}

tasks {
    processResources {
        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    jar {
        archiveFileName.set("${archiveBaseName.get()}-${archiveAppendix.get()}-${archiveVersion.get()}.${archiveExtension.get()}")
    }

    remapJar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(
            library.map {
                if (it.isDirectory) it else zipTree(it)
            }
        )

        archiveBaseName.set(rootProject.name)
        archiveAppendix.set(project.name)
        archiveClassifier.set("release")
        archiveFileName.set("${archiveBaseName.get()}-${archiveAppendix.get()}-${archiveVersion.get()}-release.${archiveExtension.get()}")
    }
}