plugins {
    kotlin("jvm")
    id("fabric-loom")
}

repositories {
    maven("https://jitpack.io")
    maven("https://maven.fabricmc.net/")
}

val library by configurations

dependencies {
    // Version variables
    val minecraftVersion: String by project
    val yarnMappings: String by project
    val loaderVersion: String by project

    // Dependencies
    implementation(project(":shared"))

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings")

    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    library(project(":shared")) {
        exclude(group = "org.apache.logging.log4j")
    }
}

loom {
    accessWidener("src/main/resources/FastMinecraft.accesswidener")
}

tasks {
    processResources {
        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    jar {
        archiveFileName.set("${archiveBaseName.get()}-${archiveAppendix.get()}-${archiveVersion.get()}.${archiveExtension.get()}")
    }

    remapJar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(
            library.map {
                if (it.isDirectory) it else zipTree(it)
            }
        )

        archiveBaseName.set(rootProject.name)
        archiveAppendix.set(project.name)
        archiveClassifier.set("release")
        archiveFileName.set("${archiveBaseName.get()}-${archiveAppendix.get()}-${archiveVersion.get()}-release.${archiveExtension.get()}")
    }

    register<Task>("genRuns") {
        group = "ide"
        doLast {
            File(rootDir, ".idea/runConfigurations/${project.name}_runClient.xml").writer().use {
                val threads = Runtime.getRuntime().availableProcessors()
                it.write(
                    """
                        <component name="ProjectRunConfigurationManager">
                          <configuration default="false" name="${project.name} runClient" type="Application" factoryName="Application">
                            <envs>
                              <env name="VERSION" value="${project.name}" />
                            </envs>
                            <option name="MAIN_CLASS_NAME" value="net.fabricmc.devlaunchinjector.Main" />
                            <module name="${rootProject.name}.${project.name}.main" />
                            <option name="PROGRAM_PARAMETERS" value="--width 1280 --height 720" />
                            <option name="VM_PARAMETERS" value="-Xms2G -Xmx2G -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch -XX:+DisableExplicitGC -XX:+ParallelRefProcEnabled -XX:+UseG1GC -XX:+UseStringDeduplication -XX:MaxGCPauseMillis=5 -XX:G1NewSizePercent=2 -XX:G1MaxNewSizePercent=10 -XX:G1HeapRegionSize=1M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=8 -XX:InitiatingHeapOccupancyPercent=60 -XX:G1MixedGCLiveThresholdPercent=75 -XX:G1RSetUpdatingPauseTimePercent=10 -XX:G1OldCSetRegionThresholdPercent=5 -XX:SurvivorRatio=5 -XX:ParallelGCThreads=${threads} -XX:ConcGCThreads=${threads / 4} -XX:FlightRecorderOptions=stackdepth=512 -Dfabric.dli.config=${'$'}PROJECT_DIR$/${project.name}/.gradle/loom-cache/launch.cfg -Dfabric.dli.env=client -Dfabric.dli.main=net.fabricmc.loader.launch.knot.KnotClient" />
                            <option name="WORKING_DIRECTORY" value="${'$'}PROJECT_DIR$/${project.name}/run/" />
                            <method v="2">
                              <option name="Make" enabled="true" />
                            </method>
                          </configuration>
                        </component>
                    """.trimIndent()
                )
            }
            File(projectDir, "run").mkdir()
        }
    }
}