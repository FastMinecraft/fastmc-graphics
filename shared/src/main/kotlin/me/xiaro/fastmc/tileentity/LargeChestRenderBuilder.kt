package me.xiaro.fastmc.tileentity

import me.xiaro.fastmc.resource.IResourceManager
import me.xiaro.fastmc.tileentity.info.IChestInfo
import java.nio.ByteBuffer

class LargeChestRenderBuilder(
    resourceManager: IResourceManager,
    builtPosX: Double,
    builtPosY: Double,
    builtPosZ: Double,
    size: Int
) : SmallChestRenderBuilder(resourceManager, builtPosX, builtPosY, builtPosZ, size) {
    override fun add(info: IChestInfo<*>) {
        var posX = (info.posX + 0.5 - builtPosX).toFloat()
        val posY = (info.posY - builtPosY).toFloat()
        var posZ = (info.posZ + 0.5 - builtPosZ).toFloat()

        if (info.direction > 3) posZ += 0.5f else posX += 0.5f

        buffer.putFloat(posX)
        buffer.putFloat(posY)
        buffer.putFloat(posZ)

        putLightMapUV(info.lightMapUV)

        when (info.direction) {
            2 -> {
                buffer.put(2)
            }
            4 -> {
                buffer.put(1)
            }
            5 -> {
                buffer.put(-1)
            }
            else -> {
                buffer.put(0)
            }
        }

        when {
            isChristmas -> {
                buffer.put(2)
            }
            info.isTrap -> {
                buffer.put(1)
            }
            else -> {
                buffer.put(0)
            }
        }

        buffer.putShort((info.prevLidAngle * 65535.0f).toInt().toShort())
        buffer.putShort((info.lidAngle * 65535.0f).toInt().toShort())
    }

    override fun uploadBuffer(buffer: ByteBuffer): TileEntityRenderBuilder.Renderer {
        return upload(buffer, model.get(resourceManager), texture)
    }

    private companion object {
        val model = model("LargeChest")
        val texture = texture("LargeChest")
    }
}