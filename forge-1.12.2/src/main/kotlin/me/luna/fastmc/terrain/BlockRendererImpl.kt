package me.luna.fastmc.terrain

import me.luna.fastmc.mixin.accessor.AccessorMinecraft
import me.luna.fastmc.shared.mixin.IPatchedBakedQuad
import me.luna.fastmc.shared.terrain.BlockRenderer
import net.minecraft.block.Block
import net.minecraft.block.BlockLiquid
import net.minecraft.block.material.Material
import net.minecraft.block.state.BlockFaceShape
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import kotlin.math.max

@Suppress("DuplicatedCode")
class BlockRendererImpl(override val context: RebuildContextImpl) : BlockRenderer<IBlockState, IBlockState>() {
    override val worldSnapshot = context.worldSnapshot

    override fun renderBlock(state: IBlockState) {
        context.setupRenderPos()

        val offsetType = state.block.offsetType
        if (offsetType != Block.EnumOffsetType.NONE) {
            val hashCode = MathHelper.getCoordinateRandom(context.blockX, 0, context.blockZ)
            context.renderPosX += (hashCode and 15L) / 30.0f - 0.25f
            context.renderPosZ += (hashCode shr 8 and 15L) / 30.0f - 0.25f
            if (offsetType == Block.EnumOffsetType.XYZ) context.renderPosY += (hashCode shr 4 and 15L) / 75.0f - 0.2f
        }

        val seed = MathHelper.getPositionRandom(context.renderBlockPos)
        val model = blockModels.getModelForState(state)

        if (Minecraft.isAmbientOcclusionEnabled() && state.getLightValue(
                worldSnapshot,
                context.renderBlockPos
            ) == 0 && model.isAmbientOcclusion(state)
        ) {
            context.random.setSeed(seed)
            val random = context.random.nextLong()
            val listD = model.getQuads(state, EnumFacing.DOWN, random)
            val listU = model.getQuads(state, EnumFacing.UP, random)
            val listN = model.getQuads(state, EnumFacing.NORTH, random)
            val listS = model.getQuads(state, EnumFacing.SOUTH, random)
            val listW = model.getQuads(state, EnumFacing.WEST, random)
            val listE = model.getQuads(state, EnumFacing.EAST, random)
            val list = model.getQuads(state, null, random)

            if (list.isNotEmpty()) {
                renderQuadsSmooth(state, list)
            }
            if (listE.isNotEmpty() && state.shouldSideBeRendered(
                    worldSnapshot,
                    context.renderBlockPos,
                    EnumFacing.EAST
                )
            ) {
                renderQuadsSmooth(state, listE)
            }
            if (listW.isNotEmpty() && state.shouldSideBeRendered(
                    worldSnapshot,
                    context.renderBlockPos,
                    EnumFacing.WEST
                )
            ) {
                renderQuadsSmooth(state, listW)
            }
            if (listS.isNotEmpty() && state.shouldSideBeRendered(
                    worldSnapshot,
                    context.renderBlockPos,
                    EnumFacing.SOUTH
                )
            ) {
                renderQuadsSmooth(state, listS)
            }
            if (listN.isNotEmpty() && state.shouldSideBeRendered(
                    worldSnapshot,
                    context.renderBlockPos,
                    EnumFacing.NORTH
                )
            ) {
                renderQuadsSmooth(state, listN)
            }
            if (listU.isNotEmpty() && state.shouldSideBeRendered(
                    worldSnapshot,
                    context.renderBlockPos,
                    EnumFacing.UP
                )
            ) {
                renderQuadsSmooth(state, listU)
            }
            if (listD.isNotEmpty() && state.shouldSideBeRendered(
                    worldSnapshot,
                    context.renderBlockPos,
                    EnumFacing.DOWN
                )
            ) {
                renderQuadsSmooth(state, listD)
            }
        } else {
            context.random.setSeed(seed)
            val random = context.random.nextLong()
            val listD = model.getQuads(state, EnumFacing.DOWN, random)
            val listU = model.getQuads(state, EnumFacing.UP, random)
            val listN = model.getQuads(state, EnumFacing.NORTH, random)
            val listS = model.getQuads(state, EnumFacing.SOUTH, random)
            val listW = model.getQuads(state, EnumFacing.WEST, random)
            val listE = model.getQuads(state, EnumFacing.EAST, random)
            val list = model.getQuads(state, null, random)

            if (list.isNotEmpty()) {
                for (i in list.indices) {
                    val bakedQuad = list[i]
                    val face = bakedQuad.face
                    getQuadDimensions(face.ordinal, state, bakedQuad.vertexData, 7)

                    val lightX: Int
                    val lightY: Int
                    val lightZ: Int

                    if (context.flags[0]) {
                        lightX = context.blockX + face.xOffset
                        lightY = context.blockY + face.yOffset
                        lightZ = context.blockZ + face.zOffset
                    } else {
                        lightX = context.blockX
                        lightY = context.blockY
                        lightZ = context.blockZ
                    }

                    val light = worldSnapshot.getLightBits(lightX, lightY, lightZ) and 0xFFFF
                    renderQuadFlat(state, bakedQuad, light)
                }
            }
            if (listE.isNotEmpty() && state.shouldSideBeRendered(
                    worldSnapshot,
                    context.renderBlockPos,
                    EnumFacing.EAST
                )
            ) {
                val light = worldSnapshot.getLightBits(context.blockX + 1, context.blockY, context.blockZ) and 0xFFFF
                for (i in listE.indices) {
                    renderQuadFlat(state, listE[i], light)
                }
            }
            if (listW.isNotEmpty() && state.shouldSideBeRendered(
                    worldSnapshot,
                    context.renderBlockPos,
                    EnumFacing.WEST
                )
            ) {
                val light = worldSnapshot.getLightBits(context.blockX - 1, context.blockY, context.blockZ) and 0xFFFF
                for (i in listW.indices) {
                    renderQuadFlat(state, listW[i], light)
                }
            }
            if (listS.isNotEmpty() && state.shouldSideBeRendered(
                    worldSnapshot,
                    context.renderBlockPos,
                    EnumFacing.SOUTH
                )
            ) {
                val light = worldSnapshot.getLightBits(context.blockX, context.blockY, context.blockZ + 1) and 0xFFFF
                for (i in listS.indices) {
                    renderQuadFlat(state, listS[i], light)
                }
            }
            if (listN.isNotEmpty() && state.shouldSideBeRendered(
                    worldSnapshot,
                    context.renderBlockPos,
                    EnumFacing.NORTH
                )
            ) {
                val light = worldSnapshot.getLightBits(context.blockX, context.blockY, context.blockZ - 1) and 0xFFFF
                for (i in listN.indices) {
                    renderQuadFlat(state, listN[i], light)
                }
            }
            if (listU.isNotEmpty() && state.shouldSideBeRendered(
                    worldSnapshot,
                    context.renderBlockPos,
                    EnumFacing.UP
                )
            ) {
                val light = worldSnapshot.getLightBits(context.blockX, context.blockY + 1, context.blockZ) and 0xFFFF
                for (i in listU.indices) {
                    renderQuadFlat(state, listU[i], light)
                }
            }
            if (listD.isNotEmpty() && state.shouldSideBeRendered(
                    worldSnapshot,
                    context.renderBlockPos,
                    EnumFacing.DOWN
                )
            ) {
                val light = worldSnapshot.getLightBits(context.blockX, context.blockY - 1, context.blockZ) and 0xFFFF
                for (i in listD.indices) {
                    renderQuadFlat(state, listD[i], light)
                }
            }
        }
    }

