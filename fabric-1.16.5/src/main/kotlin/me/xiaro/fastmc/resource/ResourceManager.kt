package me.xiaro.fastmc.resource

import me.xiaro.fastmc.shared.model.Model
import me.xiaro.fastmc.shared.model.Model.Companion.init
import me.xiaro.fastmc.shared.model.tileentity.ModelBed
import me.xiaro.fastmc.shared.model.tileentity.ModelChest
import me.xiaro.fastmc.shared.model.tileentity.ModelLargeChest
import me.xiaro.fastmc.shared.model.tileentity.ModelShulkerBox
import me.xiaro.fastmc.shared.opengl.Shader
import me.xiaro.fastmc.shared.renderbuilder.AbstractRenderBuilder
import me.xiaro.fastmc.shared.resource.IResourceManager
import me.xiaro.fastmc.shared.resource.ResourceProvider
import me.xiaro.fastmc.shared.texture.DefaultTexture
import me.xiaro.fastmc.shared.texture.ITexture
import net.minecraft.client.MinecraftClient

class ResourceManager(mc: MinecraftClient) : IResourceManager {
    override val model: ResourceProvider<Model> = ResourceProvider(
        ModelBed().init(),
        ModelChest().init(),
        ModelLargeChest().init(),
        ModelShulkerBox().init(),
    )

    override val entityShader: ResourceProvider<AbstractRenderBuilder.Shader> = ResourceProvider(
        AbstractRenderBuilder.Shader(
            "tileEntity/EnderChest",
            "/assets/shaders/tileentity/EnderChest.vsh",
            "/assets/shaders/tileentity/Default.fsh"
        ),
        AbstractRenderBuilder.Shader(
            "tileEntity/Bed",
            "/assets/shaders/tileentity/Bed.vsh",
            "/assets/shaders/tileentity/Default.fsh"
        ),
        AbstractRenderBuilder.Shader(
            "tileEntity/ShulkerBox",
            "/assets/shaders/tileentity/ShulkerBox.vsh",
            "/assets/shaders/tileentity/Default.fsh"
        ),
        AbstractRenderBuilder.Shader(
            "tileEntity/Chest",
            "/assets/shaders/tileentity/Chest.vsh",
            "/assets/shaders/tileentity/Default.fsh"
        )
    )

    override val shader: ResourceProvider<Shader> = ResourceProvider(

    )

    override val texture: ResourceProvider<ITexture> = ResourceProvider(
        DefaultTexture("tileEntity/EnderChest", transformSmallChestTexture(mc, "ender")),
        bedTexture(mc),
        shulkerTexture(mc),
        smallChestTexture(mc),
        largeChestTexture(mc)
    )
}