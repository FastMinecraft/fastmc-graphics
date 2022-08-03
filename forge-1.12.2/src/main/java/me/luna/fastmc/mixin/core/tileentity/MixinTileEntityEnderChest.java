package me.luna.fastmc.mixin.core.tileentity;

import me.luna.fastmc.shared.instancing.IParallelUpdate;
import me.luna.fastmc.tileentity.EnderChestInfo;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.SoundCategory;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(TileEntityEnderChest.class)
public abstract class MixinTileEntityEnderChest extends TileEntity implements EnderChestInfo, IParallelUpdate {
    @Shadow
    public int numPlayersUsing;
    @Shadow
    public float lidAngle;
    @Shadow
    public float prevLidAngle;
    @Shadow
    private int ticksSinceSync;

    @Override
    public void updateParallel(@NotNull List<Runnable> async, @NotNull List<Runnable> callbacks) {
        if (++this.ticksSinceSync % 20 * 4 == 0) {
            async.add(() -> this.world.addBlockEvent(this.pos, Blocks.ENDER_CHEST, 1, this.numPlayersUsing));
        }

        this.prevLidAngle = this.lidAngle;
        int i = this.pos.getX();
        int j = this.pos.getY();
        int k = this.pos.getZ();

        if (this.numPlayersUsing > 0 && this.lidAngle == 0.0F) {
            double d0 = (double) i + 0.5D;
            double d1 = (double) k + 0.5D;
            async.add(() -> this.world.playSound(null, d0, (double) j + 0.5D, d1, SoundEvents.BLOCK_ENDERCHEST_OPEN, SoundCategory.BLOCKS, 0.5F, this.world.rand.nextFloat() * 0.1F + 0.9F));
        }

        if (this.numPlayersUsing == 0 && this.lidAngle > 0.0F || this.numPlayersUsing > 0 && this.lidAngle < 1.0F) {
            float f2 = this.lidAngle;

            if (this.numPlayersUsing > 0) {
                this.lidAngle += 0.1F;
            } else {
                this.lidAngle -= 0.1F;
            }

            if (this.lidAngle > 1.0F) {
                this.lidAngle = 1.0F;
            }

            if (this.lidAngle < 0.5F && f2 >= 0.5F) {
                double d3 = (double) i + 0.5D;
                double d2 = (double) k + 0.5D;
                async.add(() -> this.world.playSound(null, d3, (double) j + 0.5D, d2, SoundEvents.BLOCK_ENDERCHEST_CLOSE, SoundCategory.BLOCKS, 0.5F, this.world.rand.nextFloat() * 0.1F + 0.9F));
            }

            if (this.lidAngle < 0.0F) {
                this.lidAngle = 0.0F;
            }
        }
    }
}
