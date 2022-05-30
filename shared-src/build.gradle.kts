dependencies {
    implementation("org.apache.logging.log4j:log4j-api:2.8.1")
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