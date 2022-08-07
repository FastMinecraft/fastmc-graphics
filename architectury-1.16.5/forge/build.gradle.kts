import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

val minecraftVersion: String by project
val forgeVersion: String by project

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    accessWidenerPath.set(file("${project(":architectury-$minecraftVersion:common").projectDir}/src/main/resources/FastMinecraft.accesswidener"))

    forge {
        convertAccessWideners.set(true)
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)
        mixinConfig("mixins.fastmc-core.json")
        mixinConfig("mixins.fastmc-accessor.json")
        mixinConfig("mixins.fastmc-patch.json")
    }
}

dependencies {
    forge("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")

    runtimeOnly(project.project(":architectury-$minecraftVersion:common").sourceSets.main.get().output)
    library(project(":architectury-$minecraftVersion:common", "transformProductionForge"))
    libraryImplementation(project(":shared:java8"))
}

tasks {
    processResources {
        from(loom.accessWidenerPath.get().asFile.path)
        filesMatching("*/mods.toml") {
            expand("version" to project.version)
        }
    }

    loom.officialMojangMappings()

    classes {
        dependsOn(project(":architectury-$minecraftVersion:common").tasks.classes)
    }

    jar {
        dependsOn(project(":architectury-$minecraftVersion:common").tasks["transformProductionForge"])
        from(
            configurations["library"].map {
                if (it.isDirectory) it else zipTree(it)
            }
        )

        archiveClassifier.set("dev")
    }

    // Hacky mapping fix
    val patchJar by register<Task>("patchJar") {
        doLast {
            val file = File(
                project.buildDir,
                "libs/${rootProject.name}-${project.name}-$minecraftVersion-${project.version}-release.jar"
            )
            val zipFile = ZipFile(file)
            val text =
                zipFile.getInputStream(zipFile.getEntry("META-INF/accesstransformer.cfg")).readBytes().decodeToString()
                    .replace("net/minecraft/util/math/Matrix3f", "net/minecraft/util/math/vector/Matrix3f")
                    .replace("net/minecraft/util/math/Matrix4f", "net/minecraft/util/math/vector/Matrix4f")

            val cacheZipEntries = zipFile.entries().asSequence()
                .filter { it.name != "META-INF/accesstransformer.cfg" }
                .map { ZipEntry(it.name) to zipFile.getInputStream(it).readBytes() }
                .toList()

            ZipOutputStream(file.outputStream()).use { zip ->
                cacheZipEntries.forEach { (zipEntry, bytes) ->
                    zip.putNextEntry(zipEntry)
                    zip.write(bytes)
                    zip.closeEntry()
                }

                zip.putNextEntry(ZipEntry("META-INF/accesstransformer.cfg"))
                zip.write(text.encodeToByteArray())
                zip.closeEntry()
            }
        }
    }

    remapJar {
        finalizedBy(patchJar)

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        archiveBaseName.set(rootProject.name)
        archiveAppendix.set("${project.name}-$minecraftVersion")
        archiveClassifier.set("release")
        archiveFileName.set("${archiveBaseName.get()}-${archiveAppendix.get()}-${archiveVersion.get()}-release.${archiveExtension.get()}")
    }

// Forge run is broken in dev env
//    register<Task>("genRuns") {
//        group = "ide"
//        doLast {
//            File(rootDir, ".idea/runConfigurations/${project.name}-${minecraftVersion}_runClient.xml").writer().use {
//                @Suppress("UNCHECKED_CAST")
//                val vmOptions = ((rootProject.ext["runVmOptions"] as List<String>) + listOf(
//                    "-Dfabric.dli.config=${project.projectDir.absolutePath}/.gradle/loom-cache/launch.cfg",
//                    "-Dfabric.dli.env=client",
//                    "-XX:+IgnoreUnrecognizedVMOptions",
//                    "--add-exports=java.base/sun.security.util=ALL-UNNAMED",
//                    "--add-exports=jdk.naming.dns/com.sun.jndi.dns=java.naming",
//                    "--add-opens=java.base/java.util.jar=ALL-UNNAMED",
//                    "-Dfabric.dli.main=net.minecraftforge.userdev.LaunchTesting",
//                    "-Darchitectury.main.class=${project.projectDir.absolutePath}/.gradle/architectury/.main_class",
//                    "-Darchitectury.runtime.transformer=${project.projectDir.absolutePath}/.gradle/architectury/.transforms",
//                    "-Darchitectury.properties=${project.projectDir.absolutePath}/.gradle/architectury/.properties",
//                    "-Djdk.attach.allowAttachSelf=true",
//                    "-javaagent:${rootProject.projectDir.absolutePath}/.gradle/architectury/architectury-transformer-agent.jar"
//                )).joinToString(" ")
//
//                it.write(
//                    """
//                        <component name="ProjectRunConfigurationManager">
//                          <configuration default="false" name="${project.name}-${minecraftVersion} runClient" type="Application" factoryName="Application">
//                            <envs>
//                              <env name="MOD_CLASSES" value="main%%${project.projectDir.absolutePath}/build/resources/main;main%%${project.projectDir.absolutePath}/build/classes/java/main;main%%${project.projectDir.absolutePath}/build/classes/kotlin/main" />
//                              <env name="MCP_MAPPINGS" value="loom.stub" />
//                              <env name="MCP_VERSION" value="20210115.111550" />
//                              <env name="FORGE_VERSION" value="$forgeVersion" />
//                              <env name="assetIndex" value="1.16.5-1.16" />
//                              <env name="assetDirectory" value="${gradle.gradleUserHomeDir}/caches/fabric-loom/assets" />
//                              <env name="nativesDirectory" value="${rootProject.projectDir.absolutePath}/.gradle/loom-cache/natives/1.16.5" />
//                              <env name="FORGE_GROUP" value="net.minecraftforge" />
//                              <env name="target" value="fmluserdevclient" />
//                              <env name="MC_VERSION" value="$minecraftVersion" />
//                            </envs>
//                            <option name="MAIN_CLASS_NAME" value="dev.architectury.transformer.TransformerRuntime" />
//                            <module name="${rootProject.name}.architectury-${minecraftVersion}.${project.name}.main" />
//                            <option name="PROGRAM_PARAMETERS" value="--width 1280 --height 720 --username TEST" />
//                            <option name="VM_PARAMETERS" value="$vmOptions" />
//                            <option name="WORKING_DIRECTORY" value="${rootProject.projectDir.absolutePath}/architectury-${minecraftVersion}/run" />
//                            <method v="2">
//                              <option name="Make" enabled="true" />
//                            </method>
//                          </configuration>
//                        </component>
//                    """.trimIndent()
//                )
//            }
//            file("${rootProject.projectDir.absolutePath}/architectury-${minecraftVersion}/run").mkdir()
//        }
//    }
}