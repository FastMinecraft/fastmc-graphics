package me.luna.fastmc.tileentity

import me.luna.fastmc.TileEntityEnderChest
import me.luna.fastmc.shared.renderbuilder.tileentity.info.IEnderChestInfo
import net.minecraft.block.EnderChestBlock

class EnderChestInfo : HDirectionalTileEntityInfo<TileEntityEnderChest>(EnderChestBlock.FACING),
    IEnderChestInfo<TileEntityEnderChest> {
    override val prevLidAngle: Float
        get() = entity.lastAnimationProgress

    override val lidAngle: Float
        get() = entity.animationProgress
}