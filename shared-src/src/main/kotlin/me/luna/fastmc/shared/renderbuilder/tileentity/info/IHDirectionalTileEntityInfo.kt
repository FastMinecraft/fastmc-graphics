package me.luna.fastmc.shared.renderbuilder.tileentity.info

interface IHDirectionalTileEntityInfo<E : Any> : ITileEntityInfo<E> {
    val hDirection: Int
}