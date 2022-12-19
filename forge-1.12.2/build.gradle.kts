import org.spongepowered.asm.gradle.plugins.MixinExtension

forgeProject {
    accessTransformer = "fastmc_at.cfg"
    mixinConfig(
        "mixins.fastmc-graphics-accessor.json",
        "mixins.fastmc-graphics-core.json",
        "mixins.fastmc-graphics-patch.json"
    )
    coreModClass.set("me.luna.fastmc.FastMcGraphicsCoremod")
    devCoreModClass.set("me.luna.fastmc.FastMcGraphicsDevFixCoremod")
}

configure<MixinExtension> {
    add(sourceSets["main"], "mixins.fastmc-graphics.refmap.json")
}