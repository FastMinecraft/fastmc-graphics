package me.luna.fastmc

import me.luna.fastmc.renderer.EntityRendererImpl
import me.luna.fastmc.renderer.FontRendererWrapper
import me.luna.fastmc.renderer.TileEntityRendererImpl
import me.luna.fastmc.renderer.WorldRendererImpl
import me.luna.fastmc.terrain.TerrainRendererImpl
import me.luna.fastmc.util.Minecraft
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
            val resourceManager = me.luna.fastmc.resource.ResourceManager(manager)
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