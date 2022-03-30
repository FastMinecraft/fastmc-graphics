package me.luna.fastmc.mixin

import me.luna.fastmc.terrain.ChunkBuilderContext
import net.minecraft.block.BlockState
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.BlockRenderView
import java.util.*

interface IPatchedBlockModelRenderer {
    fun render0(
        context: ChunkBuilderContext,
        world: BlockRenderView,
        model: BakedModel,
        state: BlockState,
        pos: BlockPos,
        matrix: MatrixStack,
        vertexConsumer: VertexConsumer,
        cull: Boolean,
        random: Random,
        seed: Long,
        overlay: Int
    ): Boolean
}