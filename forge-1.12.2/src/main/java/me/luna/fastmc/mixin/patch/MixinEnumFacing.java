package me.luna.fastmc.mixin.patch;

import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnumFacing.class)
public class MixinEnumFacing {

    private static EnumFacing[] valuesOverride;

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/EnumFacing;values()[Lnet/minecraft/util/EnumFacing;"))
    private static EnumFacing[] clinit$Redirect$INVOKE$values() {
        valuesOverride = new EnumFacing[]{ EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST };
        return valuesOverride;
    }

    /**
     * @author Luna
     * @reason Memory allocation optimization
     */
    @Overwrite
    public static EnumFacing[] values() {
        return valuesOverride;
    }
}
