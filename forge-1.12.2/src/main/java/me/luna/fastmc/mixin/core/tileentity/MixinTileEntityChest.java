package me.luna.fastmc.mixin.core.tileentity;

import me.luna.fastmc.shared.renderbuilder.IParallelUpdate;
import me.luna.fastmc.tileentity.ChestInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(TileEntityChest.class)
public abstract class MixinTileEntityChest extends TileEntity implements ChestInfo, IParallelUpdate {
    @Shadow
    public abstract void checkForAdjacentChests();

    @Shadow
    private int ticksSinceSync;

    @Shadow
    public int numPlayersUsing;

    @Shadow
    public float prevLidAngle;

    @Shadow
    public float lidAngle;

    @Shadow
    public TileEntityChest adjacentChestZNeg;

    @Shadow
    public TileEntityChest adjacentChestXNeg;

    @Shadow
    public TileEntityChest adjacentChestZPos;

    @Shadow
    public TileEntityChest adjacentChestXPos;

    @Override
    public void updateParallel(@NotNull List<Runnable> async, @NotNull List<Runnable> callbacks) {
        this.checkForAdjacentChests();
        int i = this.pos.getX();
        int j = this.pos.getY();
        int k = this.pos.getZ();
        ++this.ticksSinceSync;

        if (!this.world.isRemote && this.numPlayersUsing != 0 && (this.ticksSinceSync + i + j + k) % 200 == 0) {
            this.numPlayersUsing = 0;

            async.add(() -> {
                List<EntityPlayer> list = this.world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB((float) i - 5.0F, (float) j - 5.0F, (float) k - 5.0F, (float) (i + 1) + 5.0F, (float) (j + 1) + 5.0F, (float) (k + 1) + 5.0F));

                callbacks.add(() -> {
                    for (EntityPlayer entityplayer : list) {
                        if (entityplayer.openContainer instanceof ContainerChest) {
                            IInventory iinventory = ((ContainerChest) entityplayer.openContainer).getLowerChestInventory();

                            //noinspection EqualsBetweenInconvertibleTypes
                            if (iinventory == this || iinventory instanceof InventoryLargeChest && ((InventoryLargeChest) iinventory).isPartOfLargeChest((IInventory) this)) {
                                ++this.numPlayersUsing;
                            }
                        }
                    }
                });
            });
        }

        this.prevLidAngle = this.lidAngle;

        if (this.numPlayersUsing > 0 && this.lidAngle == 0.0F && this.adjacentChestZNeg == null && this.adjacentChestXNeg == null) {
            double d1 = (double) i + 0.5D;
            double d2 = (double) k + 0.5D;

            if (this.adjacentChestZPos != null) {
                d2 += 0.5D;
            }

            if (this.adjacentChestXPos != null) {
                d1 += 0.5D;
            }

            double finalD = d1;
            double finalD1 = d2;

            async.add(() -> this.world.playSound(null, finalD, (double) j + 0.5D, finalD1, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.5F, this.world.rand.nextFloat() * 0.1F + 0.9F));
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

            if (this.lidAngle < 0.5F && f2 >= 0.5F && this.adjacentChestZNeg == null && this.adjacentChestXNeg == null) {
                double d3 = (double) i + 0.5D;
                double d0 = (double) k + 0.5D;

                if (this.adjacentChestZPos != null) {
                    d0 += 0.5D;
                }

                if (this.adjacentChestXPos != null) {
                    d3 += 0.5D;
                }

                double finalD = d3;
                double finalD1 = d0;

                async.add(() -> this.world.playSound(null, finalD, (double) j + 0.5D, finalD1, SoundEvents.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 0.5F, this.world.rand.nextFloat() * 0.1F + 0.9F));
            }

            if (this.lidAngle < 0.0F) {
                this.lidAngle = 0.0F;
            }
        }
    }
}
