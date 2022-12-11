dependencies {
    api("dev.fastmc:fastmc-common:1.0-SNAPSHOT") {
        isTransitive = false
    }
}

subprojects {
    dependencies {
        libraryApi("dev.fastmc:fastmc-common-${project.name}:1.0-SNAPSHOT") {
            isTransitive = false
        }
    }
}