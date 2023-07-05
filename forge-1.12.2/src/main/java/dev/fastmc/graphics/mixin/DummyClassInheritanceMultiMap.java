package dev.fastmc.graphics.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.world.World;

import java.util.Iterator;

public class DummyClassInheritanceMultiMap extends ClassInheritanceMultiMap<Entity> {
    private final World world;

    public DummyClassInheritanceMultiMap(World world) {
        super(Entity.class);
        this.world = world;
    }

    @Override
    public boolean isEmpty() {
        return world.getLoadedEntityList().isEmpty();
    }

    @Override
    public Iterator<Entity> iterator() {
        return world.getLoadedEntityList().iterator();
    }
}
