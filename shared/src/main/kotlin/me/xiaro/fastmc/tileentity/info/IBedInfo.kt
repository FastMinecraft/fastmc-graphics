package me.xiaro.fastmc.tileentity.info

interface IBedInfo<E> : IHDirectionalTileEntityInfo<E> {
    val color: Int
    val isHead: Boolean
}