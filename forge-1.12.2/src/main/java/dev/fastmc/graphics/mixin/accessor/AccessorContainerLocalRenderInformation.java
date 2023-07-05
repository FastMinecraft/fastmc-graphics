package dev.fastmc.graphics.mixin.accessor;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderGlobal.ContainerLocalRenderInformation.class)
public interface AccessorContainerLocalRenderInformation {
    @Accessor
    void setRenderChunk(RenderChunk renderChunk);
}
