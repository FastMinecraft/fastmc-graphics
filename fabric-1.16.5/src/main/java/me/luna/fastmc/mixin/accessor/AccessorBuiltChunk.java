package me.luna.fastmc.mixin.accessor;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

@Mixin(ChunkBuilder.BuiltChunk.class)
public interface AccessorBuiltChunk {
    @Invoker
    void callSetNoCullingBlockEntities(Set<BlockEntity> noCullingBlockEntities);
}
