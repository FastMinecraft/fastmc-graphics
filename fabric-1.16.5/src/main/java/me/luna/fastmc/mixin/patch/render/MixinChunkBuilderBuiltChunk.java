package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedBuiltChunk;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkBuilder.BuiltChunk.class)
public class MixinChunkBuilderBuiltChunk implements IPatchedBuiltChunk {
    private int index = 0;

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }
}
