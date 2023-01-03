forgeProject {
    modPackage.set("dev.fastmc.graphics")
    accessTransformer = "fastmc_at.cfg"
    mixinConfig(
        "mixins.fastmc-graphics-accessor.json",
        "mixins.fastmc-graphics-core.json",
        "mixins.fastmc-graphics-patch.json"
    )
    coreModClass.set("dev.fastmc.graphics.FastMcGraphicsCoremod")
    devCoreModClass.set("dev.fastmc.graphics.FastMcGraphicsDevFixCoremod")
}