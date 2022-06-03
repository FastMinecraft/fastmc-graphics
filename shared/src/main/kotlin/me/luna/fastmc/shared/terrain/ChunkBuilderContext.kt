@file:Suppress("NOTHING_TO_INLINE")

package me.luna.fastmc.shared.terrain

import it.unimi.dsi.fastutil.bytes.ByteArrayList
import it.unimi.dsi.fastutil.floats.FloatArrayList
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntArrays
import it.unimi.dsi.fastutil.ints.IntComparator
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import me.luna.fastmc.shared.opengl.impl.MappedBufferPool
import me.luna.fastmc.shared.renderbuilder.tileentity.info.ITileEntityInfo
import me.luna.fastmc.shared.util.*
import me.luna.fastmc.shared.util.collection.FastObjectArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

abstract class ContextProvider {
    private var bufferPool0: MappedBufferPool? = null
    var rebuildContextPool: ArrayPriorityObjectPool<ChunkBuilderTask, RebuildContext>? = null; private set
    var sortContextPool: ArrayPriorityObjectPool<ChunkBuilderTask, SortContext>? = null; private set
    var bufferContextPool: PriorityObjectPool<ChunkBuilderTask, BufferContext>? = null; private set

    val bufferPool get() = bufferPool0!!

    protected fun postConstruct() {
        bufferPool0 = MappedBufferPool(
            (4 * 1024).countTrailingZeroBits(),
            max(ParallelUtils.CPU_THREADS * 1024, 4096),
            ParallelUtils.CPU_THREADS * 128
        )

        rebuildContextPool = ArrayPriorityObjectPool(
            ParallelUtils.CPU_THREADS * 4,
            ChunkBuilderTask.unsynchronizedComparator()
        ) {
            newRebuildContext(it)
        }

        sortContextPool = ArrayPriorityObjectPool(
            ParallelUtils.CPU_THREADS * 8,
            ChunkBuilderTask.unsynchronizedComparator()
        ) {
            object : SortContext() {
                override fun release0() {
                    it.put(this)
                }
            }
        }

        bufferContextPool = PriorityObjectPool(
            object : SuspendObjectPool<BufferContext> {
                private val objectPool = ConcurrentObjectPool { BufferContextImpl() }

                override suspend fun get(): BufferContext {
                    val context = objectPool.get()
                    context.region0.set(bufferPool0!!.allocate())
                    return context
                }

                override fun tryGet(): BufferContext? {
                    val region = bufferPool0!!.tryAllocate() ?: return null
                    val context = objectPool.get()
                    context.region0.set(region)
                    return context
                }

                override fun put(element: BufferContext) {
                    objectPool.put(element as BufferContextImpl)
                }

                inner class BufferContextImpl : BufferContext() {
                    public override val region0: AtomicReference<MappedBufferPool.Region?>
                        get() = super.region0

                    public override fun release0() {
                        super.release0()
                        put(this)
                    }
                }
            },
            ChunkBuilderTask.unsynchronizedComparator()
        )
    }

    suspend inline fun getRebuildContext(task: ChunkBuilderTask): RebuildContext {
        val context = withContext(NonCancellable) { rebuildContextPool!!.get(task) }
        context.init(task)
        return context
    }

    suspend inline fun getSortContext(task: ChunkBuilderTask): SortContext {
        val context = withContext(NonCancellable) { sortContextPool!!.get(task) }
        context.init(task)
        return context
    }

    suspend inline fun getBufferContext(task: ChunkBuilderTask): BufferContext {
        val context = withContext(NonCancellable) { bufferContextPool!!.get(task) }
        context.init(task)
        return context
    }

    open fun update() {
        bufferPool.update()
    }

    open fun destroy() {
        bufferPool.destroy()
    }

    protected abstract fun newRebuildContext(pool: ArrayPriorityObjectPool<*, RebuildContext>): RebuildContext
}

sealed class Context {
    private val taskID = AtomicInteger(Int.MIN_VALUE)

    open fun init(task: ChunkBuilderTask) {
        taskID.set(task.id)
        task.registerResource(this)
    }

    protected abstract fun release0()

    fun release(task: ChunkBuilderTask) {
        if (taskID.compareAndSet(task.id, Int.MIN_VALUE)) {
            release0()
        }
    }
}

@Suppress("UNCHECKED_CAST")
abstract class RebuildContext(layerCount: Int) : Context() {
    @JvmField
    var chunkX = 0

    @JvmField
    var chunkY = 0

    @JvmField
    var chunkZ = 0

    @JvmField
    var blockX = 0

