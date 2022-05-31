subprojects {
    kotlin {
        sourceSets["main"].apply {
            kotlin.srcDir(project(":shared").kotlin.sourceSets["main"].kotlin)
        }
    }

    sourceSets {
        main.get().java.srcDir(project(":shared").sourceSets.main.get().java)
        main.get().resources.srcDir(project(":shared").sourceSets.main.get().resources)
    }
}

tasks {
    fun disable(vararg tasks: TaskProvider<*>) {
        tasks.forEach {
            it {
                enabled = false
            }
        }
    }

    disable(compileJava)
    disable(compileKotlin)
}