package me.luna.fastmc

import me.luna.fastmc.shared.font.IFontRendererWrapper
import me.luna.fastmc.shared.opengl.IGLWrapper
import me.luna.fastmc.shared.renderer.AbstractWorldRenderer
import me.luna.fastmc.shared.resource.IResourceManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object FastMcMod {
    val logger: Logger = LogManager.getLogger("Fast Minecraft")
    var isInitialized = false; private set

    lateinit var glWrapper: IGLWrapper; private set

    lateinit var resourceManager: IResourceManager; private set
    lateinit var worldRenderer: AbstractWorldRenderer; private set
    lateinit var fontRenderer: IFontRendererWrapper; private set

    fun initGLWrapper(glWrapper: IGLWrapper) {
        this.glWrapper = glWrapper
    }

    fun init(
        resourceManager: IResourceManager,
        worldRenderer: AbstractWorldRenderer,
        fontRenderer: IFontRendererWrapper,
    ) {
        if (isInitialized) error("Already initialized!")

        this.resourceManager = resourceManager
        this.worldRenderer = worldRenderer
        this.fontRenderer = fontRenderer

        isInitialized = true
    }

    fun reloadRenderer(
        resourceManager: IResourceManager,
        entityRenderer: AbstractWorldRenderer
    ) {
        if (isInitialized) {
            this.resourceManager.destroy()
        }

        this.resourceManager = resourceManager
        this.worldRenderer = entityRenderer
    }

    fun reloadResource(
        resourceManager: IResourceManager,
        entityRenderer: AbstractWorldRenderer,
    ) {
        if (isInitialized) {
            this.resourceManager.destroy()
        }

        this.resourceManager = resourceManager
        this.worldRenderer = entityRenderer

        isInitialized = false
    }

    fun onPostTick() {
        worldRenderer.onPostTick()
    }
}