    private fun renderQuadsSmooth(
        state: IBlockState,
        quads: List<BakedQuad>
    ) {
        for (i in quads.indices) {
            renderQuadSmooth(
                state,
                quads[i].vertexData,
                7,
                quads[i].face.ordinal,
                (quads[i] as IPatchedBakedQuad).faceBit,
                quads[i].shouldApplyDiffuseLighting(),
                quads[i].hasTintIndex()
            )
        }
    }

    private fun renderQuadFlat(
        state: IBlockState,
        quad: BakedQuad,
        light: Int
    ) {
        renderQuadFlat(
            state,
            quad.vertexData,
            7,
            quad.face.ordinal,
            (quad as IPatchedBakedQuad).faceBit,
            quad.shouldApplyDiffuseLighting(),
            quad.hasTintIndex(),
            light
        )
    }

    override fun isFullCube(state: IBlockState): Boolean {
        return state.isFullCube
    }

    private val tempPos = BlockPos.MutableBlockPos()

    override fun renderFluid(state: IBlockState, blockState: IBlockState) {
        context.setupRenderPos()

        val renderD = state.shouldSideBeRendered(worldSnapshot, context.renderBlockPos, EnumFacing.DOWN)
        val renderU = state.shouldSideBeRendered(worldSnapshot, context.renderBlockPos, EnumFacing.UP)
        val renderN = state.shouldSideBeRendered(worldSnapshot, context.renderBlockPos, EnumFacing.NORTH)
        val renderS = state.shouldSideBeRendered(worldSnapshot, context.renderBlockPos, EnumFacing.SOUTH)
        val renderW = state.shouldSideBeRendered(worldSnapshot, context.renderBlockPos, EnumFacing.WEST)
        val renderE = state.shouldSideBeRendered(worldSnapshot, context.renderBlockPos, EnumFacing.EAST)

        if (!renderD && !renderU && !renderN && !renderS && !renderW && !renderE) return

        val isLava = state.material === Material.LAVA
        val sprites = if (isLava) lavaSprites else waterSprites

        val waterColor = worldSnapshot.getBlockColor(context.blockX, context.blockY, context.blockZ, state)
        val waterColorRed = waterColor shr 16 and 255
        val waterColorGreen = waterColor shr 8 and 255
        val waterColorBlue = waterColor and 255

        val material = state.material

        val yDown = if (renderD) 0.005f else 0.0f
        var fluidHeightNW = getFluidHeight(context.blockX, context.blockY, context.blockZ, material)
        var fluidHeightSW = getFluidHeight(context.blockX, context.blockY, context.blockZ + 1, material)
        var fluidHeightNE = getFluidHeight(context.blockX + 1, context.blockY, context.blockZ, material)
        var fluidHeightSE = getFluidHeight(context.blockX + 1, context.blockY, context.blockZ + 1, material)

        if (renderU) {
            fluidHeightNW -= 0.005f
            fluidHeightSW -= 0.005f
            fluidHeightNE -= 0.005f
            fluidHeightSE -= 0.005f

            val flowAngle = BlockLiquid.getSlopeAngle(worldSnapshot, context.renderBlockPos, material, state)

            val uNW: Float
            val uSW: Float
            val uSE: Float
            val uNE: Float

            val vNW: Float
            val vSW: Float
            val vSE: Float
            val vNE: Float

            if (flowAngle <= -999.0f) {
                val sprite = sprites[0]
                uNW = sprite.getInterpolatedU(0.0)
                uSW = uNW
                uSE = sprite.getInterpolatedU(16.0)
                uNE = uSE

                vNW = sprite.getInterpolatedV(0.0)
                vNE = vNW
                vSW = sprite.getInterpolatedV(16.0)
                vSE = vSW
            } else {
                val sprite = sprites[1]
                val flowX = MathHelper.sin(flowAngle) * 0.25f
                val flowZ = MathHelper.cos(flowAngle) * 0.25f

                uNW = sprite.getInterpolatedU((8.0f + (-flowZ - flowX) * 16.0f).toDouble())
                uSW = sprite.getInterpolatedU((8.0f + (-flowZ + flowX) * 16.0f).toDouble())
                uSE = sprite.getInterpolatedU((8.0f + (flowZ + flowX) * 16.0f).toDouble())
                uNE = sprite.getInterpolatedU((8.0f + (flowZ - flowX) * 16.0f).toDouble())

                vNW = sprite.getInterpolatedV((8.0f + (-flowZ + flowX) * 16.0f).toDouble())
                vSW = sprite.getInterpolatedV((8.0f + (flowZ + flowX) * 16.0f).toDouble())
                vSE = sprite.getInterpolatedV((8.0f + (flowZ - flowX) * 16.0f).toDouble())
                vNE = sprite.getInterpolatedV((8.0f + (-flowZ - flowX) * 16.0f).toDouble())
            }

            val brightnessUp = worldSnapshot.getWorldBrightness(1, true)
            val red = (waterColorRed * brightnessUp) shr 8
            val green = (waterColorGreen * brightnessUp) shr 8
            val blue = (waterColorBlue * brightnessUp) shr 8

            val lightMapUV = getLight(context.blockX, context.blockY, context.blockZ)

            context.activeVertexBuilder.putVertex(
                context.renderPosX + 0.0f,
                context.renderPosY + fluidHeightNW,
                context.renderPosZ + 0.0f,
                red,
                green,
                blue,
                uNW,
                vNW,
                lightMapUV,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + 0.0f,
                context.renderPosY + fluidHeightSW,
                context.renderPosZ + 1.0f,
                red,
                green,
                blue,
                uSW,
                vSW,
                lightMapUV,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + 1.0f,
                context.renderPosY + fluidHeightSE,
                context.renderPosZ + 1.0f,
                red,
                green,
                blue,
                uSE,
                vSE,
                lightMapUV,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + 1.0f,
                context.renderPosY + fluidHeightNE,
                context.renderPosZ + 0.0f,
                red,
                green,
                blue,
                uNE,
                vNE,
                lightMapUV,
                0b11_11_11
            )
            context.activeVertexBuilder.putQuad(0b11_11_11)

            if ((state.block as BlockLiquid).shouldRenderSides(worldSnapshot, tempPos.setPos(context.blockX, context.blockY + 1, context.blockZ))) {
                context.activeVertexBuilder.putVertex(
                    context.renderPosX + 0.0f,
                    context.renderPosY + fluidHeightNW,
                    context.renderPosZ + 0.0f,
                    red,
                    green,
                    blue,
                    uNW,
                    vNW,
                    lightMapUV,
                    0b11_11_11
                )
                context.activeVertexBuilder.putVertex(
                    context.renderPosX + 1.0f,
                    context.renderPosY + fluidHeightNE,
                    context.renderPosZ + 0.0f,
                    red,
                    green,
                    blue,
                    uNE,
                    vNE,
                    lightMapUV,
                    0b11_11_11
                )
                context.activeVertexBuilder.putVertex(
                    context.renderPosX + 1.0f,
                    context.renderPosY + fluidHeightSE,
                    context.renderPosZ + 1.0f,
                    red,
                    green,
                    blue,
                    uSE,
                    vSE,
                    lightMapUV,
                    0b11_11_11
                )
                context.activeVertexBuilder.putVertex(
                    context.renderPosX + 0.0f,
                    context.renderPosY + fluidHeightSW,
                    context.renderPosZ + 1.0f,
                    red,
                    green,
                    blue,
                    uSW,
                    vSW,
                    lightMapUV,
                    0b11_11_11
                )
                context.activeVertexBuilder.putQuad(0b11_11_11)
            }
        }

        if (renderD) {
            val u1 = sprites[0].minU
            val u2 = sprites[0].maxU
            val v1 = sprites[0].minV
            val v2 = sprites[0].maxV

            val brightnessDown = worldSnapshot.getWorldBrightness(0, true)
            val red = (waterColorRed * brightnessDown) shr 8
            val green = (waterColorGreen * brightnessDown) shr 8
            val blue = (waterColorBlue * brightnessDown) shr 8

            val lightMapUV = getLight(context.blockX, context.blockY - 1, context.blockZ)

            context.activeVertexBuilder.putVertex(
                context.renderPosX,
                context.renderPosY + yDown,
                context.renderPosZ + 1.0f,
                red,
                green,
                blue,
                u1,
                v2,
                lightMapUV,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX,
                context.renderPosY + yDown,
                context.renderPosZ,
                red,
                green,
                blue,
                u1,
                v1,
                lightMapUV,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + 1.0f,
                context.renderPosY + yDown,
                context.renderPosZ,
                red,
                green,
                blue,
                u2,
                v1,
                lightMapUV,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + 1.0f,
                context.renderPosY + yDown,
                context.renderPosZ + 1.0f,
                red,
                green,
                blue,
                u2,
                v2,
                lightMapUV,
                0b11_11_11
            )
            context.activeVertexBuilder.putQuad(0b11_11_11)
        }

        for (i in 0..3) {
            var direction: EnumFacing
            var renderSide: Boolean

            var x1: Float
            var z1: Float
            var x2: Float
            var z2: Float
            var y11: Float
            var y12: Float

            when (i) {
                0 -> {
                    y11 = fluidHeightNW
                    y12 = fluidHeightNE
                    x1 = 0.0f
                    x2 = 1.0f
                    z1 = 0.005f
                    z2 = 0.005f
                    direction = EnumFacing.NORTH
                    renderSide = renderN
                }
                1 -> {
                    y11 = fluidHeightSE
                    y12 = fluidHeightSW
                    x1 = 1.0f
                    x2 = 0.0f
                    z1 = 1.0f - 0.005f
                    z2 = 1.0f - 0.005f
                    direction = EnumFacing.SOUTH
                    renderSide = renderS
                }
                2 -> {
                    y11 = fluidHeightSW
                    y12 = fluidHeightNW
                    x1 = 0.005f
                    x2 = 0.005f
                    z1 = 1.0f
                    z2 = 0.0f
                    direction = EnumFacing.WEST
                    renderSide = renderW
                }
                else -> {
                    y11 = fluidHeightNE
                    y12 = fluidHeightSE
                    x1 = 1.0f - 0.005f
                    x2 = 1.0f - 0.005f
                    z1 = 0.0f
                    z2 = 1.0f
                    direction = EnumFacing.EAST
                    renderSide = renderE
                }
            }

            if (renderSide) {
                var sideSprites = sprites[1]
                if (!isLava) {
                    val x = context.blockX + direction.xOffset
                    val y = context.blockY + direction.yOffset
                    val z = context.blockZ + direction.zOffset

                    if (worldSnapshot.getBlockState(x, y, z)
                            .getBlockFaceShape(
                                worldSnapshot,
                                tempPos.setPos(x, y, z),
                                EnumFacing.VALUES[i + 2].opposite
                            )
                        == BlockFaceShape.SOLID
                    ) {
                        sideSprites = waterOverlaySprite
                    }
                }

                val uN = sideSprites.getInterpolatedU(0.0)
                val uS = sideSprites.getInterpolatedU(8.0)
                val vFrom1 = sideSprites.getInterpolatedV(((1.0f - y11) * 16.0f * 0.5f).toDouble())
                val vFrom2 = sideSprites.getInterpolatedV(((1.0f - y12) * 16.0f * 0.5f).toDouble())
                val vTo = sideSprites.getInterpolatedV(8.0)

                val brightness = worldSnapshot.getWorldBrightness(direction.ordinal, true)
                val red = (waterColorRed * brightness) shr 8
                val green = (waterColorGreen * brightness) shr 8
                val blue = (waterColorBlue * brightness) shr 8

                val lightMapUV = getLight(context.blockX, context.blockY, context.blockZ)

                context.activeVertexBuilder.putVertex(
                    context.renderPosX + x1,
                    context.renderPosY + y11,
                    context.renderPosZ + z1,
                    red,
                    green,
                    blue,
                    uN,
                    vFrom1,
                    lightMapUV,
                    0b11_11_11
                )
                context.activeVertexBuilder.putVertex(
                    context.renderPosX + x2,
                    context.renderPosY + y12,
                    context.renderPosZ + z2,
                    red,
                    green,
                    blue,
                    uS,
                    vFrom2,
                    lightMapUV,
                    0b11_11_11
                )
                context.activeVertexBuilder.putVertex(
                    context.renderPosX + x2,
                    context.renderPosY + yDown,
                    context.renderPosZ + z2,
                    red,
                    green,
                    blue,
                    uS,
                    vTo,
                    lightMapUV,
                    0b11_11_11
                )
                context.activeVertexBuilder.putVertex(
                    context.renderPosX + x1,
                    context.renderPosY + yDown,
                    context.renderPosZ + z1,
                    red,
                    green,
                    blue,
                    uN,
                    vTo,
                    lightMapUV,
                    0b11_11_11
                )
                context.activeVertexBuilder.putQuad(0b11_11_11)

                if (sideSprites !== waterOverlaySprite) {
                    context.activeVertexBuilder.putVertex(
                        context.renderPosX + x1,
                        context.renderPosY + yDown,
                        context.renderPosZ + z1,
                        red,
                        green,
                        blue,
                        uN,
                        vTo,
                        lightMapUV,
                        0b11_11_11
                    )
                    context.activeVertexBuilder.putVertex(
                        context.renderPosX + x2,
                        context.renderPosY + yDown,
                        context.renderPosZ + z2,
                        red,
                        green,
                        blue,
                        uS,
                        vTo,
                        lightMapUV,
                        0b11_11_11
                    )
                    context.activeVertexBuilder.putVertex(
                        context.renderPosX + x2,
                        context.renderPosY + y12,
                        context.renderPosZ + z2,
                        red,
                        green,
                        blue,
                        uS,
                        vFrom2,
                        lightMapUV,
                        0b11_11_11
                    )
                    context.activeVertexBuilder.putVertex(
                        context.renderPosX + x1,
                        context.renderPosY + y11,
                        context.renderPosZ + z1,
                        red,
                        green,
                        blue,
                        uN,
                        vFrom1,
                        lightMapUV,
                        0b11_11_11
                    )
                    context.activeVertexBuilder.putQuad(0b11_11_11)
                }
            }
        }
    }

