package com.dishanhai.ftb_quest_nexus.checker;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ConsumableTask;
import dev.ftb.mods.ftbquests.integration.item_filtering.ItemMatchingSystem;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.MobBucketItem;
import java.util.HashMap;
import java.util.Map;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Author: dishanhai. One read-only AE inventory snapshot per scan, in items and mB. */
final class InventoryCheck {
    private final KeyCounter items = new KeyCounter();
    private final KeyCounter fluids = new KeyCounter();
    private final KeyCounter storedFluids = new KeyCounter();
    private final Map<Item, KeyCounter> itemsByType = new HashMap<>();

    InventoryCheck(MEStorage storage) {
        for (var entry : storage.getAvailableStacks()) {
            long amount = entry.getLongValue();
            if (amount <= 0) continue;
            AEKey key = entry.getKey();
            if (key instanceof AEFluidKey fluid) {
                storedFluids.add(fluid, amount);
                addFluid(fluid, amount);
            } else if (key instanceof AEItemKey item) {
                items.add(item, amount);
                itemsByType.computeIfAbsent(item.getItem(), ignored -> new KeyCounter()).add(item, amount);
                // Read a single-item copy. Neither a drain nor an AE extraction is performed.
                FluidStack contained = FluidUtil.getFluidContained(item.toStack()).orElse(FluidStack.EMPTY);
                if (!contained.isEmpty()) {
                    long volume = multiply(amount, contained.getAmount());
                    addFluid(AEFluidKey.of(contained), volume);
                }
            }
        }
    }

    long available(Task task) {
        if (task instanceof FluidTask fluid) {
            if (fluid.getFluid() == net.minecraft.world.level.material.Fluids.EMPTY) return 0;
            FluidStack target = new FluidStack(fluid.getFluid(), 1000);
            target.applyComponents(fluid.getFluidDataComponentPatch());
            return fluids.get(AEFluidKey.of(target));
        }
        if (!(task instanceof ItemTask item) || item.isOnlyFromCrafting()) return 0;
        ItemStack target = item.getItemStack();
        boolean filter = ItemMatchingSystem.INSTANCE.isItemFilter(target);
        long total = 0;
        if (!filter && target.getItem() instanceof BucketItem && !(target.getItem() instanceof MobBucketItem)) {
            FluidStack contained = FluidUtil.getFluidContained(target.copyWithCount(1)).orElse(FluidStack.EMPTY);
            if (!contained.isEmpty()) {
                // Only loose ME fluid is added here; physical buckets are matched below.
                total = storedFluids.get(AEFluidKey.of(contained)) / contained.getAmount();
            }
        }
        KeyCounter candidates = !filter && !target.isEmpty() && item.getClass() == ItemTask.class
                ? itemsByType.get(target.getItem()) : items;
        if (candidates == null) return total;
        for (var entry : candidates) {
            AEItemKey key = (AEItemKey) entry.getKey();
            if (filter && key.matches(target)) continue;
            if (item.test(key.toStack())) total = add(total, entry.getLongValue());
        }
        return total;
    }

    static boolean supported(Task task) {
        return task instanceof FluidTask || task instanceof ItemTask item && !item.isOnlyFromCrafting();
    }

    static boolean complete(TeamData data, Task task, long available) {
        long required = task.getMaxProgress();
        if (!supported(task) || task.consumesResources() || required <= 0 || available < required
                || !ConsumableTask.canSubmit(data, task)) return false;
        // Set an absolute target only after the full stock is present. Never add snapshot counts.
        data.setProgress(task, required);
        return true;
    }

    private void addFluid(AEFluidKey key, long amount) {
        if (key == null) return;
        long current = fluids.get(key);
        fluids.add(key, Math.min(amount, Long.MAX_VALUE - current));
    }

    private static long add(long left, long right) {
        return left + Math.min(right, Long.MAX_VALUE - left);
    }

    private static long multiply(long count, long volume) {
        return count > Long.MAX_VALUE / volume ? Long.MAX_VALUE : count * volume;
    }
}


