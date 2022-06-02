package me.luna.fastmc.mixin.accessor;

import net.minecraft.client.render.BufferRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferRenderer.class)
public interface AccessorBufferRenderer {
    @Accessor
    static void setCurrentVertexArray(int currentVertexArray) {throw new UnsupportedOperationException();}
}
