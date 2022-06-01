val disableTask: (TaskProvider<*>) -> Unit by rootProject.ext
val sharedProject = project

subprojects {
    kotlin {
        sourceSets["main"].apply {
            kotlin.source(sharedProject.kotlin.sourceSets["main"].kotlin)
        }
    }

    sourceSets {
        main {
            java.source(sharedProject.sourceSets.main.get().java)
            resources.source(sharedProject.sourceSets.main.get().resources)
        }
    }
}

tasks {
    disableTask(compileJava)
    disableTask(compileKotlin)
    disableTask(processResources)
    disableTask(jar)
}