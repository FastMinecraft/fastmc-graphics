package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedBuiltChunk;
import me.luna.fastmc.mixin.IPatchedRenderLayer;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(ChunkBuilder.BuiltChunk.class)
public abstract class MixinChunkBuilderBuiltChunk implements IPatchedBuiltChunk {
    @Mutable
    @Shadow
    @Final
    private Map<RenderLayer, VertexBuffer> buffers;

    @Shadow
    protected abstract void clear();

    private int index = 0;
    private final VertexBuffer[] bufferArray = new VertexBuffer[RenderLayer.getBlockLayers().size()];

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(ChunkBuilder chunkBuilder, CallbackInfo ci) {
        List<RenderLayer> layers = RenderLayer.getBlockLayers();
        for (int i = 0; i < layers.size(); i++) {
            bufferArray[i] = buffers.get(layers.get(i));
        }
        this.buffers = null;
    }

    /**
     * @author Luna
     * @reason Fast look up
     */
    @Overwrite
    public VertexBuffer getBuffer(RenderLayer layer) {
        return bufferArray[((IPatchedRenderLayer) layer).getIndex()];
    }

    /**
     * @author Luna
     * @reason Fast look up
     */
    @Overwrite
    public void delete() {
        this.clear();
        for (int i = 0; i < bufferArray.length; i++) {
            bufferArray[i].close();
        }
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }
}
