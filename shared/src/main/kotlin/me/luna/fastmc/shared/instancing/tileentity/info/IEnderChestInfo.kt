package me.luna.fastmc.shared.instancing.tileentity.info

interface IEnderChestInfo<E : Any> : IHDirectionalTileEntityInfo<E> {
    val prevLidAngle: Float
    val lidAngle: Float
}