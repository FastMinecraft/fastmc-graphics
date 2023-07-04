package dev.fastmc.graphics.resource

import dev.fastmc.graphics.shared.instancing.AbstractInstancingBuilder
import dev.fastmc.graphics.shared.model.Model
import dev.fastmc.graphics.shared.model.Model.Companion.init
import dev.fastmc.graphics.shared.model.entity.ModelCow
import dev.fastmc.graphics.shared.model.tileentity.ModelBed
import dev.fastmc.graphics.shared.model.tileentity.ModelChest
import dev.fastmc.graphics.shared.model.tileentity.ModelLargeChest
import dev.fastmc.graphics.shared.model.tileentity.ModelShulkerBox
import dev.fastmc.graphics.shared.resource.IResourceManager
import dev.fastmc.graphics.shared.resource.ResourceProvider
import dev.fastmc.graphics.shared.texture.ITexture
import dev.luna5ama.glwrapper.impl.ShaderSource
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
            ShaderSource.Vert("/assets/shaders/entity/Cow.vert"),
            ShaderSource.Frag("/assets/shaders/entity/Default.frag")
        ),

        AbstractInstancingBuilder.InstancingShaderProgram(
            "tileEntity/EnderChest",
            ShaderSource.Vert("/assets/shaders/tileentity/EnderChest.vert"),
            ShaderSource.Frag("/assets/shaders/tileentity/Default.frag")
        ),
        AbstractInstancingBuilder.InstancingShaderProgram(
            "tileEntity/Bed",
            ShaderSource.Vert("/assets/shaders/tileentity/Bed.vert"),
            ShaderSource.Frag("/assets/shaders/tileentity/Default.frag")
        ),
        AbstractInstancingBuilder.InstancingShaderProgram(
            "tileEntity/ShulkerBox",
            ShaderSource.Vert("/assets/shaders/tileentity/ShulkerBox.vert"),
            ShaderSource.Frag("/assets/shaders/tileentity/Default.frag")
        ),
        AbstractInstancingBuilder.InstancingShaderProgram(
            "tileEntity/Chest",
            ShaderSource.Vert("/assets/shaders/tileentity/Chest.vert"),
            ShaderSource.Frag("/assets/shaders/tileentity/Default.frag")
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