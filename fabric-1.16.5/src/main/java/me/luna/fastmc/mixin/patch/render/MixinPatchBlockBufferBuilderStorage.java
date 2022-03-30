package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedBlockBufferBuilderStorage;
import me.luna.fastmc.mixin.IPatchedRenderLayer;
import me.luna.fastmc.terrain.ChunkBuilderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.chunk.BlockBufferBuilderStorage;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(BlockBufferBuilderStorage.class)
public class MixinPatchBlockBufferBuilderStorage implements IPatchedBlockBufferBuilderStorage {
    @Mutable
    @Shadow
    @Final
    private Map<RenderLayer, BufferBuilder> builders;

    private final ChunkBuilderContext context = new ChunkBuilderContext();
    private final BufferBuilder[] buildersOverride = new BufferBuilder[RenderLayer.getBlockLayers().size()];

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(CallbackInfo ci) {
        for (Map.Entry<RenderLayer, BufferBuilder> entry : builders.entrySet()) {
            buildersOverride[((IPatchedRenderLayer) entry.getKey()).getIndex()] = entry.getValue();
        }
        builders = null;
    }

    /**
     * @author Luna
     * @reason Fast look up
     */
    @Overwrite
    public BufferBuilder get(RenderLayer layer) {
        return buildersOverride[((IPatchedRenderLayer) layer).getIndex()];
    }

    /**
     * @author Luna
     * @reason Fast look up
     */
    @Overwrite
    public void clear() {
        for (int i = 0; i < buildersOverride.length; i++) {
            buildersOverride[i].clear();
        }
    }

    /**
     * @author Luna
     * @reason Fast look up
     */
    @Overwrite
    public void reset() {
        for (int i = 0; i < buildersOverride.length; i++) {
            buildersOverride[i].reset();
        }
    }

    @NotNull
    @Override
    public ChunkBuilderContext getContext() {
        return context;
    }
}
