package dev.fastmc.graphics.mixin.core.render;

import dev.fastmc.common.collection.FastObjectArrayList;
import dev.fastmc.graphics.FastMcMod;
import dev.fastmc.graphics.mixin.*;
import dev.fastmc.graphics.shared.renderer.WorldRenderer;
import dev.fastmc.graphics.shared.terrain.RenderChunkStorage;
import dev.fastmc.graphics.shared.terrain.TerrainRenderer;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.IRenderChunkFactory;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
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
    private Map<Integer, DestroyBlockProgress> damagedBlocks;
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
    protected abstract void cleanupDamagedBlocks(Iterator<DestroyBlockProgress> iteratorIn);

    @Unique
    private final List<RenderGlobal.ContainerLocalRenderInformation> emptyRenderInfo = new ReadOnlyList<>(new FastObjectArrayList<>(1));

    @Unique
    private final List<RenderGlobal.ContainerLocalRenderInformation> dummyRenderInfos = DummyCompiledChunk.makeDummyInfo((RenderGlobal) (Object) this);

    @Unique
    private final Set<TileEntity> emptyTileEntitySet = new ReadOnlySet<>(new ObjectArraySet<>(0));

    @Unique
    private DummyClassInheritanceMultiMap dummyClassInheritanceMultiMap;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void Inject$init$RETURN(Minecraft p_i1249_1_, CallbackInfo ci) {
        this.renderContainer = null;
        this.viewFrustum = null;
        this.renderChunkFactory = null;
        this.renderDispatcher = null;
        this.setTileEntities = emptyTileEntitySet;
        this.chunksToUpdate = new ReadOnlySet<>(new ObjectArraySet<>(0));
        this.renderInfos = emptyRenderInfo;
    }

    @Inject(method = "setWorldAndLoadRenderers", at = @At("RETURN"))
    private void Inject$setWorldAndLoadRenderers$RETURN(WorldClient worldClientIn, CallbackInfo ci) {
        if (this.world != null) {
            dummyClassInheritanceMultiMap = new DummyClassInheritanceMultiMap(this.world);
        }
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

    @SuppressWarnings("unchecked")
    @Inject(method = "renderEntities", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", args = "ldc=entities"))
    private void Inject$renderEntities$INVOKE_STRING$Profiler$endStartSection$entities(
        Entity renderViewEntity,
        ICamera camera,
        float partialTicks,
        CallbackInfo ci
    ) {
        renderInfos = dummyRenderInfos;
        setTileEntities = new ListSet<>((FastObjectArrayList<TileEntity>) (Object) FastMcMod.INSTANCE.getWorldRenderer()
            .getTerrainRenderer()
            .getGlobalTileEntityList()
            .getFront());
    }

    @SuppressWarnings({ "MixinAnnotationTarget", "InvalidInjectorMethodSignature" })
    @ModifyVariable(method = "renderEntities", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private ClassInheritanceMultiMap<Entity> ModifyVariable$renderEntities$STORE$ClassInheritanceMultiMap$Entity(
        ClassInheritanceMultiMap<Entity> entities
    ) {
        return dummyClassInheritanceMultiMap;
    }

    @Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/tileentity/TileEntityRendererDispatcher;drawBatch(I)V", remap = false))
    private void Inject$renderEntities$INVOKE$TileEntityRendererDispatcher$drawBatch(
        Entity renderViewEntity,
        ICamera camera,
        float partialTicks,
        CallbackInfo ci
    ) {
        renderTileEntityFastMc(partialTicks);
        renderInfos = emptyRenderInfo;
        setTileEntities = emptyTileEntitySet;
    }

    @Unique
    private void renderTileEntityFastMc(float tickDelta) {
        WorldRenderer worldRenderer = FastMcMod.INSTANCE.getWorldRenderer();
        worldRenderer.preRender(tickDelta);
        worldRenderer.getTileEntityRenderer().render();
        worldRenderer.postRender();
    }

    @Unique
    @NotNull
    private TerrainRenderer getTerrainRenderer() {
        return FastMcMod.INSTANCE.getWorldRenderer().getTerrainRenderer();
    }
}