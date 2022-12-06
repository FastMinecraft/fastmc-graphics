import org.spongepowered.asm.gradle.plugins.MixinExtension

forgeProject {
    accessTransformer = "fastmc_at.cfg"
}

configure<MixinExtension> {
    add(sourceSets["main"], "mixins.fastmc.refmap.json")
}