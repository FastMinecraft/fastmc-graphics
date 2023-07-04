package dev.fastmc.graphics.terrain

import dev.fastmc.graphics.mixin.accessor.AccessorMinecraft
import dev.fastmc.graphics.shared.mixin.IPatchedBakedQuad
import dev.fastmc.graphics.shared.terrain.BlockRenderer
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

        if (Minecraft.isAmbientOcclusionEnabled()
            && state.getLightValue(worldSnapshot, context.renderBlockPos) == 0
            && model.isAmbientOcclusion(state)
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

        val color = worldSnapshot.getBlockColor(context.blockX, context.blockY, context.blockZ, state)
        val red = color shr 16 and 255
        val green = color shr 8 and 255
        val blue = color and 255

        val material = state.material

        var heightNW = getFluidHeight(context.blockX, context.blockY, context.blockZ, material)
        var heightSW = getFluidHeight(context.blockX, context.blockY, context.blockZ + 1, material)
        var heightNE = getFluidHeight(context.blockX + 1, context.blockY, context.blockZ, material)
        var heightSE = getFluidHeight(context.blockX + 1, context.blockY, context.blockZ + 1, material)

        if (renderU) {
            heightNW -= FLUID_SIDE_ESP
            heightSW -= FLUID_SIDE_ESP
            heightNE -= FLUID_SIDE_ESP
            heightSE -= FLUID_SIDE_ESP

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
            val rUp = (red * brightnessUp) shr 8
            val gUp = (green * brightnessUp) shr 8
            val bUp = (blue * brightnessUp) shr 8

            val lightUp = getLight(context.blockX, context.blockY, context.blockZ)

            context.activeVertexBuilder.putVertex(
                context.renderPosX + 0.0f,
                context.renderPosY + heightNW,
                context.renderPosZ + 0.0f,
                rUp,
                gUp,
                bUp,
                uNW,
                vNW,
                lightUp,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + 0.0f,
                context.renderPosY + heightSW,
                context.renderPosZ + 1.0f,
                rUp,
                gUp,
                bUp,
                uSW,
                vSW,
                lightUp,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + 1.0f,
                context.renderPosY + heightSE,
                context.renderPosZ + 1.0f,
                rUp,
                gUp,
                bUp,
                uSE,
                vSE,
                lightUp,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + 1.0f,
                context.renderPosY + heightNE,
                context.renderPosZ + 0.0f,
                rUp,
                gUp,
                bUp,
                uNE,
                vNE,
                lightUp,
                0b11_11_11
            )
            context.activeVertexBuilder.putQuad(0b11_11_11)

            if ((state.block as BlockLiquid).shouldRenderSides(
                    worldSnapshot,
                    tempPos.setPos(context.blockX, context.blockY + 1, context.blockZ)
                )
            ) {
                context.activeVertexBuilder.putVertex(
                    context.renderPosX + 0.0f,
                    context.renderPosY + heightNW,
                    context.renderPosZ + 0.0f,
                    rUp,
                    gUp,
                    bUp,
                    uNW,
                    vNW,
                    lightUp,
                    0b11_11_11
                )
                context.activeVertexBuilder.putVertex(
                    context.renderPosX + 1.0f,
                    context.renderPosY + heightNE,
                    context.renderPosZ + 0.0f,
                    rUp,
                    gUp,
                    bUp,
                    uNE,
                    vNE,
                    lightUp,
                    0b11_11_11
                )
                context.activeVertexBuilder.putVertex(
                    context.renderPosX + 1.0f,
                    context.renderPosY + heightSE,
                    context.renderPosZ + 1.0f,
                    rUp,
                    gUp,
                    bUp,
                    uSE,
                    vSE,
                    lightUp,
                    0b11_11_11
                )
                context.activeVertexBuilder.putVertex(
                    context.renderPosX + 0.0f,
                    context.renderPosY + heightSW,
                    context.renderPosZ + 1.0f,
                    rUp,
                    gUp,
                    bUp,
                    uSW,
                    vSW,
                    lightUp,
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
            val rDown = (red * brightnessDown) shr 8
            val gDown = (green * brightnessDown) shr 8
            val bDown = (blue * brightnessDown) shr 8

            val lightDown = getLight(context.blockX, context.blockY - 1, context.blockZ)

            context.activeVertexBuilder.putVertex(
                context.renderPosX + 0.0f,
                context.renderPosY,
                context.renderPosZ + 1.0f,
                rDown,
                gDown,
                bDown,
                u1,
                v2,
                lightDown,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + 0.0f,
                context.renderPosY,
                context.renderPosZ + 0.0f,
                rDown,
                gDown,
                bDown,
                u1,
                v1,
                lightDown,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + 1.0f,
                context.renderPosY,
                context.renderPosZ + 0.0f,
                rDown,
                gDown,
                bDown,
                u2,
                v1,
                lightDown,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + 1.0f,
                context.renderPosY,
                context.renderPosZ + 1.0f,
                rDown,
                gDown,
                bDown,
                u2,
                v2,
                lightDown,
                0b11_11_11
            )
            context.activeVertexBuilder.putQuad(0b11_11_11)
        }

        for (i in 0..3) {
            val renderSide = when (i) {
                0 -> {
                    renderN
                }
                1 -> {
                    renderS
                }
                2 -> {
                    renderW
                }
                else -> {
                    renderE
                }
            }

            if (!renderSide) continue

            var direction: EnumFacing

            var x1: Float
            var z1: Float
            var x2: Float
            var z2: Float
            var y11: Float
            var y12: Float

            when (i) {
                0 -> {
                    direction = EnumFacing.NORTH

                    y11 = heightNW
                    y12 = heightNE

                    x1 = 0.0f
                    x2 = 1.0f
                    z1 = FLUID_SIDE_ESP
                    z2 = FLUID_SIDE_ESP
                }
                1 -> {
                    direction = EnumFacing.SOUTH

                    y11 = heightSE
                    y12 = heightSW

                    x1 = 1.0f
                    x2 = 0.0f
                    z1 = 1.0f - FLUID_SIDE_ESP
                    z2 = 1.0f - FLUID_SIDE_ESP
                }
                2 -> {
                    direction = EnumFacing.WEST

                    y11 = heightSW
                    y12 = heightNW

                    x1 = FLUID_SIDE_ESP
                    x2 = FLUID_SIDE_ESP
                    z1 = 1.0f
                    z2 = 0.0f
                }
                else -> {
                    direction = EnumFacing.EAST

                    y11 = heightNE
                    y12 = heightSE

                    x1 = 1.0f - FLUID_SIDE_ESP
                    x2 = 1.0f - FLUID_SIDE_ESP
                    z1 = 0.0f
                    z2 = 1.0f
                }
            }

            var sideSprites = sprites[1]
            val sideX = context.blockX + direction.xOffset
            val sideY = context.blockY + direction.yOffset
            val sideZ = context.blockZ + direction.zOffset

            if (!isLava) {
                val offsetState = worldSnapshot.getBlockState(sideX, sideY, sideZ)
                if (offsetState.getBlockFaceShape(
                        worldSnapshot,
                        tempPos.setPos(sideX, sideY, sideZ),
                        EnumFacing.VALUES[i + 2].opposite
                    ) == BlockFaceShape.SOLID
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
            val rSide = (red * brightness) shr 8
            val gSide = (green * brightness) shr 8
            val bSide = (blue * brightness) shr 8

            val lightSide = getLight(sideX, sideY, sideZ)

            context.activeVertexBuilder.putVertex(
                context.renderPosX + x1,
                context.renderPosY + y11,
                context.renderPosZ + z1,
                rSide,
                gSide,
                bSide,
                uN,
                vFrom1,
                lightSide,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + x2,
                context.renderPosY + y12,
                context.renderPosZ + z2,
                rSide,
                gSide,
                bSide,
                uS,
                vFrom2,
                lightSide,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + x2,
                context.renderPosY,
                context.renderPosZ + z2,
                rSide,
                gSide,
                bSide,
                uS,
                vTo,
                lightSide,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + x1,
                context.renderPosY,
                context.renderPosZ + z1,
                rSide,
                gSide,
                bSide,
                uN,
                vTo,
                lightSide,
                0b11_11_11
            )
            context.activeVertexBuilder.putQuad(0b11_11_11)

            if (sideSprites === waterOverlaySprite) continue

            context.activeVertexBuilder.putVertex(
                context.renderPosX + x1,
                context.renderPosY,
                context.renderPosZ + z1,
                rSide,
                gSide,
                bSide,
                uN,
                vTo,
                lightSide,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + x2,
                context.renderPosY,
                context.renderPosZ + z2,
                rSide,
                gSide,
                bSide,
                uS,
                vTo,
                lightSide,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + x2,
                context.renderPosY + y12,
                context.renderPosZ + z2,
                rSide,
                gSide,
                bSide,
                uS,
                vFrom2,
                lightSide,
                0b11_11_11
            )
            context.activeVertexBuilder.putVertex(
                context.renderPosX + x1,
                context.renderPosY + y11,
                context.renderPosZ + z1,
                rSide,
                gSide,
                bSide,
                uN,
                vFrom1,
                lightSide,
                0b11_11_11
            )
            context.activeVertexBuilder.putQuad(0b11_11_11)
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
            val x1 = x - (j and 1)
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
                val k = state.getValue(BlockLiquid.LEVEL)
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