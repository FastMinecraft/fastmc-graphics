package me.xiaro.fastmc.mixin.core.entity;

import me.xiaro.fastmc.entity.EntityInfo;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Entity.class)
public abstract class MixinEntity implements EntityInfo<Entity> {
    private final int typeID = EntityInfo.super.getTypeID();

    @Override
    public Entity getEntity() {
        return (Entity) ((Object) this);
    }

    @Override
    public int getTypeID() {
        return typeID;
    }
}
