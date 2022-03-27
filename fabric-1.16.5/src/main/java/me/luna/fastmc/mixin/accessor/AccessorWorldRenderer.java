package me.luna.fastmc.mixin.accessor;

import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldRenderer.class)
public interface AccessorWorldRenderer {
    @Accessor
    BuiltChunkStorage getChunks();

    @Accessor
    int getViewDistance();

    @Accessor
    MinecraftClient getClient();

    @Accessor
    ChunkBuilder getChunkBuilder();

    @Accessor
    boolean getNeedsTerrainUpdate();

    @Accessor
    void setNeedsTerrainUpdate(boolean needsTerrainUpdate);

    @Accessor
    int getCameraChunkX();

    @Accessor
    void setCameraChunkX(int cameraChunkX);

    @Accessor
    int getCameraChunkY();

    @Accessor
    void setCameraChunkY(int cameraChunkY);

    @Accessor
    int getCameraChunkZ();

    @Accessor
    void setCameraChunkZ(int cameraChunkZ);

    @Accessor
    double getLastCameraChunkUpdateX();

    @Accessor
    void setLastCameraChunkUpdateX(double lastCameraChunkUpdateX);

    @Accessor
    double getLastCameraChunkUpdateY();

    @Accessor
    void setLastCameraChunkUpdateY(double lastCameraChunkUpdateY);

    @Accessor
    double getLastCameraChunkUpdateZ();

    @Accessor
    void setLastCameraChunkUpdateZ(double lastCameraChunkUpdateZ);

    @Accessor
    ClientWorld getWorld();
}
