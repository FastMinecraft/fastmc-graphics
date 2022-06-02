val minecraftVersion: String by project
val fabricLoaderVersion: String by project

architectury {
    common("fabric")
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")

    compileOnly(project(":shared"))
    implementation(project(":shared:java8"))
}

loom {
    accessWidenerPath.set(file("src/main/resources/FastMinecraft.accesswidener"))
}

tasks {
    remapJar.get().isEnabled = false
}