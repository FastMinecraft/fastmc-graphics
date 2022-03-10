package me.luna.fastmc.mixin.core.world;

import me.luna.fastmc.mixin.IPatchedWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(World.class)
public abstract class MixinWorld implements IPatchedWorld {
}
