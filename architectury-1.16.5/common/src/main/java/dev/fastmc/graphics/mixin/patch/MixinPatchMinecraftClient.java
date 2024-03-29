package dev.fastmc.graphics.mixin.patch;

import dev.fastmc.graphics.mixin.accessor.AccessorThreadExecutor;
import dev.fastmc.graphics.shared.FpsDisplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(MinecraftClient.class)
public abstract class MixinPatchMinecraftClient extends ReentrantThreadExecutor<Runnable> {
    @Shadow
    @Nullable
    public ClientPlayerEntity player;
    @Shadow
    @Nullable
    public ClientWorld world;
    @Shadow
    private Profiler profiler;

    public MixinPatchMinecraftClient(String string) {
        super(string);
    }

    @Inject(method = "run", at = @At("HEAD"))
    private void Inject$run$HEAD(CallbackInfo ci) {
        Thread.currentThread().setPriority(8);
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Ljava/lang/Thread;yield()V", remap = false))
    private void render$Redirect$INVOKE$yield() {

    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;runTasks()V"))
    private void render$Redirect$INVOKE$runTasks(MinecraftClient instance) {
        runTasks0();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Mouse;updateMouse()V"))
    private void render$Inject$INVOKE$updateMouse(boolean tick, CallbackInfo ci) {
        if (!tick && this.player != null && this.world != null) {
            this.profiler.push("scheduledExecutables");
            runTasks0();
            this.profiler.pop();
        }
    }

    private void runTasks0() {
        @SuppressWarnings("unchecked")
        Queue<Runnable> tasks = ((AccessorThreadExecutor<Runnable>) this).getTasks();
        int size = tasks.size();
        if (size == 0) return;
        int min = size / 4;

        long startTime = System.currentTimeMillis();
        long targetTime = (FpsDisplay.INSTANCE.getNanoFrameTime() + FpsDisplay.INSTANCE.getLastFrameTime()) / 20_000_000;
        long elapsedTime;
        int count = 0;

        do {
            Runnable task = tasks.poll();
            if (task == null) break;
            executeTask(task);

            elapsedTime = System.currentTimeMillis() - startTime;
            count++;
        } while (count < min || elapsedTime + elapsedTime / count < targetTime);
    }
}