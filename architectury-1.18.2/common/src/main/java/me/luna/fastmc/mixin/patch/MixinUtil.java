package me.luna.fastmc.mixin.patch;

import net.minecraft.Bootstrap;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(Util.class)
public abstract class MixinUtil {
    @Shadow
    @Final
    private static AtomicInteger NEXT_WORKER_ID;

    @Shadow
    public static <T extends Throwable> T throwOrPause(T t) {
        return null;
    }

    /**
     * @author Luna
     * @reason Thread priority
     */
    @Overwrite
    private static ExecutorService createIoWorker() {
        return Executors.newCachedThreadPool((runnable) -> {
            Thread thread = new Thread(runnable);
            thread.setPriority(1);
            thread.setName("IO-Worker-" + NEXT_WORKER_ID.getAndIncrement());
            thread.setUncaughtExceptionHandler((t, throwable) -> {
                //noinspection ResultOfMethodCallIgnored
                throwOrPause(throwable);
                if (throwable instanceof CompletionException) {
                    throwable = throwable.getCause();
                }

                if (throwable instanceof CrashException) {
                    Bootstrap.println(((CrashException) throwable).getReport().asString());
                    System.exit(-1);
                }

                Util.LOGGER.error(String.format("Caught exception in t %s", t), throwable);
            });
            return thread;
        });
    }
}
