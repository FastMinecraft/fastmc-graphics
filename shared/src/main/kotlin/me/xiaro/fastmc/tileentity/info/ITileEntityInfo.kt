package me.xiaro.fastmc.tileentity.info

interface ITileEntityInfo<E> {
    var tileEntity: E

    val posX: Int
    val posY: Int
    val posZ: Int
    val lightMapUV: Int
}