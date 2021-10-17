package me.xiaro.fastmc

import me.xiaro.fastmc.font.IFontRendererWrapper
import me.xiaro.fastmc.opengl.IGLWrapper
import me.xiaro.fastmc.resource.IResourceManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object FastMcMod {
    val logger: Logger = LogManager.getLogger("Fast Minecraft")
    var isInitialized = false; private set

    lateinit var glWrapper: IGLWrapper; private set

    lateinit var resourceManager: IResourceManager; private set
    lateinit var entityRenderer: AbstractEntityRenderer; private set
    lateinit var fontRenderer: IFontRendererWrapper; private set

    fun initGLWrapper(glWrapper: IGLWrapper) {
        this.glWrapper = glWrapper
    }

    fun init(resourceManager: IResourceManager, entityRenderer: AbstractEntityRenderer, fontRenderer: IFontRendererWrapper) {
        if (isInitialized) error("Already initialized!")

        this.resourceManager = resourceManager
        this.entityRenderer = entityRenderer
        this.fontRenderer = fontRenderer

        isInitialized = true
    }

    fun reloadEntityRenderer(resourceManager: IResourceManager, entityRenderer: AbstractEntityRenderer) {
        if (isInitialized) {
            this.resourceManager.destroy()
        }

        this.resourceManager = resourceManager
        this.entityRenderer = entityRenderer
    }

    fun reloadResource(resourceManager: IResourceManager, entityRenderer: AbstractEntityRenderer, fontRenderer: IFontRendererWrapper) {
        if (isInitialized) {
            this.resourceManager.destroy()
            this.fontRenderer.destroy()
        }

        this.resourceManager = resourceManager
        this.entityRenderer = entityRenderer
        this.fontRenderer = fontRenderer
    }

    fun onPostTick() {
        entityRenderer.onPostTick()
    }
}