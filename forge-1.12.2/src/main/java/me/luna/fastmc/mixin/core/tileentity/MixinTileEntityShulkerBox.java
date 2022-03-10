package me.luna.fastmc.mixin.core.tileentity;

import me.luna.fastmc.shared.renderbuilder.IParallelUpdate;
import me.luna.fastmc.tileentity.ShulkerBoxInfo;
import net.minecraft.tileentity.TileEntityShulkerBox;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(TileEntityShulkerBox.class)
public abstract class MixinTileEntityShulkerBox implements ShulkerBoxInfo, IParallelUpdate {
    @Shadow
    private float progressOld;

    @Shadow
    private float progress;

    @Shadow
    private TileEntityShulkerBox.AnimationStatus animationStatus;

    @Shadow
    protected abstract void moveCollidedEntities();

    private final Runnable moveCollidedEntitiesCommand = this::moveCollidedEntities;

    @Override
    public void updateParallel(@NotNull List<Runnable> async, @NotNull List<Runnable> callbacks) {
        this.updateAnimationParallel(async);

        if (this.animationStatus == TileEntityShulkerBox.AnimationStatus.OPENING || this.animationStatus == TileEntityShulkerBox.AnimationStatus.CLOSING) {
            async.add(moveCollidedEntitiesCommand);
        }
    }

    private void updateAnimationParallel(@NotNull List<Runnable> commands) {
        this.progressOld = this.progress;

        switch (this.animationStatus) {
            case CLOSED:
                this.progress = 0.0F;
                break;
            case OPENING:
                this.progress += 0.1F;

                if (this.progress >= 1.0F) {
                    commands.add(moveCollidedEntitiesCommand);
                    this.animationStatus = TileEntityShulkerBox.AnimationStatus.OPENED;
                    this.progress = 1.0F;
                }
                break;
            case CLOSING:
                this.progress -= 0.1F;

                if (this.progress <= 0.0F) {
                    this.animationStatus = TileEntityShulkerBox.AnimationStatus.CLOSED;
                    this.progress = 0.0F;
                }
                break;
            case OPENED:
                this.progress = 1.0F;
        }
    }
}
