package dev.fastmc.graphics.terrain

import dev.fastmc.graphics.shared.mixin.IPatchedBakedQuad
import dev.fastmc.graphics.shared.terrain.BlockRenderer
import dev.fastmc.graphics.util.Minecraft
import net.minecraft.block.AbstractBlock.OffsetType
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.LeavesBlock
import net.minecraft.block.TransparentBlock
import net.minecraft.client.MinecraftClient
import net.minecraft.client.color.world.BiomeColors
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.client.render.model.ModelLoader
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.tag.FluidTags
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import kotlin.math.max
import kotlin.math.min

@Suppress("DuplicatedCode")
class BlockRendererImpl(override val context: RebuildContextImpl) : BlockRenderer<BlockState, FluidState>() {
    override val worldSnapshot = context.worldSnapshot

    override fun renderBlock(state: BlockState) {
        context.setupRenderPos()

        val offsetType = state.block.offsetType
        if (offsetType != OffsetType.NONE) {
            val hashCode = MathHelper.hashCode(context.blockX, 0, context.blockZ)
            context.renderPosX += (hashCode and 15L) / 30.0f - 0.25f
            context.renderPosZ += (hashCode shr 8 and 15L) / 30.0f - 0.25f
            if (offsetType == OffsetType.XYZ) context.renderPosY += (hashCode shr 4 and 15L) / 75.0f - 0.2f
        }

        val seed = state.getRenderingSeed(context.renderBlockPos)
        val model = blockModels.getModel(state)

        if (MinecraftClient.isAmbientOcclusionEnabled() && state.luminance == 0 && model.useAmbientOcclusion()) {
            context.random.setSeed(seed)
            val listD = model.getQuads(state, Direction.DOWN, context.random)
            context.random.setSeed(seed)
            val listU = model.getQuads(state, Direction.UP, context.random)
            context.random.setSeed(seed)
            val listN = model.getQuads(state, Direction.NORTH, context.random)
            context.random.setSeed(seed)
            val listS = model.getQuads(state, Direction.SOUTH, context.random)
            context.random.setSeed(seed)
            val listW = model.getQuads(state, Direction.WEST, context.random)
            context.random.setSeed(seed)
            val listE = model.getQuads(state, Direction.EAST, context.random)
            context.random.setSeed(seed)
            val list = model.getQuads(state, null, context.random)

            if (list.isNotEmpty()) {
                renderQuadsSmooth(state, list)
            }
            if (listE.isNotEmpty() && context.shouldDrawSide(state, Direction.EAST)) {
                renderQuadsSmooth(state, listE)
            }
            if (listW.isNotEmpty() && context.shouldDrawSide(state, Direction.WEST)) {
                renderQuadsSmooth(state, listW)
            }
            if (listS.isNotEmpty() && context.shouldDrawSide(state, Direction.SOUTH)) {
                renderQuadsSmooth(state, listS)
            }
            if (listN.isNotEmpty() && context.shouldDrawSide(state, Direction.NORTH)) {
                renderQuadsSmooth(state, listN)
            }
            if (listU.isNotEmpty() && context.shouldDrawSide(state, Direction.UP)) {
                renderQuadsSmooth(state, listU)
            }
            if (listD.isNotEmpty() && context.shouldDrawSide(state, Direction.DOWN)) {
                renderQuadsSmooth(state, listD)
            }
        } else {
            context.random.setSeed(seed)
            val listD = model.getQuads(state, Direction.DOWN, context.random)
            context.random.setSeed(seed)
            val listU = model.getQuads(state, Direction.UP, context.random)
            context.random.setSeed(seed)
            val listN = model.getQuads(state, Direction.NORTH, context.random)
            context.random.setSeed(seed)
            val listS = model.getQuads(state, Direction.SOUTH, context.random)
            context.random.setSeed(seed)
            val listW = model.getQuads(state, Direction.WEST, context.random)
            context.random.setSeed(seed)
            val listE = model.getQuads(state, Direction.EAST, context.random)
            context.random.setSeed(seed)
            val list = model.getQuads(state, null, context.random)

            if (list.isNotEmpty()) {
                for (i in list.indices) {
                    val bakedQuad = list[i]
                    val face = bakedQuad.face
                    getQuadDimensions(face.ordinal, state, bakedQuad.vertexData, 8)

                    val lightX: Int
                    val lightY: Int
                    val lightZ: Int

                    if (context.flags[0]) {
                        lightX = context.blockX + face.offsetX
                        lightY = context.blockY + face.offsetY
                        lightZ = context.blockZ + face.offsetZ
                    } else {
                        lightX = context.blockX
                        lightY = context.blockY
                        lightZ = context.blockZ
                    }

                    val light = worldSnapshot.getLightBits(lightX, lightY, lightZ) and 0xFFFF
                    renderQuadFlat(state, bakedQuad, light)
                }
            }
            if (listE.isNotEmpty() && context.shouldDrawSide(state, Direction.EAST)) {
                val light = worldSnapshot.getLightBits(context.blockX + 1, context.blockY, context.blockZ) and 0xFFFF
                for (i in listE.indices) {
                    renderQuadFlat(state, listE[i], light)
                }
            }
            if (listW.isNotEmpty() && context.shouldDrawSide(state, Direction.WEST)) {
                val light = worldSnapshot.getLightBits(context.blockX - 1, context.blockY, context.blockZ) and 0xFFFF
                for (i in listW.indices) {
                    renderQuadFlat(state, listW[i], light)
                }
            }
            if (listS.isNotEmpty() && context.shouldDrawSide(state, Direction.SOUTH)) {
                val light = worldSnapshot.getLightBits(context.blockX, context.blockY, context.blockZ + 1) and 0xFFFF
                for (i in listS.indices) {
                    renderQuadFlat(state, listS[i], light)
                }
            }
            if (listN.isNotEmpty() && context.shouldDrawSide(state, Direction.NORTH)) {
                val light = worldSnapshot.getLightBits(context.blockX, context.blockY, context.blockZ - 1) and 0xFFFF
                for (i in listN.indices) {
                    renderQuadFlat(state, listN[i], light)
                }
            }
            if (listU.isNotEmpty() && context.shouldDrawSide(state, Direction.UP)) {
                val light = worldSnapshot.getLightBits(context.blockX, context.blockY + 1, context.blockZ) and 0xFFFF
                for (i in listU.indices) {
                    renderQuadFlat(state, listU[i], light)
                }
            }
            if (listD.isNotEmpty() && context.shouldDrawSide(state, Direction.DOWN)) {
                val light = worldSnapshot.getLightBits(context.blockX, context.blockY - 1, context.blockZ) and 0xFFFF
                for (i in listD.indices) {
                    renderQuadFlat(state, listD[i], light)
                }
            }
        }
    }

