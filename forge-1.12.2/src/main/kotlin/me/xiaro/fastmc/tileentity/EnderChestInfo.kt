package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.shared.renderbuilder.tileentity.info.IEnderChestInfo
import net.minecraft.block.BlockEnderChest
import net.minecraft.tileentity.TileEntityEnderChest

class EnderChestInfo : HDirectionalTileEntityInfo<TileEntityEnderChest>(BlockEnderChest.FACING),
    IEnderChestInfo<TileEntityEnderChest> {
    override val prevLidAngle: Float
        get() = entity.prevLidAngle

    override val lidAngle: Float
        get() = entity.lidAngle
}