plugins {
    java
    kotlin("jvm")
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

    val library by configurations.creating

    dependencies {
        fun ModuleDependency.exclude(moduleName: String): ModuleDependency {
            return exclude(module = moduleName)
        }

        val kotlinVersion: String by rootProject
        val kotlinxCoroutineVersion: String by rootProject
        val jomlVersion: String by rootProject

        library("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion") {
            exclude("kotlin-stdlib-common")
            exclude("annotations")
        }
        library("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion") {
            exclude("kotlin-stdlib-common")
            exclude("annotations")
        }
        library("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutineVersion") {
            exclude("kotlin-stdlib-common")
            exclude("annotations")
        }

        compileOnly("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion")
        compileOnly("org.jetbrains:annotations:22.0.0")

        compileOnly("it.unimi.dsi:fastutil:7.1.0")
        library("org.joml:joml:$jomlVersion")

        implementation(library)
    }

    tasks {
        jar {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            archiveBaseName.set(rootProject.name)
            archiveAppendix.set(project.name)
        }

        compileJava {
            options.encoding = "UTF-8"
            sourceCompatibility = "1.8"
            targetCompatibility = "1.8"
        }

        compileKotlin {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs = listOf(
                    "-Xopt-in=kotlin.RequiresOptIn",
                    "-Xopt-in=kotlin.contracts.ExperimentalContracts",
                    "-Xlambdas=indy",
                    "-Xjvm-default=all"
                )
            }
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
}

tasks {
    fun disable(vararg tasks: TaskProvider<*>) {
        tasks.forEach {
            it {
                enabled = false
            }
        }
    }

    val collectJars by register<Copy>("collectJars") {
        group = "build"

        subprojects.forEach {
            it.tasks {
                dependsOn(build)
            }
        }

        subprojects.forEach {
            from(file("${it.buildDir}/libs/${rootProject.name}-${it.name}-${rootProject.version}-release.jar"))
        }
        into(file("$buildDir/libs"))
    }

    build {
        finalizedBy(collectJars)
    }
}