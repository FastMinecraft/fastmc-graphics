package dev.fastmc.graphics.mixin.accessor;

import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;
import net.minecraft.world.chunk.light.LightStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LightStorage.class)
public interface AccessorLightStorage<M extends ChunkToNibbleArrayMap<M>> {
    @Invoker
    ChunkNibbleArray invokeGetLightSection(M storage, long sectionPos);

    @Accessor
    M getUncachedStorage();
}