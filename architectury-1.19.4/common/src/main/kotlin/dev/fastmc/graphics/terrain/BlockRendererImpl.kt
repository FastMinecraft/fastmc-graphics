package dev.fastmc.graphics.terrain

import dev.fastmc.graphics.shared.mixin.IPatchedBakedQuad
import dev.fastmc.graphics.shared.terrain.BlockRenderer
import dev.fastmc.graphics.util.Minecraft
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
import net.minecraft.registry.tag.FluidTags
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

    private val random = WrappedSplitmix64Random()

    override fun renderBlock(state: BlockState) {
        context.setupRenderPos()

        if (state.hasModelOffset()) {
            val offset = state.getModelOffset(context.worldSnapshot, context.renderBlockPos)
            context.renderPosX += offset.x.toFloat()
            context.renderPosZ += offset.z.toFloat()
            context.renderPosY += offset.y.toFloat()
        }

        val seed = state.getRenderingSeed(context.renderBlockPos)
        val model = blockModels.getModel(state)

        if (MinecraftClient.isAmbientOcclusionEnabled() && state.luminance == 0 && model.useAmbientOcclusion()) {
            random.setSeed(seed)
            val listD = model.getQuads(state, Direction.DOWN, random)
            random.setSeed(seed)
            val listU = model.getQuads(state, Direction.UP, random)
            random.setSeed(seed)
            val listN = model.getQuads(state, Direction.NORTH, random)
            random.setSeed(seed)
            val listS = model.getQuads(state, Direction.SOUTH, random)
            random.setSeed(seed)
            val listW = model.getQuads(state, Direction.WEST, random)
            random.setSeed(seed)
            val listE = model.getQuads(state, Direction.EAST, random)
            random.setSeed(seed)
            val list = model.getQuads(state, null, random)

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
            random.setSeed(seed)
            val listD = model.getQuads(state, Direction.DOWN, random)
            random.setSeed(seed)
            val listU = model.getQuads(state, Direction.UP, random)
            random.setSeed(seed)
            val listN = model.getQuads(state, Direction.NORTH, random)
            random.setSeed(seed)
            val listS = model.getQuads(state, Direction.SOUTH, random)
            random.setSeed(seed)
            val listW = model.getQuads(state, Direction.WEST, random)
            random.setSeed(seed)
            val listE = model.getQuads(state, Direction.EAST, random)
            random.setSeed(seed)
            val list = model.getQuads(state, null, random)

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
        val renderD = (shouldRenderFluidSide(state, blockState, Direction.DOWN)
            && !isSideCovered(context.blockX, context.blockY, context.blockZ, Direction.DOWN, 0.890f))
        val renderN = shouldRenderFluidSide(state, blockState, Direction.NORTH)
        val renderS = shouldRenderFluidSide(state, blockState, Direction.SOUTH)
        val renderW = shouldRenderFluidSide(state, blockState, Direction.WEST)
        val renderE = shouldRenderFluidSide(state, blockState, Direction.EAST)

        if (!sameFluidOnTop && !renderD && !renderN && !renderS && !renderW && !renderE) return

        val isLava = state.isIn(FluidTags.LAVA)
        val sprites = if (isLava) lavaSprites else waterSprites

        val waterColor = if (isLava) 0xFFFFFF else BiomeColors.getWaterColor(worldSnapshot, context.renderBlockPos)
        val waterColorRed = waterColor shr 16 and 255
        val waterColorGreen = waterColor shr 8 and 255
        val waterColorBlue = waterColor and 255

        var fluidHeightNW = getFluidHeight(
            context.blockX,
            context.blockY,
            context.blockZ,
            state.fluid
        )
        var fluidHeightSW = getFluidHeight(
            context.blockX,
            context.blockY,
            context.blockZ + 1,
            state.fluid
        )
        var fluidHeightNE = getFluidHeight(
            context.blockX + 1,
            context.blockY,
            context.blockZ,
            state.fluid
        )
        var fluidHeightSE = getFluidHeight(
            context.blockX + 1,
            context.blockY,
            context.blockZ + 1,
            state.fluid
        )

        val yDown = if (renderD) 0.005f else 0.0f
        val minFluidHeight = min(min(fluidHeightNW, fluidHeightSW), min(fluidHeightSE, fluidHeightNE))

        if (sameFluidOnTop
            && !isSideCovered(context.blockX, context.blockY, context.blockZ, Direction.UP, minFluidHeight)
        ) {
            fluidHeightNW -= 0.005f
            fluidHeightSW -= 0.005f
            fluidHeightSE -= 0.005f
            fluidHeightNE -= 0.005f

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
                sprites[0].contents.width.toFloat() / (sprites[0].maxU - sprites[0].minU),
                sprites[0].contents.height.toFloat() / (sprites[0].maxV - sprites[0].minV)
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

            if (isTopClear(context.blockX, context.blockY + 1, context.blockZ, state)) {
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
            var direction: Direction
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
                    direction = Direction.NORTH
                    renderSide = renderN
                }
                1 -> {
                    y11 = fluidHeightSE
                    y12 = fluidHeightSW
                    x1 = 1.0f
                    x2 = 0.0f
                    z1 = 1.0f - 0.005f
                    z2 = 1.0f - 0.005f
                    direction = Direction.SOUTH
                    renderSide = renderS
                }
                2 -> {
                    y11 = fluidHeightSW
                    y12 = fluidHeightNW
                    x1 = 0.005f
                    x2 = 0.005f
                    z1 = 1.0f
                    z2 = 0.0f
                    direction = Direction.WEST
                    renderSide = renderW
                }
                else -> {
                    y11 = fluidHeightNE
                    y12 = fluidHeightSE
                    x1 = 1.0f - 0.005f
                    x2 = 1.0f - 0.005f
                    z1 = 0.0f
                    z2 = 1.0f
                    direction = Direction.EAST
                    renderSide = renderE
                }
            }

            if (renderSide && !isSideCovered(
                    context.blockX,
                    context.blockY,
                    context.blockZ,
                    direction,
                    max(y11, y12)
                )
            ) {
                var sideSprites = sprites[1]
                if (!isLava) {
                    val block = worldSnapshot.getBlockState0(
                        context.blockX + direction.offsetX,
                        context.blockY + direction.offsetY,
                        context.blockZ + direction.offsetZ
                    ).block
                    if (block is TransparentBlock || block is LeavesBlock) {
                        sideSprites = waterOverlaySprite
                    }
                }
                val uN = sideSprites.getFrameU(0.0)
                val uS = sideSprites.getFrameU(8.0)
                val vFrom1 = sideSprites.getFrameV(((1.0f - y11) * 16.0f * 0.5f).toDouble())
                val vFrom2 = sideSprites.getFrameV(((1.0f - y12) * 16.0f * 0.5f).toDouble())
                val vTo = sideSprites.getFrameV(8.0)

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