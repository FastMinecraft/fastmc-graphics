package me.luna.fastmc.shared.instancing.tileentity.info

interface IHDirectionalTileEntityInfo<E : Any> : ITileEntityInfo<E> {
    val hDirection: Int
}