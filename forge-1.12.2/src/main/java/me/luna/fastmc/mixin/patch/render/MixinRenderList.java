package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedChunkRenderContainer;
import me.luna.fastmc.shared.util.collection.FastObjectArrayList;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderList;
import net.minecraft.client.renderer.chunk.ListedRenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(RenderList.class)
public abstract class MixinRenderList extends ChunkRenderContainer implements IPatchedChunkRenderContainer {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void init$Inject$RETURN(CallbackInfo ci) {
        this.renderChunks = new ArrayList<>(0);
    }

    @Override
    public void renderChunkLayer(@NotNull FastObjectArrayList<RenderChunk> list, @NotNull BlockRenderLayer layer) {
        if (this.initialized) {
            if (layer == BlockRenderLayer.TRANSLUCENT) {
                for (int i = list.size() - 1; i != -1; i--) {
                    RenderChunk renderChunk = list.get(i);
                    ListedRenderChunk listedrenderchunk = (ListedRenderChunk) renderChunk;
                    GlStateManager.pushMatrix();
                    this.preRenderChunk(renderChunk);
                    GlStateManager.callList(listedrenderchunk.getDisplayList(layer, listedrenderchunk.getCompiledChunk()));
                    GlStateManager.popMatrix();
                }
            } else {
                for (int i = 0; i < list.size(); i++) {
                    RenderChunk renderChunk = list.get(i);
                    ListedRenderChunk listedrenderchunk = (ListedRenderChunk) renderChunk;
                    GlStateManager.pushMatrix();
                    this.preRenderChunk(renderChunk);
                    GlStateManager.callList(listedrenderchunk.getDisplayList(layer, listedrenderchunk.getCompiledChunk()));
                    GlStateManager.popMatrix();
                }
            }

            GlStateManager.resetColor();
            this.renderChunks.clear();
        }
    }
}
