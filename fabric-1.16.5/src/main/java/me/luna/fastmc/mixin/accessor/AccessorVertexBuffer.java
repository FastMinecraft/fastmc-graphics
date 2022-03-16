package me.luna.fastmc.mixin.accessor;

import net.minecraft.client.gl.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VertexBuffer.class)
public interface AccessorVertexBuffer {
    @Accessor
    int getVertexCount();

    @Accessor
    int getVertexBufferId();
}
