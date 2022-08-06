val minecraftVersion: String by project
val fabricLoaderVersion: String by project

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    accessWidenerPath.set(file("${project(":architectury-$minecraftVersion:common").projectDir}/src/main/resources/FastMinecraft.accesswidener"))
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")

    runtimeOnly(project.project(":architectury-$minecraftVersion:common").sourceSets.main.get().output)
    library(project(":architectury-$minecraftVersion:common", "transformProductionFabric"))
    libraryImplementation(project(":shared:java17"))
}

tasks {
    processResources {
        from(loom.accessWidenerPath.get().asFile.path)
        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    classes {
        dependsOn(project(":architectury-$minecraftVersion:common").tasks.classes)
    }

    jar {
        dependsOn(project(":architectury-$minecraftVersion:common").tasks["transformProductionFabric"])
        from(
            configurations["library"].map {
                if (it.isDirectory) it else zipTree(it)
            }
        )

        archiveClassifier.set("dev")
    }

    remapJar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        archiveBaseName.set(rootProject.name)
        archiveAppendix.set("${project.name}-$minecraftVersion")
        archiveClassifier.set("release")
        archiveFileName.set("${archiveBaseName.get()}-${archiveAppendix.get()}-${archiveVersion.get()}-release.${archiveExtension.get()}")
    }

    register<Task>("genRuns") {
        group = "ide"
        doLast {
            File(rootDir, ".idea/runConfigurations/${project.name}-${minecraftVersion}_runClient.xml").writer().use {
                val threads = Runtime.getRuntime().availableProcessors()
                val vmParameters = listOf(
                    "-Xms2G",
                    "-Xmx2G",
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+AlwaysPreTouch",
                    "-XX:+DisableExplicitGC",
                    "-XX:+ParallelRefProcEnabled",
                    "-XX:+UseG1GC",
                    "-XX:+UseStringDeduplication",
                    "-XX:MaxGCPauseMillis=1",
                    "-XX:G1NewSizePercent=2",
                    "-XX:G1MaxNewSizePercent=10",
                    "-XX:G1HeapRegionSize=1M",
                    "-XX:G1ReservePercent=20",
                    "-XX:G1HeapWastePercent=5",
                    "-XX:G1MixedGCCountTarget=16",
                    "-XX:InitiatingHeapOccupancyPercent=60",
                    "-XX:G1MixedGCLiveThresholdPercent=90",
                    "-XX:G1RSetUpdatingPauseTimePercent=10",
                    "-XX:G1OldCSetRegionThresholdPercent=5",
                    "-XX:SurvivorRatio=5",
                    "-XX:ParallelGCThreads=$threads",
                    "-XX:ConcGCThreads=${threads / 4}",
                    "-XX:FlightRecorderOptions=stackdepth=512",
                    "-Dfabric.dli.config=${project.projectDir.absolutePath}/.gradle/loom-cache/launch.cfg",
                    "-Dfabric.dli.env=client",
                    "-Dfabric.dli.main=net.fabricmc.loader.launch.knot.KnotClient",
                    "-Darchitectury.main.class=${project.projectDir.absolutePath}/.gradle/architectury/.main_class",
                    "-Darchitectury.runtime.transformer=${project.projectDir.absolutePath}/.gradle/architectury/.transforms",
                    "-Darchitectury.properties=${project.projectDir.absolutePath}/.gradle/architectury/.properties",
                    "-Djdk.attach.allowAttachSelf=true",
                    "-javaagent:${rootProject.projectDir.absolutePath}/.gradle/architectury/architectury-transformer-agent.jar"
                ).joinToString(" ")

                it.write(
                    """
                        <component name="ProjectRunConfigurationManager">
                          <configuration default="false" name="${project.name}-${minecraftVersion} runClient" type="Application" factoryName="Application">
                            <option name="MAIN_CLASS_NAME" value="dev.architectury.transformer.TransformerRuntime" />
                            <module name="${rootProject.name}.architectury-${minecraftVersion}.${project.name}.main" />
                            <option name="PROGRAM_PARAMETERS" value="--width 1280 --height 720 --username TEST" />
                            <option name="VM_PARAMETERS" value="$vmParameters" />
                            <option name="WORKING_DIRECTORY" value="${rootProject.projectDir.absolutePath}/architectury-${minecraftVersion}/run" />
                            <method v="2">
                              <option name="Make" enabled="true" />
                            </method>
                          </configuration>
                        </component>
                    """.trimIndent()
                )
            }
            file("${rootProject.projectDir.absolutePath}/architectury-${minecraftVersion}/run").mkdir()
        }
    }

}