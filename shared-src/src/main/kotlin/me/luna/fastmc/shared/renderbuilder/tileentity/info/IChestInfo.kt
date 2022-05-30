package me.luna.fastmc.shared.renderbuilder.tileentity.info

interface IChestInfo<E : Any> : ITileEntityInfo<E>, IHDirectionalTileEntityInfo<E> {
    val isTrap: Boolean
    val prevLidAngle: Float
    val lidAngle: Float
}