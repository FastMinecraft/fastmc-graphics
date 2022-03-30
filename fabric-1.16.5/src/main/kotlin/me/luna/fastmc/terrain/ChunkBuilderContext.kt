package me.luna.fastmc.terrain

import it.unimi.dsi.fastutil.ints.Int2BooleanLinkedOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap
import me.luna.fastmc.shared.util.CachedByteBuffer
import me.luna.fastmc.shared.util.ObjectPool
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.BlockState
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.function.BooleanBiFunction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockRenderView
import net.minecraft.world.BlockView
import java.util.*

@Suppress("NOTHING_TO_INLINE", "MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED_WARNING")
class ChunkBuilderContext {
    @JvmField
    val cachedByteBuffer = CachedByteBuffer(RenderLayer.getSolid().expectedBufferSize)

    @JvmField
    val brightnessCache = BrightnessCache()

    @JvmField
    val matrixStack = MatrixStack()

    @JvmField
    val mutableBlockPosPool = ObjectPool { BlockPos.Mutable() }

    @JvmField
    val brightnessArray = FloatArray(4)

    @JvmField
    val lightArray = IntArray(4)

    @JvmField
    val flags = BitSet(3)

    @JvmField
    val boxDimensionArray = FloatArray(Direction.values().size * 2)

    private val cullFaceMap = object : Int2BooleanLinkedOpenHashMap(2048, 0.25f) {
        override fun rehash(i: Int) {}
    }

    fun shouldDrawSide(self: BlockState, world: BlockView, pos: BlockPos, direction: Direction): Boolean {
        val otherPos = mutableBlockPosPool.get().set(pos, direction)
        val other = world.getBlockState(otherPos)

        val result = if (self.isSideInvisible(other, direction)) {
            false
        } else if (other.isOpaque) {
            val neighborGroup = hashNeighborGroup(self, other, direction)
            if (cullFaceMap.containsKey(neighborGroup)) {
                cullFaceMap.getAndMoveToFirst(neighborGroup)
            } else {
                val voxelShape = self.getCullingFace(world, pos, direction)
                val voxelShape2 = other.getCullingFace(world, otherPos, direction.opposite)
                val cull = VoxelShapes.matchesAnywhere(voxelShape, voxelShape2, BooleanBiFunction.ONLY_FIRST)
                if (cullFaceMap.size >= 2048) {
                    cullFaceMap.removeLastBoolean()
                }
                cullFaceMap.putAndMoveToFirst(neighborGroup, cull)
                cull
            }
        } else {
            true
        }

        mutableBlockPosPool.put(otherPos)

        return result
    }

    private fun hashNeighborGroup(self: BlockState, other: BlockState, facing: Direction): Int {
        var i = self.hashCode()
        i = 31 * i + other.hashCode()
        i = 31 * i + facing.hashCode()
        return i
    }

    inner class BrightnessCache {
        private var enabled = false
        private val intCache = object : Long2IntLinkedOpenHashMap(100, 0.25f) {
            init {
                defaultReturnValue(Int.MAX_VALUE)
            }

            override fun rehash(i: Int) {}
        }
        private val floatCache = object : Long2FloatLinkedOpenHashMap(100, 0.25f) {
            init {
                defaultReturnValue(Float.NaN)
            }

            override fun rehash(i: Int) {}
        }

        fun enable() {
            enabled = true
        }

        fun disable() {
            enabled = false
            intCache.clear()
            floatCache.clear()
        }

        fun getInt(state: BlockState, blockRenderView: BlockRenderView, pos: BlockPos): Int {
            val longPos = pos.asLong()

            return if (enabled) {
                var value = intCache.getAndMoveToFirst(longPos)

                if (value == Int.MAX_VALUE) {
                    value = WorldRenderer.getLightmapCoordinates(blockRenderView, state, pos)
                    if (intCache.size >= 100) {
                        intCache.removeLastInt()
                    }
                    intCache.putAndMoveToFirst(longPos, value)
                }

                value
            } else {
                WorldRenderer.getLightmapCoordinates(blockRenderView, state, pos)
            }
        }

        fun getFloat(state: BlockState, blockView: BlockRenderView, pos: BlockPos): Float {
            val longPos = pos.asLong()

            return if (enabled) {
                var value = floatCache.getAndMoveToFirst(longPos)

                if (value.isNaN()) {
                    value = state.getAmbientOcclusionLightLevel(blockView, pos)
                    if (floatCache.size >= 100) {
                        floatCache.removeLastFloat()
                    }
                    floatCache.putAndMoveToFirst(longPos, value)
                }

                value
            } else {
                state.getAmbientOcclusionLightLevel(blockView, pos)
            }
        }
    }

