package me.luna.fastmc.shared

import me.luna.fastmc.FastMcMod
import me.luna.fastmc.shared.font.FontRenderer
import me.luna.fastmc.shared.util.ColorARGB
import org.joml.Matrix4f
import kotlin.math.max
import kotlin.math.min

object FpsDisplay {
    private var lastRender = System.nanoTime()
    private val shortFps = ArrayList<Pair<Long, Int>>()
    private val longFps = ArrayList<Pair<Long, Int>>()

    private var renderString: CharSequence = ""
    private val projection = Matrix4f()
    private val modelView = Matrix4f()

    fun onPostRenderTick() {
        val current = System.nanoTime()

        shortFps.removeIf {
            it.first <= current
        }

        shortFps.add(current + 1_000_000_000 to (current - lastRender).toInt())

        lastRender = current
    }

    fun onPostTick() {
        val current = System.nanoTime()

        val millisSum = shortFps.sumOf {
            it.second.toDouble() / 1_000_000.0
        }
        val renderTime = millisSum / shortFps.size
        val fps = (1000.0 / renderTime).toInt()

        longFps.removeIf {
            it.first <= current
        }
        longFps.add(current + 5_000_000_000 to fps)

        val stringBuilder = StringBuilder()
        stringBuilder.append("FPS ")
        stringBuilder.append(fps)

        if (FastMcMod.config.avgFps) {
            var avg = 0

            for ((_, value) in longFps) {
                avg += value
            }

            avg /= longFps.size
            stringBuilder.append("  AVG ")
            stringBuilder.append(avg)
        }

        renderString = stringBuilder
    }

    fun render(width: Float, height: Float, fontRenderer: FontRenderer) {
        if (FastMcMod.config.fps) {
            projection.identity()
            modelView.identity()

            projection.ortho(0.0f, width, height, 0.0f, 1000.0f, 3000.0f)
            modelView.translate(0.0f, 0.0f, -2000.0f)

            fontRenderer.drawString(projection, modelView, renderString, 2.0f, 2.0f, ColorARGB(255, 255, 255, 255), 1.0f, true)
        }
    }
}