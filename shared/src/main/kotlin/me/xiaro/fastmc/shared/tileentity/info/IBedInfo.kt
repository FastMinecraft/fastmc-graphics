package me.xiaro.fastmc.shared.tileentity.info

interface IBedInfo<E> : IHDirectionalTileEntityInfo<E> {
    val color: Int
    val isHead: Boolean
}