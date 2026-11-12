package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import mekanism.api.chemical.ChemicalResource;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import mekanism.api.chemical.Chemical;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Author: dishanhai. Packages confirmed chemical rewards into native Mekanism tanks. */
public final class ChemicalPackaging {
    private ChemicalPackaging() {}

    public static boolean canPackage(Identifier chemicalId) {
        return ModList.get().isLoaded("mekanism") && WithMekanism.canPackage(chemicalId);
    }

    /** Returns the mB delivered into the player's inventory. */
    public static long give(ServerPlayer player, Identifier chemicalId, long amount) {
        return amount > 0 && ModList.get().isLoaded("mekanism")
                ? WithMekanism.give(player.getInventory(), chemicalId, amount) : 0;
    }

    private static final class WithMekanism {
        private static boolean canPackage(Identifier chemicalId) {
            Holder<Chemical> chemical = ChemicalStorage.find(chemicalId);
            if (chemical == null) return false;
            var tank = tankOf(new ItemStack(MekanismBlocks.BASIC_CHEMICAL_TANK));
            if (tank == null) return false;
            try (var transaction = Transaction.openRoot()) {
                return tank.insert(0, ChemicalResource.of(chemical), 1, transaction) == 1;
            }
        }

        private static long give(Inventory inventory, Identifier chemicalId, long amount) {
            Holder<Chemical> chemical = ChemicalStorage.find(chemicalId);
            if (chemical == null) return 0;

            var resource = ChemicalResource.of(chemical);
            long delivered = 0;
            while (delivered < amount) {
                int slot = inventory.getFreeSlot();
                if (slot < 0) break;

                ItemStack item = new ItemStack(MekanismBlocks.BASIC_CHEMICAL_TANK);
                var tank = tankOf(item);
                if (tank == null || tank.getAmountAsLong(0) != 0) break;

                int requested = (int) Math.min(amount - delivered, tank.getCapacityAsInt(0, resource));
                if (requested <= 0) break;
                int inserted;
                try (var transaction = Transaction.openRoot()) {
                    inserted = tank.insert(0, resource, requested, transaction);
                    if (inserted <= 0 || inserted > requested || !resource.equals(tank.getResource(0))
                            || tank.getAmountAsLong(0) != inserted) break;
                    transaction.commit();
                }

                inventory.setItem(slot, item);
                delivered += inserted;
            }
            if (delivered > 0) inventory.setChanged();
            return delivered;
        }

        private static ResourceHandler<ChemicalResource> tankOf(ItemStack item) {
            var handler = ItemAccess.forStack(item).getCapability(Capabilities.CHEMICAL.item());
            return handler != null && handler.size() == 1 ? handler : null;
        }
    }
}
