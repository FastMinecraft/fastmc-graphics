package me.luna.fastmc.mixin.core;

import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import me.luna.fastmc.FastMcMod;
import me.luna.fastmc.GLWrapper;
import me.luna.fastmc.RendererReloader;
import me.luna.fastmc.shared.FpsDisplay;
import me.luna.fastmc.shared.terrain.ChunkBuilderTask;
import me.luna.fastmc.shared.util.FastMcCoreScope;
import me.luna.fastmc.shared.util.FastMcExtendScope;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("InjectIntoConstructor")
@Mixin(MinecraftClient.class)
public abstract class MixinCoreMinecraftClient extends ReentrantThreadExecutor<Runnable> {
    @Shadow
    @Nullable
    public ClientWorld world;

    public MixinCoreMinecraftClient(String string) {
        super(string);
    }

    @Shadow
    public abstract Profiler getProfiler();

    @Shadow @Final private ReloadableResourceManagerImpl resourceManager;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(IZ)V", shift = At.Shift.AFTER))
    public void init$Inject$INVOKE$initializeTextures(CallbackInfo ci) {
        FastMcMod.INSTANCE.initGLWrapper(new GLWrapper());
        //noinspection ConstantConditions
        FastMcMod.INSTANCE.initProfiler(new me.luna.fastmc.mixin.Profiler((MinecraftClient) (Object) this));
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;<init>(Lnet/minecraft/client/MinecraftClient;)V"))
    public void Inject$init$INVOKE$MinecraftClient$setOverlay(CallbackInfo ci) {
        this.resourceManager.registerReloader(RendererReloader.INSTANCE);
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;shutdownExecutors()V"))
    public void Inject$close$INVOKE$Util$shutdownExecutors(CallbackInfo ci) {
        FastMcCoreScope.INSTANCE.getPool().shutdown();
        FastMcExtendScope.INSTANCE.getPool().shutdown();
        ChunkBuilderTask.cancelAllAndJoin();

        boolean shutdown;
        try {
            shutdown = FastMcCoreScope.INSTANCE.getPool().awaitTermination(3L, TimeUnit.SECONDS)
                && FastMcExtendScope.INSTANCE.getPool().awaitTermination(3L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            shutdown = false;
        }

        if (!shutdown) {
            FastMcCoreScope.INSTANCE.getPool().shutdownNow();
            FastMcExtendScope.INSTANCE.getPool().shutdownNow();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void runTick$Inject$RETURN(CallbackInfo ci) {
        getProfiler().push("fastMinecraft");
        FastMcMod.INSTANCE.onPostTick();
        getProfiler().pop();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getFramerateLimit()I"))
    public void runTick$Inject$INVOKE$getFramerateLimit(CallbackInfo ci) {
        FpsDisplay.INSTANCE.onPostRenderTick();
    }
}
