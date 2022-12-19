package dev.fastmc.graphics.mixin.core.entity;

import dev.fastmc.graphics.entity.CowInfo;
import net.minecraft.entity.passive.EntityCow;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityCow.class)
public abstract class MixinEntityCow implements CowInfo {
}