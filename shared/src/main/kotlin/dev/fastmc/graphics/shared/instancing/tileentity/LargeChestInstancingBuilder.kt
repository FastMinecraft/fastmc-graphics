package dev.fastmc.graphics.shared.instancing.tileentity

import dev.fastmc.common.isOdd
import dev.fastmc.graphics.shared.instancing.tileentity.info.IChestInfo
import dev.fastmc.graphics.shared.model.Model
import dev.fastmc.graphics.shared.resource.ResourceEntry
import dev.fastmc.graphics.shared.texture.ITexture
import dev.luna5ama.kmogus.Ptr

class LargeChestInstancingBuilder : SmallChestInstancingBuilder() {
    override fun add(ptr: Ptr, info: IChestInfo<*>) {
        var posX = (info.posX + 0.5 - builtPosX).toFloat()
        val posY = (info.posY - builtPosY).toFloat()
        var posZ = (info.posZ + 0.5 - builtPosZ).toFloat()

        if (info.hDirection.isOdd) posZ += 0.5f else posX += 0.5f

        ptr.setFloatInc(posX)
            .setFloatInc(posY)
            .setFloatInc(posZ)

            .putLightMapUV(info)
            .putHDirection(info.hDirection)

            .run {
                when {
                    isChristmas -> {
                        setByteInc(2)
                    }
                    info.isTrap -> {
                        setByteInc(1)
                    }
                    else -> {
                        setByteInc(0)
                    }
                }
            }

            .setShortInc((info.prevLidAngle * 65535.0f).toInt().toShort())
            .setShortInc((info.lidAngle * 65535.0f).toInt().toShort())
    }

    override val model: ResourceEntry<Model> get() = Companion.model
    override val shader: ResourceEntry<InstancingShaderProgram> get() = SmallChestInstancingBuilder.shader
    override val texture: ResourceEntry<ITexture> get() = Companion.texture

    private companion object {
        val model = model("LargeChest")
        val texture = texture("LargeChest")
    }
}