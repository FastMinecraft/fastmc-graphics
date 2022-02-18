package me.luna.fastmc.shared.renderbuilder.tileentity.info

interface IBedInfo<E : Any> : IHDirectionalTileEntityInfo<E> {
    val color: Int
    val isHead: Boolean
}