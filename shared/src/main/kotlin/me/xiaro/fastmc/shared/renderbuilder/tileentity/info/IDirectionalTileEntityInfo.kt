package me.xiaro.fastmc.shared.renderbuilder.tileentity.info

interface IDirectionalTileEntityInfo<E : Any> : ITileEntityInfo<E> {
    val direction: Int
}