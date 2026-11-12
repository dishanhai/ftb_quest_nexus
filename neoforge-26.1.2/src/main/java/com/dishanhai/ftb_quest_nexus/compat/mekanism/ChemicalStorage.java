package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import mekanism.api.chemical.ChemicalResource;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import appeng.api.stacks.AEKey;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;


/** Author: dishanhai. References to the optional Mekanism and Applied Mekanistics APIs. */
final class ChemicalStorage {
    private ChemicalStorage() {}

    static AEKey key(Identifier id) {
        var holder = find(id);
        return holder == null ? null : AppliedStorage.key(holder);
    }

    static long submitFromInventory(Inventory inventory, Identifier id, long required, boolean consume) {
        var chemical = find(id);
        if (chemical == null) return 0;
        var resource = ChemicalResource.of(chemical);
        long amount = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE && amount < required; slot++) {
            amount += transfer(inventory, slot, resource, required - amount, consume);
        }
        if (amount < required) {
            amount += transfer(inventory, Inventory.SLOT_OFFHAND, resource, required - amount, consume);
        }
        if (consume && amount > 0) inventory.setChanged();
        return amount;
    }

    private static long transfer(Inventory inventory, int slot, ChemicalResource chemical, long required,
                                 boolean consume) {
        ItemStack original = inventory.getItem(slot);
        if (original.isEmpty() || (consume && original.getCount() != 1)) return 0;
        ItemStack container = consume ? original.copy() : original;
        var handler = ItemAccess.forStack(container).getCapability(Capabilities.CHEMICAL.item());
        if (handler == null) return 0;
        long amount = 0;
        if (!consume) {
            for (int tank = 0; tank < handler.size() && amount < required; tank++) {
                if (chemical.equals(handler.getResource(tank))) {
                    amount += Math.min(required - amount, handler.getAmountAsLong(tank));
                }
            }
            return amount;
        }
        try (var transaction = Transaction.openRoot()) {
            for (int tank = 0; tank < handler.size() && amount < required; tank++) {
                if (!chemical.equals(handler.getResource(tank))) continue;
                long before = handler.getAmountAsLong(tank);
                int requested = (int) Math.min(Integer.MAX_VALUE, Math.min(required - amount, before));
                int extracted = handler.extract(tank, chemical, requested, transaction);
                // Creative tanks can report extraction without changing the container.
                if (before - handler.getAmountAsLong(tank) != extracted || extracted > requested) return 0;
                amount += extracted;
            }
            transaction.commit();
        }
        if (amount > 0) inventory.setItem(slot, container);
        return amount;
    }

    static Component name(Identifier id) {
        var holder = find(id);
        return holder == null ? ChemicalTask.missingName(id)
                : holder.value().getTextComponent();
    }

    static Icon icon(Identifier id) {
        var holder = find(id);
        if (holder == null) return Icon.empty();
        Chemical chemical = holder.value();
        return Icon.getIcon(chemical.getIcon()).withTint(Color4I.rgb(chemical.getTint()));
    }

    static Holder.Reference<Chemical> find(Identifier id) {
        if (id.equals(MekanismAPI.EMPTY_CHEMICAL_KEY.identifier())) return null;
        return MekanismAPI.CHEMICAL_REGISTRY.get(id).orElse(null);
    }

    private static final class AppliedStorage {
        static AEKey key(Holder<Chemical> chemical) {
            return MekanismKey.of(new ChemicalStack(chemical, 1));
        }
    }
}
