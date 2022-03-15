package me.luna.fastmc.mixin.patch.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerControllerMP.class)
public abstract class MixinPlayerControllerMP {
    @Shadow
    private int currentPlayerItem;

    /**
     * @author Luna
     * @reason Random NPE fix
     */
    @Overwrite
    private void syncCurrentPlayItem() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player != null) {
            int i = player.inventory.currentItem;

            if (i != this.currentPlayerItem) {
                this.currentPlayerItem = i;
                player.connection.sendPacket(new CPacketHeldItemChange(this.currentPlayerItem));
            }
        }
    }
}
