package dev.fastmc.graphics

import dev.fastmc.graphics.renderer.EntityRendererImpl
import dev.fastmc.graphics.renderer.FontRendererWrapper
import dev.fastmc.graphics.renderer.TileEntityRendererImpl
import dev.fastmc.graphics.renderer.WorldRendererImpl
import dev.fastmc.graphics.terrain.TerrainRendererImpl
import dev.fastmc.graphics.util.Minecraft
import net.minecraft.resource.ResourceManager
import net.minecraft.resource.ResourceReloader
import net.minecraft.util.profiler.Profiler
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

object RendererReloader : ResourceReloader {
    private val mc = Minecraft.getInstance()

    override fun reload(
        synchronizer: ResourceReloader.Synchronizer,
        manager: ResourceManager,
        prepareProfiler: Profiler,
        applyProfiler: Profiler,
        prepareExecutor: Executor,
        applyExecutor: Executor
    ): CompletableFuture<Void> {
        return synchronizer.whenPrepared(null).thenAcceptAsync({
            val resourceManager = dev.fastmc.graphics.resource.ResourceManager(manager)
            FastMcMod.logger.info("Resource manager initialized")

            val fontRenderer = FontRendererWrapper(manager)
            FastMcMod.logger.info("Font Renderer initialized")
            fontRenderer.wrapped.unicode = mc.options.forceUnicodeFont

            val worldRenderer = WorldRendererImpl(mc, resourceManager)
            worldRenderer.init(
                TileEntityRendererImpl(mc, worldRenderer),
                EntityRendererImpl(mc, worldRenderer),
                TerrainRendererImpl(worldRenderer)
            )
            if (mc.world != null) {
                worldRenderer.terrainRenderer.updateChunkStorage(mc.options.viewDistance)
            }
            FastMcMod.logger.info("World renderer initialized")
            FastMcMod.init(resourceManager, worldRenderer, fontRenderer)
        }, applyExecutor)
    }
}