package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.TileEntityChest
import me.xiaro.fastmc.shared.tileentity.info.IChestInfo
import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.TrappedChestBlockEntity

class ChestInfo : HDirectionalTileEntityInfo<TileEntityChest>(ChestBlock.FACING), IChestInfo<TileEntityChest> {
    override val isTrap: Boolean
        get() = tileEntity is TrappedChestBlockEntity

    override val prevLidAngle: Float
        get() = tileEntity.getAnimationProgress(0.0f)

    override val lidAngle: Float
        get() = tileEntity.getAnimationProgress(1.0f)
}