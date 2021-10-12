package me.xiaro.fastmc.tileentity.info

interface IBedInfo : ITileEntityInfo {
    val direction: Int
    val color: Int
    val isHead: Boolean
}