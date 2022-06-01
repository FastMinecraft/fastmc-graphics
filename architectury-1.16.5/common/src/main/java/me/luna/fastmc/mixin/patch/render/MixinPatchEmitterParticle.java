package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.tileentity.IPatchedParticle;
import net.minecraft.client.particle.EmitterParticle;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EmitterParticle.class)
public abstract class MixinPatchEmitterParticle extends NoRenderParticle implements IPatchedParticle {
    protected MixinPatchEmitterParticle(ClientWorld clientWorld, double d, double e, double f) {
        super(clientWorld, d, e, f);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tick$Inject$RETURN(CallbackInfo ci) {
        updateBrightness();
    }
}