    fun calculateAO(
        world: BlockRenderView,
        state: BlockState,
        pos: BlockPos,
        direction: Direction,
        box: FloatArray,
        flags: BitSet,
        shaded: Boolean
    ) {
        val blockPos = mutableBlockPosPool.get()
        if (flags[0]) blockPos.set(pos, direction) else blockPos.set(pos)

        val neighborData = NeighborData.getData(direction)

        val mutableBlockPos = mutableBlockPosPool.get()
        val brightnessCache = brightnessCache
        mutableBlockPos.set(blockPos, neighborData.faces[0])
        val blockState1 = world.getBlockState(mutableBlockPos)
        val light1 = brightnessCache.getInt(blockState1, world, mutableBlockPos)
        val ao1 = brightnessCache.getFloat(blockState1, world, mutableBlockPos)

        mutableBlockPos.set(blockPos, neighborData.faces[1])
        val blockState2 = world.getBlockState(mutableBlockPos)
        val light2 = brightnessCache.getInt(blockState2, world, mutableBlockPos)
        val ao2 = brightnessCache.getFloat(blockState2, world, mutableBlockPos)

        mutableBlockPos.set(blockPos, neighborData.faces[2])
        val blockState3 = world.getBlockState(mutableBlockPos)
        val light3 = brightnessCache.getInt(blockState3, world, mutableBlockPos)
        val ao3 = brightnessCache.getFloat(blockState3, world, mutableBlockPos)

        mutableBlockPos.set(blockPos, neighborData.faces[3])
        val blockState4 = world.getBlockState(mutableBlockPos)
        val light4 = brightnessCache.getInt(blockState4, world, mutableBlockPos)
        val ao4 = brightnessCache.getFloat(blockState4, world, mutableBlockPos)

        mutableBlockPos.set(blockPos, neighborData.faces[0]).move(direction)
        val opaque1 = world.getBlockState(mutableBlockPos).getOpacity(world, mutableBlockPos) == 0

        mutableBlockPos.set(blockPos, neighborData.faces[1]).move(direction)
        val opaque2 = world.getBlockState(mutableBlockPos).getOpacity(world, mutableBlockPos) == 0

        mutableBlockPos.set(blockPos, neighborData.faces[2]).move(direction)
        val opaque3 = world.getBlockState(mutableBlockPos).getOpacity(world, mutableBlockPos) == 0

        mutableBlockPos.set(blockPos, neighborData.faces[3]).move(direction)
        val opaque4 = world.getBlockState(mutableBlockPos).getOpacity(world, mutableBlockPos) == 0

        val p: Float
        val q: Int
        if (!opaque3 && !opaque1) {
            p = ao1
            q = light1
        } else {
            mutableBlockPos.set(blockPos, neighborData.faces[0]).move(neighborData.faces[2])
            val blockState = world.getBlockState(mutableBlockPos)
            p = brightnessCache.getFloat(blockState, world, mutableBlockPos)
            q = brightnessCache.getInt(blockState, world, mutableBlockPos)
        }

        val t: Float
        val u: Int
        if (!opaque4 && !opaque1) {
            t = ao1
            u = light1
        } else {
            mutableBlockPos.set(blockPos, neighborData.faces[0]).move(neighborData.faces[3])
            val blockState = world.getBlockState(mutableBlockPos)
            t = brightnessCache.getFloat(blockState, world, mutableBlockPos)
            u = brightnessCache.getInt(blockState, world, mutableBlockPos)
        }

        val x: Float
        val y: Int
        if (!opaque3 && !opaque2) {
            x = ao1
            y = light1
        } else {
            mutableBlockPos.set(blockPos, neighborData.faces[1]).move(neighborData.faces[2])
            val blockState = world.getBlockState(mutableBlockPos)
            x = brightnessCache.getFloat(blockState, world, mutableBlockPos)
            y = brightnessCache.getInt(blockState, world, mutableBlockPos)
        }

        val ab: Float
        val ac: Int
        if (!opaque4 && !opaque2) {
            ab = ao1
            ac = light1
        } else {
            mutableBlockPos.set(blockPos, neighborData.faces[1]).move(neighborData.faces[3])
            val blockState = world.getBlockState(mutableBlockPos)
            ab = brightnessCache.getFloat(blockState, world, mutableBlockPos)
            ac = brightnessCache.getInt(blockState, world, mutableBlockPos)
        }

        var ad = brightnessCache.getInt(state, world, pos)
        mutableBlockPos.set(pos, direction)
        val blockState9 = world.getBlockState(mutableBlockPos)
        if (flags[0] || !blockState9.isOpaqueFullCube(world, mutableBlockPos)) {
            ad = brightnessCache.getInt(blockState9, world, mutableBlockPos)
        }

        val ae = if (flags[0]) brightnessCache.getFloat(world.getBlockState(blockPos), world, blockPos)
        else brightnessCache.getFloat(world.getBlockState(pos), world, pos)

        val translation = Translation.getTranslations(direction)
        val corners = translation.corners

        if (flags[1]) {
            val bh = (ao4 + ao1 + t + ae) * 0.25f
            val ag = (ao3 + ao1 + p + ae) * 0.25f
            val ah = (ao3 + ao2 + x + ae) * 0.25f
            val ai = (ao4 + ao2 + ab + ae) * 0.25f

            val an = box[neighborData.orientation1[0].shape] * box[neighborData.orientation1[1].shape]
            val ao = box[neighborData.orientation1[2].shape] * box[neighborData.orientation1[3].shape]
            val ap = box[neighborData.orientation1[4].shape] * box[neighborData.orientation1[5].shape]
            val aq = box[neighborData.orientation1[6].shape] * box[neighborData.orientation1[7].shape]
            val ar = box[neighborData.orientation2[0].shape] * box[neighborData.orientation2[1].shape]
            val `as` = box[neighborData.orientation2[2].shape] * box[neighborData.orientation2[3].shape]
            val at = box[neighborData.orientation2[4].shape] * box[neighborData.orientation2[5].shape]
            val au = box[neighborData.orientation2[6].shape] * box[neighborData.orientation2[7].shape]
            val av = box[neighborData.orientation3[0].shape] * box[neighborData.orientation3[1].shape]
            val aw = box[neighborData.orientation3[2].shape] * box[neighborData.orientation3[3].shape]
            val ax = box[neighborData.orientation3[4].shape] * box[neighborData.orientation3[5].shape]
            val ay = box[neighborData.orientation3[6].shape] * box[neighborData.orientation3[7].shape]
            val az = box[neighborData.orientation4[0].shape] * box[neighborData.orientation4[1].shape]
            val ba = box[neighborData.orientation4[2].shape] * box[neighborData.orientation4[3].shape]
            val bb = box[neighborData.orientation4[4].shape] * box[neighborData.orientation4[5].shape]
            val bc = box[neighborData.orientation4[6].shape] * box[neighborData.orientation4[7].shape]

            brightnessArray[corners[0]] = bh * an + ag * ao + ah * ap + ai * aq
            brightnessArray[corners[1]] = bh * ar + ag * `as` + ah * at + ai * au
            brightnessArray[corners[2]] = bh * av + ag * aw + ah * ax + ai * ay
            brightnessArray[corners[3]] = bh * az + ag * ba + ah * bb + ai * bc

            val bd = getAoBrightness(light4, light1, u, ad)
            val be = getAoBrightness(light3, light1, q, ad)
            val bf = getAoBrightness(light3, light2, y, ad)
            val bg = getAoBrightness(light4, light2, ac, ad)

            lightArray[corners[0]] = calcLight(bd, be, bf, bg, an, ao, ap, aq)
            lightArray[corners[1]] = calcLight(bd, be, bf, bg, ar, `as`, at, au)
            lightArray[corners[2]] = calcLight(bd, be, bf, bg, av, aw, ax, ay)
            lightArray[corners[3]] = calcLight(bd, be, bf, bg, az, ba, bb, bc)
        } else {
            val bh = (ao4 + ao1 + t + ae) * 0.25f
            val ag = (ao3 + ao1 + p + ae) * 0.25f
            val ah = (ao3 + ao2 + x + ae) * 0.25f
            val ai = (ao4 + ao2 + ab + ae) * 0.25f

            lightArray[corners[0]] = getAoBrightness(light4, light1, u, ad)
            lightArray[corners[1]] = getAoBrightness(light3, light1, q, ad)
            lightArray[corners[2]] = getAoBrightness(light3, light2, y, ad)
            lightArray[corners[3]] = getAoBrightness(light4, light2, ac, ad)

            brightnessArray[corners[0]] = bh
            brightnessArray[corners[1]] = ag
            brightnessArray[corners[2]] = ah
            brightnessArray[corners[3]] = ai
        }
        val mainBrightness = world.getBrightness(direction, shaded)

        for (i in brightnessArray.indices) {
            brightnessArray[i] *= mainBrightness
        }

        mutableBlockPosPool.put(blockPos)
        mutableBlockPosPool.put(mutableBlockPos)
    }

