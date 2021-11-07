package me.xiaro.fastmc.tileentity.info

interface IBedInfo<E> : IDirectionalTileEntityInfo<E> {
    val color: Int
    val isHead: Boolean
}