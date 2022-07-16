package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.shared.mixin.IPatchedBakedQuad;
import net.minecraft.client.render.model.BakedQuad;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BakedQuad.class)
public class MixinPatchBakedQuad implements IPatchedBakedQuad {
    @Shadow
    @Final
    protected int[] vertexData;

    int faceBit = -1;

    @Override
    public int getFaceBit() {
        if (faceBit == -1) {
            faceBit = IPatchedBakedQuad.calcFaceBit(this.vertexData, 8);
        }

        return faceBit;
    }
}
