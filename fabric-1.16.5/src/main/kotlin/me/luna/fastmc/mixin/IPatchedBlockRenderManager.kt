package me.luna.fastmc.mixin

import me.luna.fastmc.terrain.ChunkBuilderContext
import net.minecraft.block.BlockState
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockRenderView
import java.util.*

interface IPatchedBlockRenderManager {
    fun renderBlock0(
        context: ChunkBuilderContext,
        state: BlockState,
        pos: BlockPos,
        world: BlockRenderView,
        matrix: MatrixStack,
        vertexConsumer: VertexConsumer,
        cull: Boolean,
        random: Random
    ): Boolean
}