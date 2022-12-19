package dev.fastmc.graphics.mixin.patch.world;

import dev.fastmc.graphics.mixin.IPatchedWorld;
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
    protected MixinWorldClient(
        ISaveHandler saveHandlerIn,
        WorldInfo info,
        WorldProvider providerIn,
        Profiler profilerIn,
        boolean client
    ) {
        super(saveHandlerIn, info, providerIn, profilerIn, client);
    }

    /**
     * @author Luna
     * @reason General optimization
     */
    @Overwrite
    public void removeAllEntities() {
        batchRemoveEntities();

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