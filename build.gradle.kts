val disableTask: (TaskProvider<*>) -> Unit = {
    it.get().enabled = false
}

ext {
    set("disableTask", disableTask)
}

plugins {
    java
    kotlin("jvm")
    idea
}

idea {
    module {
        excludeDirs.add(file(".architectury-transformer"))
        subprojects.forEach {
            excludeDirs.add(file("${it.projectDir}/run"))
        }
    }
}

allprojects {
    group = "me.luna"
    version = "0.0.1"

    apply {
        plugin("java")
        plugin("kotlin")
    }

    repositories {
        mavenCentral()
        maven("https://libraries.minecraft.net")
    }

    val libraryImplementation by configurations.creating
    val library by configurations.creating

    dependencies {
        fun ModuleDependency.exclude(moduleName: String): ModuleDependency {
            return exclude(module = moduleName)
        }

        val kotlinVersion: String by rootProject
        val kotlinxCoroutineVersion: String by rootProject
        val jomlVersion: String by rootProject

        libraryImplementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion") {
            exclude("kotlin-stdlib-common")
            exclude("annotations")
        }
        libraryImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion") {
            exclude("kotlin-stdlib-common")
            exclude("annotations")
        }
        libraryImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutineVersion") {
            exclude("kotlin-stdlib-common")
            exclude("annotations")
        }

        compileOnly("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion")
        compileOnly("org.jetbrains:annotations:23.0.0")
        compileOnly("org.apache.logging.log4j:log4j-api:2.8.1")

        compileOnly("it.unimi.dsi:fastutil:7.1.0")
        libraryImplementation("org.joml:joml:$jomlVersion")

        library(libraryImplementation)
        implementation(libraryImplementation)
    }
}

subprojects {
    val javaVersion = findProperty("javaVersion")?.toString()?.toInt() ?: 8
    val fullJavaVersion = if (javaVersion < 9) "1.$javaVersion" else javaVersion.toString()

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }
    }

    tasks {
        jar {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        compileJava {
            options.encoding = "UTF-8"
            sourceCompatibility = fullJavaVersion
            targetCompatibility = fullJavaVersion
        }

        compileKotlin {
            kotlinOptions {
                jvmTarget = fullJavaVersion
                freeCompilerArgs = listOf(
                    "-opt-in=kotlin.RequiresOptIn",
                    "-opt-in=kotlin.contracts.ExperimentalContracts",
                    "-Xlambdas=indy",
                    "-Xjvm-default=all"
                )
            }
        }
    }
}

tasks {
    disableTask(jar)

    val collectJars by register<Copy>("collectJars") {
        group = "build"

        subprojects.asSequence()
            .filterNot {
                it.name.contains("shared")
            }
            .forEach {
                dependsOn(it.tasks.assemble)
            }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        subprojects.forEach { project ->
            val regex =
                "${rootProject.name}-(fabric|forge)-${project.findProperty("minecraftVersion")}-${rootProject.version}-release\\.jar".toRegex()
            from(file("${project.buildDir}/libs/")) {
                include {
                    it.name.matches(regex)
                }
            }
        }

        into(file("$buildDir/libs"))
    }

    assemble {
        finalizedBy(collectJars)
    }
}