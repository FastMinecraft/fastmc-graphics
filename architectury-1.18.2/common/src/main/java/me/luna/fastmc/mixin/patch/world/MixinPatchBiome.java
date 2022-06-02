package me.luna.fastmc.mixin.patch.world;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Biome.class)
public abstract class MixinPatchBiome {

    @Shadow
    @Final
    private BiomeEffects effects;

    @Shadow
    protected abstract int getDefaultFoliageColor();

    @Shadow
    protected abstract int getDefaultGrassColor();

    private int foliageColorOverride;
    private int grassColorOverride;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void init$Inject$RETURN(CallbackInfo ci) {
        foliageColorOverride = this.effects.getFoliageColor().orElseGet(this::getDefaultFoliageColor);
        grassColorOverride = this.effects.getGrassColor().orElseGet(this::getDefaultGrassColor);
    }

    /**
     * @author Luna
     * @reason Fast biome color
     */
    @Overwrite
    @Environment(EnvType.CLIENT)
    public int getGrassColorAt(double x, double z) {
        return this.effects.getGrassColorModifier().getModifiedGrassColor(x, z, grassColorOverride);
    }

    /**
     * @author Luna
     * @reason Fast biome color
     */
    @Overwrite
    @Environment(EnvType.CLIENT)
    public int getFoliageColor() {
        return foliageColorOverride;
    }
}
