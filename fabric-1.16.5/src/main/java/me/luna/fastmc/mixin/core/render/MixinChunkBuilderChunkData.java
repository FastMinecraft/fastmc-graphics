package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.mixin.IPatchedChunkData;
import me.luna.fastmc.shared.util.collection.FastObjectArrayList;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkBuilder.ChunkData.class)
public abstract class MixinChunkBuilderChunkData implements IPatchedChunkData {
    private final FastObjectArrayList<BlockEntity> instancingRenderTileEntities = new FastObjectArrayList<>();

    @NotNull
    @Override
    public FastObjectArrayList<BlockEntity> getInstancingRenderTileEntities() {
        return instancingRenderTileEntities;
    }
}
