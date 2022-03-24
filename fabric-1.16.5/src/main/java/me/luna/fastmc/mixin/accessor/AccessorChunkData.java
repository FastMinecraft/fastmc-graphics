package me.luna.fastmc.mixin.accessor;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(ChunkBuilder.ChunkData.class)
public interface AccessorChunkData {
    @Accessor
    Set<RenderLayer> getInitializedLayers();

    @Accessor
    BufferBuilder.State getBufferState();

    @Accessor
    void setBufferState(BufferBuilder.State bufferState);

    @Accessor
    Set<RenderLayer> getNonEmptyLayers();
}
