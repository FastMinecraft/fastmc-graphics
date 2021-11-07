package me.xiaro.fastmc.tileentity.info

interface IDirectionalTileEntityInfo<E> : ITileEntityInfo<E> {
    val direction: Int
}