    private fun renderQuadsSmooth(
        state: BlockState,
        quads: List<BakedQuad>
    ) {
        for (i in quads.indices) {
            renderQuadSmooth(
                state,
                quads[i].vertexData,
                8,
                quads[i].face.ordinal,
                (quads[i] as IPatchedBakedQuad).faceBit,
                quads[i].hasShade(),
                quads[i].hasColor()
            )
        }
    }

    private fun renderQuadFlat(
        state: BlockState,
        quad: BakedQuad,
        light: Int
    ) {
        renderQuadFlat(
            state,
            quad.vertexData,
            8,
            quad.face.ordinal,
            (quad as IPatchedBakedQuad).faceBit,
            quad.hasShade(),
            quad.hasColor(),
            light
        )
    }

    override fun isFullCube(state: BlockState): Boolean {
        return state.isFullCube(worldSnapshot, context.renderBlockPos)
    }

    override fun renderFluid(state: FluidState, blockState: BlockState) {
        context.setupRenderPos()

        val sameFluidOnTop = !isSameFluid(context.blockX, context.blockY, context.blockZ, Direction.UP, state)
        val renderD = shouldRenderFluidSide(state, blockState, Direction.DOWN)
            && !isSideCovered(context.blockX, context.blockY, context.blockZ, Direction.DOWN, 0.890f)
        val renderN = shouldRenderFluidSide(state, blockState, Direction.NORTH)
        val renderS = shouldRenderFluidSide(state, blockState, Direction.SOUTH)
        val renderW = shouldRenderFluidSide(state, blockState, Direction.WEST)
        val renderE = shouldRenderFluidSide(state, blockState, Direction.EAST)

        if (!sameFluidOnTop && !renderD && !renderN && !renderS && !renderW && !renderE) return

        val isLava = state.isIn(FluidTags.LAVA)
        val sprites = if (isLava) lavaSprites else waterSprites

        val color = if (isLava) 0xFFFFFF else BiomeColors.getWaterColor(worldSnapshot, context.renderBlockPos)
        val red = color shr 16 and 255
        val green = color shr 8 and 255
        val blue = color and 255

        var heightNW = getFluidHeight(
            context.blockX,
            context.blockY,
            context.blockZ,
            state.fluid
        )
        var heightSW = getFluidHeight(
            context.blockX,
            context.blockY,
            context.blockZ + 1,
            state.fluid
        )
        var heightNE = getFluidHeight(
            context.blockX + 1,
            context.blockY,
            context.blockZ,
            state.fluid
        )
        var heightSE = getFluidHeight(
            context.blockX + 1,
            context.blockY,
            context.blockZ + 1,
            state.fluid
        )

        val minFluidHeight = min(min(heightNW, heightSW), min(heightSE, heightNE))

        if (sameFluidOnTop
            && !isSideCovered(context.blockX, context.blockY, context.blockZ, Direction.UP, minFluidHeight)
        ) {
            heightNW -= FLUID_SIDE_ESP
            heightSW -= FLUID_SIDE_ESP
            heightSE -= FLUID_SIDE_ESP
            heightNE -= FLUID_SIDE_ESP

            val fluidVelocity = state.getVelocity(worldSnapshot, context.renderBlockPos)

            var uNW: Float
            var uSW: Float
            var uSE: Float
            var uNE: Float

            var vNW: Float
            var vSW: Float
            var vSE: Float
            var vNE: Float

            if (fluidVelocity.x == 0.0 && fluidVelocity.z == 0.0) {
                val sprite = sprites[0]
                uNW = sprite.getFrameU(0.0)
                uSW = uNW
                uSE = sprite.getFrameU(16.0)
                uNE = uSE

                vNW = sprite.getFrameV(0.0)
                vNE = vNW
                vSW = sprite.getFrameV(16.0)
                vSE = vSW
            } else {
                val sprite = sprites[1]
                val flowAngle = MathHelper.atan2(fluidVelocity.z, fluidVelocity.x).toFloat() - 1.5707964f
                val flowX = MathHelper.sin(flowAngle) * 0.25f
                val flowZ = MathHelper.cos(flowAngle) * 0.25f
                uNW = sprite.getFrameU((8.0f + (-flowZ - flowX) * 16.0f).toDouble())
                uSW = sprite.getFrameU((8.0f + (-flowZ + flowX) * 16.0f).toDouble())
                uSE = sprite.getFrameU((8.0f + (flowZ + flowX) * 16.0f).toDouble())
                uNE = sprite.getFrameU((8.0f + (flowZ - flowX) * 16.0f).toDouble())

                vNW = sprite.getFrameV((8.0f + (-flowZ + flowX) * 16.0f).toDouble())
                vSW = sprite.getFrameV((8.0f + (flowZ + flowX) * 16.0f).toDouble())
                vSE = sprite.getFrameV((8.0f + (flowZ - flowX) * 16.0f).toDouble())
                vNE = sprite.getFrameV((8.0f + (-flowZ - flowX) * 16.0f).toDouble())
            }

            val avgU = (uNW + uSW + uSE + uNE) / 4.0f
            val avgV = (vNW + vSW + vSE + vNE) / 4.0f
            val start = 4.0f / max(
                sprites[0].width.toFloat() / (sprites[0].maxU - sprites[0].minU),
                sprites[0].height.toFloat() / (sprites[0].maxV - sprites[0].minV)
            )

            uNW = MathHelper.lerp(start, uNW, avgU)
            uSW = MathHelper.lerp(start, uSW, avgU)
            uSE = MathHelper.lerp(start, uSE, avgU)
            uNE = MathHelper.lerp(start, uNE, avgU)

            vNW = MathHelper.lerp(start, vNW, avgV)
            vSW = MathHelper.lerp(start, vSW, avgV)
            vSE = MathHelper.lerp(start, vSE, avgV)
            vNE = MathHelper.lerp(start, vNE, avgV)

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

            if (isTopClear(context.blockX, context.blockY + 1, context.blockZ, state)) {
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
                context.renderPosX,
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
                context.renderPosX,
                context.renderPosY,
                context.renderPosZ,
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
                context.renderPosZ,
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

            var direction: Direction

            var x1: Float
            var z1: Float
            var x2: Float
            var z2: Float
            var y11: Float
            var y12: Float

            when (i) {
                0 -> {
                    direction = Direction.NORTH

                    y11 = heightNW
                    y12 = heightNE

                    x1 = 0.0f
                    x2 = 1.0f
                    z1 = FLUID_SIDE_ESP
                    z2 = FLUID_SIDE_ESP
                }
                1 -> {
                    direction = Direction.SOUTH

                    y11 = heightSE
                    y12 = heightSW

                    x1 = 1.0f
                    x2 = 0.0f
                    z1 = 1.0f - FLUID_SIDE_ESP
                    z2 = 1.0f - FLUID_SIDE_ESP
                }
                2 -> {
                    direction = Direction.WEST

                    y11 = heightSW
                    y12 = heightNW

                    x1 = FLUID_SIDE_ESP
                    x2 = FLUID_SIDE_ESP
                    z1 = 1.0f
                    z2 = 0.0f
                }
                else -> {
                    direction = Direction.EAST

                    y11 = heightNE
                    y12 = heightSE

                    x1 = 1.0f - FLUID_SIDE_ESP
                    x2 = 1.0f - FLUID_SIDE_ESP
                    z1 = 0.0f
                    z2 = 1.0f
                }
            }

            if (isSideCovered(context.blockX, context.blockY, context.blockZ, direction, max(y11, y12))) continue

            var sideSprites = sprites[1]
            val sideX = context.blockX + direction.offsetX
            val sideY = context.blockY + direction.offsetY
            val sideZ = context.blockZ + direction.offsetZ

            if (!isLava) {
                val offsetBlock = worldSnapshot.getBlockState0(sideX, sideY, sideZ).block
                if (offsetBlock is TransparentBlock || offsetBlock is LeavesBlock) {
                    sideSprites = waterOverlaySprite
                }
            }

            val uN = sideSprites.getFrameU(0.0)
            val uS = sideSprites.getFrameU(8.0)
            val vFrom1 = sideSprites.getFrameV(((1.0f - y11) * 16.0f * 0.5f).toDouble())
            val vFrom2 = sideSprites.getFrameV(((1.0f - y12) * 16.0f * 0.5f).toDouble())
            val vTo = sideSprites.getFrameV(8.0)

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

    private val tempPos15756 = BlockPos.Mutable()

    private fun isTopClear(
        x: Int,
        y: Int,
        z: Int,
        state: FluidState
    ): Boolean {
        val fluid = state.fluid

        for (ix in -1..1) {
            for (iz in -1..1) {
                val x1 = x + ix
                val z1 = z + iz

                val fluidState = worldSnapshot.getFluidState(x1, y, z1)
                if (!fluidState.fluid.matchesType(fluid)
                    && !worldSnapshot.getBlockState0(x1, y, z1)
                        .isOpaqueFullCube(worldSnapshot, tempPos15756.set(x1, y, z1))
                ) {
                    return true
                }
            }
        }

        return false
    }

    private fun shouldRenderFluidSide(
        fluidState: FluidState,
        blockState: BlockState,
        direction: Direction
    ): Boolean {
        return !isSameFluid(context.blockX, context.blockY, context.blockZ, direction, fluidState)
            && !isSideCovered(context.blockX, context.blockY, context.blockZ, direction.opposite, blockState, 1.0f)
    }

    private fun isSideCovered(
        x: Int,
        y: Int,
        z: Int,
        direction: Direction,
        maxDeviation: Float
    ): Boolean {
        val x1 = x + direction.offsetX
        val y1 = y + direction.offsetY
        val z1 = z + direction.offsetZ
        return isSideCovered(x1, y1, z1, direction, worldSnapshot.getBlockState0(x1, y1, z1), maxDeviation)
    }

    private val tempPosIsSideCovered = BlockPos.Mutable()

    private fun isSideCovered(
        x: Int,
        y: Int,
        z: Int,
        direction: Direction,
        blockState: BlockState,
        maxDeviation: Float
    ): Boolean {
        return if (blockState.isOpaque) {
            val a = cuboidCache[(maxDeviation * 200.0f).toInt()]
            val b = blockState.getCullingShape(context.worldSnapshot, tempPosIsSideCovered.set(x, y, z))
            context.isSideCovered(a, b, direction)
        } else {
            false
        }
    }

    private fun isSameFluid(
        x: Int,
        y: Int,
        z: Int,
        direction: Direction,
        state: FluidState
    ): Boolean {
        return worldSnapshot.getFluidState(x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ)
            .fluid.matchesType(state.fluid)
    }

    private val tempPosNWFluidHeight = BlockPos.Mutable()

    private fun getFluidHeight(
        x: Int,
        y: Int,
        z: Int,
        fluid: Fluid
    ): Float {
        var count = 0
        var sum = 0.0f

        for (i in 0..3) {
            val x1 = x - (i and 1)
            val z1 = z - (i shr 1 and 1)

            if (worldSnapshot.getFluidState(x1, y + 1, z1).fluid.matchesType(fluid)) {
                return 1.0f
            }

            val fluidState = worldSnapshot.getFluidState(x1, y, z1)
            if (fluidState.fluid.matchesType(fluid)) {
                val fluidHeight = fluidState.getHeight(worldSnapshot, tempPosNWFluidHeight.set(x1, y, z1))
                if (fluidHeight >= 0.8f) {
                    sum += fluidHeight * 10.0f
                    count += 10
                } else {
                    sum += fluidHeight
                    ++count
                }
            } else if (!worldSnapshot.getBlockState0(x1, y, z1).material.isSolid) {
                ++count
            }
        }

        return sum / count.toFloat()
    }

    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    private companion object {
        @JvmField
        val blockModels = Minecraft.getInstance().bakedModelManager.blockModels!!

        @JvmField
        val cuboidCache = Array<VoxelShape>(201) {
            VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, it.toDouble() * 0.005, 1.0)
        }
        private val lavaSprites = arrayOf(
            blockModels.getModel(Blocks.LAVA.defaultState).particleSprite!!,
            ModelLoader.LAVA_FLOW.sprite!!
        )
        private val waterSprites = arrayOf(
            blockModels.getModel(Blocks.WATER.defaultState).particleSprite!!,
            ModelLoader.WATER_FLOW.sprite!!
        )
        private val waterOverlaySprite = ModelLoader.WATER_OVERLAY.sprite!!
    }
}