package me.luna.fastmc.mixin.patch.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.fluid.FluidState;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.biome.source.BiomeAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BackgroundRenderer.class)
public class MixinPatchBackgroundRenderer {
    @Shadow
    private static long lastWaterFogColorUpdateTime;

    @Shadow
    private static int waterFogColor;

    @Shadow
    private static int nextWaterFogColor;

    @Shadow
    private static float red;

    @Shadow
    private static float green;

    @Shadow
    private static float blue;

    private static final double[] DENSITY_CURVE = new double[]{ 0.0D, 1.0D, 4.0D, 6.0D, 4.0D, 1.0D, 0.0D };

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public static void render(Camera camera, float tickDelta, ClientWorld world, int i, float f) {
        FluidState fluidState = camera.getSubmergedFluidState();
        int af;
        float aa;
        float ad;
        float z;
        float ag;
        float ah;
        if (fluidState.isIn(FluidTags.WATER)) {
            long l = Util.getMeasuringTimeMs();
            af = world.getBiome(new BlockPos(camera.getPos())).getWaterFogColor();
            if (lastWaterFogColorUpdateTime < 0L) {
                waterFogColor = af;
                nextWaterFogColor = af;
                lastWaterFogColorUpdateTime = l;
            }

            int k = waterFogColor >> 16 & 255;
            int m = waterFogColor >> 8 & 255;
            int n = waterFogColor & 255;
            int o = nextWaterFogColor >> 16 & 255;
            int p = nextWaterFogColor >> 8 & 255;
            int q = nextWaterFogColor & 255;
            aa = MathHelper.clamp((float) (l - lastWaterFogColorUpdateTime) / 5000.0F, 0.0F, 1.0F);
            ad = MathHelper.lerp(aa, (float) o, (float) k);
            z = MathHelper.lerp(aa, (float) p, (float) m);
            float s = MathHelper.lerp(aa, (float) q, (float) n);
            red = ad / 255.0F;
            green = z / 255.0F;
            blue = s / 255.0F;
            if (waterFogColor != af) {
                waterFogColor = af;
                nextWaterFogColor = MathHelper.floor(ad) << 16 | MathHelper.floor(z) << 8 | MathHelper.floor(s);
                lastWaterFogColorUpdateTime = l;
            }
        } else if (fluidState.isIn(FluidTags.LAVA)) {
            red = 0.6F;
            green = 0.1F;
            blue = 0.0F;
            lastWaterFogColorUpdateTime = -1L;
        } else {
            float t = 0.25F + 0.75F * (float) i / 32.0F;
            t = 1.0F - (float) Math.pow((double) t, 0.25D);
            Vec3d vec3d = world.method_23777(camera.getBlockPos(), tickDelta);
            ag = (float) vec3d.x;
            ah = (float) vec3d.y;
            float w = (float) vec3d.z;
            float sunHeight = MathHelper.clamp(MathHelper.cos(world.getSkyAngle(tickDelta) * 6.2831855F) * 2.0F + 0.5F, 0.0F, 1.0F);
            BiomeAccess biomeAccess = world.getBiomeAccess();
            Vec3d vec3d2 = camera.getPos().subtract(2.0D, 2.0D, 2.0D).multiply(0.25D);
            int i1 = MathHelper.floor(vec3d2.getX());
            int j1 = MathHelper.floor(vec3d2.getY());
            int k = MathHelper.floor(vec3d2.getZ());
            double d = vec3d2.getX() - (double) i1;
            double e = vec3d2.getY() - (double) j1;
            double f1 = vec3d2.getZ() - (double) k;
            double g = 0.0D;

            double colorR = 0.0;
            double colorG = 0.0;
            double colorB = 0.0;

            for (int l = 0; l < 6; ++l) {
                double h = MathHelper.lerp(d, DENSITY_CURVE[l + 1], DENSITY_CURVE[l]);
                int m = i1 - 2 + l;

                for (int n = 0; n < 6; ++n) {
                    double o = MathHelper.lerp(e, DENSITY_CURVE[n + 1], DENSITY_CURVE[n]);
                    int p = j1 - 2 + n;

                    for (int q = 0; q < 6; ++q) {
                        double r = MathHelper.lerp(f1, DENSITY_CURVE[q + 1], DENSITY_CURVE[q]);
                        int s = k - 2 + q;
                        double value = h * o * r;
                        g += value;
                        Vec3d vec = world.getSkyProperties().adjustFogColor(Vec3d.unpackRgb(biomeAccess.getBiomeForNoiseGen(m, p, s).getFogColor()), sunHeight);
                        colorR += vec.x * value;
                        colorG += vec.y * value;
                        colorB += vec.z * value;
                    }
                }
            }

            double mul = 1.0 / g;
            red = (float) (colorR * mul);
            green = (float) (colorG * mul);
            blue = (float) (colorB * mul);

            if (i >= 4) {
                aa = MathHelper.sin(world.getSkyAngleRadians(tickDelta)) > 0.0F ? -1.0F : 1.0F;
                Vec3f vec3f = new Vec3f(aa, 0.0F, 0.0F);
                z = camera.getHorizontalPlane().dot(vec3f);
                if (z < 0.0F) {
                    z = 0.0F;
                }

                if (z > 0.0F) {
                    float[] fs = world.getSkyProperties().getFogColorOverride(world.getSkyAngle(tickDelta), tickDelta);
                    if (fs != null) {
                        z *= fs[3];
                        red = red * (1.0F - z) + fs[0] * z;
                        green = green * (1.0F - z) + fs[1] * z;
                        blue = blue * (1.0F - z) + fs[2] * z;
                    }
                }
            }

            red += (ag - red) * t;
            green += (ah - green) * t;
            blue += (w - blue) * t;
            aa = world.getRainGradient(tickDelta);
            if (aa > 0.0F) {
                ad = 1.0F - aa * 0.5F;
                z = 1.0F - aa * 0.4F;
                red *= ad;
                green *= ad;
                blue *= z;
            }

            ad = world.getThunderGradient(tickDelta);
            if (ad > 0.0F) {
                z = 1.0F - ad * 0.5F;
                red *= z;
                green *= z;
                blue *= z;
            }

            lastWaterFogColorUpdateTime = -1L;
        }

        double d = camera.getPos().y * world.getLevelProperties().getHorizonShadingRatio();
        Entity focusedEntity = camera.getFocusedEntity();
        if (focusedEntity instanceof LivingEntity) {
            StatusEffectInstance effect = ((LivingEntity) focusedEntity).getStatusEffect(StatusEffects.BLINDNESS);
            if (effect != null) {
                af = effect.getDuration();
                if (af < 20) {
                    d *= 1.0F - (float) af / 20.0F;
                } else {
                    d = 0.0D;
                }
            }
        }

        if (d < 1.0D && !fluidState.isIn(FluidTags.LAVA)) {
            if (d < 0.0D) {
                d = 0.0D;
            }

            d *= d;
            red = (float) ((double) red * d);
            green = (float) ((double) green * d);
            blue = (float) ((double) blue * d);
        }

        if (f > 0.0F) {
            red = red * (1.0F - f) + red * 0.7F * f;
            green = green * (1.0F - f) + green * 0.6F * f;
            blue = blue * (1.0F - f) + blue * 0.6F * f;
        }

        if (fluidState.isIn(FluidTags.WATER)) {
            ag = 0.0F;
            if (camera.getFocusedEntity() instanceof ClientPlayerEntity) {
                ClientPlayerEntity clientPlayerEntity = (ClientPlayerEntity) camera.getFocusedEntity();
                ag = clientPlayerEntity.getUnderwaterVisibility();
            }

            ah = Math.min(1.0F / red, Math.min(1.0F / green, 1.0F / blue));
            red = red * (1.0F - ag) + red * ah * ag;
            green = green * (1.0F - ag) + green * ah * ag;
            blue = blue * (1.0F - ag) + blue * ah * ag;
        } else if (camera.getFocusedEntity() instanceof LivingEntity && ((LivingEntity) camera.getFocusedEntity()).hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            ag = GameRenderer.getNightVisionStrength((LivingEntity) camera.getFocusedEntity(), tickDelta);
            ah = Math.min(1.0F / red, Math.min(1.0F / green, 1.0F / blue));
            red = red * (1.0F - ag) + red * ah * ag;
            green = green * (1.0F - ag) + green * ah * ag;
            blue = blue * (1.0F - ag) + blue * ah * ag;
        }

        RenderSystem.clearColor(red, green, blue, 0.0F);
    }

}
