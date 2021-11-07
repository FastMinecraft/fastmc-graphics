package me.xiaro.fastmc.tileentity.info

interface IEnderChestInfo<E> : IDirectionalTileEntityInfo<E> {
    val prevLidAngle: Float
    val lidAngle: Float
}