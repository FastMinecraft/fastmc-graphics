package me.luna.fastmc.mixin.patch.world;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import me.luna.fastmc.shared.util.DoubleBufferedCollection;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEventData;
import net.minecraft.network.play.server.SPacketBlockAction;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(WorldServer.class)
public abstract class MixinWorldServer extends World {
    @Shadow
    protected abstract boolean fireBlockEvent(BlockEventData event);

    @Shadow
    @Final
    private MinecraftServer server;

    protected MixinWorldServer(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client) {
        super(saveHandlerIn, info, providerIn, profilerIn, client);
    }

    private final DoubleBufferedCollection<IntSet> blockEventDataSet = new DoubleBufferedCollection<>(new IntOpenHashSet());
    private final DoubleBufferedCollection<List<BlockEventData>> blockEventDataList = new DoubleBufferedCollection<>(new ArrayList<>());

    /**
     * @author Luna
     * @reason Distinction/Memory allocation optimization
     */
    @Overwrite
    public void addBlockEvent(@NotNull BlockPos pos, @NotNull Block blockIn, int eventID, int eventParam) {
        int hash = pos.hashCode();
        hash = 31 * hash + blockIn.hashCode();
        hash = 31 * hash + eventID;
        hash = 31 * hash + eventParam;

        if (!blockEventDataSet.get().contains(hash)) {
            BlockEventData blockEventData = new BlockEventData(pos, blockIn, eventID, eventParam);
            blockEventDataList.get().add(blockEventData);
            blockEventDataSet.get().add(hash);
        }
    }

    /**
     * @author Luna
     * @reason Distinction/Memory allocation optimization
     */
    @Overwrite
    private void sendQueuedBlockEvents() {
        if (blockEventDataList.get().isEmpty()) return;

        blockEventDataSet.getAndSwap();
        List<BlockEventData> temp = blockEventDataList.getAndSwap();

        for (BlockEventData blockEventData : temp) {
            if (this.fireBlockEvent(blockEventData)) {
                this.server.getPlayerList().sendToAllNearExcept(null, blockEventData.getPosition().getX(), blockEventData.getPosition().getY(), blockEventData.getPosition().getZ(), 64.0D, this.provider.getDimension(), new SPacketBlockAction(blockEventData.getPosition(), blockEventData.getBlock(), blockEventData.getEventID(), blockEventData.getEventParameter()));
            }
        }
    }
}
