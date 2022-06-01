package me.luna.fastmc.tileentity

import net.minecraft.client.render.WorldRenderer
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

interface IPatchedParticle {
    val world: World
    val blockPos: BlockPos
    var brightness: Int

    fun updateBrightness() {
        this.brightness = WorldRenderer.getLightmapCoordinates(world, blockPos)
    }
}