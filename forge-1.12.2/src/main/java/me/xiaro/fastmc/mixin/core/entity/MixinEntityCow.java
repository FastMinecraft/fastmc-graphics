package me.xiaro.fastmc.mixin.core.entity;

import me.xiaro.fastmc.entity.CowInfo;
import net.minecraft.entity.passive.EntityCow;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityCow.class)
public abstract class MixinEntityCow implements CowInfo {
}
