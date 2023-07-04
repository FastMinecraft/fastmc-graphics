import dev.fastmc.modsetup.minecraftVersion

architecturyProject {
    modPackage.set("dev.fastmc.graphics")
    mixinConfig(
        "mixins.fastmc-graphics-core.json",
        "mixins.fastmc-graphics-accessor.json",
        "mixins.fastmc-graphics-patch.json"
    )
    accessWidenerPath.set(file("common/src/main/resources/fastmc-graphics.accesswidener").absoluteFile)

    commonProject {
        dependencies {
            modCore("dev.luna5ama:gl-wrapper-lwjgl-3:1.0-SNAPSHOT") {
                isTransitive = false
            }
            modCore("dev.luna5ama:gl-wrapper-${project.minecraftVersion}:1.0-SNAPSHOT") {
                isTransitive = false
            }
        }
    }
}