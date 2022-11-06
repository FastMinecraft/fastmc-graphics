package me.luna.fastmc

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.luna.fastmc.shared.Config
import me.luna.fastmc.shared.FpsDisplay
import me.luna.fastmc.shared.font.IFontRendererWrapper
import me.luna.fastmc.shared.opengl.IGLWrapper
import me.luna.fastmc.shared.renderer.WorldRenderer
import me.luna.fastmc.shared.resource.IResourceManager
import me.luna.fastmc.shared.util.FastMcCoreScope
import me.luna.fastmc.shared.util.IProfiler
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
