package dev.fastmc.graphics

import dev.fastmc.graphics.renderer.EntityInstancingRendererImpl
import dev.fastmc.graphics.renderer.FontRendererWrapper
import dev.fastmc.graphics.renderer.TileEntityInstancingRendererImpl
import dev.fastmc.graphics.renderer.WorldRendererImpl
import dev.fastmc.graphics.shared.font.IFontRendererWrapper
import dev.fastmc.graphics.terrain.TerrainRendererImpl
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.IResourceManager
import net.minecraftforge.client.resource.IResourceType
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener
import net.minecraftforge.client.resource.VanillaResourceType
import java.util.function.Predicate

object RendererReloader : ISelectiveResourceReloadListener {
    private val mc = Minecraft.getMinecraft()

    override fun onResourceManagerReload(
        resourceManager: IResourceManager,
        resourcePredicate: Predicate<IResourceType>
    ) {
        if (resourcePredicate.test(VanillaResourceType.MODELS) || resourcePredicate.test(VanillaResourceType.TEXTURES)) {
            val resourceManager1 = dev.fastmc.graphics.resource.ResourceManager(resourceManager)
            FastMcMod.logger.info("Resource manager initialized")

            val fontRenderer: IFontRendererWrapper = FontRendererWrapper(resourceManager)
            FastMcMod.logger.info("Font Renderer initialized")
            fontRenderer.wrapped.unicode = mc.gameSettings.forceUnicodeFont

            val worldRenderer = WorldRendererImpl(mc, resourceManager1)
            worldRenderer.init(
                TileEntityInstancingRendererImpl(mc, worldRenderer),
                EntityInstancingRendererImpl(mc, worldRenderer),
                TerrainRendererImpl(worldRenderer)
            )
            if (mc.world != null) {
                worldRenderer.terrainRenderer.updateChunkStorage(mc.gameSettings.renderDistanceChunks)
            }
            FastMcMod.init(resourceManager1, worldRenderer, fontRenderer)
            FastMcMod.logger.info("World renderer initialized")
        }
    }
}