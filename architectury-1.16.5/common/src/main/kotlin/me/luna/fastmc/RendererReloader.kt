package me.luna.fastmc

import me.luna.fastmc.renderer.EntityRenderer
import me.luna.fastmc.renderer.FontRendererWrapper
import me.luna.fastmc.renderer.TileEntityRenderer
import me.luna.fastmc.renderer.WorldRenderer
import me.luna.fastmc.shared.font.IFontRendererWrapper
import me.luna.fastmc.shared.resource.IResourceManager
import me.luna.fastmc.shared.util.EMPTY_RUNNABLE
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
        return CompletableFuture.runAsync(EMPTY_RUNNABLE, applyExecutor).thenCompose {
            val resourceManager: IResourceManager = me.luna.fastmc.resource.ResourceManager(mc)
            FastMcMod.logger.info("Resource manager initialized")

            val fontRenderer: IFontRendererWrapper = FontRendererWrapper(mc)
            FastMcMod.logger.info("Font Renderer initialized")
            fontRenderer.wrapped.unicode = mc.options.forceUnicodeFont

            val worldRenderer = WorldRenderer(mc, resourceManager)
            worldRenderer.init(
                TileEntityRenderer(mc, worldRenderer),
                EntityRenderer(mc, worldRenderer),
                TerrainRendererImpl(worldRenderer)
            )
            if (mc.world != null) {
                worldRenderer.terrainRenderer.updateChunkStorage(mc.options.viewDistance)
            }
            FastMcMod.logger.info("World renderer initialized")

            synchronizer.whenPrepared(Triple(resourceManager, worldRenderer, fontRenderer))
        }.thenAccept {
            FastMcMod.init(it.first, it.second, it.third)
        }
    }
}