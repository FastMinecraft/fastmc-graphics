package me.luna.fastmc.resource

import me.luna.fastmc.shared.model.Model
import me.luna.fastmc.shared.model.Model.Companion.init
import me.luna.fastmc.shared.model.entity.ModelCow
import me.luna.fastmc.shared.model.tileentity.ModelBed
import me.luna.fastmc.shared.model.tileentity.ModelChest
import me.luna.fastmc.shared.model.tileentity.ModelLargeChest
import me.luna.fastmc.shared.model.tileentity.ModelShulkerBox
import me.luna.fastmc.shared.opengl.ShaderSource
import me.luna.fastmc.shared.instancing.AbstractInstancingBuilder
import me.luna.fastmc.shared.resource.IResourceManager
import me.luna.fastmc.shared.resource.ResourceProvider
import me.luna.fastmc.shared.texture.DefaultTexture
import me.luna.fastmc.shared.texture.ITexture

class ResourceManager(resourceManager: net.minecraft.resource.ResourceManager) : IResourceManager {
    override val model: ResourceProvider<Model> = ResourceProvider(
        ModelCow().init(),

        ModelBed().init(),
        ModelChest().init(),
        ModelLargeChest().init(),
        ModelShulkerBox().init(),
    )

    override val entityShader: ResourceProvider<AbstractInstancingBuilder.InstancingShaderProgram> = ResourceProvider(
        AbstractInstancingBuilder.InstancingShaderProgram(
            "entity/Cow",
            ShaderSource.Vertex("/assets/shaders/entity/Cow.vert"),
            ShaderSource.Fragment("/assets/shaders/entity/Default.frag")
        ),

        AbstractInstancingBuilder.InstancingShaderProgram(
            "tileEntity/EnderChest",
            ShaderSource.Vertex("/assets/shaders/tileentity/EnderChest.vert"),
            ShaderSource.Fragment("/assets/shaders/tileentity/Default.frag")
        ),
        AbstractInstancingBuilder.InstancingShaderProgram(
            "tileEntity/Bed",
            ShaderSource.Vertex("/assets/shaders/tileentity/Bed.vert"),
            ShaderSource.Fragment("/assets/shaders/tileentity/Default.frag")
        ),
        AbstractInstancingBuilder.InstancingShaderProgram(
            "tileEntity/ShulkerBox",
            ShaderSource.Vertex("/assets/shaders/tileentity/ShulkerBox.vert"),
            ShaderSource.Fragment("/assets/shaders/tileentity/Default.frag")
        ),
        AbstractInstancingBuilder.InstancingShaderProgram(
            "tileEntity/Chest",
            ShaderSource.Vertex("/assets/shaders/tileentity/Chest.vert"),
            ShaderSource.Fragment("/assets/shaders/tileentity/Default.frag")
        )
    )

    override val texture: ResourceProvider<ITexture> = ResourceProvider(
        DefaultTexture("tileEntity/EnderChest", transformSmallChestTexture(resourceManager, "ender")),
        bedTexture(resourceManager),
        shulkerTexture(resourceManager),
        smallChestTexture(resourceManager),
        largeChestTexture(resourceManager)
    )
}