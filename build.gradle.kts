import me.luna.jaroptimizer.JarOptimizerPluginExtension
import kotlin.math.max

val disableTask: (TaskProvider<*>) -> Unit = {
    it.get().enabled = false
}

ext {
    set("disableTask", disableTask)

    val threads = Runtime.getRuntime().availableProcessors()
    set(
        "runVmOptions", listOf(
            "-Xms2G",
            "-Xmx2G",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+AlwaysPreTouch",
            "-XX:+ExplicitGCInvokesConcurrent",
            "-XX:+ParallelRefProcEnabled",
            "-XX:+UseG1GC",
            "-XX:+UseStringDeduplication",
            "-XX:MaxGCPauseMillis=1",
            "-XX:G1NewSizePercent=2",
            "-XX:G1MaxNewSizePercent=10",
            "-XX:G1HeapRegionSize=1M",
            "-XX:G1ReservePercent=15",
            "-XX:G1HeapWastePercent=10",
            "-XX:G1MixedGCCountTarget=16",
            "-XX:InitiatingHeapOccupancyPercent=50",
            "-XX:G1MixedGCLiveThresholdPercent=50",
            "-XX:G1RSetUpdatingPauseTimePercent=25",
            "-XX:G1OldCSetRegionThresholdPercent=5",
            "-XX:SurvivorRatio=5",
            "-XX:ParallelGCThreads=$threads",
            "-XX:ConcGCThreads=${max(threads / 4, 1)}",
            "-XX:FlightRecorderOptions=stackdepth=512"
        )
    )
}

plugins {
    java
    kotlin("jvm")
    idea
    id("architectury-plugin").apply(false)
    id("dev.architectury.loom").apply(false)
    id("me.luna.jaroptimizer").version("1.1")
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

    tasks.register("cleanJars") {
        group = "build"
        File(buildDir, "libs").deleteRecursively()
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

    kotlin {
        kotlinDaemonJvmArgs = listOf(
            "-Xms1G",
            "-Xmx2G",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+AlwaysPreTouch",
            "-XX:+ParallelRefProcEnabled",
            "-XX:+UseG1GC",
            "-XX:+UseStringDeduplication",
            "-XX:MaxGCPauseMillis=200",
            "-XX:G1NewSizePercent=10",
            "-XX:G1MaxNewSizePercent=25",
            "-XX:G1HeapRegionSize=1M",
            "-XX:G1ReservePercent=10",
            "-XX:G1HeapWastePercent=10",
            "-XX:G1MixedGCCountTarget=8",
            "-XX:InitiatingHeapOccupancyPercent=75",
            "-XX:G1MixedGCLiveThresholdPercent=60",
            "-XX:G1RSetUpdatingPauseTimePercent=30",
            "-XX:G1OldCSetRegionThresholdPercent=25",
            "-XX:SurvivorRatio=8"
        )
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
        finalizedBy("optimizeJars")

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

    configure<JarOptimizerPluginExtension> {
        add(collectJars, "me.luna.fastmc", "org.spongepowered")
    }

    assemble {
        finalizedBy(collectJars)
    }

    val clearRuns by register<Task>("clearRuns") {
        doLast {
            val regex =
                "Minecraft_(Server|Client)___architectury.+?(fabric|forge)__architectury-.+?(fabric|forge).xml".toRegex()
            File(rootProject.projectDir.absoluteFile, ".idea/runConfigurations").listFiles()?.let { files ->
                files.asSequence()
                    .filter { it.name.matches(regex) }
                    .forEach { it.delete() }
            }
        }
    }

    val count = run {
        val regex = "project '\\:architectury-[\\d.]+\\:(forge|fabric)'".toRegex()
        subprojects.filter { it.displayName.matches(regex) }.size
    }

    val taskList = mutableListOf<Task>()

    subprojects {
        afterEvaluate {
            tasks.findByName("ideaSyncTask")?.finalizedBy(clearRuns)
            tasks.findByName("transformProductionForge")?.let {
                taskList.add(it)
            }
            tasks.findByName("transformProductionFabric")?.let {
                taskList.add(it)
            }

            if (taskList.size == count) {
                var last = taskList.first()
                for (i in 1 until taskList.size) {
                    val task = taskList[i]
                    task.mustRunAfter(last)
                    last = task
                }
            }
        }
    }
}