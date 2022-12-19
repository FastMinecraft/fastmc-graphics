package dev.fastmc.graphics.mixin.core.render;

import dev.fastmc.common.collection.FastObjectArrayList;
import dev.fastmc.graphics.FastMcMod;
import dev.fastmc.graphics.renderer.EntityRendererImpl;
import dev.fastmc.graphics.shared.renderer.WorldRenderer;
import dev.fastmc.graphics.shared.terrain.RenderChunkStorage;
import dev.fastmc.graphics.shared.terrain.TerrainRenderer;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.IRenderChunkFactory;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(value = RenderGlobal.class, priority = Integer.MAX_VALUE)
public abstract class MixinCoreRenderGlobal {
    @Shadow
    IRenderChunkFactory renderChunkFactory;
    @Shadow
    @Final
    private Minecraft mc;
    @Shadow
    private int renderEntitiesStartupCounter;
    @Shadow
    private WorldClient world;
    @Shadow
    private int renderDistanceChunks;
    @Shadow
    private boolean vboEnabled;
    @Shadow
    private ChunkRenderContainer renderContainer;
    @Shadow
    private ViewFrustum viewFrustum;
    @Shadow
    private ChunkRenderDispatcher renderDispatcher;
    @Mutable
    @Shadow
    @Final
    private Set<TileEntity> setTileEntities;
    @Shadow
    private Set<RenderChunk> chunksToUpdate;
    @Shadow
    @Final
    private RenderManager renderManager;
    @Shadow
    private int countEntitiesTotal;
    @Shadow
    private int countEntitiesRendered;
    @Shadow
    private int countEntitiesHidden;
    @Shadow
    @Final
    private Map<Integer, DestroyBlockProgress> damagedBlocks;
    @Shadow
    private ShaderGroup entityOutlineShader;
    @Shadow
    private Framebuffer entityOutlineFramebuffer;
    @Shadow
    private boolean entityOutlinesRendered;
    @Shadow
    private List<RenderGlobal.ContainerLocalRenderInformation> renderInfos;
    @Shadow
    private int cloudTickCounter;
    @Shadow
    @Final
    private Set<BlockPos> setLightUpdates;

    @Shadow
    protected abstract void generateStars();

    @Shadow
    protected abstract void generateSky();

    @Shadow
    protected abstract void generateSky2();

    @Shadow
    protected abstract void preRenderDamagedBlocks();

    @Shadow
    protected abstract void postRenderDamagedBlocks();

    @Shadow
    protected abstract boolean isRenderEntityOutlines();

    @Shadow
    protected abstract boolean isOutlineActive(Entity entityIn, Entity viewer, ICamera camera);

    @Shadow
    protected abstract void cleanupDamagedBlocks(Iterator<DestroyBlockProgress> iteratorIn);

    private final FastObjectArrayList<Entity> outlineRenderEntityList = new FastObjectArrayList<>();
    private final FastObjectArrayList<Entity> multiPassRenderEntityList = new FastObjectArrayList<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void Inject$init$RETURN(Minecraft p_i1249_1_, CallbackInfo ci) {
        this.renderContainer = null;
        this.viewFrustum = null;
        this.renderChunkFactory = null;
        this.renderDispatcher = null;
        this.setTileEntities = ObjectSets.emptySet();
        this.chunksToUpdate = ObjectSets.emptySet();
        this.renderInfos = ObjectLists.emptyList();
    }

    /**
     * @author Luna
     * @reason Render override
     */
    @Overwrite
    public void loadRenderers() {
        if (this.world != null) {
            Blocks.LEAVES.setGraphicsLevel(this.mc.gameSettings.fancyGraphics);
            Blocks.LEAVES2.setGraphicsLevel(this.mc.gameSettings.fancyGraphics);
            this.renderDistanceChunks = this.mc.gameSettings.renderDistanceChunks;

            boolean flag = this.vboEnabled;
            this.vboEnabled = OpenGlHelper.useVbo();

            if (flag != this.vboEnabled) {
                this.generateStars();
                this.generateSky();
                this.generateSky2();
            }

            TerrainRenderer terrainRenderer = getTerrainRenderer();
            terrainRenderer.clear();
            terrainRenderer.updateChunkStorage(this.mc.gameSettings.renderDistanceChunks);
            terrainRenderer.reload();
        }
    }

