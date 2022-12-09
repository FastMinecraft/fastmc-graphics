import org.spongepowered.asm.gradle.plugins.MixinExtension

forgeProject {
    accessTransformer = "fastmc_at.cfg"
    coreModClass.set("me.luna.fastmc.FastMcGraphicsCoremod")
    devCoreModClass.set("me.luna.fastmc.FastMcGraphicsDevFixCoremod")
}

configure<MixinExtension> {
    add(sourceSets["main"], "mixins.fastmc.refmap.json")
}