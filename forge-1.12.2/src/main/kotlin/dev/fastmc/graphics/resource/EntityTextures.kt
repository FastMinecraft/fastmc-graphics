package dev.fastmc.graphics.resource

import dev.fastmc.graphics.shared.texture.CowTexture
import dev.fastmc.graphics.shared.texture.DefaultTexture
import dev.fastmc.graphics.shared.texture.ITexture
import net.minecraft.util.ResourceLocation

fun cowTexture(resourceManager: net.minecraft.client.resources.IResourceManager): ITexture {
    val image = CowTexture.v112(ResourceLocation("textures/entity/cow/cow.png").readImage(resourceManager))
    return DefaultTexture("entity/Cow", image)
}