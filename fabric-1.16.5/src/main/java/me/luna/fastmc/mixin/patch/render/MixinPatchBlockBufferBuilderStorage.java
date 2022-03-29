package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedBlockBufferBuilderStorage;
import me.luna.fastmc.shared.util.CachedByteBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockBufferBuilderStorage.class)
public class MixinPatchBlockBufferBuilderStorage implements IPatchedBlockBufferBuilderStorage {
    private final CachedByteBuffer cachedByteBuffer = new CachedByteBuffer(RenderLayer.getSolid().getExpectedBufferSize());

    @NotNull
    @Override
    public CachedByteBuffer getCachedByteBuffer() {
        return cachedByteBuffer;
    }
}
