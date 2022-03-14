package me.luna.fastmc.mixin

import net.minecraft.block.entity.BlockEntity

interface IPatchedWorldRenderer {
    val renderTileEntityList: List<BlockEntity>
}