import net.minecraftforge.gradle.userdev.UserDevExtension
import org.spongepowered.asm.gradle.plugins.MixinExtension

buildscript {
    repositories {
        maven("https://files.minecraftforge.net/maven")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }

    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:5.+")
        classpath("org.spongepowered:mixingradle:0.7-SNAPSHOT")
    }
}

apply {
    plugin("net.minecraftforge.gradle")
    plugin("org.spongepowered.mixin")
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

val library by configurations

val minecraftVersion: String by project
val forgeVersion: String by project
val mappingsChannel: String by project
val mappingsVersion: String by project

dependencies {
    // Jar packaging
    fun ModuleDependency.exclude(moduleName: String): ModuleDependency {
        return exclude(module = moduleName)
    }

    // Forge
    val minecraft = "minecraft"
    minecraft("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")

    // Dependencies
    library(project(":shared")) {
        exclude(group = "org.apache.logging.log4j")
    }

    library("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        exclude("commons-io")
        exclude("gson")
        exclude("guava")
        exclude("launchwrapper")
        exclude(group = "org.apache.logging.log4j")
    }

    annotationProcessor("org.spongepowered:mixin:0.8.4:processor") {
        exclude("gson")
    }
}

configure<MixinExtension> {
    add(sourceSets["main"], "mixins.fastmc.refmap.json")
}

configure<UserDevExtension> {
    mappings(mappingsChannel, mappingsVersion)

    accessTransformer(file("src/main/resources/fastmc_at.cfg"))

    runs {
        create("client") {
            workingDirectory = project.file("run").path
            ideaModule("${rootProject.name}.${project.name}.main")

            properties(
                mapOf(
                    "forge.logging.markers" to "SCAN,REGISTRIES,REGISTRYDUMP",
                    "forge.logging.console.level" to "info",
                    "fml.coreMods.load" to "me.luna.fastmc.FastMcCoremod",
                    "mixin.env.disableRefMap" to "true"
                )
            )
        }
    }
}

tasks {
    jar {
        manifest {
            attributes(
                "Manifest-Version" to 1.0,
                "MixinConfigs" to "mixins.fastmc-core.json, mixins.fastmc-accessor.json, mixins.fastmc-patch.json",
                "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
                "FMLCorePluginContainsFMLMod" to true,
                "FMLCorePlugin" to "me.luna.fastmc.FastMcCoremod"
            )
        }

        from(
            library.map {
                if (it.isDirectory) it else zipTree(it)
            }
        )

        archiveBaseName.set(rootProject.name)
        archiveAppendix.set(project.name)
        archiveClassifier.set("release")
    }

    register<Task>("genRuns") {
        group = "ide"
        doLast {
            File(rootDir, ".idea/runConfigurations/${project.name}_runClient.xml").writer().use {
                it.write(
                    """
                        <component name="ProjectRunConfigurationManager">
                          <configuration default="false" name="${project.name} runClient" type="Application" factoryName="Application">
                            <envs>
                              <env name="MCP_TO_SRG" value="${'$'}PROJECT_DIR$/../${rootProject.name}/${project.name}/build/createSrgToMcp/output.srg" />
                              <env name="MOD_CLASSES" value="${'$'}PROJECT_DIR$/../${rootProject.name}/${project.name}/build/resources/main;${'$'}PROJECT_DIR$/../${rootProject.name}/${project.name}/build/classes/java/main;${'$'}PROJECT_DIR$/../${rootProject.name}/${project.name}/build/classes/kotlin/main" />
                              <env name="mainClass" value="net.minecraft.launchwrapper.Launch" />
                              <env name="MCP_MAPPINGS" value="${mappingsChannel}_$mappingsVersion" />
                              <env name="FORGE_VERSION" value="$forgeVersion" />
                              <env name="assetIndex" value="${minecraftVersion.substringBeforeLast('.')}" />
                              <env name="assetDirectory" value="${
                        gradle.gradleUserHomeDir.path.replace(
                            '\\',
                            '/'
                        )
                    }/caches/forge_gradle/assets" />
                              <env name="nativesDirectory" value="${'$'}PROJECT_DIR$/../${rootProject.name}/${project.name}/build/natives" />
                              <env name="FORGE_GROUP" value="net.minecraftforge" />
                              <env name="tweakClass" value="net.minecraftforge.fml.common.launcher.FMLTweaker" />
                              <env name="MC_VERSION" value="${'$'}{MC_VERSION}" />
                              <env name="VERSION" value="${project.name}" />
                            </envs>
                            <option name="MAIN_CLASS_NAME" value="net.minecraftforge.legacydev.MainClient" />
                            <module name="${rootProject.name}.${project.name}.main" />
                            <option name="PROGRAM_PARAMETERS" value="--width 1280 --height 720" />
                            <option name="VM_PARAMETERS" value="-Xms2G -Xmx2G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:+UnlockExperimentalVMOptions -XX:+AlwaysPreTouch -XX:+UseLargePages -XX:+UseStringDeduplication -XX:MaxGCPauseMillis=50 -XX:G1NewSizePercent=1 -XX:G1MaxNewSizePercent=25 -XX:G1HeapRegionSize=1M -XX:G1ReservePercent=20 -XX:G1HeapWastePercent=30 -XX:G1MixedGCCountTarget=8 -XX:InitiatingHeapOccupancyPercent=30 -XX:G1MixedGCLiveThresholdPercent=80 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:MaxTenuringThreshold=1 -XX:MinHeapFreeRatio=1 -XX:MaxHeapFreeRatio=25 -XX:ParallelGCThreads=${Runtime.getRuntime().availableProcessors()} -XX:ConcGCThreads=${Runtime.getRuntime().availableProcessors() / 4} -XX:FlightRecorderOptions=stackdepth=2048 -Dforge.logging.console.level=info -Dforge.logging.markers=SCAN,REGISTRIES,REGISTRYDUMP -Dmixin.env.disableRefMap=true -Dfml.coreMods.load=me.luna.fastmc.FastMcCoremod" />
                            <option name="WORKING_DIRECTORY" value="${'$'}PROJECT_DIR$/${project.name}/run" />
                            <method v="2">
                              <option name="Gradle.BeforeRunTask" enabled="true" tasks="${project.name}:prepareRunClient" externalProjectPath="${'$'}PROJECT_DIR$" />
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