package me.luna.fastmc.resource

import me.luna.fastmc.shared.texture.CowTexture
import me.luna.fastmc.shared.texture.DefaultTexture
import me.luna.fastmc.shared.texture.ITexture
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation

fun cowTexture(mc: Minecraft): ITexture {
    val image = CowTexture.v112(ResourceLocation("textures/entity/cow/cow.png").readImage(mc))
    return DefaultTexture("entity/Cow", image)
}