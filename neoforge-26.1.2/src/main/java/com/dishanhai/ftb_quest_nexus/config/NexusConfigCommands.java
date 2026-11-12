package com.dishanhai.ftb_quest_nexus.config;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** Author: dishanhai. The server is the authority for machine defaults and automation rules. */
public final class NexusConfigCommands {
    private static final String KEY = "config.ftb_quest_nexus.server";
    private static final SimpleCommandExceptionType SAVE_FAILED =
            new SimpleCommandExceptionType(Component.translatable(KEY + ".save_failed"));

    private NexusConfigCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var config = Commands.literal("config").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(context -> {
                    for (ServerOption option : ServerOption.values()) report(context.getSource(), option, false);
                    return ServerOption.values().length;
                });
        var defaults = Commands.literal("defaults");
        var rules = Commands.literal("rules");
        for (ServerOption option : ServerOption.values()) {
            var group = option.section.equals("defaults") ? defaults : rules;
            group.then(Commands.literal(option.key)
                    .executes(context -> report(context.getSource(), option, false))
                    .then(Commands.argument("enabled", BoolArgumentType.bool()).executes(context -> {
                        if (!ServerConfig.save(option, BoolArgumentType.getBool(context, "enabled"))) {
                            throw SAVE_FAILED.create();
                        }
                        report(context.getSource(), option, true);
                        return 1;
                    })));
        }
        dispatcher.register(Commands.literal("ftbqn").then(config.then(defaults).then(rules)));
    }

    private static int report(CommandSourceStack source, ServerOption option, boolean broadcast) {
        boolean value = ServerConfig.get(option);
        source.sendSuccess(() -> Component.translatable(KEY + ".value",
                Component.translatable(option.translationKey()),
                Component.translatable(value ? "options.on" : "options.off")), broadcast);
        return value ? 1 : 0;
    }
}
