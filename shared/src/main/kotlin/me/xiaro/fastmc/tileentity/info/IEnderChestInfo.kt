package me.xiaro.fastmc.tileentity.info

interface IEnderChestInfo<E> : IHDirectionalTileEntityInfo<E> {
    val prevLidAngle: Float
    val lidAngle: Float
}