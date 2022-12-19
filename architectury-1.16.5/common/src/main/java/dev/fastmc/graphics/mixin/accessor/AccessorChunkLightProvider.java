package dev.fastmc.graphics.mixin.accessor;

import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkLightProvider.class)
public interface AccessorChunkLightProvider<S extends LightStorage<?>> {
    @Accessor
    S getLightStorage();
}