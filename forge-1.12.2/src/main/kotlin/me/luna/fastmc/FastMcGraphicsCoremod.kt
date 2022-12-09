package me.luna.fastmc

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.Mixins

@IFMLLoadingPlugin.Name("FastMcGraphicsCoremod")
@MCVersion("1.12.2")
class FastMcGraphicsCoremod : IFMLLoadingPlugin {
    init {
        MixinBootstrap.init()
        Mixins.addConfigurations(
            "mixins.fastmc-core.json",
            "mixins.fastmc-accessor.json",
            "mixins.fastmc-patch.json",
            "mixins.fastmc-devfix.json"
        )
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
