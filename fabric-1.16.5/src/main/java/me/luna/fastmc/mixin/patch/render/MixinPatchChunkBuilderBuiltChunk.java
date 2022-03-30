package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedBuiltChunk;
import me.luna.fastmc.terrain.ChunkVertexData;
import me.luna.fastmc.terrain.RenderRegion;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(ChunkBuilder.BuiltChunk.class)
public abstract class MixinPatchChunkBuilderBuiltChunk implements IPatchedBuiltChunk {
    @Shadow
    protected abstract void clear();

    @Shadow
    @Final
    public AtomicReference<ChunkBuilder.ChunkData> data;

    private static final int LAYER_SIZE = RenderLayer.getBlockLayers().size();

    private int index = 0;
    private RenderRegion region;
    private final ChunkVertexData[] chunkVertexDataArray = new ChunkVertexData[LAYER_SIZE];

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayer;getBlockLayers()Ljava/util/List;"))
    private List<RenderLayer> init$Redirect$INVOKE$getBlockLayers() {
        return Collections.emptyList();
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
            if (data != null) data.vboInfo.vbo.destroy();
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
