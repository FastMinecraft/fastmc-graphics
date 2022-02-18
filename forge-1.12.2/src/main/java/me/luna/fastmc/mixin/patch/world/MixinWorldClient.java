package me.luna.fastmc.mixin.patch.world;

import me.luna.fastmc.mixin.IPatchedWorld;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(WorldClient.class)
public abstract class MixinWorldClient extends World implements IPatchedWorld {
    protected MixinWorldClient(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client) {
        super(saveHandlerIn, info, providerIn, profilerIn, client);
    }

    /**
     * @author Luna
     * @reason General optimization
     */
    @Overwrite
    public void removeAllEntities() {
        this.loadedEntityList.removeIf(it -> getUnloadedEntitiesOverride().contains(it.getEntityId()));

        for (Entity entity : unloadedEntityList) {
            int chunkCoordX = entity.chunkCoordX;
            int chunkCoordZ = entity.chunkCoordZ;

            if (entity.addedToChunk && isChunkLoaded(chunkCoordX, chunkCoordZ, true)) {
                this.getChunk(chunkCoordX, chunkCoordZ).removeEntity(entity);
            }
        }

        for (Entity entity : unloadedEntityList) {
            this.onEntityRemoved(entity);
        }

        this.getUnloadedEntitiesOverride().clear();
        this.unloadedEntityList.clear();

        for (Entity entity : loadedEntityList) {
            Entity ridingEntity = entity.getRidingEntity();

            if (ridingEntity != null) {
                if (!ridingEntity.isDead && ridingEntity.isPassenger(entity)) {
                    continue;
                }

                entity.dismountRidingEntity();
            }

            if (entity.isDead) {
                markRemoving(entity);
            }
        }

        batchRemoveEntities();
    }
}
