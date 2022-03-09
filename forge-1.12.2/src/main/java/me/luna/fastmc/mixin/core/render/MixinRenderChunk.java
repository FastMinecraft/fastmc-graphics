package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.mixin.IPatchedCompiledChunk;
import me.luna.fastmc.renderer.TileEntityRenderer;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(RenderChunk.class)
public abstract class MixinRenderChunk {
    @Shadow public abstract CompiledChunk getCompiledChunk();

    @Inject(method = "stopCompileTask", at = @At("HEAD"))
    private void stopCompileTask$Inject$HEAD(CallbackInfo ci) {
        ((TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer())
            .updateEntities(Collections.emptyList(), this.getCompiledChunk().getTileEntities());
    }

    @Inject(method = "setCompiledChunk", at = @At("HEAD"))
    private void setCompiledChunk$Inject$HEAD(CompiledChunk compiledChunkIn, CallbackInfo ci) {
        List<TileEntity> oldList = ((IPatchedCompiledChunk) this.getCompiledChunk()).getInstancingRenderTileEntities();
        List<TileEntity> newList = ((IPatchedCompiledChunk) compiledChunkIn).getInstancingRenderTileEntities();

        boolean oldEmpty = oldList.isEmpty();
        boolean newEmpty = newList.isEmpty();

        if (!oldEmpty || !newEmpty) {
            TileEntityRenderer renderer = ((TileEntityRenderer) FastMcMod.INSTANCE.getWorldRenderer().getTileEntityRenderer());

            if (oldEmpty) {
                renderer.updateEntities(newList, Collections.emptyList());
            } else if (newEmpty) {
                renderer.updateEntities(Collections.emptyList(), oldList);
            } else {
                Set<TileEntity> oldSet = new HashSet<>(oldList);
                Set<TileEntity> newSet = new HashSet<>(newList);

                List<TileEntity> adding = new ArrayList<>();
                List<TileEntity> removing = new ArrayList<>();

                for (TileEntity e : newList) {
                    if (!oldSet.contains(e)) {
                        adding.add(e);
                    }
                }

                for (TileEntity e : oldList) {
                    if (!newSet.contains(e)) {
                        removing.add(e);
                    }
                }

                renderer.updateEntities(adding, removing);
            }
        }
    }
}
