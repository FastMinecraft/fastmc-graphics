architecturyProject {
    mixinConfig(
        "mixins.fastmc-graphics-core.json",
        "mixins.fastmc-graphics-accessor.json",
        "mixins.fastmc-graphics-patch.json"
    )
    accessWidenerPath.set(file("common/src/main/resources/fastmc-graphics.accesswidener").absoluteFile)
    forge {
        atPatch {
            patch("net/minecraft/util/math/Matrix3f", "net/minecraft/util/math/vector/Matrix3f")
            patch("net/minecraft/util/math/Matrix4f", "net/minecraft/util/math/vector/Matrix4f")
        }
    }
}