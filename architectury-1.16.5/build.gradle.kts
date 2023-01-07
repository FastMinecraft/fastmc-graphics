architecturyProject {
    modPackage.set("dev.fastmc.graphics")
    mixinConfig(
        "mixins.fastmc-graphics-core.json",
        "mixins.fastmc-graphics-accessor.json",
        "mixins.fastmc-graphics-patch.json"
    )
    accessWidenerPath.set(file("common/src/main/resources/fastmc-graphics.accesswidener").absoluteFile)
}

modLoader {
    forgeModClass.set("dev.fastmc.graphics.FastMcForgeEntryPoint")
}