package dev.fastmc.graphics.util

import dev.fastmc.common.distanceSq
import dev.fastmc.common.fastFloor
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

fun rayTrace(
    world: World,
    start: Vec3d,
    end: Vec3d,
    stopOnLiquid: Boolean,
    ignoreBlockWithoutBoundingBox: Boolean,
    returnLastUncollidableBlock: Boolean
): RayTraceResult? {
    var currentX = start.x
    var currentY = start.y
    var currentZ = start.z

    // Int start position
    var currentBlockX = currentX.fastFloor()
    var currentBlockY = currentY.fastFloor()
    var currentBlockZ = currentZ.fastFloor()

    // Raytrace start block
    val blockPos = BlockPos.MutableBlockPos(currentBlockX, currentBlockY, currentBlockZ)
    val startBlockState = world.getBlockState(blockPos)

    val endX = end.x
    val endY = end.y
    val endZ = end.z

    if ((!ignoreBlockWithoutBoundingBox
            || startBlockState.getCollisionBoundingBox(world, blockPos) != null)
        && startBlockState.block.canCollideCheck(startBlockState, stopOnLiquid)
    ) {
        startBlockState.collisionRayTrace(world, blockPos, Vec3d(currentX, currentY, currentZ), end).let { return it }
    }

    // Int end position
    val endBlockX = endX.fastFloor()
    val endBlockY = endY.fastFloor()
    val endBlockZ = endZ.fastFloor()

    var count = 200

    while (count-- >= 0) {
        if (currentBlockX == endBlockX && currentBlockY == endBlockY && currentBlockZ == endBlockZ) {
            break
        }

        var nextX = 999
        var nextY = 999
        var nextZ = 999

        var stepX = 999.0
        var stepY = 999.0
        var stepZ = 999.0
        val diffX = end.x - currentX
        val diffY = end.y - currentY
        val diffZ = end.z - currentZ

        if (endBlockX > currentBlockX) {
            nextX = currentBlockX + 1
            stepX = (nextX - currentX) / diffX
        } else if (endBlockX < currentBlockX) {
            nextX = currentBlockX
            stepX = (nextX - currentX) / diffX
        }

        if (endBlockY > currentBlockY) {
            nextY = currentBlockY + 1
            stepY = (nextY - currentY) / diffY
        } else if (endBlockY < currentBlockY) {
            nextY = currentBlockY
            stepY = (nextY - currentY) / diffY
        }

        if (endBlockZ > currentBlockZ) {
            nextZ = currentBlockZ + 1
            stepZ = (nextZ - currentZ) / diffZ
        } else if (endBlockZ < currentBlockZ) {
            nextZ = currentBlockZ
            stepZ = (nextZ - currentZ) / diffZ
        }

        if (stepX < stepY && stepX < stepZ) {
            currentX = nextX.toDouble()
            currentY += diffY * stepX
            currentZ += diffZ * stepX

            currentBlockX = nextX - (endBlockX - currentBlockX ushr 31)
            currentBlockY = currentY.fastFloor()
            currentBlockZ = currentZ.fastFloor()
        } else if (stepY < stepZ) {
            currentX += diffX * stepY
            currentY = nextY.toDouble()
            currentZ += diffZ * stepY

            currentBlockX = currentX.fastFloor()
            currentBlockY = nextY - (endBlockY - currentBlockY ushr 31)
            currentBlockZ = currentZ.fastFloor()
        } else {
            currentX += diffX * stepZ
            currentY += diffY * stepZ
            currentZ = nextZ.toDouble()

            currentBlockX = currentX.fastFloor()
            currentBlockY = currentY.fastFloor()
            currentBlockZ = nextZ - (endBlockZ - currentBlockZ ushr 31)
        }

        blockPos.setPos(currentBlockX, currentBlockY, currentBlockZ)
        val blockState = world.getBlockState(blockPos)

        if ((!ignoreBlockWithoutBoundingBox
                || blockState.getCollisionBoundingBox(world, blockPos) != null)
            && blockState.block.canCollideCheck(blockState, stopOnLiquid)
        ) {
            @Suppress("UNNECESSARY_SAFE_CALL")
            startBlockState.collisionRayTrace(world, blockPos, Vec3d(currentX, currentY, currentZ), end)
                ?.let { return it }
        }
    }

    return if (returnLastUncollidableBlock) {
        val enumFacing = if (currentX == currentX.fastFloor().toDouble()) {
            if (end.x > start.x) EnumFacing.WEST
            else EnumFacing.EAST
        } else if (currentY == currentY.fastFloor().toDouble()) {
            if (end.y > start.y) EnumFacing.DOWN
            else EnumFacing.UP
        } else {
            if (end.z > start.z) EnumFacing.NORTH
            else EnumFacing.SOUTH
        }

        RayTraceResult(
            RayTraceResult.Type.MISS,
            Vec3d(currentX, currentY, currentZ),
            enumFacing,
            blockPos.toImmutable()
        )
    } else {
        null
    }
}

