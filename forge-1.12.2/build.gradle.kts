import dev.fastmc.modsetup.minecraftVersion

forgeProject {
    modPackage.set("dev.fastmc.graphics")
    accessTransformer = "fastmc-graphics-at.cfg"
    mixinConfig(
        "mixins.fastmc-graphics-accessor.json",
        "mixins.fastmc-graphics-core.json",
        "mixins.fastmc-graphics-patch.json"
    )
    coreModClass.set("dev.fastmc.graphics.FastMcGraphicsCoremod")
    devCoreModClass.set("dev.fastmc.graphics.FastMcGraphicsDevFixCoremod")
}

dependencies {
    modCore("dev.luna5ama:gl-wrapper-lwjgl-2:1.0-SNAPSHOT")
    modCore("dev.luna5ama:gl-wrapper-${project.minecraftVersion}:1.0-SNAPSHOT")
}