package com.dishanhai.ftb_quest_nexus.submitter;

import com.dishanhai.ftb_quest_nexus.NexusContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Author: dishanhai. Actions use vanilla's container-id checked button packet. */
public final class SubmitterMenu extends AbstractContainerMenu {
    public final SubmitterBlockEntity submitter;
    public SubmitterMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, findSubmitter(inventory, data));
    }
    private static SubmitterBlockEntity findSubmitter(Inventory inventory, FriendlyByteBuf data) {
        var entity = inventory.player.level().getBlockEntity(data.readBlockPos());
        return entity instanceof SubmitterBlockEntity submitter ? submitter : null;
    }
    public SubmitterMenu(int id, Inventory inventory, SubmitterBlockEntity submitter) {
        super(NexusContent.MENU.get(), id);
        this.submitter = submitter;
    }
    @Override public boolean stillValid(Player player) {
        return submitter != null && !submitter.isRemoved() && player.level().getBlockEntity(submitter.getBlockPos()) == submitter
                && player.distanceToSqr(submitter.getBlockPos().getX() + .5, submitter.getBlockPos().getY() + .5, submitter.getBlockPos().getZ() + .5) <= 64;
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer serverPlayer) || button < 0 || button > 3 || !stillValid(player)) return false;
        return submitter.applyAction(serverPlayer, button);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
