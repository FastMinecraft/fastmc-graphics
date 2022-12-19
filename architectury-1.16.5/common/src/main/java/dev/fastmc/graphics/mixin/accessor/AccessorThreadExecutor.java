package dev.fastmc.graphics.mixin.accessor;

import net.minecraft.util.thread.ThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Queue;

@Mixin(ThreadExecutor.class)
public interface AccessorThreadExecutor<R> {
    @Accessor
    Queue<R> getTasks();
}