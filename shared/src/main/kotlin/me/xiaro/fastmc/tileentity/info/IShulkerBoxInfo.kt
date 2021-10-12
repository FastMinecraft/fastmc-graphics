package me.xiaro.fastmc.tileentity.info

interface IShulkerBoxInfo : ITileEntityInfo {
    val direction: Int
    val color: Int
    val prevProgress: Float
    val progress: Float
}