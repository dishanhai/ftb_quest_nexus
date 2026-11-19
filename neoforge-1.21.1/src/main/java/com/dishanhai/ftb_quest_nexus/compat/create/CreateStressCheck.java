package com.dishanhai.ftb_quest_nexus.compat.create;

import com.dishanhai.ftb_quest_nexus.api.ftbq.ConsumableTask;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Explicit server-side check. The client does not provide coordinates, RPM or stress values. */
public final class CreateStressCheck {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ftbqn").then(Commands.literal("stress").then(Commands.literal("check")
                .then(Commands.argument("task", StringArgumentType.word()).executes(context ->
                        run(context.getSource(), StringArgumentType.getString(context, "task")))))));
    }

    private static int run(CommandSourceStack source, String taskId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        var file = ServerQuestFile.INSTANCE;
        if (file == null) return failure(source, "unavailable");
        long id;
        try { id = Long.parseUnsignedLong(taskId, 16); }
        catch (NumberFormatException invalid) { return failure(source, "task_invalid"); }
        if (!(file.getTask(id) instanceof CreateStressTask task)) return failure(source, "task_invalid");
        var team = file.getTeamData(player).orElse(null);
        if (team == null) return failure(source, "unavailable");
        int[] result = {0};
        file.withPlayerContext(player, () -> result[0] = submit(player, team, task));
        return result[0];
    }

    static int submit(ServerPlayer player, TeamData team, CreateStressTask task) {
        if (player.isSpectator() || player instanceof net.neoforged.neoforge.common.util.FakePlayer || !ConsumableTask.canSubmit(team, task)) return failure(player, "unavailable");
        if (!CreateStressometer.available()) return failure(player, "missing_mod");
        var level = player.serverLevel();

        double reach = Math.min(player.blockInteractionRange(), 6);
        var start = player.getEyePosition();
        var end = start.add(player.getLookAngle().scale(reach));
        // Vanilla's pick reads block states along the ray. Prove the whole ray is in loaded chunks first.
        int minX = (int)Math.floor(Math.min(start.x, end.x)) >> 4, maxX = (int)Math.floor(Math.max(start.x, end.x)) >> 4;
        int minZ = (int)Math.floor(Math.min(start.z, end.z)) >> 4, maxZ = (int)Math.floor(Math.max(start.z, end.z)) >> 4;
        for (int x=minX; x<=maxX; x++) for (int z=minZ; z<=maxZ; z++)
            if (level.getChunkSource().getChunkNow(x,z) == null) return failure(player, "inactive");
        if (!(player.pick(reach, 1, false) instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !level.shouldTickBlocksAt(new ChunkPos(hit.getBlockPos()).toLong())) return failure(player, "inactive");
        var reading = CreateStressometer.read(level.getBlockEntity(hit.getBlockPos()));
        if (reading == null) return failure(player, "inactive");
        if (!reading.reaches(task.metric(), task.getMaxProgress())) {
            player.displayClientMessage(Component.translatable(CreateStressTask.LANG + ".insufficient",
                    task.metric().value(reading), task.formatMaxProgress()), true);
            return 0;
        }
        team.setProgress(task, task.getMaxProgress());
        player.displayClientMessage(Component.translatable(CreateStressTask.LANG + ".passed"), true);
        return 1;
    }

    private static int failure(CommandSourceStack source, String suffix) {
        source.sendFailure(Component.translatable(CreateStressTask.LANG + "." + suffix));
        return 0;
    }

    private static int failure(ServerPlayer player, String suffix) {
        player.displayClientMessage(Component.translatable(CreateStressTask.LANG + "." + suffix), true);
        return 0;
    }

    private CreateStressCheck() {}
}
