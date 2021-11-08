package me.xiaro.fastmc.shared.tileentity.info

interface IShulkerBoxInfo<E> : IDirectionalTileEntityInfo<E> {
    val color: Int
    val prevProgress: Float
    val progress: Float
}