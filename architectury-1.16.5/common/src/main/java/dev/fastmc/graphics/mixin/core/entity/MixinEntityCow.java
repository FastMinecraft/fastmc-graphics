package dev.fastmc.graphics.mixin.core.entity;

import dev.fastmc.graphics.entity.CowInfo;
import net.minecraft.entity.passive.CowEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CowEntity.class)
public abstract class MixinEntityCow implements CowInfo {
}