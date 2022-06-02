package me.luna.fastmc.mixin.patch;

import me.luna.fastmc.shared.util.FastMcExtendScope;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ExecutorService;

@Mixin(Util.class)
public abstract class MixinUtil {
    @Shadow
    private static ExecutorService createWorker(String name) {
        return null;
    }

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;createWorker(Ljava/lang/String;)Ljava/util/concurrent/ExecutorService;"))
    private static ExecutorService clinit$INVOKE$createWorker(String name) {
        if (!name.equals("Bootstrap")) {
            return FastMcExtendScope.INSTANCE.getPool();
        } else {
            return createWorker(name);
        }
    }
}
