package me.luna.fastmc.mixin.accessor;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.chunk.ChunkOcclusionData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(ChunkBuilder.ChunkData.class)
public interface AccessorChunkData {
    @Accessor
    Set<RenderLayer> getInitializedLayers();

    @Accessor
    boolean isEmpty();

    @Accessor
    void setEmpty(boolean empty);

    @Accessor
    BufferBuilder.State getBufferState();

    @Accessor
    void setBufferState(BufferBuilder.State bufferState);

    @Accessor
    Set<RenderLayer> getNonEmptyLayers();

    @Accessor
    ChunkOcclusionData getOcclusionGraph();

    @Accessor
    void setOcclusionGraph(ChunkOcclusionData occlusionGraph);
}
