package me.luna.fastmc.shared

import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.font.FontRenderer
import me.luna.fastmc.shared.util.ColorARGB
import org.joml.Matrix4f

object FpsDisplay {
    private var lastRender = System.nanoTime()
    private val shortFps = ArrayList<Pair<Long, Int>>()
    private val longFps = ArrayList<Pair<Long, Int>>()
    private val chunkUpdate = ArrayList<Pair<Long, Int>>()

    var fpsValue = 0; private set
    var chunkUpdates = 0; private set

    var lastFrameTime = 16_666_667; private set
    var nanoFrameTime = 16_666_667; private set

    private var renderString: CharSequence = ""
    private val projection = Matrix4f()
    private val modelView = Matrix4f()

    fun onChunkUpdate(count: Int) {
        chunkUpdate.add(System.nanoTime() + 1_000_000_000 to count)
    }

    fun onPostRenderTick() {
        val current = System.nanoTime()
        val frameTime = (current - lastRender).toInt()
        lastFrameTime = frameTime
        shortFps.add(current + 1_000_000_000 to frameTime)
        lastRender = current
    }

    fun onPostTick() {
        val current = System.nanoTime()

        shortFps.removeIf {
            it.first <= current
        }
        longFps.removeIf {
            it.first <= current
        }
        chunkUpdate.removeIf {
            it.first <= current
        }

        val millisSum = shortFps.sumOf {
            it.second.toDouble()
        }
        val frameTime = millisSum / shortFps.size
        val fps = (1_000_000_000.0 / frameTime).toInt()
        this.nanoFrameTime = frameTime.toInt()
        fpsValue = fps

        longFps.add(current + 5_000_000_000 to frameTime.toInt())

        val stringBuilder = StringBuilder()
        stringBuilder.append("FPS ")
        stringBuilder.append(fps)

        if (FastMcMod.config.avgFps) {
            var avg = longFps.sumOf {
                it.second.toDouble()
            }

            avg /= longFps.size
            stringBuilder.append("  AVG ")
            stringBuilder.append((1_000_000_000.0 / avg).toInt())
            stringBuilder.append(" (%.1f ms)".format(avg / 1_000_000.0))
        }

        if (FastMcMod.config.maxFrameTime) {
            var max = 0

            for ((_, value) in shortFps) {
                if (value > max) max = value
            }

            stringBuilder.append("  MAX ")
            stringBuilder.append("%.1f ms".format(max.toDouble() / 1_000_000.0))
        }

        if (FastMcMod.config.chunkUpdate) {
            val updates = chunkUpdate.sumOf {
                it.second
            }
            chunkUpdates = updates

            stringBuilder.append("  C ")
            stringBuilder.append(updates)
        }

        renderString = stringBuilder
    }

    fun render(width: Float, height: Float, fontRenderer: FontRenderer) {
        if (FastMcMod.config.fps) {
            projection.identity()
            modelView.identity()

            projection.ortho(0.0f, width, height, 0.0f, 1000.0f, 3000.0f)
            modelView.translate(0.0f, 0.0f, -2000.0f)

            fontRenderer.drawString(
                projection,
                modelView,
                renderString,
                2.0f,
                2.0f,
                ColorARGB(255, 255, 255, 255),
                1.0f,
                true
            )
        }
    }
}