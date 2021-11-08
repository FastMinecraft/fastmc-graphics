package me.xiaro.fastmc.shared.tileentity.info

interface IDirectionalTileEntityInfo<E> : ITileEntityInfo<E> {
    val direction: Int
}