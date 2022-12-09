dependencies {
    api("dev.fastmc:common") {
        isTransitive = false
    }
}

subprojects {
    dependencies {
        libraryApi("dev.fastmc:common-${project.name}") {
            isTransitive = false
        }
    }
}