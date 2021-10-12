package me.xiaro.fastmc.tileentity.info

interface IEnderChestInfo : ITileEntityInfo {
    val direction: Int
    val prevLidAngle: Float
    val lidAngle: Float
}