    private inline fun getAoBrightness(light1: Int, light2: Int, light3: Int, light4: Int): Int {
        return ((if (light1 != 0) light1 else light4)
            + (if (light2 != 0) light2 else light4)
            + (if (light3 != 0) light3 else light4)
            + light4) shr 2 and 16711935
    }

    private inline fun calcLight(
        light1: Int,
        light2: Int,
        light3: Int,
        light4: Int,
        ao1: Float,
        ao2: Float,
        ao3: Float,
        ao4: Float
    ): Int {
        val skyLight = ((light1 shr 16 and 255).toFloat() * ao1
            + (light2 shr 16 and 255).toFloat() * ao2
            + (light3 shr 16 and 255).toFloat() * ao3
            + (light4 shr 16 and 255).toFloat() * ao4).toInt() and 255
        val blockLight = ((light1 and 255).toFloat() * ao1
            + (light2 and 255).toFloat() * ao2
            + (light3 and 255).toFloat() * ao3
            + (light4 and 255).toFloat() * ao4).toInt() and 255

        return skyLight shl 16 or blockLight
    }

    @Environment(EnvType.CLIENT)
    enum class Translation(
        corner0: Int,
        corner1: Int,
        corner2: Int,
        corner3: Int
    ) {
        DOWN(0, 1, 2, 3),
        UP(2, 3, 0, 1),
        NORTH(3, 0, 1, 2),
        SOUTH(0, 1, 2, 3),
        WEST(3, 0, 1, 2),
        EAST(1, 2, 3, 0);

        val corners = intArrayOf(corner0, corner1, corner2, corner3)

        companion object {
            @JvmField
            val VALUES = arrayOf(DOWN, UP, NORTH, SOUTH, WEST, EAST)

            @JvmStatic
            inline fun getTranslations(direction: Direction): Translation {
                return VALUES[direction.id]
            }
        }
    }

