package com.dishanhai.ftb_quest_nexus.compat.pneumatic;

import appeng.api.stacks.AEKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/** Optional upstream item capability and ME key. Amounts are unscaled mL, not pressure. */
final class AirStorage {
    private static final Set<String> CANISTERS = Set.of("air_canister", "reinforced_air_canister", "reinforced_air_canister_array");
    private static final Set<String> PORTABLE = Set.of("1k","4k","16k","64k","256k","1m","4m","16m","64m","256m");
    private record Access(ItemCapability<?, ?> capability, Method getAir, Method addAir) {}
    private static final class Pneumatic {
        static final Access ACCESS = load();
        private static Access load() {
            try {
                var caps = Class.forName("me.desht.pneumaticcraft.api.PNCCapabilities");
                var type = Class.forName("me.desht.pneumaticcraft.api.tileentity.IAirHandler");
                return new Access((ItemCapability<?, ?>) caps.getField("AIR_HANDLER_ITEM").get(null),
                        type.getMethod("getAir"), type.getMethod("addAir", int.class));
            } catch (ReflectiveOperationException | LinkageError incompatible) { return null; }
        }
    }
    private static final class Applied {
        static final AEKey KEY = load();
        static final Method READ = findCounter();
        private static Method findCounter() {
            try { return Class.forName("com.wintercogs.appliedpneumatics.common.items.IAirStorageCell")
                    .getMethod("getStoredAir", ItemStack.class); }
            catch (ReflectiveOperationException | LinkageError incompatible) { return null; }
        }
        private static AEKey load() {
            try {
                return (AEKey) Class.forName("com.wintercogs.appliedpneumatics.common.me.keys.AirKey")
                        .getField("INSTANCE").get(null);
            } catch (ReflectiveOperationException | LinkageError incompatible) { return null; }
        }
    }

    static AEKey key() { return Applied.KEY; }
    static boolean available() { return Pneumatic.ACCESS != null; }

    static boolean dedicatedContainer(String namespace, String path) {
        return namespace.equals("pneumaticcraft") && CANISTERS.contains(path)
                || namespace.equals("appliedpneumatics") && path.startsWith("portable_air_cell_")
                && PORTABLE.contains(path.substring("portable_air_cell_".length()));
    }

    static long submit(Inventory inventory, long required, boolean consume) {
        var access = Pneumatic.ACCESS;
        if (access == null) return 0;
        long amount = transfer(inventory.items, access, required, consume);
        amount += transfer(inventory.offhand, access, required - amount, consume);
        if (consume && amount > 0) inventory.setChanged();
        return amount;
    }

    static long transfer(List<ItemStack> stacks, long required, boolean consume) {
        var access = Pneumatic.ACCESS;
        return access == null ? 0 : transfer(stacks, access, required, consume);
    }

    @SuppressWarnings("unchecked")
    private static long transfer(List<ItemStack> stacks, Access access, long required, boolean consume) {
        long moved = 0;
        for (int slot = 0; slot < stacks.size() && moved < required; slot++) {
            var original = stacks.get(slot);
            if (original.isEmpty() || original.getCount() != 1) continue;
            var id = BuiltInRegistries.ITEM.getKey(original.getItem());
            if (!dedicatedContainer(id.getNamespace(), id.getPath())) continue;
            // Even a third-party provider that mutates during reads only sees a disposable copy.
            var copy = original.copy();
            try {
                Object handler = copy.getCapability((ItemCapability<Object, Void>) access.capability());
                if (handler == null) continue;
                boolean applied = id.getNamespace().equals("appliedpneumatics");
                if (applied && Applied.READ == null) continue;
                long before = applied ? (long) Applied.READ.invoke(null, copy) : (int) access.getAir().invoke(handler);
                int requested = (int) Math.min(Integer.MAX_VALUE, Math.min(required - moved, Math.max(0, before)));
                if (requested <= 0) continue;
                if (!consume) { moved += requested; continue; }
                access.addAir().invoke(handler, -requested);
                Object refreshed = copy.getCapability((ItemCapability<Object, Void>) access.capability());
                if (refreshed == null) continue;
                long after = applied ? (long) Applied.READ.invoke(null, copy) : (int) access.getAir().invoke(refreshed);
                int delta = acceptedDebit(before, after, requested);
                if (delta == 0) continue;
                stacks.set(slot, copy);
                moved += delta;
            } catch (ReflectiveOperationException | LinkageError incompatible) {
                // The original inventory slot was not touched.
            }
        }
        return moved;
    }

    static int acceptedDebit(long before, long after, int requested) {
        if (after < 0 || before <= after || requested <= 0) return 0;
        long delta = before - after;
        return delta <= requested ? (int) delta : 0;
    }

    private AirStorage() {}
}
