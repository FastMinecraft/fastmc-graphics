package dev.fastmc.graphics.shared.instancing.tileentity.info

interface IDirectionalTileEntityInfo<E : Any> : ITileEntityInfo<E> {
    val direction: Int
}