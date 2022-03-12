package me.luna.fastmc.mixin.accessor;

import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderChunk.class)
public interface AccessorRenderChunk {
    @Accessor
    int getIndex();
}