    private fun getLight(x: Int, y: Int, z: Int): Int {
        val lightThis = worldSnapshot.getLightBits(x, y, z)
        val lightUp = worldSnapshot.getLightBits(x, y + 1, z)
        return max(lightThis shr 8 and 255, lightUp shr 8 and 255) shl 8 or
            max(lightThis and 255, lightUp and 255)
    }

    private fun getFluidHeight(x: Int, y: Int, z: Int, blockMaterial: Material): Float {
        var i = 0
        var f = 0.0f
        for (j in 0..3) {
            val x1 = x -(j and 1)
            val y1 = y
            val z1 = z - (j shr 1 and 1)
            if (worldSnapshot.getBlockState(x1, y1 + 1, z1).material === blockMaterial) {
                return 1.0f
            }
            val state = worldSnapshot.getBlockState(x1, y1, z1)
            val material = state.material
            if (material !== blockMaterial) {
                if (!material.isSolid) {
                    ++f
                    ++i
                }
            } else {
                val k = (state.getValue(BlockLiquid.LEVEL) as Int).toInt()
                if (k >= 8 || k == 0) {
                    f += BlockLiquid.getLiquidHeightPercent(k) * 10.0f
                    i += 10
                }
                f += BlockLiquid.getLiquidHeightPercent(k)
                ++i
            }
        }
        return 1.0f - f / i.toFloat()
    }

    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    private companion object {
        @JvmField
        val blockModels = (Minecraft.getMinecraft() as AccessorMinecraft).modelManager.blockModelShapes!!
        private val lavaSprites: Array<TextureAtlasSprite>
        private val waterSprites: Array<TextureAtlasSprite>
        private val waterOverlaySprite: TextureAtlasSprite

        init {
            val textureMap = Minecraft.getMinecraft().textureMapBlocks
            lavaSprites = arrayOf(
                textureMap.getAtlasSprite("minecraft:blocks/lava_still"),
                textureMap.getAtlasSprite("minecraft:blocks/lava_flow")
            )
            waterSprites = arrayOf(
                textureMap.getAtlasSprite("minecraft:blocks/water_still"),
                textureMap.getAtlasSprite("minecraft:blocks/water_flow")
            )
            waterOverlaySprite = textureMap.getAtlasSprite("minecraft:blocks/water_overlay")
        }
    }
}