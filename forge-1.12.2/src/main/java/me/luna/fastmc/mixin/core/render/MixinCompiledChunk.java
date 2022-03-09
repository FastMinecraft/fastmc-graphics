package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.mixin.IPatchedCompiledChunk;
import me.luna.fastmc.renderer.TileEntityRenderer;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.tileentity.TileEntity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(CompiledChunk.class)
public class MixinCompiledChunk implements IPatchedCompiledChunk {
    private final List<TileEntity> instancingRenderTileEntities = new ArrayList<>();

    @NotNull
    @Override
    public List<TileEntity> getInstancingRenderTileEntities() {
        return instancingRenderTileEntities;
    }

    @Inject(method = "addTileEntity", at = @At("HEAD"), cancellable = true)
    private void addTileEntity$Inject$HEAD(TileEntity tileEntityIn, CallbackInfo ci) {
        if (((TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer()).hasRenderer(tileEntityIn)) {
            instancingRenderTileEntities.add(tileEntityIn);
            ci.cancel();
        }
    }
}