    @JvmField
    var blockY = 0

    @JvmField
    var blockZ = 0

    @JvmField
    var renderPosX = 0.0f

    @JvmField
    var renderPosY = 0.0f

    @JvmField
    var renderPosZ = 0.0f

    var activeLayer = 0
    val translucentChunkVertexBuilder = TranslucentChunkVertexBuilder()
    val layerVertexBuilderArray = Array(layerCount) {
        if (it == 3) translucentChunkVertexBuilder else ChunkVertexBuilder()
    }
    val layerVertexBuilder: ChunkVertexBuilder
        get() = layerVertexBuilderArray[activeLayer]

    @JvmField
    val random = Splitmix64Random(69420)

    @JvmField
    val occlusionDataBuilder = ChunkOcclusionData.Builder()

    @JvmField
    val tileEntityList = FastObjectArrayList<ITileEntityInfo<*>>()

    @JvmField
    val instancingTileEntityList = FastObjectArrayList<ITileEntityInfo<*>>()

    @JvmField
    val globalTileEntityList = FastObjectArrayList<ITileEntityInfo<*>>()

    @JvmField
    val brightnessArray = IntArray(4)

    @JvmField
    val lightMapUVArray = IntArray(4)

    @JvmField
    val flags = BooleanArray(3)

    @JvmField
    val boxDimension = FloatArray(Direction.values().size * 2)

    abstract val worldSnapshot: WorldSnapshot112<*, *, *>
    abstract val blockRenderer: BlockRenderer<*, *>

    suspend inline fun setActiveLayer(task: ChunkBuilderTask, index: Int) {
        layerVertexBuilderArray[index].initBuffer(task, task.renderer.contextProvider)
        activeLayer = index
    }

    override fun init(task: ChunkBuilderTask) {
        super.init(task)

        chunkX = task.chunkX
        chunkY = task.chunkY
        chunkZ = task.chunkZ

        blockX = 0
        blockY = 0
        blockZ = 0

        renderPosX = 0.0f
        renderPosY = 0.0f
        renderPosZ = 0.0f

        activeLayer = 0

        occlusionDataBuilder.clear()
    }

    override fun release0() {
        for (i in layerVertexBuilderArray.indices) {
            layerVertexBuilderArray[i].clearBuffer()
        }

        tileEntityList.clear()
        instancingTileEntityList.clear()
        globalTileEntityList.clear()
        worldSnapshot.clear()
    }

    abstract suspend fun renderChunk(task: RebuildTask)

    fun setupRenderPos() {
        renderPosX = (blockX and 15).toFloat()
        renderPosY = (blockY and 15).toFloat()
        renderPosZ = (blockZ and 15).toFloat()
    }

    inline fun calculateAO(
        thisX: Int,
        thisY: Int,
        thisZ: Int,
        direction: Int,
        shaded: Boolean
    ) {
        calculateAO(thisX, thisY, thisZ, Direction.VALUES[direction], shaded)
    }

