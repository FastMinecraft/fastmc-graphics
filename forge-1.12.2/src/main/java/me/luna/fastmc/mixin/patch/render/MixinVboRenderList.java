package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedChunkRenderContainer;
import me.luna.fastmc.shared.util.collection.FastObjectArrayList;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.BlockRenderLayer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(VboRenderList.class)
public abstract class MixinVboRenderList extends ChunkRenderContainer implements IPatchedChunkRenderContainer {
    @Shadow
    protected abstract void setupArrayPointers();

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
                    VertexBuffer vertexbuffer = renderChunk.getVertexBufferByLayer(layer.ordinal());
                    GlStateManager.pushMatrix();
                    this.preRenderChunk(renderChunk);
                    renderChunk.multModelviewMatrix();
                    vertexbuffer.bindBuffer();
                    this.setupArrayPointers();
                    vertexbuffer.drawArrays(7);
                    GlStateManager.popMatrix();
                }
            } else {
                for (int i = 0; i < list.size(); i++) {
                    RenderChunk renderChunk = list.get(i);
                    VertexBuffer vertexbuffer = renderChunk.getVertexBufferByLayer(layer.ordinal());
                    GlStateManager.pushMatrix();
                    this.preRenderChunk(renderChunk);
                    renderChunk.multModelviewMatrix();
                    vertexbuffer.bindBuffer();
                    this.setupArrayPointers();
                    vertexbuffer.drawArrays(7);
                    GlStateManager.popMatrix();
                }
            }

            OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
            GlStateManager.resetColor();
            this.renderChunks.clear();
        }
    }
}
