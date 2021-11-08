package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.TileEntityEnderChest
import me.xiaro.fastmc.tileentity.info.IEnderChestInfo
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.ChestBlock
import net.minecraft.block.EnderChestBlock
import net.minecraft.block.entity.EnderChestBlockEntity
import net.minecraft.util.math.Direction

class EnderChestInfo : HDirectionalTileEntityInfo<TileEntityEnderChest>(EnderChestBlock.FACING), IEnderChestInfo<TileEntityEnderChest> {
    override val prevLidAngle: Float
        get() = tileEntity.lastAnimationProgress

    override val lidAngle: Float
        get() = tileEntity.animationProgress
}