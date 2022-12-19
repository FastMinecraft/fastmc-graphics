package dev.fastmc.graphics.mixin.core.render;

import dev.fastmc.graphics.shared.mixin.IPatchedBakedQuad;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BakedQuad.class)
public class MixinCoreBakedQuad implements IPatchedBakedQuad {
    @Shadow
    @Final
    protected int[] vertexData;

    int faceBit = -1;

    @Override
    public int getFaceBit() {
        if (faceBit == -1) {
            faceBit = IPatchedBakedQuad.calcFaceBit(this.vertexData, 7);
        }

        return faceBit;
    }
}