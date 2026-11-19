package com.dishanhai.ftb_quest_nexus.compat.ars;

import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Ars Nouveau 1.21.1 ordinary carried jars, using the authoritative typed component. */
final class SourceJars {
    private static final ResourceLocation JAR = ResourceLocation.fromNamespaceAndPath("ars_nouveau", "source_jar");
    private static final ResourceLocation FILL = ResourceLocation.fromNamespaceAndPath("ars_nouveau", "block_fill_contents");
    private static final int CAPACITY = 10_000;
    private static final String EXPECTED_FILL = "com.hollingsworth.arsnouveau.common.items.data.BlockFillContents";

    static long submit(Inventory inventory, long limit, boolean consume) {
        if (limit <= 0) return 0;
        var type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(FILL);
        if (type == null) return 0;
        long total = transfer(inventory.items, inventory.items, type, limit, consume);
        total += transfer(inventory.offhand, inventory.items, type, limit - total, consume);
        if (consume && total > 0) inventory.setChanged();
        return total;
    }

    private static <T> long transfer(List<ItemStack> slots, List<ItemStack> splitDestination,
                                     DataComponentType<T> type, long limit, boolean consume) {
        long total = 0;
        for (int i = 0; i < slots.size() && total < limit; ++i) {
            var stack = slots.get(i);
            if (stack.isEmpty() || !JAR.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) continue;
            T component = stack.get(type);
            if (component == null || !component.getClass().getName().equals(EXPECTED_FILL)) continue;
            try {
                int perJar = (int) component.getClass().getMethod("amount").invoke(component);
                if (perJar <= 0 || perJar > CAPACITY) continue;
                int count = stack.getCount();
                long debit = Math.min(limit - total, (long) perJar * count);
                if (!consume) { total += debit; continue; }
                int slot = -1;
                if (debit % count != 0) slot = firstEmpty(splitDestination);
                var plan = SourceStackDebit.plan(perJar, count, debit, slot >= 0);
                if (plan.removed() == 0) continue;
                int additional = plan.splitCount();
                // Construct all replacement items before touching slots; preserve all other components.
                var reduced = stack.copyWithCount(count - additional);
                write(type, component, reduced, perJar - plan.basePerJar());
                ItemStack split = null;
                if (additional > 0) {
                    split = stack.copyWithCount(additional);
                    write(type, component, split, perJar - plan.basePerJar() - 1);
                }
                slots.set(i, reduced);
                if (split != null) splitDestination.set(slot, split);
                total += plan.removed();
            } catch (ReflectiveOperationException | LinkageError incompatibility) {
                // Optional unsupported version: never mutate or credit an unreadable jar.
            }
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    private static <T> void write(DataComponentType<T> type, T original, ItemStack item, int amount)
            throws ReflectiveOperationException {
        T updated = (T) original.getClass().getConstructor(int.class).newInstance(amount);
        item.set(type, updated);
    }

    private static int firstEmpty(List<ItemStack> slots) {
        for (int i = 0; i < slots.size(); i++) if (slots.get(i).isEmpty()) return i;
        return -1;
    }

    private SourceJars() {}
}
