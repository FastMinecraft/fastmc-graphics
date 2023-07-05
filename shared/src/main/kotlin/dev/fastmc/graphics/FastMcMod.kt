package dev.fastmc.graphics

import dev.fastmc.graphics.shared.Config
import dev.fastmc.graphics.shared.FpsDisplay
import dev.fastmc.graphics.shared.font.IFontRendererWrapper
import dev.fastmc.graphics.shared.renderer.WorldRenderer
import dev.fastmc.graphics.shared.resource.IResourceManager
import dev.fastmc.graphics.shared.util.FastMcCoreScope
import dev.fastmc.graphics.shared.util.IProfiler
import dev.luna5ama.glwrapper.api.GLWrapper
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import java.util.*

object FastMcMod {
    val logger: Logger = LogManager.getLogger("Fast Minecraft")
    var isInitialized = false; private set

    var config = Config(Properties().apply {
        runCatching {
            File("fastmc-graphics/config.properties").inputStream().use { load(it) }
        }
    }); private set

    var lightMapUnit = 1; private set

    lateinit var profiler: IProfiler; private set

    lateinit var resourceManager: IResourceManager; private set
    lateinit var worldRenderer: WorldRenderer; private set
    lateinit var fontRenderer: IFontRendererWrapper; private set

    init{
        File("fastmc-graphics").mkdir()
        Runtime.getRuntime().addShutdownHook(Thread {
            File("fastmc-graphics/config.properties").outputStream().use {
                config.toProperties().store(it, null)
            }
        })
    }

    fun initGLWrapper(glWrapper: GLWrapper, lightMapUnit: Int) {
        GLWrapper.init(glWrapper)
        this.lightMapUnit = lightMapUnit
    }

    fun initProfiler(profiler: IProfiler) {
        this.profiler = profiler
    }

    fun init(
        resourceManager: IResourceManager,
        worldRenderer: WorldRenderer,
        fontRenderer: IFontRendererWrapper
    ) {
        if (isInitialized) {
            this.resourceManager.destroy()
            this.worldRenderer.destroy()
            this.fontRenderer.destroy()
        }

        this.resourceManager = resourceManager
        this.worldRenderer = worldRenderer
        this.fontRenderer = fontRenderer
        isInitialized = true
    }

    fun onPostTick() {
        if (isInitialized) {
            runBlocking {
                worldRenderer.onPostTick(this.coroutineContext, this)
                launch(FastMcCoreScope.context) { FpsDisplay.onPostTick() }
            }
        }
    }
}