private fun IBlockState.raytrace(
    world: World,
    blockPos: BlockPos.MutableBlockPos,
    x1: Double,
    y1: Double,
    z1: Double,
    x2: Double,
    y2: Double,
    z2: Double
): RayTraceResult? {
    val x1f = (x1 - blockPos.x).toFloat()
    val y1f = (y1 - blockPos.y).toFloat()
    val z1f = (z1 - blockPos.z).toFloat()

    val box = this.getBoundingBox(world, blockPos)

    val minX = box.minX.toFloat()
    val minY = box.minY.toFloat()
    val minZ = box.minZ.toFloat()
    val maxX = box.maxX.toFloat()
    val maxY = box.maxY.toFloat()
    val maxZ = box.maxZ.toFloat()

    val xDiff = (x2 - blockPos.x).toFloat() - x1f
    val yDiff = (y2 - blockPos.y).toFloat() - y1f
    val zDiff = (z2 - blockPos.z).toFloat() - z1f

    var hitVecX = Float.NaN
    var hitVecY = Float.NaN
    var hitVecZ = Float.NaN
    var side = EnumFacing.WEST
    var none = true

    if (xDiff * xDiff >= 1.0E-7f) {
        val factorMin = (minX - x1f) / xDiff
        if (factorMin in 0.0..1.0) {
            val newY = y1f + yDiff * factorMin
            val newZ = z1f + zDiff * factorMin

            if (newY in minY..maxY && newZ in minZ..maxZ) {
                val newX = x1f + xDiff * factorMin

                if (none || distanceSq(x1f, y1f, z1f, newX, newY, newZ) < distanceSq(
                        x1f,
                        y1f,
                        z1f,
                        hitVecX,
                        hitVecY,
                        hitVecZ
                    )
                ) {
                    hitVecX = newX
                    hitVecY = newY
                    hitVecZ = newZ
                    side = EnumFacing.WEST
                    none = false
                }
            }
        } else {
            val factorMax = (maxX - x1f) / xDiff
            if (factorMax in 0.0..1.0) {
                val newY = y1f + yDiff * factorMax
                val newZ = z1f + zDiff * factorMax

                if (newY in minY..maxY && newZ in minZ..maxZ) {
                    val newX = x1f + xDiff * factorMax

                    if (none || distanceSq(x1f, y1f, z1f, newX, newY, newZ) < distanceSq(
                            x1f,
                            y1f,
                            z1f,
                            hitVecX,
                            hitVecY,
                            hitVecZ
                        )
                    ) {
                        hitVecX = newX
                        hitVecY = newY
                        hitVecZ = newZ
                        side = EnumFacing.EAST
                        none = false
                    }
                }
            }
        }
    }

    if (yDiff * yDiff >= 1.0E-7f) {
        val factorMin = (minY - y1f) / yDiff
        if (factorMin in 0.0f..1.0f) {
            val newX = x1f + xDiff * factorMin
            val newZ = z1f + zDiff * factorMin

            if (newX in minX..maxX && newZ in minZ..maxZ) {
                val newY = y1f + yDiff * factorMin

                if (none || distanceSq(x1f, y1f, z1f, newX, newY, newZ) < distanceSq(
                        x1f,
                        y1f,
                        z1f,
                        hitVecX,
                        hitVecY,
                        hitVecZ
                    )
                ) {
                    hitVecX = newX
                    hitVecY = newY
                    hitVecZ = newZ
                    side = EnumFacing.DOWN
                    none = false
                }
            }
        } else {
            val factorMax = (maxY - y1f) / yDiff
            if (factorMax in 0.0f..1.0f) {
                val newX = x1f + xDiff * factorMax
                val newZ = z1f + zDiff * factorMax

                if (newX in minX..maxX && newZ in minZ..maxZ) {
                    val newY = y1f + yDiff * factorMax

                    if (none || distanceSq(x1f, y1f, z1f, newX, newY, newZ) < distanceSq(
                            x1f,
                            y1f,
                            z1f,
                            hitVecX,
                            hitVecY,
                            hitVecZ
                        )
                    ) {
                        hitVecX = newX
                        hitVecY = newY
                        hitVecZ = newZ
                        side = EnumFacing.UP
                        none = false
                    }
                }
            }
        }
    }

    if (zDiff * zDiff >= 1.0E-7) {
        val factorMin = (minZ - z1f) / zDiff
        if (factorMin in 0.0f..1.0f) {
            val newX = x1f + xDiff * factorMin
            val newY = y1f + yDiff * factorMin

            if (newX in minX..maxX && newY in minY..maxY) {
                val newZ = z1f + zDiff * factorMin

                if (none || distanceSq(x1f, y1f, z1f, newX, newY, newZ) < distanceSq(
                        x1f,
                        y1f,
                        z1f,
                        hitVecX,
                        hitVecY,
                        hitVecZ
                    )
                ) {
                    hitVecX = newX
                    hitVecY = newY
                    hitVecZ = newZ
                    side = EnumFacing.NORTH
                    none = false
                }
            }
        } else {
            val factorMax = (maxZ - z1f) / zDiff
            if (factorMax in 0.0f..1.0f) {
                val newX = x1f + xDiff * factorMax
                val newY = y1f + yDiff * factorMax

                if (newX in minX..maxX && newY in minY..maxY) {
                    val newZ = z1f + zDiff * factorMax

                    if (none || distanceSq(x1f, y1f, z1f, newX, newY, newZ) < distanceSq(
                            x1f,
                            y1f,
                            z1f,
                            hitVecX,
                            hitVecY,
                            hitVecZ
                        )
                    ) {
                        hitVecX = newX
                        hitVecY = newY
                        hitVecZ = newZ
                        side = EnumFacing.SOUTH
                        none = false
                    }
                }
            }
        }
    }

    return if (!none) {
        val hitVec =
            Vec3d(hitVecX.toDouble() + blockPos.x, hitVecY.toDouble() + blockPos.y, hitVecZ.toDouble() + blockPos.z)
        RayTraceResult(hitVec, side, blockPos.toImmutable())
    } else {
        null
    }
}

enum class RayTraceAction {
    SKIP, NULL, CALC
}