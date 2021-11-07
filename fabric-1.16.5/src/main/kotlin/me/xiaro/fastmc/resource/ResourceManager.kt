package me.xiaro.fastmc.resource

import me.xiaro.fastmc.model.Model
import me.xiaro.fastmc.model.tileentity.ModelBed
import me.xiaro.fastmc.model.tileentity.ModelChest
import me.xiaro.fastmc.model.tileentity.ModelLargeChest
import me.xiaro.fastmc.model.tileentity.ModelShulkerBox
import me.xiaro.fastmc.opengl.DefaultTexture
import me.xiaro.fastmc.opengl.ITexture
import me.xiaro.fastmc.tileentity.TileEntityRenderBuilder
import net.minecraft.client.MinecraftClient

class ResourceManager(mc: MinecraftClient) : IResourceManager {
    override val model: ResourceProvider<Model> = ResourceProvider(
        ModelBed().apply { init() },
        ModelChest().apply { init() },
        ModelLargeChest().apply { init() },
        ModelShulkerBox().apply { init() },
    )

    override val tileEntityShader: ResourceProvider<TileEntityRenderBuilder.Shader> = ResourceProvider(
        TileEntityRenderBuilder.Shader(
            "tileEntity/EnderChest",
            "/assets/shaders/tileentity/EnderChest.vsh",
            "/assets/shaders/tileentity/Default.fsh"
        ),
        TileEntityRenderBuilder.Shader(
            "tileEntity/Bed",
            "/assets/shaders/tileentity/Bed.vsh",
            "/assets/shaders/tileentity/Default.fsh"
        ),
        TileEntityRenderBuilder.Shader(
            "tileEntity/ShulkerBox",
            "/assets/shaders/tileentity/ShulkerBox.vsh",
            "/assets/shaders/tileentity/Default.fsh"
        ),
        TileEntityRenderBuilder.Shader(
            "tileEntity/Chest",
            "/assets/shaders/tileentity/Chest.vsh",
            "/assets/shaders/tileentity/Default.fsh"
        )
    )

    override val texture: ResourceProvider<ITexture> = ResourceProvider(
        DefaultTexture("tileEntity/EnderChest", transformSmallChestTexture(mc, "ender")),
        bedTexture(mc),
        shulkerTexture(mc),
        smallChestTexture(mc),
        largeChestTexture(mc)
    )
}