package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import appeng.api.stacks.AEKey;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IMekanismChemicalHandler;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Author: dishanhai. References to the optional Mekanism and Applied Mekanistics APIs. */
final class ChemicalStorage {
    private ChemicalStorage() {}

    static AEKey key(ResourceLocation id) {
        var holder = find(id);
        return holder == null ? null : AppliedStorage.key(holder);
    }

    static long submitFromInventory(Inventory inventory, ResourceLocation id, long required, boolean consume) {
        var chemical = find(id);
        if (chemical == null) return 0;
        long amount = transfer(inventory.items, chemical, required, consume);
        amount += transfer(inventory.offhand, chemical, required - amount, consume);
        if (consume && amount > 0) inventory.setChanged();
        return amount;
    }

    private static long transfer(List<ItemStack> stacks, Holder<Chemical> chemical, long required, boolean consume) {
        long amount = 0;
        for (int slot = 0; slot < stacks.size() && amount < required; slot++) {
            ItemStack original = stacks.get(slot);
            if (original.isEmpty() || (consume && original.getCount() != 1)) continue;
            ItemStack container = consume ? original.copy() : original;
            IChemicalHandler handler = container.getCapability(Capabilities.CHEMICAL.item());
            if (handler == null) continue;
            long transferred = transferFromTanks(handler, chemical, required - amount, consume);
            if (consume && transferred > 0) stacks.set(slot, container);
            amount += transferred;
        }
        return amount;
    }

    private static long transferFromTanks(IChemicalHandler handler, Holder<Chemical> chemical, long required,
                                          boolean consume) {
        long amount = 0;
        for (int tank = 0; tank < handler.getChemicalTanks() && amount < required; tank++) {
            ChemicalStack stored = handler.getChemicalInTank(tank);
            if (!stored.is(chemical)) continue;
            long before = stored.getAmount();
            long available = Math.min(required - amount, before);
            if (!consume) {
                amount += available;
                continue;
            }
            var nativeTank = handler instanceof IMekanismChemicalHandler nativeHandler
                    ? nativeHandler.getChemicalTank(tank, null) : null;
            ChemicalStack extracted = nativeTank == null ? handler.extractChemical(tank, available, Action.EXECUTE)
                    : nativeTank.extract(available, Action.EXECUTE, AutomationType.MANUAL);
            ChemicalStack remainder = handler.getChemicalInTank(tank);
            // Creative tanks can report a successful extraction without changing the container.
            long removed = extracted.getAmount();
            if ((!extracted.isEmpty() && !extracted.is(chemical)) || removed > available
                    || (!remainder.isEmpty() && !remainder.is(chemical)) || before - remainder.getAmount() != removed) {
                return 0;
            }
            amount += removed;
        }
        return amount;
    }

    static Component name(ResourceLocation id) {
        var holder = find(id);
        return holder == null ? ChemicalTask.missingName(id)
                : holder.value().getTextComponent();
    }

    static Icon icon(ResourceLocation id) {
        var holder = find(id);
        if (holder == null) return Icon.empty();
        Chemical chemical = holder.value();
        return Icon.getIcon(chemical.getIcon()).withTint(Color4I.rgb(chemical.getTint()));
    }

    static Holder.Reference<Chemical> find(ResourceLocation id) {
        if (id.equals(MekanismAPI.EMPTY_CHEMICAL_KEY.location())) return null;
        return MekanismAPI.CHEMICAL_REGISTRY.getHolder(id).orElse(null);
    }

    private static final class AppliedStorage {
        static AEKey key(Holder<Chemical> chemical) {
            return MekanismKey.of(new ChemicalStack(chemical, 1));
        }
    }
}
