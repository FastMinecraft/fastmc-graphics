package me.luna.fastmc.mixin.core.entity;

import me.luna.fastmc.entity.CowInfo;
import net.minecraft.entity.passive.CowEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CowEntity.class)
public abstract class MixinEntityCow implements CowInfo {
}
