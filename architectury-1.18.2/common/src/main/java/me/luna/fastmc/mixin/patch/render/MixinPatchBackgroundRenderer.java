package me.luna.fastmc.mixin.patch.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.CubicSampler;
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

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public static void render(Camera camera, float tickDelta, ClientWorld world, int i2, float f) {
        CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
        Entity entity = camera.getFocusedEntity();
        if (cameraSubmersionType == CameraSubmersionType.WATER) {
            long l = Util.getMeasuringTimeMs();
            int j2 = world.getBiome(new BlockPos(camera.getPos())).value().getWaterFogColor();
            if (lastWaterFogColorUpdateTime < 0L) {
                waterFogColor = j2;
                nextWaterFogColor = j2;
                lastWaterFogColorUpdateTime = l;
            }
            int k2 = waterFogColor >> 16 & 0xFF;
            int m = waterFogColor >> 8 & 0xFF;
            int n = waterFogColor & 0xFF;
            int o = nextWaterFogColor >> 16 & 0xFF;
            int p = nextWaterFogColor >> 8 & 0xFF;
            int q = nextWaterFogColor & 0xFF;
            float g = MathHelper.clamp((float)(l - lastWaterFogColorUpdateTime) / 5000.0f, 0.0f, 1.0f);
            float h = MathHelper.lerp(g, o, k2);
            float r = MathHelper.lerp(g, p, m);
            float s = MathHelper.lerp(g, q, n);
            red = h / 255.0f;
            green = r / 255.0f;
            blue = s / 255.0f;
            if (waterFogColor != j2) {
                waterFogColor = j2;
                nextWaterFogColor = MathHelper.floor(h) << 16 | MathHelper.floor(r) << 8 | MathHelper.floor(s);
                lastWaterFogColorUpdateTime = l;
            }
        } else if (cameraSubmersionType == CameraSubmersionType.LAVA) {
            red = 0.6f;
            green = 0.1f;
            blue = 0.0f;
            lastWaterFogColorUpdateTime = -1L;
        } else if (cameraSubmersionType == CameraSubmersionType.POWDER_SNOW) {
            red = 0.623f;
            green = 0.734f;
            blue = 0.785f;
            lastWaterFogColorUpdateTime = -1L;
            RenderSystem.clearColor(red, green, blue, 0.0f);
        } else {
            float h;
            float r;
            float g;
            float t = 0.25f + 0.75f * (float)i2 / 32.0f;
            t = 1.0f - (float)Math.pow(t, 0.25);
            Vec3d vec3d = world.getSkyColor(camera.getPos(), tickDelta);
            float u = (float)vec3d.x;
            float v = (float)vec3d.y;
            float w = (float)vec3d.z;
            float x = MathHelper.clamp(MathHelper.cos(world.getSkyAngle(tickDelta) * ((float)Math.PI * 2)) * 2.0f + 0.5f, 0.0f, 1.0f);
            BiomeAccess biomeAccess = world.getBiomeAccess();
            Vec3d vec3d2 = camera.getPos().subtract(2.0, 2.0, 2.0).multiply(0.25);
            Vec3d vec3d3 = CubicSampler.sampleColor(vec3d2, (i, j, k) -> world.getDimensionEffects().adjustFogColor(Vec3d.unpackRgb(biomeAccess.getBiomeForNoiseGen(i, j, k).value().getFogColor()), x));
            red = (float)vec3d3.getX();
            green = (float)vec3d3.getY();
            blue = (float)vec3d3.getZ();
            if (i2 >= 4) {
                float[] fs;
                g = MathHelper.sin(world.getSkyAngleRadians(tickDelta)) > 0.0f ? -1.0f : 1.0f;
                Vec3f vec3f = new Vec3f(g, 0.0f, 0.0f);
                r = camera.getHorizontalPlane().dot(vec3f);
                if (r < 0.0f) {
                    r = 0.0f;
                }
                if (r > 0.0f && (fs = world.getDimensionEffects().getFogColorOverride(world.getSkyAngle(tickDelta), tickDelta)) != null) {
                    red = red * (1.0f - (r *= fs[3])) + fs[0] * r;
                    green = green * (1.0f - r) + fs[1] * r;
                    blue = blue * (1.0f - r) + fs[2] * r;
                }
            }
            red += (u - red) * t;
            green += (v - green) * t;
            blue += (w - blue) * t;
            g = world.getRainGradient(tickDelta);
            if (g > 0.0f) {
                float h2 = 1.0f - g * 0.5f;
                r = 1.0f - g * 0.4f;
                red *= h2;
                green *= h2;
                blue *= r;
            }
            if ((h = world.getThunderGradient(tickDelta)) > 0.0f) {
                r = 1.0f - h * 0.5f;
                red *= r;
                green *= r;
                blue *= r;
            }
            lastWaterFogColorUpdateTime = -1L;
        }
        float t = ((float)camera.getPos().y - (float)world.getBottomY()) * world.getLevelProperties().getHorizonShadingRatio();
        if (camera.getFocusedEntity() instanceof LivingEntity) {
            StatusEffectInstance effect = ((LivingEntity) camera.getFocusedEntity()).getStatusEffect(StatusEffects.BLINDNESS);
            if (effect != null) {
                int y = effect.getDuration();
                t = y < 20 ? 1.0f - (float) y / 20.0f : 0.0f;
            }
        }

        if (t < 1.0f && cameraSubmersionType != CameraSubmersionType.LAVA && cameraSubmersionType != CameraSubmersionType.POWDER_SNOW) {
            if (t < 0.0f) {
                t = 0.0f;
            }
            t *= t;
            red *= t;
            green *= t;
            blue *= t;
        }
        if (f > 0.0f) {
            red = red * (1.0f - f) + red * 0.7f * f;
            green = green * (1.0f - f) + green * 0.6f * f;
            blue = blue * (1.0f - f) + blue * 0.6f * f;
        }
        float z = cameraSubmersionType == CameraSubmersionType.WATER ? (entity instanceof ClientPlayerEntity ? ((ClientPlayerEntity)entity).getUnderwaterVisibility() : 1.0f) : (entity instanceof LivingEntity && ((LivingEntity)entity).hasStatusEffect(StatusEffects.NIGHT_VISION) ? GameRenderer.getNightVisionStrength((LivingEntity)entity, tickDelta) : 0.0f);
        if (red != 0.0f && green != 0.0f && blue != 0.0f) {
            float u = Math.min(1.0f / red, Math.min(1.0f / green, 1.0f / blue));
            red = red * (1.0f - z) + red * u * z;
            green = green * (1.0f - z) + green * u * z;
            blue = blue * (1.0f - z) + blue * u * z;
        }

        RenderSystem.clearColor(red, green, blue, 0.0f);
    }

}
