package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.tileentity.info.IEnderChestInfo
import net.minecraft.tileentity.TileEntityEnderChest

class EnderChestInfo : TileEntityInfo<TileEntityEnderChest>(), IEnderChestInfo {
    override lateinit var tileEntity: TileEntityEnderChest

    override val direction: Int
        get() = if (tileEntity.hasWorld()) {
            tileEntity.blockMetadata
        } else {
            0
        }

    override val prevLidAngle: Float
        get() = tileEntity.prevLidAngle

    override val lidAngle: Float
        get() = tileEntity.lidAngle
}