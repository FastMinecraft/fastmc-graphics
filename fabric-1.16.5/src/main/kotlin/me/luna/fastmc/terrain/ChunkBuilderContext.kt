package me.luna.fastmc.terrain

import me.luna.fastmc.shared.util.CachedByteBuffer
import me.luna.fastmc.shared.util.ObjectPool
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

    private val lightCache = IntArray(32768)
    private val aoLightCache = FloatArray(32768)

    fun init() {
        lightCache.fill(Int.MAX_VALUE)
        aoLightCache.fill(Float.NaN)
    }

    fun shouldDrawSide(self: BlockState, blockView: BlockView, pos: BlockPos, direction: Direction): Boolean {
        val otherPos = mutableBlockPosPool.get().set(pos, direction)
        val other = blockView.getBlockState(otherPos)
        val otherDirection = direction.opposite

        val value = if (self.isSideInvisible(other, direction)) {
            false
        } else if (other.isOpaque) {
            val selfShape = self.getCullingFace(blockView, pos, direction)
            val otherShape = other.getCullingFace(blockView, otherPos, otherDirection)
            VoxelShapes.matchesAnywhere(selfShape, otherShape, BooleanBiFunction.ONLY_FIRST)
        } else {
            true
        }

        mutableBlockPosPool.put(otherPos)
        return value
    }

    fun getLight(state: BlockState, blockView: BlockRenderView, pos: BlockPos): Int {
        val index = index(pos)
        var value = lightCache[index]
        if (value == Int.MAX_VALUE) {
            value = WorldRenderer.getLightmapCoordinates(blockView, state, pos)
            lightCache[index] = value
        }
        return value
    }

    fun getAoLight(state: BlockState, blockView: BlockRenderView, pos: BlockPos): Float {
        val index = index(pos)
        var value = aoLightCache[index]
        if (value.isNaN()) {
            value = state.getAmbientOcclusionLightLevel(blockView, pos)
            aoLightCache[index] = value
        }
        return value
    }

    private inline fun index(pos: BlockPos): Int {
        return (((pos.x + 8) and 31) shl 10) or (((pos.y + 8) and 31) shl 5) or ((pos.z + 8) and 31)
    }

    fun calculateAO(
        world: BlockRenderView,
        thisState: BlockState,
        posThis: BlockPos,
        direction: Direction,
        box: FloatArray,
        flags: BitSet,
        shaded: Boolean
    ) {
        val tempBlockPos = mutableBlockPosPool.get()
        if (flags[0]) tempBlockPos.set(posThis, direction) else tempBlockPos.set(posThis)

        val neighborData = NeighborData[direction]

        val mutableBlockPos = mutableBlockPosPool.get()
        mutableBlockPos.set(tempBlockPos, neighborData.faces[0])
        val blockState1 = world.getBlockState(mutableBlockPos)
        val light1 = getLight(blockState1, world, mutableBlockPos)
        val ao1 = getAoLight(blockState1, world, mutableBlockPos)

        mutableBlockPos.move(direction)
        val opaque1 = world.getBlockState(mutableBlockPos).getOpacity(world, mutableBlockPos) == 0


        mutableBlockPos.set(tempBlockPos, neighborData.faces[1])
        val blockState2 = world.getBlockState(mutableBlockPos)
        val light2 = getLight(blockState2, world, mutableBlockPos)
        val ao2 = getAoLight(blockState2, world, mutableBlockPos)

        mutableBlockPos.move(direction)
        val opaque2 = world.getBlockState(mutableBlockPos).getOpacity(world, mutableBlockPos) == 0


        mutableBlockPos.set(tempBlockPos, neighborData.faces[2])
        val blockState3 = world.getBlockState(mutableBlockPos)
        val light3 = getLight(blockState3, world, mutableBlockPos)
        val ao3 = getAoLight(blockState3, world, mutableBlockPos)

        mutableBlockPos.set(tempBlockPos, neighborData.faces[2]).move(direction)
        val opaque3 = world.getBlockState(mutableBlockPos).getOpacity(world, mutableBlockPos) == 0


        mutableBlockPos.set(tempBlockPos, neighborData.faces[3])
        val blockState4 = world.getBlockState(mutableBlockPos)
        val light4 = getLight(blockState4, world, mutableBlockPos)
        val ao4 = getAoLight(blockState4, world, mutableBlockPos)

        mutableBlockPos.move(direction)
        val opaque4 = world.getBlockState(mutableBlockPos).getOpacity(world, mutableBlockPos) == 0

        val aoCorner1: Float
        val lightCorner1: Int
        if (!opaque3 && !opaque1) {
            aoCorner1 = ao1
            lightCorner1 = light1
        } else {
            mutableBlockPos.set(tempBlockPos, neighborData.faces[0]).move(neighborData.faces[2])
            val blockState = world.getBlockState(mutableBlockPos)
            aoCorner1 = getAoLight(blockState, world, mutableBlockPos)
            lightCorner1 = getLight(blockState, world, mutableBlockPos)
        }

        val aoCorner2: Float
        val lightCorner2: Int
        if (!opaque4 && !opaque1) {
            aoCorner2 = ao1
            lightCorner2 = light1
        } else {
            mutableBlockPos.set(tempBlockPos, neighborData.faces[0]).move(neighborData.faces[3])
            val blockState = world.getBlockState(mutableBlockPos)
            aoCorner2 = getAoLight(blockState, world, mutableBlockPos)
            lightCorner2 = getLight(blockState, world, mutableBlockPos)
        }

        val aoCorner3: Float
        val lightCorner3: Int
        if (!opaque3 && !opaque2) {
            aoCorner3 = ao1
            lightCorner3 = light1
        } else {
            mutableBlockPos.set(tempBlockPos, neighborData.faces[1]).move(neighborData.faces[2])
            val blockState = world.getBlockState(mutableBlockPos)
            aoCorner3 = getAoLight(blockState, world, mutableBlockPos)
            lightCorner3 = getLight(blockState, world, mutableBlockPos)
        }

        val aoCorner4: Float
        val lightCorner4: Int
        if (!opaque4 && !opaque2) {
            aoCorner4 = ao1
            lightCorner4 = light1
        } else {
            mutableBlockPos.set(tempBlockPos, neighborData.faces[1]).move(neighborData.faces[3])
            val blockState = world.getBlockState(mutableBlockPos)
            aoCorner4 = getAoLight(blockState, world, mutableBlockPos)
            lightCorner4 = getLight(blockState, world, mutableBlockPos)
        }

        val lightDirection = if (flags[0]) {
            getLight(world.getBlockState(tempBlockPos), world, mutableBlockPos)
        } else {
            mutableBlockPos.set(posThis, direction)
            val directionBlockState = world.getBlockState(mutableBlockPos)
            if (!directionBlockState.isOpaqueFullCube(world, mutableBlockPos)) {
                getLight(directionBlockState, world, mutableBlockPos)
            } else {
                getLight(thisState, world, posThis)
            }
        }

        val aoThis = getAoLight(world.getBlockState(tempBlockPos), world, tempBlockPos)
        val translation = Translation[direction]
        val corners = translation.corners

        if (flags[1]) {
            val light311T = (ao3 + ao1 + aoCorner1 + aoThis) * 0.25f
            val light412T = (ao4 + ao1 + aoCorner2 + aoThis) * 0.25f
            val light323T = (ao3 + ao2 + aoCorner3 + aoThis) * 0.25f
            val light424T = (ao4 + ao2 + aoCorner4 + aoThis) * 0.25f

            val ao101 = box[neighborData.orientation1[0].shape] * box[neighborData.orientation1[1].shape]
            val ao123 = box[neighborData.orientation1[2].shape] * box[neighborData.orientation1[3].shape]
            val ao145 = box[neighborData.orientation1[4].shape] * box[neighborData.orientation1[5].shape]
            val ao167 = box[neighborData.orientation1[6].shape] * box[neighborData.orientation1[7].shape]

            val ao201 = box[neighborData.orientation2[0].shape] * box[neighborData.orientation2[1].shape]
            val ao223 = box[neighborData.orientation2[2].shape] * box[neighborData.orientation2[3].shape]
            val ao245 = box[neighborData.orientation2[4].shape] * box[neighborData.orientation2[5].shape]
            val ao267 = box[neighborData.orientation2[6].shape] * box[neighborData.orientation2[7].shape]

            val ao301 = box[neighborData.orientation3[0].shape] * box[neighborData.orientation3[1].shape]
            val ao323 = box[neighborData.orientation3[2].shape] * box[neighborData.orientation3[3].shape]
            val ao345 = box[neighborData.orientation3[4].shape] * box[neighborData.orientation3[5].shape]
            val ao367 = box[neighborData.orientation3[6].shape] * box[neighborData.orientation3[7].shape]

            val ao401 = box[neighborData.orientation4[0].shape] * box[neighborData.orientation4[1].shape]
            val ao423 = box[neighborData.orientation4[2].shape] * box[neighborData.orientation4[3].shape]
            val ao445 = box[neighborData.orientation4[4].shape] * box[neighborData.orientation4[5].shape]
            val ao467 = box[neighborData.orientation4[6].shape] * box[neighborData.orientation4[7].shape]

            brightnessArray[corners[0]] = light412T * ao101 + light311T * ao123 + light323T * ao145 + light424T * ao167
            brightnessArray[corners[1]] = light412T * ao201 + light311T * ao223 + light323T * ao245 + light424T * ao267
            brightnessArray[corners[2]] = light412T * ao301 + light311T * ao323 + light323T * ao345 + light424T * ao367
            brightnessArray[corners[3]] = light412T * ao401 + light311T * ao423 + light323T * ao445 + light424T * ao467

            val light311D = combineLights(light3, light1, lightCorner1, lightDirection)
            val light412D = combineLights(light4, light1, lightCorner2, lightDirection)
            val light323D = combineLights(light3, light2, lightCorner3, lightDirection)
            val light424D = combineLights(light4, light2, lightCorner4, lightDirection)

            lightArray[corners[0]] = combineLightWithAo(light412D, light311D, light323D, light424D, ao101, ao123, ao145, ao167)
            lightArray[corners[1]] = combineLightWithAo(light412D, light311D, light323D, light424D, ao201, ao223, ao245, ao267)
            lightArray[corners[2]] = combineLightWithAo(light412D, light311D, light323D, light424D, ao301, ao323, ao345, ao367)
            lightArray[corners[3]] = combineLightWithAo(light412D, light311D, light323D, light424D, ao401, ao423, ao445, ao467)
        } else {
            lightArray[corners[0]] = combineLights(light4, light1, lightCorner2, lightDirection)
            lightArray[corners[1]] = combineLights(light3, light1, lightCorner1, lightDirection)
            lightArray[corners[2]] = combineLights(light3, light2, lightCorner3, lightDirection)
            lightArray[corners[3]] = combineLights(light4, light2, lightCorner4, lightDirection)

            brightnessArray[corners[0]] = (ao4 + ao1 + aoCorner2 + aoThis) * 0.25f
            brightnessArray[corners[1]] = (ao3 + ao1 + aoCorner1 + aoThis) * 0.25f
            brightnessArray[corners[2]] = (ao3 + ao2 + aoCorner3 + aoThis) * 0.25f
            brightnessArray[corners[3]] = (ao4 + ao2 + aoCorner4 + aoThis) * 0.25f
        }

        val globalBrightness = world.getBrightness(direction, shaded)
        for (i in brightnessArray.indices) {
            brightnessArray[i] *= globalBrightness
        }

        mutableBlockPosPool.put(tempBlockPos)
        mutableBlockPosPool.put(mutableBlockPos)
    }

    private inline fun combineLights(light1: Int, light2: Int, light3: Int, light4: Int): Int {
        return ((if (light1 != 0) light1 else light4)
            + (if (light2 != 0) light2 else light4)
            + (if (light3 != 0) light3 else light4)
            + light4) shr 2 and 0xFF00FF
    }

    private inline fun combineLightWithAo(
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

    private  enum class Translation(
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
            inline operator fun get(direction: Direction): Translation {
                return VALUES[direction.id]
            }
        }
    }

    private enum class NeighborData(
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
            inline operator fun get(direction: Direction): NeighborData {
                return VALUES[direction.id]
            }
        }
    }

    private enum class NeighborOrientation(direction: Direction, flipped: Boolean) {
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
}