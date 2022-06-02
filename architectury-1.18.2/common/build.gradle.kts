val minecraftVersion: String by project
val fabricLoaderVersion: String by project

architectury {
    common("fabric")
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")

    compileOnly(project(":shared"))
    implementation(project(":shared:java17"))
}

loom {
    accessWidenerPath.set(file("src/main/resources/FastMinecraft.accesswidener"))
}

tasks {
    processResources {
        doFirst {
            findProject(":architectury-$minecraftVersion:fabric")?.let {
                file("${projectDir.absolutePath}/src/main/resources/FastMinecraft.accessWidener")
                    .copyTo(file("${it.projectDir.absolutePath}/src/main/resources/FastMinecraft.accessWidener"), true)
            }
        }
    }

    remapJar.get().isEnabled = false
}