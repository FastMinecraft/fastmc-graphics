package me.luna.fastmc.mixin.patch;

import me.luna.fastmc.mixin.IPatchedWorldRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MixinPatchMinecraftClient {
    @Shadow @Final public WorldRenderer worldRenderer;

    @Inject(method = "tick", at = @At("RETURN"))
    public void runTick$Inject$RETURN(CallbackInfo ci) {
        ((IPatchedWorldRenderer) this.worldRenderer).setTicked(true);
    }
}
