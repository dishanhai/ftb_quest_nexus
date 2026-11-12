package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import com.dishanhai.ftb_quest_nexus.config.ServerConfig;
import com.dishanhai.ftb_quest_nexus.config.ServerOption;
import com.dishanhai.ftb_quest_nexus.network.ChemicalRewardPackets;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.ToLongFunction;

/** Author: dishanhai. Server-side AE delivery and explicit tank redemption. */
public final class ChemicalRewards {
    private static final String COMMAND = "nexus_chemical_rewards";
    private static final String LANG = ChemicalReward.LANG + ".delivery.";

    private ChemicalRewards() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(COMMAND).executes(context -> open(context.getSource().getPlayerOrException())));
    }

    public static boolean hasPending(ServerPlayer player) {
        return player != null && ledger(player).first(player.getUUID()) != null;
    }

    public static void remind(ServerPlayer player) {
        if (hasPending(player)) sendReopen(player);
    }

    static void receive(ServerPlayer player, Identifier chemical, long amount) {
        receive(player, chemical, amount, entry -> ServerConfig.get(ServerOption.NETWORK_REWARDS)
                ? insertBound(player, entry) : 0);
    }

    public static boolean claimFromSubmitter(ServerPlayer player, TeamData data, ChemicalReward reward,
                                             MEStorage storage, IActionSource source) {
        if (player == null || hasPending(player) || !ChemicalTask.isMekanismAvailable()
                || !data.markRewardAsClaimed(player.getUUID(), reward, System.currentTimeMillis())) return false;
        receive(player, reward.chemical(), reward.amount(), entry -> {
            var key = ChemicalTask.isAvailable() ? ChemicalStorage.key(entry.chemical()) : null;
            return key == null ? 0 : storage.insert(key, entry.amount(), Actionable.MODULATE, source);
        });
        return true;
    }

    private static void receive(ServerPlayer player, Identifier chemical, long amount,
                                ToLongFunction<ChemicalRewardLedger.Pending> insertion) {
        var ledger = ledger(player);
        UUID owner = player.getUUID();
        boolean first = ledger.first(owner) == null;
        var entry = ledger.add(owner, chemical, amount);
        ledger.deliver(owner, entry.id(), insertion);
        if (first && ledger.first(owner) != null) sendOffer(player, ledger.first(owner));
    }

    private static int open(ServerPlayer player) {
        var ledger = ledger(player);
        UUID owner = player.getUUID();
        var pending = ledger.first(owner);
        if (pending == null) {
            player.sendSystemMessage(Component.translatable(LANG + "none"));
            return 0;
        }
        long inserted = ledger.deliver(owner, pending.id(), entry -> insertBound(player, entry));
        if (inserted > 0) player.sendSystemMessage(Component.translatable(LANG + "inserted", inserted));
        var remaining = ledger.first(owner);
        if (remaining != null) sendOffer(player, remaining);
        return 1;
    }

    public static void packagePending(ServerPlayer player, UUID id) {
        var ledger = ledger(player);
        UUID owner = player.getUUID();
        var pending = ledger.first(owner);
        if (pending == null || !pending.id().equals(id)) return;
        long packaged = ledger.deliver(owner, id, entry -> ChemicalTask.isMekanismAvailable()
                && ChemicalPackaging.canPackage(entry.chemical())
                ? ChemicalPackaging.give(player, entry.chemical(), entry.amount()) : 0);
        ledger.invalidateOffer(owner, id);
        if (packaged > 0) player.sendSystemMessage(Component.translatable(LANG + "packaged", packaged));
        if (packaged < pending.amount()) {
            player.sendSystemMessage(Component.translatable(LANG + "remaining", pending.amount() - packaged));
        }
        remind(player);
    }

    private static long insertBound(ServerPlayer player, ChemicalRewardLedger.Pending entry) {
        var key = ChemicalTask.isAvailable() ? ChemicalStorage.key(entry.chemical()) : null;
        return key == null ? 0 : SubmitterBlockEntity.insertChemicalReward(player, key, entry.amount());
    }

    private static void sendOffer(ServerPlayer player, ChemicalRewardLedger.Pending pending) {
        boolean canPackage = ChemicalTask.isMekanismAvailable() && ChemicalPackaging.canPackage(pending.chemical());
        PacketDistributor.sendToPlayer(player, new ChemicalRewardPackets.Offer(pending.id(), pending.chemical(),
                pending.amount(), canPackage));
        sendReopen(player);
    }

    private static void sendReopen(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable(LANG + "reopen").withStyle(style ->
                style.withClickEvent(new ClickEvent.RunCommand( "/" + COMMAND))));
    }

    private static ChemicalRewardLedger ledger(ServerPlayer player) {
        return ChemicalRewardLedger.get(player.level().getServer());
    }
}
