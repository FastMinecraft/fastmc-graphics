package me.luna.fastmc.mixin.patch;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinPatchMinecraftServer {
    @Inject(method = "run", at = @At("HEAD"))
    private void Inject$runServer$HEAD(CallbackInfo ci) {
        Thread.currentThread().setPriority(1);
    }
}
