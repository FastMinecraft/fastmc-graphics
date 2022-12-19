package dev.fastmc.graphics.shared.instancing.tileentity.info

interface IHDirectionalTileEntityInfo<E : Any> : ITileEntityInfo<E> {
    val hDirection: Int
}