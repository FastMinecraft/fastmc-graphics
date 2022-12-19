package dev.fastmc.graphics.shared.instancing.tileentity.info

interface IShulkerBoxInfo<E : Any> : IDirectionalTileEntityInfo<E> {
    val color: Int
    val prevProgress: Float
    val progress: Float
}