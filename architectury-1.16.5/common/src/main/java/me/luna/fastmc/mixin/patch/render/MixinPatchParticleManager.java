package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.mixin.IPatchedParticleManager;
import me.luna.fastmc.tileentity.IPatchedParticle;
import net.minecraft.client.particle.EmitterParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleManager.class)
public abstract class MixinPatchParticleManager implements IPatchedParticleManager {
    @Shadow
    @Final
    private Map<ParticleTextureSheet, Queue<Particle>> particles;

    @Shadow
    @Final
    private Queue<EmitterParticle> newEmitterParticles;

    @Shadow
    @Final
    private Queue<Particle> newParticles;

    @Shadow
    protected abstract void tickParticle(Particle particle);

    @Inject(method = "tickParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;tick()V", shift = At.Shift.AFTER))
    private void tickParticle$Inject$INVOKE$tick$AFTER(Particle particle, CallbackInfo ci) {
        ((IPatchedParticle) particle).updateBrightness();
    }

    /**
     * @author Luna
     * @reason Parallel particle update
     */
    @Overwrite
    public void tick() {
        tick0(particles, newEmitterParticles, newParticles);
    }

    @Override
    public void tickParticle0(@NotNull Particle particle) {
        tickParticle(particle);
    }
}