    fun calculateAO(
        thisX: Int,
        thisY: Int,
        thisZ: Int,
        direction: Direction,
        shaded: Boolean
    ) {
        val worldSnapshot = worldSnapshot as WorldSnapshot112<Any, Any, Any>

        val aoX: Int
        val aoY: Int
        val aoZ: Int
        if (flags[0]) {
            aoX = thisX + direction.offsetX
            aoY = thisY + direction.offsetY
            aoZ = thisZ + direction.offsetZ
        } else {
            aoX = thisX
            aoY = thisY
            aoZ = thisZ
        }

        val neighborData = NeighborData[direction]
        val neighborFaces = neighborData.faces

        val a1X = aoX + neighborFaces[0].offsetX
        val a1Y = aoY + neighborFaces[0].offsetY
        val a1Z = aoZ + neighborFaces[0].offsetZ
        val aBits1 = worldSnapshot.getLightBits(a1X, a1Y, a1Z)
        val light1 = worldSnapshot.unpackLight(aBits1)
        val ao1 = worldSnapshot.unpackAoLight(aBits1)

        val a2X = aoX + neighborFaces[1].offsetX
        val a2Y = aoY + neighborFaces[1].offsetY
        val a2Z = aoZ + neighborFaces[1].offsetZ
        val aBits2 = worldSnapshot.getLightBits(a2X, a2Y, a2Z)
        val light2 = worldSnapshot.unpackLight(aBits2)
        val ao2 = worldSnapshot.unpackAoLight(aBits2)

        val a3X = aoX + neighborFaces[2].offsetX
        val a3Y = aoY + neighborFaces[2].offsetY
        val a3Z = aoZ + neighborFaces[2].offsetZ
        val aBits3 = worldSnapshot.getLightBits(a3X, a3Y, a3Z)
        val light3 = worldSnapshot.unpackLight(aBits3)
        val ao3 = worldSnapshot.unpackAoLight(aBits3)

        val a4X = aoX + neighborFaces[3].offsetX
        val a4Y = aoY + neighborFaces[3].offsetY
        val a4Z = aoZ + neighborFaces[3].offsetZ
        val aBits4 = worldSnapshot.getLightBits(a4X, a4Y, a4Z)
        val light4 = worldSnapshot.unpackLight(aBits4)
        val ao4 = worldSnapshot.unpackAoLight(aBits4)

        val b1x = aoX + neighborFaces[0].offsetX + neighborFaces[2].offsetX
        val b1y = aoY + neighborFaces[0].offsetY + neighborFaces[2].offsetY
        val b1z = aoZ + neighborFaces[0].offsetZ + neighborFaces[2].offsetZ
        val bBits1 = worldSnapshot.getLightBits(b1x, b1y, b1z)
        val aoCorner1 = worldSnapshot.unpackAoLight(bBits1)
        val lightCorner1 = worldSnapshot.unpackLight(bBits1)

        val b2x = aoX + neighborFaces[0].offsetX + neighborFaces[3].offsetX
        val b2y = aoY + neighborFaces[0].offsetY + neighborFaces[3].offsetY
        val b2z = aoZ + neighborFaces[0].offsetZ + neighborFaces[3].offsetZ
        val bBits2 = worldSnapshot.getLightBits(b2x, b2y, b2z)
        val aoCorner2 = worldSnapshot.unpackAoLight(bBits2)
        val lightCorner2 = worldSnapshot.unpackLight(bBits2)

        val b3x = aoX + neighborFaces[1].offsetX + neighborFaces[2].offsetX
        val b3y = aoY + neighborFaces[1].offsetY + neighborFaces[2].offsetY
        val b3z = aoZ + neighborFaces[1].offsetZ + neighborFaces[2].offsetZ
        val bBits3 = worldSnapshot.getLightBits(b3x, b3y, b3z)
        val aoCorner3 = worldSnapshot.unpackAoLight(bBits3)
        val lightCorner3 = worldSnapshot.unpackLight(bBits3)

        val b4x = aoX + neighborFaces[1].offsetX + neighborFaces[3].offsetX
        val b4y = aoY + neighborFaces[1].offsetY + neighborFaces[3].offsetY
        val b4z = aoZ + neighborFaces[1].offsetZ + neighborFaces[3].offsetZ
        val bBits4 = worldSnapshot.getLightBits(b4x, b4y, b4z)
        val aoCorner4 = worldSnapshot.unpackAoLight(bBits4)
        val lightCorner4 = worldSnapshot.unpackLight(bBits4)

        val bitsThis = worldSnapshot.getLightBits(aoX, aoY, aoZ)
        val lightDirection = if (flags[0]) {
            worldSnapshot.unpackLight(bitsThis)
        } else {
            val ldX = thisX + direction.offsetX
            val ldY = thisY + direction.offsetY
            val ldZ = thisZ + direction.offsetZ
            val directionBits = worldSnapshot.getLightBits(ldX, ldY, ldZ)
            if (!worldSnapshot.unpackIsOpaqueFullCube(directionBits)) {
                worldSnapshot.unpackLight(directionBits)
            } else {
                worldSnapshot.unpackLight(worldSnapshot.getLightBits(thisX, thisY, thisZ))
            }
        }

        val aoThis = worldSnapshot.unpackAoLight(bitsThis)
        val translation = Translation[direction]
        val corners = translation.corners

        if (flags[1]) {
            val light311T = (ao3 + ao1 + aoCorner1 + aoThis) shr 2
            val light412T = (ao4 + ao1 + aoCorner2 + aoThis) shr 2
            val light323T = (ao3 + ao2 + aoCorner3 + aoThis) shr 2
            val light424T = (ao4 + ao2 + aoCorner4 + aoThis) shr 2

            val orientation1 = neighborData.orientation1
            val size101 =
                (boxDimension[orientation1[0].shape] * boxDimension[orientation1[1].shape] * 255.0f).toInt()
            val size123 =
                (boxDimension[orientation1[2].shape] * boxDimension[orientation1[3].shape] * 255.0f).toInt()
            val size145 =
                (boxDimension[orientation1[4].shape] * boxDimension[orientation1[5].shape] * 255.0f).toInt()
            val size167 =
                (boxDimension[orientation1[6].shape] * boxDimension[orientation1[7].shape] * 255.0f).toInt()

            val orientation2 = neighborData.orientation2
            val size201 =
                (boxDimension[orientation2[0].shape] * boxDimension[orientation2[1].shape] * 255.0f).toInt()
            val size223 =
                (boxDimension[orientation2[2].shape] * boxDimension[orientation2[3].shape] * 255.0f).toInt()
            val size245 =
                (boxDimension[orientation2[4].shape] * boxDimension[orientation2[5].shape] * 255.0f).toInt()
            val size267 =
                (boxDimension[orientation2[6].shape] * boxDimension[orientation2[7].shape] * 255.0f).toInt()

            val orientation3 = neighborData.orientation3
            val size301 =
                (boxDimension[orientation3[0].shape] * boxDimension[orientation3[1].shape] * 255.0f).toInt()
            val size323 =
                (boxDimension[orientation3[2].shape] * boxDimension[orientation3[3].shape] * 255.0f).toInt()
            val size345 =
                (boxDimension[orientation3[4].shape] * boxDimension[orientation3[5].shape] * 255.0f).toInt()
            val size367 =
                (boxDimension[orientation3[6].shape] * boxDimension[orientation3[7].shape] * 255.0f).toInt()

            val orientation4 = neighborData.orientation4
            val size401 =
                (boxDimension[orientation4[0].shape] * boxDimension[orientation4[1].shape] * 255.0f).toInt()
            val size423 =
                (boxDimension[orientation4[2].shape] * boxDimension[orientation4[3].shape] * 255.0f).toInt()
            val size445 =
                (boxDimension[orientation4[4].shape] * boxDimension[orientation4[5].shape] * 255.0f).toInt()
            val size467 =
                (boxDimension[orientation4[6].shape] * boxDimension[orientation4[7].shape] * 255.0f).toInt()

            brightnessArray[corners[0]] =
                (light412T * size101 + light311T * size123 + light323T * size145 + light424T * size167) shr 8
            brightnessArray[corners[1]] =
                (light412T * size201 + light311T * size223 + light323T * size245 + light424T * size267) shr 8
            brightnessArray[corners[2]] =
                (light412T * size301 + light311T * size323 + light323T * size345 + light424T * size367) shr 8
            brightnessArray[corners[3]] =
                (light412T * size401 + light311T * size423 + light323T * size445 + light424T * size467) shr 8

            val light311D = combineLights(light3, light1, lightCorner1, lightDirection)
            val light412D = combineLights(light4, light1, lightCorner2, lightDirection)
            val light323D = combineLights(light3, light2, lightCorner3, lightDirection)
            val light424D = combineLights(light4, light2, lightCorner4, lightDirection)

            lightMapUVArray[corners[0]] =
                interpolateLight(light412D, light311D, light323D, light424D, size101, size123, size145, size167)
            lightMapUVArray[corners[1]] =
                interpolateLight(light412D, light311D, light323D, light424D, size201, size223, size245, size267)
            lightMapUVArray[corners[2]] =
                interpolateLight(light412D, light311D, light323D, light424D, size301, size323, size345, size367)
            lightMapUVArray[corners[3]] =
                interpolateLight(light412D, light311D, light323D, light424D, size401, size423, size445, size467)
        } else {
            lightMapUVArray[corners[0]] = combineLights(light4, light1, lightCorner2, lightDirection)
            lightMapUVArray[corners[1]] = combineLights(light3, light1, lightCorner1, lightDirection)
            lightMapUVArray[corners[2]] = combineLights(light3, light2, lightCorner3, lightDirection)
            lightMapUVArray[corners[3]] = combineLights(light4, light2, lightCorner4, lightDirection)

            brightnessArray[corners[0]] = (ao4 + ao1 + aoCorner2 + aoThis) shr 2
            brightnessArray[corners[1]] = (ao3 + ao1 + aoCorner1 + aoThis) shr 2
            brightnessArray[corners[2]] = (ao3 + ao2 + aoCorner3 + aoThis) shr 2
            brightnessArray[corners[3]] = (ao4 + ao2 + aoCorner4 + aoThis) shr 2
        }

        val globalBrightness = worldSnapshot.getWorldBrightness(direction.ordinal, shaded)
        for (i in brightnessArray.indices) {
            brightnessArray[i] = (brightnessArray[i] * globalBrightness) shr 8
        }
    }

