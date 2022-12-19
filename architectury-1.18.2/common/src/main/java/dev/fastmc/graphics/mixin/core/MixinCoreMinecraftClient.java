package dev.fastmc.graphics.mixin.core;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.fastmc.common.FastMcCoreScope;
import dev.fastmc.common.FastMcExtendScope;
import dev.fastmc.graphics.FastMcMod;
import dev.fastmc.graphics.GLWrapper;
import dev.fastmc.graphics.RendererReloader;
import dev.fastmc.graphics.shared.FpsDisplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("ConstantConditions")
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

    @Shadow
    @Final
    private ReloadableResourceManagerImpl resourceManager;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;initRenderer(IZ)V"))
    public void Redirect$init$INVOKE$RenderSystem$initRenderer(int debugVerbosity, boolean debugSync) {
        RenderSystem.initRenderer(debugVerbosity, debugSync);
        FastMcMod.INSTANCE.initGLWrapper(new GLWrapper());
        FastMcMod.INSTANCE.initProfiler(new dev.fastmc.graphics.mixin.Profiler((MinecraftClient) (Object) this));
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/SplashOverlay;init(Lnet/minecraft/client/MinecraftClient;)V"))
    public void Redirect$init$INVOKE$SplashScreen$init(MinecraftClient minecraft) {
        SplashOverlay.init(minecraft);
        this.resourceManager.registerReloader(RendererReloader.INSTANCE);
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;shutdownExecutors()V"))
    public void Inject$close$INVOKE$Util$shutdownExecutors(CallbackInfo ci) {
        FastMcCoreScope.INSTANCE.getPool().shutdown();
        FastMcExtendScope.INSTANCE.getPool().shutdown();
        FastMcMod.INSTANCE.getWorldRenderer().getTerrainRenderer().getChunkBuilder().clear();

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