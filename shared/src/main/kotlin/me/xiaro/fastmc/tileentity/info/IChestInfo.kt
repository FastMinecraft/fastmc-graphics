package me.xiaro.fastmc.tileentity.info

interface IChestInfo : ITileEntityInfo {
    val direction: Int
    val hasAdjChestXNeg: Boolean
    val hasAdjChestZNeg: Boolean
    val hasAdjChestXPos: Boolean
    val hasAdjChestZPos: Boolean
    val isTrap: Boolean
    val prevLidAngle: Float
    val lidAngle: Float
}