package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedChunkData;
import me.luna.fastmc.mixin.IPatchedRenderLayer;
import me.luna.fastmc.shared.util.collection.ExtendedBitSet;
import me.luna.fastmc.shared.util.collection.FastObjectArrayList;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;

@Mixin(ChunkBuilder.ChunkData.class)
public abstract class MixinChunkBuilderChunkData implements IPatchedChunkData {
    private final ExtendedBitSet nonEmptyLayersOverride = new ExtendedBitSet();
    @Mutable
    @Shadow
    @Final
    private List<BlockEntity> blockEntities;
    @Shadow
    @Final
    private Set<RenderLayer> nonEmptyLayers;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(CallbackInfo ci) {
        this.blockEntities = new FastObjectArrayList<>();
    }


    /**
     * @author Luna
     * @reason Fast look up
     */
    @Overwrite
    public boolean isEmpty(RenderLayer layer) {
        return !this.nonEmptyLayersOverride.contains(((IPatchedRenderLayer) layer).getIndex());
    }

    @Override
    public void onComplete() {
        for (RenderLayer renderLayer : nonEmptyLayers) {
            nonEmptyLayersOverride.add(((IPatchedRenderLayer) renderLayer).getIndex());
        }
    }
}
