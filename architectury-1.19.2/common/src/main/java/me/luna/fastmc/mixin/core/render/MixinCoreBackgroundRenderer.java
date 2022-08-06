package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.mixin.accessor.AccessorBackgroundRenderer;
import me.luna.fastmc.shared.terrain.TerrainShaderManager;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(BackgroundRenderer.class)
public class MixinCoreBackgroundRenderer {
    @Inject(method = "applyFog", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void Inject$applyFog$RETURN(
        Camera camera,
        BackgroundRenderer.FogType fogType,
        float viewDistance,
        boolean thickFog,
        float tickDelta,
        CallbackInfo ci,
        CameraSubmersionType cameraSubmersionType,
        Entity entity,
        BackgroundRenderer.FogData fogData
    ) {
        TerrainShaderManager.FogShape fogShape = switch (fogData.fogShape) {
            case SPHERE -> TerrainShaderManager.FogShape.SPHERE;
            case CYLINDER -> TerrainShaderManager.FogShape.CYLINDER;
        };
        FastMcMod.INSTANCE.getWorldRenderer().getTerrainRenderer().getShaderManager().linearFog(
            fogShape,
            fogData.fogStart,
            fogData.fogEnd,
            AccessorBackgroundRenderer.getRed(),
            AccessorBackgroundRenderer.getGreen(),
            AccessorBackgroundRenderer.getBlue()
        );
    }
}