    @Suppress("NAME_SHADOWING")
    private inline fun combineLights(light1: Int, light2: Int, light3: Int, light4: Int): Int {
        val light1 = if (light1 == 0) light4 else light1
        val light2 = if (light2 == 0) light4 else light2
        val light3 = if (light3 == 0) light4 else light3

        val skyLight = ((light1 shr 8) + (light2 shr 8) + (light3 shr 8) + (light4 shr 8)) shr 2
        val blockLight = ((light1 and 255) + (light2 and 255) + (light3 and 255) + (light4 and 255)) shr 2

        return (skyLight shl 8) or (blockLight)
    }

    private inline fun interpolateLight(
        light1: Int,
        light2: Int,
        light3: Int,
        light4: Int,
        size1: Int,
        size2: Int,
        size3: Int,
        size4: Int
    ): Int {
        val skyLight = (((light1 shr 8) * size1
            + (light2 shr 8) * size2
            + (light3 shr 8) * size3
            + (light4 shr 8) * size4) shr 8) and 255

        val blockLight = (((light1 and 255) * size1
            + (light2 and 255) * size2
            + (light3 and 255) * size3
            + (light4 and 255) * size4) shr 8) and 255

        return (skyLight shl 8) or blockLight
    }

    private enum class Translation(
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
                return VALUES[direction.ordinal]
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
            arrayOf(Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH),
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
            arrayOf(Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH),
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
            arrayOf(Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST),
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
            arrayOf(Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP),
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
            arrayOf(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH),
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
            arrayOf(Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH),
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
                return VALUES[direction.ordinal]
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

        val shape = direction.ordinal + if (flipped) Direction.values().size else 0
    }
}

