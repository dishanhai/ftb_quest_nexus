package com.dishanhai.ftb_quest_nexus.api;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Author: dishanhai. Server-side consent for changing the player bound to a block. */
public final class BindingTransfer {
    private static final String COMMAND = "ftbqn_binding";
    private static final long LIFETIME_MS = 60_000;
    private static final Map<UUID, Request> PENDING = new HashMap<>();

    private BindingTransfer() {}

    public static boolean request(BlockEntity entity, ServerPlayer requester, Supplier<UUID> currentOwner,
                                  Predicate<ServerPlayer> onAccept) {
        if (!(entity.getLevel() instanceof ServerLevel level) || requester.serverLevel() != level
                || entity.isRemoved()) return false;
        UUID ownerId = currentOwner.get();
        if (ownerId == null || ownerId.equals(requester.getUUID())) return false;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            requester.sendSystemMessage(message("owner_offline"));
            return false;
        }
        PENDING.values().removeIf(request -> !request.isCurrent());
        if (PENDING.values().stream().anyMatch(request -> request.entity() == entity)) {
            requester.sendSystemMessage(message("pending"));
            return true;
        }
        UUID token = UUID.randomUUID();
        PENDING.put(token, new Request(entity, level, ownerId, requester.getUUID(), Util.getMillis() + LIFETIME_MS,
                currentOwner, onAccept));
        owner.sendSystemMessage(message("prompt", requester.getDisplayName(), entity.getBlockState().getBlock().getName(),
                entity.getBlockPos().toShortString(), level.dimension().location().toString(),
                button(token, "accept", ChatFormatting.GREEN), button(token, "decline", ChatFormatting.RED)));
        requester.sendSystemMessage(message("sent", owner.getDisplayName()));
        return true;
    }

    public static boolean respond(ServerPlayer owner, UUID token, boolean accepted) {
        Request request = PENDING.get(token);
        if (request == null || request.level().getServer() != owner.getServer()
                || !request.ownerId().equals(owner.getUUID())) {
            owner.sendSystemMessage(message("expired"));
            return false;
        }
        PENDING.remove(token);
        if (!request.isCurrent()) {
            owner.sendSystemMessage(message("expired"));
            return false;
        }
        ServerPlayer requester = request.level().getServer().getPlayerList().getPlayer(request.requesterId());
        if (!accepted) {
            owner.sendSystemMessage(message("declined_owner"));
            if (requester != null) requester.sendSystemMessage(message("declined_requester"));
            return true;
        }
        if (requester == null || !request.onAccept().test(requester)) {
            owner.sendSystemMessage(message("unavailable"));
            if (requester != null) requester.sendSystemMessage(message("unavailable"));
            return false;
        }
        owner.sendSystemMessage(message("transferred", requester.getDisplayName()));
        requester.sendSystemMessage(message("received"));
        return true;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(COMMAND).then(Commands.argument("request", UuidArgument.uuid())
                .then(Commands.literal("accept").executes(context -> respond(context.getSource().getPlayerOrException(),
                        UuidArgument.getUuid(context, "request"), true) ? 1 : 0))
                .then(Commands.literal("decline").executes(context -> respond(context.getSource().getPlayerOrException(),
                        UuidArgument.getUuid(context, "request"), false) ? 1 : 0))));
    }

    public static void clear(MinecraftServer server) {
        PENDING.values().removeIf(request -> request.level().getServer() == server);
    }

    private static Component button(UUID token, String action, ChatFormatting color) {
        return Component.translatable("binding.ftb_quest_nexus." + action).withStyle(style -> style.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + COMMAND + " " + token + " " + action)));
    }

    private static Component message(String key, Object... args) {
        return Component.translatable("binding.ftb_quest_nexus." + key, args);
    }

    private record Request(BlockEntity entity, ServerLevel level, UUID ownerId, UUID requesterId, long expires,
                           Supplier<UUID> currentOwner, Predicate<ServerPlayer> onAccept) {
        private boolean isCurrent() {
            return Util.getMillis() < expires && !entity.isRemoved() && level.hasChunkAt(entity.getBlockPos())
                    && level.getBlockEntity(entity.getBlockPos()) == entity && ownerId.equals(currentOwner.get());
        }
    }
}
