package me.xiaro.fastmc.shared.renderbuilder.tileentity.info

interface IEnderChestInfo<E> : IHDirectionalTileEntityInfo<E> {
    val prevLidAngle: Float
    val lidAngle: Float
}