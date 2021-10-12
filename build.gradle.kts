plugins {
    java
    kotlin("jvm")
}

allprojects {
    group = "me.xiaro"
    version = "0.0.1"

    apply {
        plugin("java")
        plugin("kotlin")
    }

    repositories {
        mavenCentral()
        maven("https://libraries.minecraft.net")
    }

    dependencies {
        val kotlinVersion: String by rootProject
        val kotlinxCoroutineVersion: String by rootProject

        implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion") {
            exclude("kotlin-stdlib-common")
            exclude("annotations")
        }
        implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion") {
            exclude("kotlin-stdlib-common")
            exclude("annotations")
        }
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion") {
            exclude("kotlin-stdlib-common")
            exclude("annotations")
        }
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutineVersion") {
            exclude("kotlin-stdlib-common")
            exclude("annotations")
        }

        compileOnly("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion")
        compileOnly("org.jetbrains:annotations:22.0.0")

        val lwjglVersion: String by project
        val jomlVersion: String by project

        compileOnly("it.unimi.dsi:fastutil:7.1.0")
        implementation("org.joml:joml:$jomlVersion")
    }

    tasks {
        jar {
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
                    "-Xlambdas=indy"
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

    disable(compileKotlin, compileJava, compileTestKotlin, compileTestJava)
    disable(processResources, processTestResources)
    disable(classes, testClasses)
    disable(inspectClassesForKotlinIC)
    disable(test, check)
    disable(jar, assemble, build)

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