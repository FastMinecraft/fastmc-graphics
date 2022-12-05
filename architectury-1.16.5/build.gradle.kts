architecturyProject {
    mixinConfig("mixins.fastmc-core.json", "mixins.fastmc-accessor.json", "mixins.fastmc-patch.json")
    accessWidenerPath.set(file("common/src/main/resources/FastMinecraft.accesswidener").absoluteFile)
    forge {
        atPatch {
            patch("net/minecraft/util/math/Matrix3f", "net/minecraft/util/math/vector/Matrix3f")
            patch("net/minecraft/util/math/Matrix4f", "net/minecraft/util/math/vector/Matrix4f")
        }
    }
}