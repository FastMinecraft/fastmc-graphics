package dev.fastmc.graphics.mixin.patch;

import dev.fastmc.graphics.shared.util.FastMcCoreScope;
import dev.fastmc.graphics.shared.util.FastMcExtendScope;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ExecutorService;

@Mixin(Util.class)
public abstract class MixinUtil {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;createWorker(Ljava/lang/String;)Ljava/util/concurrent/ExecutorService;"))
    private static ExecutorService clinit$INVOKE$createWorker(String name) {
        if (name.equals("Bootstrap")) {
            return FastMcCoreScope.INSTANCE.getPool();
        } else {
            return FastMcExtendScope.INSTANCE.getPool();
        }
    }
}