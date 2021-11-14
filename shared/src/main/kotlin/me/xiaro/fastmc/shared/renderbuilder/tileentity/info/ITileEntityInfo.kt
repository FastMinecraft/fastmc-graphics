package me.xiaro.fastmc.shared.renderbuilder.tileentity.info

import me.xiaro.fastmc.shared.renderbuilder.IInfo

interface ITileEntityInfo<E> : IInfo<E> {
    val posX: Int
    val posY: Int
    val posZ: Int
    val lightMapUV: Int
}