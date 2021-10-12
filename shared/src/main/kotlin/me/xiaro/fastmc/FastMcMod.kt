package me.xiaro.fastmc

import me.xiaro.fastmc.opengl.IGLWrapper
import me.xiaro.fastmc.resource.IResourceManager
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object FastMcMod {
    val logger: Logger = LogManager.getLogger("Fast Minecraft")
    var initialized = false; private set

    lateinit var glWrapper: IGLWrapper; private set

    lateinit var entityRenderer: AbstractEntityRenderer; private set
    lateinit var resourceManager: IResourceManager; private set

    fun initGLWrapper(glWrapper: IGLWrapper) {
        this.glWrapper = glWrapper
    }

    fun init(resourceManager: IResourceManager, entityRenderer: AbstractEntityRenderer) {
        this.resourceManager = resourceManager
        this.entityRenderer = entityRenderer
        initialized = true
    }

    fun reloadResource(resourceManager: IResourceManager, entityRenderer: AbstractEntityRenderer) {
        if (!initialized) return

        this.resourceManager.destroy()
        this.resourceManager = resourceManager
        this.entityRenderer = entityRenderer
    }

    fun onPostTick() {
        entityRenderer.onPostTick()
    }
}