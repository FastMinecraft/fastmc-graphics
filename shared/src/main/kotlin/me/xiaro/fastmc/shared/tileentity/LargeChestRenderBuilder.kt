package me.xiaro.fastmc.shared.tileentity

import me.xiaro.fastmc.shared.tileentity.info.IChestInfo
import me.xiaro.fastmc.shared.util.isOdd
import java.nio.ByteBuffer

class LargeChestRenderBuilder : SmallChestRenderBuilder() {
    override fun add(info: IChestInfo<*>) {
        var posX = (info.posX + 0.5 - builtPosX).toFloat()
        val posY = (info.posY - builtPosY).toFloat()
        var posZ = (info.posZ + 0.5 - builtPosZ).toFloat()

        if (info.hDirection.isOdd) posZ += 0.5f else posX += 0.5f

        buffer.putFloat(posX)
        buffer.putFloat(posY)
        buffer.putFloat(posZ)

        putLightMapUV(info)
        putHDirection(info.hDirection)

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