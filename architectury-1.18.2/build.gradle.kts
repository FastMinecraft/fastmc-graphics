architecturyProject {
    mixinConfig("mixins.fastmc-core.json", "mixins.fastmc-accessor.json", "mixins.fastmc-patch.json")
    accessWidenerPath.set(file("common/src/main/resources/FastMinecraft.accesswidener").absoluteFile)
}