@Suppress("UNCHECKED_CAST")
abstract class SortContext : Context() {
    val tempIndexData = ByteArrayList(ByteArrayList.DEFAULT_INITIAL_CAPACITY + 1)
    val tempQuadCenter = FloatArrayList(FloatArrayList.DEFAULT_INITIAL_CAPACITY + 1)

    private val distanceList = FloatArrayList(FloatArrayList.DEFAULT_INITIAL_CAPACITY + 1)
    private val indexList = IntArrayList(IntArrayList.DEFAULT_INITIAL_CAPACITY + 1)
    private val sortSuppIndexList = IntArrayList(IntArrayList.DEFAULT_INITIAL_CAPACITY + 1)

    inline fun sortQuads(task: ChunkBuilderTask, data: TranslucentData): TranslucentData {
        return sortQuads(task, data.indexData, data.quadCenter, data.quadCenter.size / 3)
    }

    fun sortQuads(
        task: ChunkBuilderTask,
        indexData: ByteArray,
        quadCenter: FloatArray,
        quadCount: Int
    ): TranslucentData {
        distanceList.ensureCapacity(quadCount)
        indexList.ensureCapacity(quadCount)
        sortSuppIndexList.ensureCapacity(quadCount)

        val distanceArray = distanceList.elements()
        val indexArray = indexList.elements()
        val sortSuppIndexArray = sortSuppIndexList.elements()

        for (i in 0 until quadCount) {
            val quadCenterIndex = i * 3
            distanceArray[i] = -distanceSq(
                quadCenter[quadCenterIndex],
                quadCenter[quadCenterIndex + 1],
                quadCenter[quadCenterIndex + 2],
                task.relativeCameraX,
                task.relativeCameraY,
                task.relativeCameraZ
            )
            indexArray[i] = i
            sortSuppIndexArray[i] = i
        }

        val comparator = object : IntComparator {
            override fun compare(k1: Int, k2: Int): Int {
                return distanceArray[k1].compareTo(distanceArray[k2])
            }
        }

        IntArrays.mergeSort(
            indexArray,
            0,
            quadCount,
            comparator,
            sortSuppIndexArray
        )

        val newIndexData = ByteArray(quadCount * 6 * 4)
        val newQuadCenter = FloatArray(quadCount * 3)

        for (i in 0 until quadCount) {
            val sortedQuadIndex = indexArray[i]
            System.arraycopy(indexData, sortedQuadIndex * 24, newIndexData, i * 24, 24)
            System.arraycopy(quadCenter, sortedQuadIndex * 3, newQuadCenter, i * 3, 3)
        }

        return TranslucentData(newIndexData, newQuadCenter)
    }
}

@Suppress("UNCHECKED_CAST")
abstract class BufferContext : Context() {
    protected open val region0 = AtomicReference<MappedBufferPool.Region?>()
    val region get() = region0.get()!!

    override fun release0() {
        region0.getAndSet(null)?.release()
    }
}