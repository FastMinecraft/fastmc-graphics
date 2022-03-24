package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedBuiltChunk;
import me.luna.fastmc.terrain.RenderRegion;
import me.luna.fastmc.terrain.ChunkVertexData;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(ChunkBuilder.BuiltChunk.class)
public abstract class MixinPatchChunkBuilderBuiltChunk implements IPatchedBuiltChunk {
    @Mutable
    @Shadow
    @Final
    private Map<RenderLayer, VertexBuffer> buffers;

    @Shadow
    protected abstract void clear();

    @Shadow @Final public AtomicReference<ChunkBuilder.ChunkData> data;
    private int index = 0;
    private RenderRegion region;
    private final ChunkVertexData[] chunkVertexDataArray = new ChunkVertexData[RenderLayer.getBlockLayers().size()];

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(ChunkBuilder chunkBuilder, CallbackInfo ci) {
        List<RenderLayer> layers = RenderLayer.getBlockLayers();
        for (int i = 0; i < layers.size(); i++) {
            buffers.get(layers.get(i)).close();
        }
        this.buffers = null;
    }

    @Inject(method = "clear", at = @At("RETURN"))
    private void clear$Inject$RETURN(CallbackInfo ci) {
        RenderRegion region = this.region;
        if (region != null) region.setDirty(true);
    }

    @Inject(method = "scheduleRebuild(Z)V", at = @At("RETURN"))
    private void scheduleRebuild$Inject$RETURN(CallbackInfo ci) {
        RenderRegion region = this.region;
        if (region != null) region.setDirty(true);
    }

    /**
     * @author Luna
     * @reason Render override
     */
    @Overwrite
    public VertexBuffer getBuffer(RenderLayer layer) {
        return null;
    }

    /**
     * @author Luna
     * @reason Render override
     */
    @Overwrite
    public void delete() {
        for (int i = 0; i < chunkVertexDataArray.length; i++) {
            ChunkVertexData data = chunkVertexDataArray[i];
            if (data != null) data.getVbo().destroy();
        }
        this.clear();
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public ChunkVertexData @NotNull [] getChunkVertexDataArray() {
        return chunkVertexDataArray;
    }

    @NotNull
    @Override
    public RenderRegion getRegion() {
        return region;
    }

    @Override
    public void setRegion(@NotNull RenderRegion region) {
        this.region = region;
    }
}
