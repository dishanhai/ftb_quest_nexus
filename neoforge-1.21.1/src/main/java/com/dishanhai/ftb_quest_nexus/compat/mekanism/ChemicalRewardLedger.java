package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToLongFunction;

/** Author: dishanhai. Outstanding chemical reward amounts, owned by the FTBQ claimant. */
final class ChemicalRewardLedger extends SavedData {
    private static final Factory<ChemicalRewardLedger> FACTORY =
            new Factory<>(ChemicalRewardLedger::new, ChemicalRewardLedger::load, null);
    private final Map<UUID, LinkedHashMap<UUID, Pending>> players = new HashMap<>();

    static ChemicalRewardLedger get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "ftb_quest_nexus_chemical_rewards");
    }

    Pending add(UUID player, ResourceLocation chemical, long amount) {
        var pending = new Pending(UUID.randomUUID(), chemical, amount);
        players.computeIfAbsent(player, ignored -> new LinkedHashMap<>()).put(pending.id(), pending);
        setDirty();
        return pending;
    }

    Pending first(UUID player) {
        var entries = players.get(player);
        return entries == null ? null : entries.firstEntry().getValue();
    }

    long deliver(UUID player, UUID id, ToLongFunction<Pending> delivery) {
        var entries = players.get(player);
        var pending = entries == null ? null : entries.get(id);
        if (pending == null) return 0;
        long delivered = Math.clamp(delivery.applyAsLong(pending), 0, pending.amount());
        if (delivered == 0) return 0;
        if (delivered == pending.amount()) {
            entries.remove(id);
            if (entries.isEmpty()) players.remove(player);
        } else {
            entries.put(id, new Pending(id, pending.chemical(), pending.amount() - delivered));
        }
        setDirty();
        return delivered;
    }

    void invalidateOffer(UUID player, UUID id) {
        var entries = players.get(player);
        if (entries == null) return;
        var pending = entries.remove(id);
        if (pending == null) return;
        UUID nextId = UUID.randomUUID();
        entries.putFirst(nextId, new Pending(nextId, pending.chemical(), pending.amount()));
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        players.forEach((player, pending) -> pending.values().forEach(delivery -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", player);
            entry.putUUID("id", delivery.id());
            entry.putString("chemical", delivery.chemical().toString());
            entry.putLong("amount", delivery.amount());
            entries.add(entry);
        }));
        tag.put("pending", entries);
        return tag;
    }

    static ChemicalRewardLedger load(CompoundTag tag, HolderLookup.Provider registries) {
        var ledger = new ChemicalRewardLedger();
        var entries = tag.getList("pending", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.getCompound(i);
            ResourceLocation chemical = ResourceLocation.tryParse(entry.getString("chemical"));
            long amount = entry.getLong("amount");
            if (!entry.hasUUID("player") || !entry.hasUUID("id") || chemical == null || amount <= 0) continue;
            var pending = new Pending(entry.getUUID("id"), chemical, amount);
            ledger.players.computeIfAbsent(entry.getUUID("player"), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(pending.id(), pending);
        }
        return ledger;
    }

    record Pending(UUID id, ResourceLocation chemical, long amount) {}
}
