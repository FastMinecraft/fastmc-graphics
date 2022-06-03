package me.luna.fastmc

import me.luna.fastmc.renderer.EntityRenderer
import me.luna.fastmc.renderer.FontRendererWrapper
import me.luna.fastmc.renderer.TileEntityRenderer
import me.luna.fastmc.renderer.WorldRenderer
import me.luna.fastmc.shared.font.IFontRendererWrapper
import me.luna.fastmc.terrain.TerrainRendererImpl
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
            val resourceManager1 = me.luna.fastmc.resource.ResourceManager(resourceManager)
            FastMcMod.logger.info("Resource manager initialized")

            val fontRenderer: IFontRendererWrapper = FontRendererWrapper(resourceManager)
            FastMcMod.logger.info("Font Renderer initialized")
            fontRenderer.wrapped.unicode = mc.gameSettings.forceUnicodeFont

            val worldRenderer = WorldRenderer(mc, resourceManager1)
            worldRenderer.init(
                TileEntityRenderer(mc, worldRenderer),
                EntityRenderer(mc, worldRenderer),
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