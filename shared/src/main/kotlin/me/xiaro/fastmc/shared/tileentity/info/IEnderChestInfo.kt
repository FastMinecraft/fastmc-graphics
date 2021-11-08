package me.xiaro.fastmc.shared.tileentity.info

interface IEnderChestInfo<E> : IHDirectionalTileEntityInfo<E> {
    val prevLidAngle: Float
    val lidAngle: Float
}