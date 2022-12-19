dependencies {
    compileOnly("dev.fastmc:fastmc-common:1.0-SNAPSHOT")
}

subprojects {
    dependencies {
        "modCore"("dev.fastmc:fastmc-common-${project.name}:1.0-SNAPSHOT") {
            isTransitive = false
        }
    }
}