package dev.fastmc.graphics.mixin.patch;

import dev.fastmc.graphics.shared.util.FastMcCoreScope;
import dev.fastmc.graphics.shared.util.FastMcExtendScope;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(Util.class)
public abstract class MixinUtil {
    @Shadow
    @Final
    private static AtomicInteger NEXT_WORKER_ID;

    @Shadow
    private static void method_18347(Thread par1, Throwable par2) {}
    @Shadow
    private static ExecutorService createWorker(String name) {
        return null;
    }

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;createWorker(Ljava/lang/String;)Ljava/util/concurrent/ExecutorService;"))
    private static ExecutorService clinit$INVOKE$createWorker(String name) {
        if (name.equals("Main")) {
            return FastMcExtendScope.INSTANCE.getPool();
        } else {
            return createWorker(name);
        }
    }

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;createIoWorker()Ljava/util/concurrent/ExecutorService;"))
    private static ExecutorService clinit$INVOKE$createIoWorker() {
        return Executors.newCachedThreadPool((runnable) -> {
            Thread thread = new Thread(runnable);
            thread.setPriority(4);
            thread.setName("IO-Worker-" + NEXT_WORKER_ID.getAndIncrement());
            thread.setUncaughtExceptionHandler(MixinUtil::method_18347);
            return thread;
        });
    }
}