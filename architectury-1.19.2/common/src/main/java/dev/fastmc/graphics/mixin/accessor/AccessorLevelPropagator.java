package dev.fastmc.graphics.mixin.accessor;

import net.minecraft.world.chunk.light.LevelPropagator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelPropagator.class)
public interface AccessorLevelPropagator {
    @Invoker
    boolean invokeHasPendingUpdates();
}