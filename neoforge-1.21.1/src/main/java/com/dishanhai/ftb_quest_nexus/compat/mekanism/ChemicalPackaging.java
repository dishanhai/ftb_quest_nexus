package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Author: dishanhai. Packages confirmed chemical rewards into native Mekanism tanks. */
public final class ChemicalPackaging {
    private ChemicalPackaging() {}

    public static boolean canPackage(ResourceLocation chemicalId) {
        return ModList.get().isLoaded("mekanism") && WithMekanism.canPackage(chemicalId);
    }

    /** Returns the mB delivered into the player's inventory. */
    public static long give(ServerPlayer player, ResourceLocation chemicalId, long amount) {
        return amount > 0 && ModList.get().isLoaded("mekanism")
                ? WithMekanism.give(player.getInventory(), chemicalId, amount) : 0;
    }

    private static final class WithMekanism {
        private static boolean canPackage(ResourceLocation chemicalId) {
            Holder<Chemical> chemical = ChemicalStorage.find(chemicalId);
            if (chemical == null) return false;
            IChemicalTank tank = tankOf(new ItemStack(MekanismBlocks.BASIC_CHEMICAL_TANK));
            return tank != null && tank.insert(new ChemicalStack(chemical, 1), Action.SIMULATE,
                    AutomationType.MANUAL).isEmpty();
        }

        private static long give(Inventory inventory, ResourceLocation chemicalId, long amount) {
            Holder<Chemical> chemical = ChemicalStorage.find(chemicalId);
            if (chemical == null) return 0;

            long delivered = 0;
            while (delivered < amount) {
                int slot = inventory.getFreeSlot();
                if (slot < 0) break;

                ItemStack item = new ItemStack(MekanismBlocks.BASIC_CHEMICAL_TANK);
                IChemicalTank tank = tankOf(item);
                if (tank == null || !tank.isEmpty()) break;

                long requested = Math.min(amount - delivered, tank.getCapacity());
                if (requested <= 0) break;
                var remainder = tank.insert(new ChemicalStack(chemical, requested), Action.EXECUTE, AutomationType.MANUAL);
                var stored = tank.getStack();
                long inserted = stored.getAmount();
                if (!stored.is(chemical) || inserted <= 0 || inserted > requested
                        || inserted != requested - remainder.getAmount()) break;

                inventory.setItem(slot, item);
                delivered += inserted;
            }
            if (delivered > 0) inventory.setChanged();
            return delivered;
        }

        private static IChemicalTank tankOf(ItemStack item) {
            var handler = item.getCapability(Capabilities.CHEMICAL.item());
            return handler instanceof IMekanismChemicalHandler nativeHandler && nativeHandler.getChemicalTanks() == 1
                    ? nativeHandler.getChemicalTank(0, null) : null;
        }
    }
}
