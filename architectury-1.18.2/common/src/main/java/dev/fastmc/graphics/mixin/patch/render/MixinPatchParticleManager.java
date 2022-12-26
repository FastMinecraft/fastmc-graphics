package dev.fastmc.graphics.mixin.patch.render;

import dev.fastmc.graphics.mixin.IPatchedParticleManager;
import net.minecraft.client.particle.EmitterParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

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