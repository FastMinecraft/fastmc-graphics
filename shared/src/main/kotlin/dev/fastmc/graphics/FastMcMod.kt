package dev.fastmc.graphics

import dev.fastmc.graphics.shared.Config
import dev.fastmc.graphics.shared.FpsDisplay
import dev.fastmc.graphics.shared.font.IFontRendererWrapper
import dev.fastmc.graphics.shared.opengl.IGLWrapper
import dev.fastmc.graphics.shared.renderer.WorldRenderer
import dev.fastmc.graphics.shared.resource.IResourceManager
import dev.fastmc.graphics.shared.util.FastMcCoreScope
import dev.fastmc.graphics.shared.util.IProfiler
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object FastMcMod {
    val logger: Logger = LogManager.getLogger("Fast Minecraft")
    var isInitialized = false; private set

    var config = Config(); private set

    lateinit var glWrapper: IGLWrapper; private set

    lateinit var profiler: IProfiler; private set

    lateinit var resourceManager: IResourceManager; private set
    lateinit var worldRenderer: WorldRenderer; private set
    lateinit var fontRenderer: IFontRendererWrapper; private set

    fun initGLWrapper(glWrapper: IGLWrapper) {
        this.glWrapper = glWrapper
        logger.info("GL Wrapper initialized")
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