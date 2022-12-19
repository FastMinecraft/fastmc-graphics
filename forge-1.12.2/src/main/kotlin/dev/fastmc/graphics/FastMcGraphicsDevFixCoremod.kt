package dev.fastmc.graphics

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.Mixins

@IFMLLoadingPlugin.Name("FastMcGrahpics")
@MCVersion("1.12.2")
class FastMcGraphicsDevFixCoremod : IFMLLoadingPlugin {
    private val enableMod = true

    init {
        MixinBootstrap.init()
        if (enableMod) {
            Mixins.addConfigurations(
                "mixins.fastmc-graphics-core.json",
                "mixins.fastmc-graphics-accessor.json",
                "mixins.fastmc-graphics-patch.json",
                "mixins.fastmc-graphics-devfix.json"
            )
        } else {
            Mixins.addConfigurations(
                "mixins.fastmc-graphics-devfix.json"
            )
        }
    }

    override fun injectData(data: Map<String, Any>) {

    }

    override fun getASMTransformerClass(): Array<String> {
        return emptyArray()
    }

    override fun getModContainerClass(): String? {
        return null
    }

    override fun getSetupClass(): String? {
        return null
    }

    override fun getAccessTransformerClass(): String? {
        return null
    }
}