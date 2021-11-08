package me.xiaro.fastmc.tileentity.info

interface IChestInfo<E> : ITileEntityInfo<E>, IHDirectionalTileEntityInfo<E> {
    val isTrap: Boolean
    val prevLidAngle: Float
    val lidAngle: Float
}