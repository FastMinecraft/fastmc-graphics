val disableTask: (TaskProvider<*>) -> Unit by rootProject.ext

plugins {
    id("architectury-plugin").version("3.4+")
    id("dev.architectury.loom").version("0.12+").apply(false)
}

val minecraftVersion: String by project
val yarnMappings: String by project
val fabricLoaderVersion: String by project

architectury {
    minecraft = minecraftVersion
}

subprojects {
    apply {
        plugin("architectury-plugin")
        plugin("dev.architectury.loom")
    }

    dependencies {
        "minecraft"("com.mojang:minecraft:$minecraftVersion")
        "mappings"("net.fabricmc:yarn:$yarnMappings")
    }
}

tasks {
    disableTask(jar)
}