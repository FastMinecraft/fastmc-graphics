package me.xiaro.fastmc.mixin.render;

import me.xiaro.fastmc.FastMcMod;
import me.xiaro.fastmc.TextureUpdater;
import net.minecraft.client.renderer.texture.ITickable;
import net.minecraft.client.renderer.texture.TextureManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TextureManager.class)
public abstract class MixinTextureManager {
    @Shadow @Final private List<ITickable> listTickables;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void setupCameraTransform$Inject$HEAD(CallbackInfo ci) {
        ci.cancel();

        TextureUpdater.INSTANCE.onTickPre();

        for (ITickable itickable : this.listTickables) {
            itickable.tick();
        }

        TextureUpdater.INSTANCE.onTickPost();
    }
}
