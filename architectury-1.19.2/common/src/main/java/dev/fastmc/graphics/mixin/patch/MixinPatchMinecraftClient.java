package dev.fastmc.graphics.mixin.patch;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MixinPatchMinecraftClient extends ReentrantThreadExecutor<Runnable> {
    @Shadow
    @Nullable
    public ClientWorld world;

    public MixinPatchMinecraftClient(String string) {
        super(string);
    }

    @Inject(method = "run", at = @At("HEAD"))
    private void Inject$run$HEAD(CallbackInfo ci) {
        Thread.currentThread().setPriority(8);
    }

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;setPriority(I)V"))
    private void Redirect$run$INVOKE$setPriority(Thread instance, int newPriority) {
        Thread.currentThread().setPriority(8);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V", remap = false))
    private void render$Redirect$INVOKE$yield() {

    }
}