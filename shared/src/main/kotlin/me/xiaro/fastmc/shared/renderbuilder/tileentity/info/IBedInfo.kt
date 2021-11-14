package me.xiaro.fastmc.shared.renderbuilder.tileentity.info

interface IBedInfo<E> : IHDirectionalTileEntityInfo<E> {
    val color: Int
    val isHead: Boolean
}