package me.xiaro.fastmc.shared.renderbuilder.tileentity.info

interface IDirectionalTileEntityInfo<E> : ITileEntityInfo<E> {
    val direction: Int
}