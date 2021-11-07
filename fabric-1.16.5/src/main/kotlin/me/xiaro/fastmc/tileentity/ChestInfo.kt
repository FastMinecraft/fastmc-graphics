package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.TileEntityChest
import me.xiaro.fastmc.tileentity.info.IChestInfo
import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.block.entity.TrappedChestBlockEntity
import net.minecraft.block.enums.ChestType
import net.minecraft.util.math.Direction

class ChestInfo : DirectionalTileEntityInfo<TileEntityChest>(), IChestInfo<TileEntityChest> {
    override val isTrap: Boolean
        get() = tileEntity is TrappedChestBlockEntity

    override val prevLidAngle: Float
        get() = tileEntity.getAnimationProgress(0.0f)

    override val lidAngle: Float
        get() = tileEntity.getAnimationProgress(1.0f)
}