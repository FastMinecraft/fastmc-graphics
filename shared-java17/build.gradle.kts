dependencies {
    implementation("org.apache.logging.log4j:log4j-api:2.8.1")
}

kotlin {
    sourceSets["main"].apply {
        kotlin.srcDir(project(":shared-src").kotlin.sourceSets["main"].kotlin)
    }
}

sourceSets {
    main.get().java.srcDir(project(":shared-src").sourceSets.main.get().java)
}

tasks {
    fun disable(vararg tasks: TaskProvider<*>) {
        tasks.forEach {
            it {
                enabled = false
            }
        }
    }
}