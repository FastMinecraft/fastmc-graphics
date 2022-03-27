package me.luna.fastmc.mixin.accessor;

import net.minecraft.client.render.chunk.ChunkOcclusionData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.BitSet;

@Mixin(ChunkOcclusionData.class)
public interface AccessorChunkOcclusionData {
    @Accessor
    BitSet getVisibility();
}
