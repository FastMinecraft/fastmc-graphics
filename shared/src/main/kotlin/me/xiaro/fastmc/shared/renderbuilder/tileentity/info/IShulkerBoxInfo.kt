package me.xiaro.fastmc.shared.renderbuilder.tileentity.info

interface IShulkerBoxInfo<E : Any> : IDirectionalTileEntityInfo<E> {
    val color: Int
    val prevProgress: Float
    val progress: Float
}