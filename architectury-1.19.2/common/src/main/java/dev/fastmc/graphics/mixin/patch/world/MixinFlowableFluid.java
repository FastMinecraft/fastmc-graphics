package dev.fastmc.graphics.mixin.patch.world;

import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FlowableFluid.class)
public abstract class MixinFlowableFluid {
    private static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[]{ Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
    private static final Vec3d INVALID = new Vec3d(0.0, -1.0, 0.0);
    @Shadow
    @Final
    public static BooleanProperty FALLING;

    @Shadow
    protected abstract boolean isEmptyOrThis(FluidState state);

    @Shadow
    protected abstract boolean method_15749(BlockView world, BlockPos pos, Direction direction);

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public Vec3d getVelocity(BlockView world, BlockPos pos, FluidState state) {
        double x = 0.0;
        double z = 0.0;
        BlockPos.Mutable mutable1 = new BlockPos.Mutable();
        BlockPos.Mutable mutable2 = new BlockPos.Mutable();

        for (int i = 0; i < HORIZONTAL_DIRECTIONS.length; i++) {
            Direction direction = HORIZONTAL_DIRECTIONS[i];
            mutable1.set(pos, direction);
            FluidState fluidState = world.getFluidState(mutable1);
            if (this.isEmptyOrThis(fluidState)) {
                float f = fluidState.getHeight();
                float g = 0.0F;

                if (f == 0.0F) {
                    if (!world.getBlockState(mutable1).getMaterial().blocksMovement()) {
                        FluidState fluidState2 = world.getFluidState(mutable2.set(mutable1, Direction.DOWN));
                        if (this.isEmptyOrThis(fluidState2)) {
                            f = fluidState2.getHeight();
                            if (f > 0.0F) {
                                g = state.getHeight() - (f - 0.8888889F);
                            }
                        }
                    }
                } else if (f > 0.0F) {
                    g = state.getHeight() - f;
                }

                if (g != 0.0F) {
                    x += direction.getOffsetX() * g;
                    z += direction.getOffsetZ() * g;
                }
            }
        }

        if (state.get(FALLING)) {
            for (int i = 0; i < HORIZONTAL_DIRECTIONS.length; i++) {
                Direction direction = HORIZONTAL_DIRECTIONS[i];
                mutable1.set(pos, direction);

                if (this.method_15749(world, mutable1, direction)
                    || this.method_15749(world, mutable2.set(mutable1, Direction.UP), direction)) {
                    double length = MathHelper.sqrt((float) (x * x + z * z));
                    if (length < 1.0E-4D) {
                        return INVALID;
                    } else {
                        x /= length;
                        z /= length;
                        length = MathHelper.sqrt((float) (x * x + 36.0 + z * z));
                        return new Vec3d(x / length, -6.0 / length, z / length);
                    }
                }
            }
        }

        double length = MathHelper.sqrt((float) (x * x + z * z));
        return length < 1.0E-4D ? INVALID : new Vec3d(x / length, 0.0, z / length);
    }
}