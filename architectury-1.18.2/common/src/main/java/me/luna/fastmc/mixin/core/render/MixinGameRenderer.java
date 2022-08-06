package me.luna.fastmc.mixin.core.render;

import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.shared.FpsDisplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/util/math/MatrixStack;F)V", shift = At.Shift.AFTER))
    private void render$Inject$INVOKE$InGameHud$render$AFTER(
        float tickDelta,
        long startTime,
        boolean tick,
        CallbackInfo ci
    ) {
        if (this.client.options.debugEnabled) return;
        Window window = this.client.getWindow();
        FpsDisplay.INSTANCE.render(
            (float) (window.getFramebufferWidth() / window.getScaleFactor()),
            (float) (window.getFramebufferHeight() / window.getScaleFactor()),
            FastMcMod.INSTANCE.getFontRenderer().getWrapped()
        );
    }
}
