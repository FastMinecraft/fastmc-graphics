package dev.fastmc.graphics.shared.instancing.tileentity.info

interface IBedInfo<E : Any> : IHDirectionalTileEntityInfo<E> {
    val color: Int
    val isHead: Boolean
}