    enum class NeighborOrientation(direction: Direction, flipped: Boolean) {
        DOWN(Direction.DOWN, false),
        UP(Direction.UP, false),
        NORTH(Direction.NORTH, false),
        SOUTH(Direction.SOUTH, false),
        WEST(Direction.WEST, false),
        EAST(Direction.EAST, false),
        FLIP_DOWN(Direction.DOWN, true),
        FLIP_UP(Direction.UP, true),
        FLIP_NORTH(Direction.NORTH, true),
        FLIP_SOUTH(Direction.SOUTH, true),
        FLIP_WEST(Direction.WEST, true),
        FLIP_EAST(Direction.EAST, true);

        val shape = direction.id + if (flipped) Direction.values().size else 0
    }

    @Environment(EnvType.CLIENT)
    enum class NeighborData(
        val faces: Array<Direction>,
        val orientation1: Array<NeighborOrientation>,
        val orientation2: Array<NeighborOrientation>,
        val orientation3: Array<NeighborOrientation>,
        val orientation4: Array<NeighborOrientation>
    ) {
        DOWN(
            arrayOf(
                Direction.WEST,
                Direction.EAST,
                Direction.NORTH,
                Direction.SOUTH
            ),
            arrayOf(
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.SOUTH,
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.WEST,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.WEST,
                NeighborOrientation.SOUTH
            ),
            arrayOf(
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.NORTH,
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.WEST,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.WEST,
                NeighborOrientation.NORTH
            ),
            arrayOf(
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.NORTH,
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.EAST,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.EAST,
                NeighborOrientation.NORTH
            ),
            arrayOf(
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.SOUTH,
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.EAST,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.EAST,
                NeighborOrientation.SOUTH
            )
        ),
        UP(
            arrayOf(
                Direction.EAST,
                Direction.WEST,
                Direction.NORTH,
                Direction.SOUTH
            ),
            arrayOf(
                NeighborOrientation.EAST,
                NeighborOrientation.SOUTH,
                NeighborOrientation.EAST,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.SOUTH
            ),
            arrayOf(
                NeighborOrientation.EAST,
                NeighborOrientation.NORTH,
                NeighborOrientation.EAST,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.NORTH
            ),
            arrayOf(
                NeighborOrientation.WEST,
                NeighborOrientation.NORTH,
                NeighborOrientation.WEST,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.NORTH
            ),
            arrayOf(
                NeighborOrientation.WEST,
                NeighborOrientation.SOUTH,
                NeighborOrientation.WEST,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.SOUTH
            )
        ),
        NORTH(
            arrayOf(
                Direction.UP,
                Direction.DOWN,
                Direction.EAST,
                Direction.WEST
            ),
            arrayOf(
                NeighborOrientation.UP,
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.UP,
                NeighborOrientation.WEST,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.WEST,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.FLIP_WEST
            ),
            arrayOf(
                NeighborOrientation.UP,
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.UP,
                NeighborOrientation.EAST,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.EAST,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.FLIP_EAST
            ),
            arrayOf(
                NeighborOrientation.DOWN,
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.DOWN,
                NeighborOrientation.EAST,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.EAST,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.FLIP_EAST
            ),
            arrayOf(
                NeighborOrientation.DOWN,
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.DOWN,
                NeighborOrientation.WEST,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.WEST,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.FLIP_WEST
            )
        ),
        SOUTH(
            arrayOf(
                Direction.WEST,
                Direction.EAST,
                Direction.DOWN,
                Direction.UP
            ),
            arrayOf(
                NeighborOrientation.UP,
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.WEST,
                NeighborOrientation.UP,
                NeighborOrientation.WEST
            ),
            arrayOf(
                NeighborOrientation.DOWN,
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.FLIP_WEST,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.WEST,
                NeighborOrientation.DOWN,
                NeighborOrientation.WEST
            ),
            arrayOf(
                NeighborOrientation.DOWN,
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.EAST,
                NeighborOrientation.DOWN,
                NeighborOrientation.EAST
            ),
            arrayOf(
                NeighborOrientation.UP,
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.FLIP_EAST,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.EAST,
                NeighborOrientation.UP,
                NeighborOrientation.EAST
            )
        ),
        WEST(
            arrayOf(
                Direction.UP,
                Direction.DOWN,
                Direction.NORTH,
                Direction.SOUTH
            ),
            arrayOf(
                NeighborOrientation.UP,
                NeighborOrientation.SOUTH,
                NeighborOrientation.UP,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.SOUTH
            ),
            arrayOf(
                NeighborOrientation.UP,
                NeighborOrientation.NORTH,
                NeighborOrientation.UP,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.NORTH
            ),
            arrayOf(
                NeighborOrientation.DOWN,
                NeighborOrientation.NORTH,
                NeighborOrientation.DOWN,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.NORTH
            ),
            arrayOf(
                NeighborOrientation.DOWN,
                NeighborOrientation.SOUTH,
                NeighborOrientation.DOWN,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.SOUTH
            )
        ),
        EAST(
            arrayOf(
                Direction.DOWN,
                Direction.UP,
                Direction.NORTH,
                Direction.SOUTH
            ),
            arrayOf(
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.SOUTH,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.DOWN,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.DOWN,
                NeighborOrientation.SOUTH
            ),
            arrayOf(
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.NORTH,
                NeighborOrientation.FLIP_DOWN,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.DOWN,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.DOWN,
                NeighborOrientation.NORTH
            ),
            arrayOf(
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.NORTH,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.UP,
                NeighborOrientation.FLIP_NORTH,
                NeighborOrientation.UP,
                NeighborOrientation.NORTH
            ),
            arrayOf(
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.SOUTH,
                NeighborOrientation.FLIP_UP,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.UP,
                NeighborOrientation.FLIP_SOUTH,
                NeighborOrientation.UP,
                NeighborOrientation.SOUTH
            )
        );

        companion object {
            @JvmField
            val VALUES = arrayOf(DOWN, UP, NORTH, SOUTH, WEST, EAST)

            @JvmStatic
            inline fun getData(direction: Direction): NeighborData {
                return VALUES[direction.id]
            }
        }
    }
}