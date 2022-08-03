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
import me.luna.fastmc.shared.texture.ITexture
import net.minecraft.util.ResourceLocation

class ResourceManager(resourceManager: net.minecraft.client.resources.IResourceManager) : IResourceManager {
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
            ShaderSource.Vertex("/assets/shaders/entity/Cow.vsh"),
            ShaderSource.Fragment("/assets/shaders/entity/Default.fsh")
        ),

        AbstractInstancingBuilder.InstancingShaderProgram(
            "tileEntity/EnderChest",
            ShaderSource.Vertex("/assets/shaders/tileentity/EnderChest.vsh"),
            ShaderSource.Fragment("/assets/shaders/tileentity/Default.fsh")
        ),
        AbstractInstancingBuilder.InstancingShaderProgram(
            "tileEntity/Bed",
            ShaderSource.Vertex("/assets/shaders/tileentity/Bed.vsh"),
            ShaderSource.Fragment("/assets/shaders/tileentity/Default.fsh")
        ),
        AbstractInstancingBuilder.InstancingShaderProgram(
            "tileEntity/ShulkerBox",
            ShaderSource.Vertex("/assets/shaders/tileentity/ShulkerBox.vsh"),
            ShaderSource.Fragment("/assets/shaders/tileentity/Default.fsh")
        ),
        AbstractInstancingBuilder.InstancingShaderProgram(
            "tileEntity/Chest",
            ShaderSource.Vertex("/assets/shaders/tileentity/Chest.vsh"),
            ShaderSource.Fragment("/assets/shaders/tileentity/Default.fsh")
        )
    )

    override val texture: ResourceProvider<ITexture> = ResourceProvider(
        cowTexture(resourceManager),

        ResourceLocationTexture("tileEntity/EnderChest", ResourceLocation("textures/entity/chest/ender.png")),
        bedTexture(resourceManager),
        shulkerTexture(resourceManager),
        smallChestTexture(resourceManager),
        largeChestTexture(resourceManager)
    )
}