    /**
     * @author Luna
     * @reason Debug
     */
    @Overwrite
    public String getDebugInfoRenders() {
        return getTerrainRenderer().getDebugInfoString();
    }

    /**
     * @author Luna
     * @reason Render override
     */
    @Overwrite
    private void markBlocksForUpdate(
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ,
        boolean updateImmediately
    ) {
        minX >>= 4;
        minY >>= 4;
        minZ >>= 4;

        maxX >>= 4;
        maxY >>= 4;
        maxZ >>= 4;

        RenderChunkStorage chunkStorage = getTerrainRenderer().getChunkStorage();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    dev.fastmc.graphics.shared.terrain.RenderChunk renderChunk =
                        chunkStorage.getRenderChunkByChunk(x, y, z);
                    if (renderChunk != null) {
                        renderChunk.isDirty = true;
                    }
                }
            }
        }
    }

    /**
     * @author Luna
     * @reason Render override
     */
    @Overwrite
    public void updateClouds() {
        ++this.cloudTickCounter;

        if (this.cloudTickCounter % 20 == 0) {
            this.cleanupDamagedBlocks(this.damagedBlocks.values().iterator());
        }

        if (!this.setLightUpdates.isEmpty()) {
            Iterator<BlockPos> iterator = this.setLightUpdates.iterator();

            while (iterator.hasNext()) {
                BlockPos blockpos = iterator.next();
                iterator.remove();
                int k1 = blockpos.getX();
                int l1 = blockpos.getY();
                int i2 = blockpos.getZ();
                this.markBlocksForUpdate(k1 - 1, l1 - 1, i2 - 1, k1 + 1, l1 + 1, i2 + 1, false);
            }
        }
    }

    /**
     * @author Luna
     * @reason Render override
     */
    @Overwrite
    public boolean hasNoChunkUpdates() {
        return getTerrainRenderer().getChunkBuilder().getTotalTaskCount() == 0;
    }

    /**
     * @author Luna
     * @reason Render override
     */
    @Overwrite
    protected int getRenderedChunks() {
        int count = 0;

        dev.fastmc.graphics.shared.terrain.RenderChunk[] chunkArray = getTerrainRenderer().getChunkStorage().renderChunkArray;
        for (int i = 0; i < chunkArray.length; i++) {
            if (chunkArray[i].isBuilt) {
                count++;
            }
        }

        return count;
    }

    /**
     * @author Luna
     * @reason Render override
     */
    @Overwrite
    public void renderEntities(Entity renderViewEntity, ICamera camera, float partialTicks) {
        int pass = net.minecraftforge.client.MinecraftForgeClient.getRenderPass();
        if (this.renderEntitiesStartupCounter > 0) {
            if (pass > 0) return;
            --this.renderEntitiesStartupCounter;
        } else {
            this.world.profiler.endStartSection("entities");
            this.world.profiler.startSection("setup");
            TileEntityRendererDispatcher.instance.prepare(
                this.world,
                this.mc.getTextureManager(),
                this.mc.fontRenderer,
                renderViewEntity,
                this.mc.objectMouseOver,
                partialTicks
            );

            this.renderManager.cacheActiveRenderInfo(
                this.world,
                this.mc.fontRenderer,
                renderViewEntity,
                this.mc.pointedEntity,
                this.mc.gameSettings,
                partialTicks
            );

            if (pass == 0) {
                this.countEntitiesTotal = 0;
                this.countEntitiesRendered = 0;
                this.countEntitiesHidden = 0;
            }

            double renderPosX = renderViewEntity.lastTickPosX + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * (double) partialTicks;
            double renderPosY = renderViewEntity.lastTickPosY + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * (double) partialTicks;
            double renderPosZ = renderViewEntity.lastTickPosZ + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * (double) partialTicks;
            TileEntityRendererDispatcher.staticPlayerX = renderPosX;
            TileEntityRendererDispatcher.staticPlayerY = renderPosY;
            TileEntityRendererDispatcher.staticPlayerZ = renderPosZ;
            this.renderManager.setRenderPosition(renderPosX, renderPosY, renderPosZ);
            this.mc.entityRenderer.enableLightmap();

            this.world.profiler.endStartSection("weather");
            List<Entity> list = this.world.getLoadedEntityList();
            if (pass == 0) {
                this.countEntitiesTotal = list.size();
            }

            for (int i = 0; i < this.world.weatherEffects.size(); ++i) {
                Entity entity1 = this.world.weatherEffects.get(i);
                if (!entity1.shouldRenderInPass(pass)) continue;
                ++this.countEntitiesRendered;

                if (entity1.isInRangeToRender3d(renderPosX, renderPosY, renderPosZ)) {
                    this.renderManager.renderEntityStatic(entity1, partialTicks, false);
                }
            }

            this.world.profiler.endStartSection("vanilla");
            WorldRenderer worldRenderer = FastMcMod.INSTANCE.getWorldRenderer();
            EntityRendererImpl entityRenderer = (EntityRendererImpl) worldRenderer.getEntityRenderer();
            BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
            List<Entity> loadedEntityList = this.world.loadedEntityList;
            boolean renderOutline = pass == 0 && this.isRenderEntityOutlines();

            for (int i = 0; i < loadedEntityList.size(); i++) {
                Entity entity = loadedEntityList.get(i);
                if (!entity.shouldRenderInPass(pass)) continue;

                if (this.renderManager.shouldRender(entity, camera, renderPosX, renderPosY, renderPosZ)
                    || entity.isRidingOrBeingRiddenBy(this.mc.player)) {

                    if ((entity != renderViewEntity
                        || this.mc.gameSettings.thirdPersonView != 0
                        || renderViewEntity instanceof EntityLivingBase && ((EntityLivingBase) renderViewEntity).isPlayerSleeping())
                        && (entity.posY < 0.0D || entity.posY >= 256.0D || this.world.isBlockLoaded(mutableBlockPos.setPos(
                        entity)))) {

                        this.countEntitiesRendered++;
                        if (!entityRenderer.hasRenderer(entity)) {
                            this.renderManager.renderEntityStatic(entity, partialTicks, false);
                        }

                        if (renderOutline && this.isOutlineActive(entity, renderViewEntity, camera)) {
                            outlineRenderEntityList.add(entity);
                        }

                        if (this.renderManager.isRenderMultipass(entity)) {
                            multiPassRenderEntityList.add(entity);
                        }
                    }
                }
            }

            this.world.profiler.endStartSection("fastMinecraft");
            worldRenderer.preRender(partialTicks);
            entityRenderer.render();
            worldRenderer.postRender();

            if (!multiPassRenderEntityList.isEmpty()) {
                for (int i = 0; i < multiPassRenderEntityList.size(); i++) {
                    this.renderManager.renderMultipass(multiPassRenderEntityList.get(i), partialTicks);
                }
            }

            if (!outlineRenderEntityList.isEmpty() || this.entityOutlinesRendered) {
                this.world.profiler.endStartSection("entityOutlines");
                this.entityOutlineFramebuffer.framebufferClear();
                this.entityOutlinesRendered = !outlineRenderEntityList.isEmpty();

                if (!outlineRenderEntityList.isEmpty()) {
                    GlStateManager.depthFunc(519);
                    GlStateManager.disableFog();
                    this.entityOutlineFramebuffer.bindFramebuffer(false);
                    RenderHelper.disableStandardItemLighting();
                    this.renderManager.setRenderOutlines(true);

                    for (int i = 0; i < outlineRenderEntityList.size(); ++i) {
                        this.renderManager.renderEntityStatic(outlineRenderEntityList.get(i), partialTicks, false);
                    }

                    entityRenderer.render();
                    worldRenderer.postRender();
                    this.renderManager.setRenderOutlines(false);
                    RenderHelper.enableStandardItemLighting();
                    GlStateManager.depthMask(false);
                    this.entityOutlineShader.render(partialTicks);
                    GlStateManager.enableLighting();
                    GlStateManager.depthMask(true);
                    GlStateManager.enableFog();
                    GlStateManager.enableBlend();
                    GlStateManager.enableColorMaterial();
                    GlStateManager.depthFunc(515);
                    GlStateManager.enableDepth();
                    GlStateManager.enableAlpha();
                }

                this.mc.getFramebuffer().bindFramebuffer(false);
            }

            outlineRenderEntityList.clear();
            multiPassRenderEntityList.clear();
            this.world.profiler.endSection();

            this.world.profiler.endStartSection("tileEntities");
            this.world.profiler.startSection("vanilla");
            renderTileEntityVanilla(camera, partialTicks, pass);
            this.world.profiler.endStartSection("fastMinecraft");
            renderTileEntityFastMc(partialTicks);
            this.world.profiler.endStartSection("destroyProgress");
            renderTileEntityDestroyProgress(partialTicks);
            this.world.profiler.endSection();
        }
    }

    @SuppressWarnings("unchecked")
    private void renderTileEntityVanilla(ICamera camera, float partialTicks, int pass) {
        FastObjectArrayList<TileEntity> renderTileEntityList = (FastObjectArrayList<TileEntity>) (Object) getTerrainRenderer().getRenderTileEntityList().get();

        RenderHelper.enableStandardItemLighting();
        TileEntityRendererDispatcher.instance.preDrawBatch();

        for (int i = 0; i < renderTileEntityList.size(); i++) {
            TileEntity tileEntity = renderTileEntityList.get(i);
            if (!tileEntity.shouldRenderInPass(pass)) continue;
            if (!camera.isBoundingBoxInFrustum(tileEntity.getRenderBoundingBox())) continue;
            TileEntityRendererDispatcher.instance.render(tileEntity, partialTicks, -1);
        }

        FastObjectArrayList<TileEntity> globalTileEntityList = (FastObjectArrayList<TileEntity>) (Object) getTerrainRenderer().getGlobalTileEntityList().get();
        for (int i = 0; i < globalTileEntityList.size(); i++) {
            TileEntity tileEntity = globalTileEntityList.get(i);
            if (!tileEntity.shouldRenderInPass(pass)) continue;
            if (!camera.isBoundingBoxInFrustum(tileEntity.getRenderBoundingBox())) continue;
            TileEntityRendererDispatcher.instance.render(tileEntity, partialTicks, -1);
        }

        TileEntityRendererDispatcher.instance.drawBatch(pass);
    }

    private void renderTileEntityDestroyProgress(float partialTicks) {
        this.preRenderDamagedBlocks();
        for (DestroyBlockProgress destroyblockprogress : this.damagedBlocks.values()) {
            BlockPos blockpos = destroyblockprogress.getPosition();
            IBlockState blockState = this.world.getBlockState(blockpos);

            if (blockState.getBlock().hasTileEntity(blockState)) {
                TileEntity tileEntity = this.world.getTileEntity(blockpos);

                if (tileEntity instanceof TileEntityChest) {
                    TileEntityChest tileentitychest = (TileEntityChest) tileEntity;

                    if (tileentitychest.adjacentChestXNeg != null) {
                        blockpos = blockpos.offset(EnumFacing.WEST);
                        tileEntity = this.world.getTileEntity(blockpos);
                    } else if (tileentitychest.adjacentChestZNeg != null) {
                        blockpos = blockpos.offset(EnumFacing.NORTH);
                        tileEntity = this.world.getTileEntity(blockpos);
                    }
                }

                if (tileEntity != null && this.world.getBlockState(blockpos).hasCustomBreakingProgress()) {
                    TileEntityRendererDispatcher.instance.render(
                        tileEntity,
                        partialTicks,
                        destroyblockprogress.getPartialBlockDamage()
                    );
                }
            }
        }

        this.postRenderDamagedBlocks();
        this.mc.entityRenderer.disableLightmap();
    }

    private void renderTileEntityFastMc(float tickDelta) {
        WorldRenderer worldRenderer = FastMcMod.INSTANCE.getWorldRenderer();
        worldRenderer.preRender(tickDelta);
        worldRenderer.getTileEntityRenderer().render();
        worldRenderer.postRender();
    }

    @NotNull
    private TerrainRenderer getTerrainRenderer() {
        return FastMcMod.INSTANCE.getWorldRenderer().getTerrainRenderer();
    }
}