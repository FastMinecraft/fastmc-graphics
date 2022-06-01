package me.luna.fastmc.mixin.patch.render;

import me.luna.fastmc.tileentity.IPatchedParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Particle.class)
public abstract class MixinPatchParticle implements IPatchedParticle {
    @Shadow
    @Final
    protected ClientWorld world;
    @Shadow
    protected double x;
    @Shadow
    protected double y;
    @Shadow
    protected double z;

    private int brightness = 0;

    /**
     * @author Luna
     * @reason Particle render optimization
     */
    @Overwrite
    public int getBrightness(float tint) {
        return brightness;
    }

    @Override
    public int getBrightness() {
        return brightness;
    }

    @Override
    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }

    @NotNull
    @Override
    public World getWorld() {
        return world;
    }

    @NotNull
    @Override
    public BlockPos getBlockPos() {
        return new BlockPos(this.x, this.y, this.z);
    }
}
