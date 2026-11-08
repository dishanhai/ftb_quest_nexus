package com.dishanhai.ftb_quest_nexus.submitter;

// Author: dishanhai. Real block/FTBQ/AE regression paths; only network power is bypassed.
import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.IStorageProvider;
import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqAutoClaimDeferral;
import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftblibrary.config.Tristate;
import dev.ftb.mods.ftbquests.quest.*;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ChoiceRewardNotices;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import dev.ftb.mods.ftbquests.quest.loot.WeightedReward;
import dev.ftb.mods.ftbquests.quest.reward.AllTableReward;
import dev.ftb.mods.ftbquests.quest.reward.ChoiceReward;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.XPLevelsReward;
import dev.ftb.mods.ftbquests.quest.reward.XPReward;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.XPTask;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import net.minecraft.world.level.material.Fluids;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(QuestNexus.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SubmitterRegressionGameTests {
    private SubmitterRegressionGameTests() {}

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void foreignTeamCannotRebind(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            helper.assertTrue(f.entity.applyAction(f.owner, 0), "Initial binding failed");
            CompoundTag before = userData(f);
            ServerPlayer outsider = player(helper);
            outsider.setPos(f.owner.position());
            helper.assertTrue(!f.entity.canManage(outsider), "Fixture did not use separate teams");
            helper.assertTrue(!f.entity.applyAction(outsider, 0), "Foreign player rebound the submitter");
            helper.assertTrue(before.equals(userData(f)), "Rejected binding changed user data");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void offlineOwnerCannotBeReplacedAndPendingRewardsPreventBinding(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            helper.assertTrue(f.entity.applyAction(f.owner, 0), "Initial binding failed");
            CompoundTag bound = userData(f);
            CompoundTag departed = userData(f);
            departed.putString("rewardPlayerIdText", UUID.randomUUID().toString());
            f.entity.loadUserData(departed);
            helper.assertTrue(!f.entity.applyAction(f.owner, 0), "Same-team member replaced an offline owner");
            helper.assertTrue(departed.equals(userData(f)), "Rejected binding changed saved data");
            CompoundTag user = bound;
            net.minecraft.nbt.ListTag pending = new net.minecraft.nbt.ListTag();
            CompoundTag entry = new CompoundTag();
            entry.put("key", AEItemKey.of(Items.GOLD_INGOT).toTag());
            entry.putLong("amount", 3);
            pending.add(entry);
            user.put("FtbqPendingRewards", pending);
            f.entity.loadUserData(user);
            f.entity.applyAction(f.owner, 0);
            helper.assertTrue(user.equals(userData(f)), "Rebinding altered pending rewards");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void originalOwnerMustApproveTransferBeforeExperienceCanFollowBinding(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            BindingOwner original = new BindingOwner(f.teammate);
            CompoundTag saved = userData(f);
            saved.putString("rewardPlayerIdText", original.getUUID().toString());
            f.entity.loadUserData(saved);

            withOnlinePlayers(List.of(original, f.owner), () -> {
                helper.assertTrue(f.entity.applyAction(f.owner, 0), "Same-team transfer request was not sent");
                helper.assertTrue(original.prompts.size() == 1, "Original owner did not receive the request");
                helper.assertTrue(saved.equals(userData(f)), "A pending request changed the binding");
                helper.assertTrue(f.entity.applyAction(f.owner, 0) && original.prompts.size() == 1,
                        "Repeated request sent another approval prompt");

                String firstAccept = approvalCommand(original, true);
                helper.assertTrue(executeBindingCommand(f.owner, firstAccept) == 0 && saved.equals(userData(f)),
                        "The requester could approve their own transfer");
                helper.assertTrue(executeBindingCommand(original, approvalCommand(original, false)) == 1
                                && saved.equals(userData(f)),
                        "Declining the request changed the binding");
                helper.assertTrue(executeBindingCommand(original, firstAccept) == 0,
                        "A declined approval could still be used");

                helper.assertTrue(f.entity.applyAction(f.owner, 0) && original.prompts.size() == 2,
                        "A new approval request was not sent after decline");
                String accepted = approvalCommand(original, true);
                helper.assertTrue(executeBindingCommand(original, accepted) == 1, "Original owner could not approve");
                helper.assertTrue(f.owner.getUUID().toString().equals(f.entity.getRewardPlayerIdText())
                                && f.entity.getQueueSize() == 1,
                        "Approved same-team transfer did not preserve the queue and change the binding");
                helper.assertTrue(executeBindingCommand(original, accepted) == 0,
                        "A used approval token could be replayed");
            });

            XPReward reward = new XPReward(f.file.newID(), f.quest, 100);
            f.quest.addReward(reward);
            f.file.refreshIDMap();
            int before = f.owner.totalExperience;
            runWork(f);
            helper.assertTrue(f.owner.totalExperience == before + 100
                            && f.data.isRewardClaimed(f.owner.getUUID(), reward),
                    "Experience did not follow the approved binding");
        });
    }

    private static String approvalCommand(BindingOwner owner, boolean accepted) {
        Component prompt = owner.prompts.getLast();
        Object[] args = ((TranslatableContents) prompt.getContents()).getArgs();
        Component button = (Component) args[args.length - (accepted ? 2 : 1)];
        return button.getStyle().getClickEvent().getValue().substring(1);
    }

    private static int executeBindingCommand(ServerPlayer player, String command) {
        try {
            return player.getServer().getCommands().getDispatcher().execute(command, player.createCommandSourceStack());
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new AssertionError(e);
        }
    }

    private static void withOnlinePlayers(List<ServerPlayer> players, Runnable action) {
        helperReflect(() -> {
            var field = PlayerList.class.getDeclaredField("playersByUUID");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<UUID, ServerPlayer> online = (Map<UUID, ServerPlayer>) field.get(players.getFirst().getServer().getPlayerList());
            Map<UUID, ServerPlayer> previous = new HashMap<>();
            for (ServerPlayer player : players) previous.put(player.getUUID(), online.put(player.getUUID(), player));
            try {
                action.run();
            } finally {
                previous.forEach((id, player) -> {
                    if (player == null) online.remove(id);
                    else online.put(id, player);
                });
            }
        });
    }

    private static final class BindingOwner extends ServerPlayer {
        private final List<Component> prompts = new ArrayList<>();

        BindingOwner(ServerPlayer player) {
            super(player.getServer(), player.serverLevel(), player.getGameProfile());
        }

        @Override
        public void sendSystemMessage(Component message) {
            if (message.getContents() instanceof TranslatableContents text
                    && (text.getKey().equals("binding.ftb_quest_nexus.prompt")
                    || text.getKey().equals("chat.ftb_quest_nexus.choice_rewards.ready"))) prompts.add(message);
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void registrationIncludesEveryEligibleItemTarget(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            ItemTask second = itemTask(f.file, f.quest, Items.GOLD_INGOT);
            ItemTask nonConsuming = itemTask(f.file, f.quest, Items.DIAMOND);
            nonConsuming.setConsumeItems(Tristate.FALSE);
            ItemTask crafting = new ItemTask(f.file.newID(), f.quest) {
                @Override public boolean isOnlyFromCrafting() { return true; }
            };
            crafting.setConsumeItems(Tristate.TRUE);
            f.quest.addTask(crafting);
            f.file.refreshIDMap();
            helper.assertTrue(f.entity.applyAction(f.owner, 0), "Binding failed");
            UUID id = UUID.fromString(f.entity.getSubmitterIdText());
            helper.assertTrue(SubmitterBlockEntity.queueTask(f.owner, id, f.task.id), "Registration rejected");
            helper.assertTrue(f.entity.getQueueSize() == 2, "Registration omitted a sibling item target");
            helper.assertTrue(f.entity.getQueuedTaskIds().contains(second.id), "Second target missing");
            SubmitterBlockEntity.queueTask(f.owner, id, f.task.id);
            helper.assertTrue(f.entity.getQueueSize() == 2, "Repeated registration duplicated targets");
            ServerPlayer outsider = player(helper);
            helper.assertTrue(!SubmitterBlockEntity.queueTask(outsider, id, f.task.id),
                    "Foreign player registered targets");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void automaticPersonalRewardReturnsToAeWithoutTakingTeammateReward(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            ItemReward reward = automaticReward(f, false);
            runWork(f);
            AEItemKey gold = AEItemKey.of(Items.GOLD_INGOT);
            helper.assertTrue(f.storage.items.get(gold) == 8, "Automatic reward bypassed AE: iron="
                    + f.storage.items.get(AEItemKey.of(Items.IRON_INGOT)) + ", progress=" + f.data.getProgress(f.task)
                    + ", claimed=" + f.data.isRewardClaimed(f.owner.getUUID(), reward)
                    + ", status=" + f.entity.getStatusComponent().getString());
            helper.assertTrue(!f.owner.getInventory().contains(new ItemStack(Items.GOLD_INGOT)),
                    "Bound player also received the reward");
            helper.assertTrue(f.teammate.getInventory().contains(new ItemStack(Items.GOLD_INGOT)),
                    "Submitter suppressed another player's personal reward");
            helper.assertTrue(f.data.isRewardClaimed(f.owner.getUUID(), reward), "Reward claim was not recorded");
            runWork(f);
            helper.assertTrue(f.storage.items.get(gold) == 8, "Automatic reward duplicated on retry");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void automaticTeamRewardReturnsToAeEvenWithAnotherPlayerContext(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            automaticReward(f, true);
            f.file.withPlayerContext(f.teammate, () -> runWork(f));
            helper.assertTrue(f.storage.items.get(AEItemKey.of(Items.GOLD_INGOT)) == 8,
                    "Team reward was claimed by another player before AE");
            helper.assertTrue(!f.teammate.getInventory().contains(new ItemStack(Items.GOLD_INGOT)),
                    "Team reward also reached a player");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void allTableRewardReturnsEveryEntryToAeOnce(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            RewardTable table = new RewardTable(f.file.newID(), f.file);
            table.addReward(new WeightedReward(new ItemReward(0, f.quest, new ItemStack(Items.GOLD_INGOT), 2), 1));
            table.addReward(new WeightedReward(new ItemReward(0, f.quest, new ItemStack(Items.EMERALD), 3), 8));
            table.addReward(new WeightedReward(new ItemReward(0, f.quest, new ItemStack(Items.GOLD_INGOT), 4), 1));
            f.file.addRewardTable(table);
            f.file.refreshIDMap();
            AllTableReward reward = new AllTableReward(f.file.newID(), f.quest);
            reward.setTable(table);
            configureAutomaticReward(f, reward, true);
            helper.assertTrue(reward.getTable() == table, "Fixture lost the reward table during config round trip");

            runWork(f);
            runWork(f);
            helper.assertTrue(f.storage.items.get(AEItemKey.of(Items.GOLD_INGOT)) == 6
                            && f.storage.items.get(AEItemKey.of(Items.EMERALD)) == 3,
                    "Every table entry must reach AE exactly once");
            helper.assertTrue(f.data.isRewardClaimed(f.owner.getUUID(), reward), "Table claim was not recorded");
            helper.assertTrue(!f.owner.getInventory().contains(new ItemStack(Items.GOLD_INGOT))
                            && !f.teammate.getInventory().contains(new ItemStack(Items.GOLD_INGOT)),
                    "The team reward also reached a player");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void choiceRewardsOpenOnePrivateBatchAndRejectReplay(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            Quest secondQuest = new Quest(f.file.newID(), f.quest.getChapter());
            f.quest.getChapter().addQuest(secondQuest);
            ItemTask secondTask = itemTask(f.file, secondQuest, Items.IRON_INGOT);
            RewardTable table = new RewardTable(f.file.newID(), f.file);
            table.addReward(table.makeWeightedItemReward(new ItemStack(Items.GOLD_INGOT), 1));
            table.addReward(table.makeWeightedItemReward(new ItemStack(Items.EMERALD), 1));
            f.file.addRewardTable(table);
            f.file.refreshIDMap();
            ChoiceReward first = new ChoiceReward(f.file.newID(), f.quest);
            first.setTable(table);
            configureAutomaticReward(f, first, true);
            helper.assertTrue(first.getTable() == table, "Fixture lost the choice table during config round trip");
            ChoiceReward second = new ChoiceReward(f.file.newID(), secondQuest);
            second.setTable(table);
            secondQuest.addReward(second);
            f.file.refreshIDMap();
            helper.assertTrue(SubmitterBlockEntity.queueTask(f.owner, UUID.fromString(f.entity.getSubmitterIdText()),
                    secondTask.id), "Second quest could not be queued");

            BindingOwner owner = new BindingOwner(f.owner);
            BindingOwner teammate = new BindingOwner(f.teammate);
            withOnlinePlayers(List.of(owner, teammate), () -> {
                runWork(f);
                runWork(f);
                runWork(f);
                helper.assertTrue(owner.prompts.size() == 1 && teammate.prompts.isEmpty(),
                        "Choice prompt was duplicated or sent to an unbound player");
                helper.assertTrue(!f.data.isRewardClaimed(owner.getUUID(), first),
                        "Automatic claim consumed the player's choice");
                String click = owner.prompts.getFirst().getStyle().getClickEvent().getValue().substring(1);
                var dispatcher = new com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
                List<ChoiceRewardNotices.Batch> opened = new ArrayList<>();
                ChoiceRewardNotices.register(dispatcher, SubmitterBlockEntity::availableChoiceRewards,
                        (recipient, batch) -> {
                            helper.assertTrue(recipient == owner, "Choice page went to another player");
                            opened.add(batch);
                        });
                try {
                    helper.assertTrue(dispatcher.execute(click, teammate.createCommandSourceStack()) == 0,
                            "An unbound player could use the prompt");
                    helper.assertTrue(dispatcher.execute(click, owner.createCommandSourceStack()) == 1,
                            "Prompt did not open the choice page");
                } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                    throw new AssertionError(e);
                }
                var batch = opened.getFirst();
                helper.assertTrue(opened.size() == 1 && batch.rewardIds().equals(List.of(first.id, second.id)),
                        "The choice page omitted a queued quest reward: " + batch.rewardIds());
                List<Integer> delivered = new ArrayList<>();
                ChoiceRewardNotices.ChoiceDelivery delivery = (recipient, data, reward, index) -> {
                    if (data.markRewardAsClaimed(recipient.getUUID(), reward, System.currentTimeMillis())) {
                        delivered.add(index);
                    }
                };
                var selections = Map.of(first.id, 1, second.id, 0);
                helper.assertTrue(ChoiceRewardNotices.select(teammate, batch.token(), selections,
                        SubmitterBlockEntity::availableChoiceRewards, delivery) == null,
                        "Another player used the owner's batch token");
                var updated = ChoiceRewardNotices.select(owner, batch.token(), selections,
                        SubmitterBlockEntity::availableChoiceRewards, delivery);
                helper.assertTrue(updated != null && updated.rewardIds().isEmpty()
                                && delivered.size() == 2 && delivered.containsAll(List.of(0, 1)),
                        "The batch did not claim each chosen option");
                helper.assertTrue(ChoiceRewardNotices.select(owner, batch.token(), selections,
                        SubmitterBlockEntity::availableChoiceRewards, delivery) == null && delivered.size() == 2,
                        "The same choice batch could be replayed");
            });
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void fullAeRetainsAutomaticRewardAndRetriesExactlyOnce(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            automaticReward(f, false);
            f.storage.capacity = 0;
            runWork(f);
            helper.assertTrue(userData(f).getList("FtbqPendingRewards", 10).getCompound(0).getLong("amount") == 8,
                    "Full AE lost the deferred reward");
            helper.assertTrue(!f.owner.getInventory().contains(new ItemStack(Items.GOLD_INGOT)),
                    "Full AE sent the reward to the player");
            f.storage.capacity = 100;
            runWork(f);
            helper.assertTrue(userData(f).getList("FtbqPendingRewards", 10).isEmpty(), "Retry did not flush");
            helper.assertTrue(f.storage.items.get(AEItemKey.of(Items.GOLD_INGOT)) == 8,
                    "Retry lost or duplicated the automatic reward");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void legacySubmitterClaimsExperienceWithoutPlacementData(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            CompoundTag saved = userData(f);
            helper.assertTrue(!saved.contains("placerId"), "Legacy fixture has a placement UUID");
            f.entity.loadUserData(saved);
            XPReward xp = new XPReward(f.file.newID(), f.quest, 12);
            configureAutomaticReward(f, xp, false);
            automaticReward(f, false);
            int before = f.owner.totalExperience;
            runWork(f);
            helper.assertTrue(f.owner.totalExperience == before + 12, "XP auto-claim was suppressed");
            helper.assertTrue(f.storage.items.get(AEItemKey.of(Items.GOLD_INGOT)) == 8, "Item reward missed AE");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void fullAeDefersAllItemRewardsAcrossReloadAndQueueClear(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            ItemReward first = automaticReward(f, false);
            ItemReward second = new ItemReward(f.file.newID(), f.quest, new ItemStack(Items.EMERALD), 8);
            configureAutomaticReward(f, second, false);
            AEItemKey gold = AEItemKey.of(Items.GOLD_INGOT);
            AEItemKey emerald = AEItemKey.of(Items.EMERALD);
            f.storage.capacity = 54;
            runWork(f);
            helper.assertTrue(f.data.isRewardClaimed(f.owner.getUUID(), first)
                            && !f.data.isRewardClaimed(f.owner.getUUID(), second)
                            && !f.data.isRewardClaimed(f.teammate.getUUID(), second),
                    "Native replay claimed a second item reward while the AE buffer was full");

            f.entity.loadUserData(userData(f));
            helper.assertTrue(f.entity.applyAction(f.owner, 1), "Fixture could not clear the manual queue");
            f.data.checkAutoCompletion(f.quest);
            helper.assertTrue(!f.data.isRewardClaimed(f.owner.getUUID(), second)
                            && !f.data.isRewardClaimed(f.teammate.getUUID(), second),
                    "Native completion bypassed a loaded pending AE reservation");
            f.storage.capacity = 70;
            runWork(f);
            helper.assertTrue(f.storage.items.get(gold) == 8 && f.storage.items.get(emerald) == 8
                            && f.data.isRewardClaimed(f.owner.getUUID(), second),
                    "Clearing the queue after reload lost a previously reserved AE reward");
            helper.assertTrue(f.data.isRewardClaimed(f.teammate.getUUID(), second),
                    "Native personal auto-claim did not resume for the teammate");
            runWork(f);
            helper.assertTrue(f.storage.items.get(gold) == 8 && f.storage.items.get(emerald) == 8,
                    "Reward replay inserted a duplicate");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void manualAndMatchAdvancePastZeroCooldownRepeatables(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            CompoundTag config = new CompoundTag();
            f.quest.writeData(config);
            config.putBoolean("can_repeat", true);
            config.putInt("repeat_cooldown", 0);
            f.quest.readData(config);
            automaticReward(f, false);
            Quest later = new Quest(f.file.newID(), f.quest.getChapter());
            f.quest.getChapter().addQuest(later);
            ItemTask laterTask = itemTask(f.file, later, Items.DIAMOND);
            f.file.refreshIDMap();
            f.storage.items.add(AEItemKey.of(Items.DIAMOND), 20);
            UUID id = UUID.fromString(f.entity.getSubmitterIdText());
            helper.assertTrue(SubmitterBlockEntity.queueTask(f.owner, id, laterTask.id), "Second task was not queued");

            runWork(f);
            helper.assertTrue(f.data.getProgress(f.task) == 0 && f.data.getProgress(laterTask) == 0,
                    "Fixture did not reset its first quest or ran more than one submission");
            runWork(f);
            helper.assertTrue(f.data.isCompleted(laterTask), "Repeatable quest starved the manual queue");

            f.data.setProgress(laterTask, 0);
            helper.assertTrue(f.entity.applyAction(f.owner, 6), "Fixture could not enable matching");
            runWork(f);
            helper.assertTrue(f.data.getProgress(laterTask) == 0, "Match mode ran more than one submission");
            runWork(f);
            helper.assertTrue(f.data.isCompleted(laterTask), "Repeatable quest starved matching");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void largeQueuesSurviveBinaryNbtRoundTrip(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            List<Long> ids = java.util.stream.LongStream.range(0, 4000)
                    .mapToObj(i -> 0x123456789ABC0000L + i).toList();
            String legacy = ids.stream().map(id -> Long.toUnsignedString(id, 16))
                    .collect(java.util.stream.Collectors.joining(","));
            CompoundTag seed = userData(f);
            seed.remove("queuedTaskIds");
            seed.remove("matchedTaskIds");
            seed.putString("queuedTaskIdsText", legacy);
            seed.putString("matchedTaskIdsText", legacy);
            f.entity.loadUserData(seed);
            helper.assertTrue(f.entity.getQueuedTaskIds().equals(ids), "Legacy task IDs failed to load in order");
            try {
                var bytes = new java.io.ByteArrayOutputStream();
                try (var output = new java.io.DataOutputStream(bytes)) {
                    net.minecraft.nbt.NbtIo.write(userData(f), output);
                }
                var input = new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()));
                CompoundTag restored = net.minecraft.nbt.NbtIo.read(input);
                f.entity.loadUserData(restored);
                helper.assertTrue(f.entity.getQueuedTaskIds().equals(ids), "Saved manual queue lost ordered task IDs");
                helper.assertTrue(f.entity.applyAction(f.owner, 6) && f.entity.getQueuedTaskIds().equals(ids),
                        "Saved match history lost ordered task IDs");
            } catch (java.io.IOException e) {
                throw new AssertionError("Large task queues could not be saved", e);
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void experienceRewardsFollowBindingAcrossReload(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            CompoundTag saved = userData(f);
            saved.remove("rewardPlayerIdText");
            saved.putUUID("placerId", f.teammate.getUUID());
            f.entity.loadUserData(saved);
            helper.assertTrue(f.entity.applyAction(f.owner, 0), "Fixture binding failed");
            f.entity.loadUserData(userData(f));
            XPReward points = new XPReward(f.file.newID(), f.quest, 3);
            XPLevelsReward levels = new XPLevelsReward(f.file.newID(), f.quest, 5);
            f.quest.addReward(points);
            f.quest.addReward(levels);
            f.file.refreshIDMap();
            int beforePoints = f.owner.totalExperience;
            int beforeLevels = f.owner.experienceLevel;
            int teammatePoints = f.teammate.totalExperience;
            int teammateLevels = f.teammate.experienceLevel;

            runWork(f);
            runWork(f);

            helper.assertTrue(f.owner.totalExperience == beforePoints + 3
                            && f.owner.experienceLevel == beforeLevels + 5,
                    "The bound player did not receive exactly 3 points and 5 levels");
            helper.assertTrue(f.data.isRewardClaimed(f.owner.getUUID(), points)
                            && f.data.isRewardClaimed(f.owner.getUUID(), levels),
                    "Personal experience claims did not use the bound player UUID");
            helper.assertTrue(f.teammate.totalExperience == teammatePoints
                            && f.teammate.experienceLevel == teammateLevels,
                    "The previous placement UUID received experience");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void offlineBoundPlayerKeepsTeamExperienceUnclaimedUntilReturn(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            XPLevelsReward levels = new XPLevelsReward(f.file.newID(), f.quest, 5);
            configureAutomaticReward(f, levels, true);
            f.onlineMembers.remove(f.owner);
            int before = f.owner.experienceLevel;
            int teammateBefore = f.teammate.experienceLevel;

            f.file.withPlayerContext(f.teammate, () -> runWork(f));
            helper.assertTrue(f.data.isCompleted(f.quest)
                            && !f.data.isRewardClaimed(f.owner.getUUID(), levels),
                    "An offline bound player lost an unclaimed team experience reward");
            helper.assertTrue(f.teammate.experienceLevel == teammateBefore, "Team XP went to another player");

            f.onlineMembers.add(f.owner);
            runWork(f);
            runWork(f);
            helper.assertTrue(f.data.isRewardClaimed(f.owner.getUUID(), levels)
                            && f.owner.experienceLevel == before + 5,
                    "The bound player's experience was not delivered exactly once on return");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void allRewardsClaimsExperienceFromAnExperienceOnlyQuest(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            f.quest.removeTask(f.task);
            XPTask task = new XPTask(f.file.newID(), f.quest);
            task.setValue(1);
            f.quest.addTask(task);
            XPLevelsReward reward = new XPLevelsReward(f.file.newID(), f.quest, 5);
            f.quest.addReward(reward);
            f.file.refreshIDMap();
            helper.assertTrue(f.entity.applyAction(f.owner, 1), "Fixture did not clear the submitter queue");
            helper.assertTrue(f.entity.applyAction(f.owner, 3), "Fixture did not enable all-quest rewards");
            int before = f.owner.experienceLevel;
            runWork(f);
            helper.assertTrue(f.owner.experienceLevel == before, "Incomplete quest granted experience");

            f.data.setProgress(task, 1);
            runWork(f);
            helper.assertTrue(f.owner.experienceLevel == before + 5
                            && f.data.isRewardClaimed(f.owner.getUUID(), reward),
                    "All-quest mode missed experience from a completed XP-only quest");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void queuedCheckmarkQuestClaimsExperienceForBoundPlayer(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            f.quest.removeTask(f.task);
            CheckmarkTask task = new CheckmarkTask(f.file.newID(), f.quest);
            f.quest.addTask(task);
            XPReward reward = new XPReward(f.file.newID(), f.quest, 100);
            ItemReward itemReward = new ItemReward(f.file.newID(), f.quest, new ItemStack(Items.GOLD_INGOT), 8);
            f.quest.addReward(reward);
            f.quest.addReward(itemReward);
            f.file.refreshIDMap();
            helper.assertTrue(f.entity.applyAction(f.owner, 1), "Fixture did not clear the submitter queue");
            helper.assertTrue(f.entity.isClaimOnlyQueuedRewards(), "Fixture is not in queue-only mode");
            int before = f.owner.totalExperience;
            int teammateBefore = f.teammate.totalExperience;
            f.data.setProgress(task, 1);
            runWork(f);
            helper.assertTrue(f.owner.totalExperience == before, "Unregistered quest granted experience");

            UUID submitterId = UUID.fromString(f.entity.getSubmitterIdText());
            helper.assertTrue(SubmitterBlockEntity.queueTask(f.owner, submitterId, task.id),
                    "Completed checkmark quest with an XP reward could not be registered");
            f.entity.loadUserData(userData(f));
            f.onlineMembers.remove(f.owner);
            runWork(f);
            helper.assertTrue(!f.data.isRewardClaimed(f.owner.getUUID(), reward),
                    "The reward-only entry marked experience claimed while the bound player was offline");
            f.onlineMembers.add(f.owner);
            runWork(f);
            runWork(f);

            helper.assertTrue(f.entity.getQueueSize() == 1
                            && f.entity.queuePage(0, List.of(task.id), false).rows().get(0).valid(),
                    "The registered XP quest was removed or displayed as an invalid queue entry");
            helper.assertTrue(f.owner.totalExperience == before + 100
                            && f.teammate.totalExperience == teammateBefore
                            && f.data.isRewardClaimed(f.owner.getUUID(), reward),
                    "Queued quest did not award exactly 100 points to the bound player UUID");
            helper.assertTrue(!f.data.isRewardClaimed(f.owner.getUUID(), itemReward),
                    "A reward-only registration claimed an item reward");
            helper.assertTrue(f.entity.applyAction(f.owner, 3), "Fixture did not enable all-quest rewards");
            runWork(f);
            helper.assertTrue(f.storage.items.get(AEItemKey.of(Items.GOLD_INGOT)) == 8,
                    "All-quest mode did not claim the item reward");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void claimScopeRestoresAfterNestedFailure(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            ItemReward reward = automaticReward(f, false);
            try {
                FtbqAutoClaimDeferral.run(f.data, QuestWork::canClaimItem, () -> {
                    helper.assertTrue(FtbqAutoClaimDeferral.shouldDefer(f.data, reward),
                            "Owner's reward was not reserved");
                    helper.assertTrue(!FtbqAutoClaimDeferral.shouldDefer(
                                    new TeamData(UUID.randomUUID(), f.file), reward),
                            "Scope intercepted another team's rewards");
                    try {
                        FtbqAutoClaimDeferral.run(f.data, r -> false, () -> {
                            throw new IllegalStateException("nested regression probe");
                        });
                    } catch (IllegalStateException expected) {
                        helper.assertTrue(FtbqAutoClaimDeferral.shouldDefer(f.data, reward),
                                "Nested failure did not restore outer scope");
                    }
                    throw new IllegalStateException("outer regression probe");
                });
            } catch (IllegalStateException expected) {
                helper.assertTrue(!FtbqAutoClaimDeferral.shouldDefer(f.data, reward),
                        "Failed submission leaked its reward scope");
            }
            f.data.addProgress(f.task, 10);
            helper.assertTrue(f.owner.getInventory().contains(new ItemStack(Items.GOLD_INGOT)),
                    "Later ordinary completion no longer uses vanilla rewards");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void repeatablePersonalRewardCannotResetBeforeAeClaims(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            CompoundTag config = new CompoundTag();
            f.quest.writeData(config);
            config.putBoolean("can_repeat", true);
            f.quest.readData(config);
            automaticReward(f, false);
            runWork(f);
            helper.assertTrue(f.storage.items.get(AEItemKey.of(Items.GOLD_INGOT)) == 8,
                    "Teammate auto-claim reset the repeatable quest before AE delivery");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void queuePagesKeepOrderAndReportProgressWithoutConsuming(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            ItemTask last = f.task;
            for (int i = 0; i < 12; i++) last = itemTask(f.file, f.quest, Items.GOLD_INGOT);
            f.file.refreshIDMap();
            SubmitterBlockEntity.queueTask(f.owner, UUID.fromString(f.entity.getSubmitterIdText()), f.task.id);
            f.data.addProgress(f.task, 4);
            CompoundTag before = userData(f);
            long stock = f.storage.items.get(AEItemKey.of(Items.IRON_INGOT));
            CompoundTag first = f.entity.writeQueueState(-1);
            var rows = first.getList("rows", net.minecraft.nbt.Tag.TAG_COMPOUND);
            helper.assertTrue(first.getInt("page") == 0 && first.getInt("pages") == 3,
                    "Queue page bounds/count incorrect");
            helper.assertTrue(rows.size() == 6 && rows.getCompound(0).getLong("id") == f.task.id,
                    "First page lost queue order or size");
            helper.assertTrue(rows.getCompound(0).getLong("progress") == 4
                    && rows.getCompound(0).getLong("required") == 10, "Queue progress incorrect");
            CompoundTag end = f.entity.writeQueueState(Integer.MAX_VALUE);
            var tail = end.getList("rows", net.minecraft.nbt.Tag.TAG_COMPOUND);
            helper.assertTrue(end.getInt("page") == 2 && tail.size() == 1
                    && tail.getCompound(0).getLong("id") == last.id, "Last page lost its final target");
            f.data.addProgress(f.task, 6);
            helper.assertTrue(f.entity.writeQueueState(0).getList("rows", net.minecraft.nbt.Tag.TAG_COMPOUND)
                    .getCompound(0).getBoolean("completed"), "Completed target was not marked");
            helper.assertTrue(before.equals(userData(f)) && f.storage.items.get(AEItemKey.of(Items.IRON_INGOT)) == stock,
                    "Viewing queue changed saved data or AE stock");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void queueMenusPageIndependentlyAndClampAfterClear(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            for (int i = 0; i < 6; i++) itemTask(f.file, f.quest, Items.GOLD_INGOT);
            f.file.refreshIDMap();
            SubmitterBlockEntity.queueTask(f.owner, UUID.fromString(f.entity.getSubmitterIdText()), f.task.id);
            SubmitterMenu first = new SubmitterMenu(70, f.owner.getInventory(), f.entity);
            SubmitterMenu second = new SubmitterMenu(71, f.owner.getInventory(), f.entity);
            helper.assertTrue(first.clickMenuButton(f.owner, 5), "Next page rejected");
            helper.assertTrue(first.getPage() == 1 && second.getPage() == 0, "Viewer pages interfered");
            first.clickMenuButton(f.owner, 5);
            helper.assertTrue(first.getPage() == 1, "Next page exceeded queue bounds");
            helper.assertTrue(first.clickMenuButton(f.owner, 1), "Clear action rejected");
            helper.assertTrue(first.getPage() == 0 && first.getPageCount() == 1
                    && f.entity.writeQueueState(0).getList("rows", net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty(),
                    "Clearing queue did not reset page bounds");
            CompoundTag oldSave = userData(f);
            oldSave.remove("queuedTaskIds");
            oldSave.putString("queuedTaskIdsText", "7ffffffffffffffe");
            f.entity.loadUserData(oldSave);
            var invalid = f.entity.writeQueueState(0).getList("rows", net.minecraft.nbt.Tag.TAG_COMPOUND);
            helper.assertTrue(invalid.size() == 1 && !invalid.getCompound(0).getBoolean("valid"),
                    "Missing target did not retain a safe fallback row");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void queueSearchCoversAllPagesAndKeepsViewerScope(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            for (int i = 0; i < 12; i++) itemTask(f.file, f.quest, Items.GOLD_INGOT);
            f.file.refreshIDMap();
            SubmitterBlockEntity.queueTask(f.owner, UUID.fromString(f.entity.getSubmitterIdText()), f.task.id);
            SubmitterMenu first = new SubmitterMenu(72, f.owner.getInventory(), f.entity);
            SubmitterMenu second = new SubmitterMenu(73, f.owner.getInventory(), f.entity);
            first.broadcastChanges();
            second.broadcastChanges();
            CompoundTag before = userData(f);
            java.util.BitSet matches = new java.util.BitSet();
            matches.set(12);
            matches.set(200); // Untrusted positions outside the queue are ignored.
            helper.assertTrue(first.applySearch(f.owner, first.getQueueRevision(), true, matches.toLongArray()),
                    "Search request rejected");
            helper.assertTrue(first.getMatchedCount() == 1 && first.getPageCount() == 1
                    && second.getPageCount() == 3, "Search missed another page or affected another viewer");
            helper.assertTrue(first.applySearch(f.owner, first.getQueueRevision(), true, new long[0])
                    && first.getMatchedCount() == 0 && first.getPageCount() == 1,
                    "Empty search results have invalid bounds");
            helper.assertTrue(first.applySearch(f.owner, first.getQueueRevision(), false, new long[0])
                    && first.getMatchedCount() == 13 && first.getPageCount() == 3 && first.getPage() == 0,
                    "Clearing search did not restore the full queue");
            helper.assertTrue(before.equals(userData(f)), "Searching changed the saved queue");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void queueSearchRejectsStaleSnapshotsAndOtherViewers(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            SubmitterMenu menu = new SubmitterMenu(74, f.owner.getInventory(), f.entity);
            menu.broadcastChanges();
            long oldRevision = menu.getQueueRevision();
            menu.applySearch(f.owner, oldRevision, true, new long[]{1});
            itemTask(f.file, f.quest, Items.GOLD_INGOT);
            f.file.refreshIDMap();
            SubmitterBlockEntity.queueTask(f.owner, UUID.fromString(f.entity.getSubmitterIdText()), f.task.id);
            helper.assertTrue(!menu.applySearch(f.owner, oldRevision, true, new long[]{1}),
                    "Stale match positions were applied to a changed queue");
            helper.assertTrue(menu.getMatchedCount() == 1 && menu.getQueueRevision() > oldRevision,
                    "Stale request did not refresh the catalog");
            helper.assertTrue(!menu.applySearch(f.owner, oldRevision, false, new long[0])
                    && menu.isFiltering(), "Stale clear request used the wrong queue snapshot");
            helper.assertTrue(menu.applySearch(f.owner, menu.getQueueRevision(), false, new long[0])
                    && !menu.isFiltering() && menu.getMatchedCount() == 2,
                    "Retrying clear after catalog refresh did not restore all targets");
            helper.assertTrue(!menu.applySearch(f.teammate, menu.getQueueRevision(), true, new long[0]),
                    "Another viewer changed this menu's search");
            helper.assertTrue(menu.applySearch(f.owner, menu.getQueueRevision(), true, new long[]{2})
                    && menu.getMatchedCount() == 1, "Refreshed search could not select the new target");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void matchedSubmissionConsumesStockAndScopesRewards(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            helper.assertTrue(f.entity.applyAction(f.owner, 1), "Fixture did not clear the manual queue");
            // The old all-quests setting must not broaden matched rewards.
            helper.assertTrue(f.entity.applyAction(f.owner, 3), "Fixture did not set all-quests rewards");
            var other = new Quest(f.file.newID(), f.quest.getChapter());
            f.quest.getChapter().addQuest(other);
            ItemTask otherTask = itemTask(f.file, other, Items.DIAMOND);
            ItemReward otherReward = new ItemReward(f.file.newID(), other, new ItemStack(Items.EMERALD), 1);
            other.addReward(otherReward);
            f.file.refreshIDMap();
            f.data.addProgress(otherTask, 10);
            automaticReward(f, false);
            helper.assertTrue(f.entity.applyAction(f.owner, 6), "Matched submission was not enabled");
            runWork(f);
            helper.assertTrue(f.data.getProgress(f.task) == 10
                    && f.storage.items.get(AEItemKey.of(Items.IRON_INGOT)) == 54,
                    "Matched submission did not deduct exactly the accepted items");
            helper.assertTrue(f.storage.items.get(AEItemKey.of(Items.GOLD_INGOT)) == 8
                    && f.storage.items.get(AEItemKey.of(Items.EMERALD)) == 0,
                    "Matched reward routing omitted its own reward or claimed an unrelated quest");
            CompoundTag saved = userData(f);
            f.entity.loadUserData(saved);
            helper.assertTrue(f.entity.isMatching() && f.entity.getQueuedTaskIds().equals(List.of(f.task.id)),
                    "Matched provenance was not restored");
            helper.assertTrue(f.entity.applyAction(f.owner, 6) && f.entity.getQueueSize() == 0,
                    "Returning to the manual queue reused matched targets");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void manualQueueWaitsWithoutBuildingGlobalIndex(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            var reward = automaticReward(f, false);
            var iron = AEItemKey.of(Items.IRON_INGOT);
            long stock = f.storage.items.get(iron);
            f.storage.items.remove(iron, stock);
            int scans = f.file.questScans;
            runWork(f);
            helper.assertTrue(f.data.getProgress(f.task) == 0 && f.file.questScans == scans,
                    "Manual queue walked the full quest file while waiting for stock");

            f.storage.items.add(iron, stock);
            runWork(f);
            helper.assertTrue(f.data.isCompleted(f.task), "Manual queue did not submit its target");
            helper.assertTrue(f.data.isRewardClaimed(f.owner.getUUID(), reward)
                            && f.storage.items.get(AEItemKey.of(Items.GOLD_INGOT)) == 8,
                    "Manual queue did not claim its quest reward into AE");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void matchingReusesQuestScanUntilTaskEdit(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            f.task.setConsumeItems(Tristate.FALSE);
            f.file.clearCachedData();
            helper.assertTrue(f.entity.applyAction(f.owner, 6), "Fixture did not enable matching");
            runWork(f);
            int scans = f.file.questScans;
            runWork(f);
            helper.assertTrue(f.file.questScans == scans, "Matching walked the full quest file again");
            helper.assertTrue(f.data.getProgress(f.task) == 0, "Matching consumed a read-only task");

            f.task.setConsumeItems(Tristate.TRUE);
            f.file.clearCachedData();
            runWork(f);
            helper.assertTrue(f.data.isCompleted(f.task), "In-place task edit did not rebuild the matching index");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void matchingSeesAddedQuestAfterFileDirty(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            f.task.setConsumeItems(Tristate.FALSE);
            var chapter = f.quest.getChapter();
            var addedQuest = new Quest(f.file.newID(), chapter);
            var addedTask = itemTask(f.file, addedQuest, Items.IRON_INGOT);
            // Prime FTBQ's flattened quest cache before changing the chapter.
            f.file.forAllQuests(ignored -> {});
            helper.assertTrue(f.entity.applyAction(f.owner, 6), "Fixture did not enable matching");
            runWork(f);
            helper.assertTrue(f.data.getProgress(addedTask) == 0, "A quest outside the chapter was matched");

            chapter.addQuest(addedQuest);
            f.file.markDirty();
            runWork(f);
            helper.assertTrue(f.data.getProgress(addedTask) == 10, "Matching ignored the new quest in the live chapter");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void allRewardsReuseQuestScanAndPickUpNewRewards(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            helper.assertTrue(f.entity.applyAction(f.owner, 1), "Fixture did not clear the queue");
            helper.assertTrue(f.entity.applyAction(f.owner, 3), "Fixture did not enable all-quest rewards");
            runWork(f);
            int scans = f.file.questScans;
            runWork(f);
            helper.assertTrue(f.file.questScans == scans, "Reward polling walked the full quest file again");

            var reward = new ItemReward(f.file.newID(), f.quest, new ItemStack(Items.GOLD_INGOT), 8);
            f.quest.addReward(reward);
            f.file.markDirty();
            f.data.setProgress(f.task, 10);
            runWork(f);
            helper.assertTrue(f.storage.items.get(AEItemKey.of(Items.GOLD_INGOT)) == 8,
                    "A newly added reward was missed after marking the quest file dirty");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void registeredFluidConsumesOnlyNativeAeFluidAndCreditsActualMillibuckets(GameTestHelper helper) {
        withSubmitter(helper, f -> {
            var fluid = new FluidTask(f.file.newID(), f.quest).setFluid(Fluids.WATER);
            CompoundTag config = new CompoundTag();
            fluid.writeData(config);
            config.putLong("amount", 2500);
            fluid.readData(config);
            f.quest.addTask(fluid);
            f.file.refreshIDMap();
            helper.assertTrue(SubmitterBlockEntity.queueTask(f.owner, UUID.fromString(f.entity.getSubmitterIdText()), fluid.id),
                    "Fluid registration rejected");
            f.data.addProgress(f.task, 10);
            AEFluidKey water = AEFluidKey.of(Fluids.WATER);
            f.storage.items.add(water, 1200);
            f.storage.items.add(AEItemKey.of(Items.WATER_BUCKET), 3);
            runWork(f);
            helper.assertTrue(f.data.getProgress(fluid) == 1200 && f.entity.getLastSubmittedFluid() == 1200
                    && f.storage.items.get(water) == 0, "Fluid progress differed from extracted millibuckets");
            helper.assertTrue(f.storage.items.get(AEItemKey.of(Items.WATER_BUCKET)) == 3,
                    "Fluid goal extracted stored item containers");
            f.storage.items.add(water, 2000);
            helper.assertTrue(f.entity.applyAction(f.owner, 6), "Could not enable matched mode for a fluid goal");
            runWork(f);
            helper.assertTrue(f.data.isCompleted(fluid) && f.storage.items.get(water) == 700,
                    "Fluid extraction exceeded the remaining requirement");
            var rows = f.entity.writeQueueState(0).getList("rows", net.minecraft.nbt.Tag.TAG_COMPOUND);
            helper.assertTrue(rows.getCompound(0).getBoolean("fluid")
                    && rows.getCompound(0).getLong("required") == 2500,
                    "Queue row lost its fluid type or amount");
        });
    }

    private static ItemReward automaticReward(Fixture f, boolean team) {
        ItemReward reward = new ItemReward(f.file.newID(), f.quest, new ItemStack(Items.GOLD_INGOT), 8);
        configureAutomaticReward(f, reward, team);
        return reward;
    }

    private static void configureAutomaticReward(Fixture f, Reward reward, boolean team) {
        CompoundTag config = new CompoundTag();
        reward.writeData(config);
        config.putString("auto", "enabled");
        config.putBoolean("team_reward", team);
        reward.readData(config);
        f.quest.addReward(reward);
        f.file.refreshIDMap();
    }

    @GameTest(template = "empty")
    public static void colocatedSubmittersDoNotAllWorkOnTheSameTick(GameTestHelper helper) {
        var state = NexusContent.SUBMITTER.get().defaultBlockState();
        int active = 0;
        int total = 400;
        for (int i = 0; i < total; i++) {
            BlockPos pos = helper.absolutePos(new BlockPos(i % 20, 1, i / 20));
            var submitter = new SubmitterBlockEntity(pos, state);
            SubmitterBlockEntity.serverTick(helper.getLevel(), pos, state, submitter);
            if (submitter.writeClientState().getString("status").equals("offline")) active++;
        }
        helper.assertTrue(active > 0 && active < total, "Submitters did not spread work across ticks");
        helper.succeed();
    }

    private static void runWork(Fixture f) {
        helperReflect(() -> {
            // Exercise runQueuedWork directly: channel/power tests belong to AE, not this regression.
            var online = SubmitterBlockEntity.class.getDeclaredField("online");
            online.setAccessible(true);
            online.setBoolean(f.entity, true);
            var work = SubmitterBlockEntity.class.getDeclaredMethod("runQueuedWork");
            work.setAccessible(true);
            work.invoke(f.entity);
        });
    }

    private static CompoundTag userData(Fixture f) {
        CompoundTag tag = new CompoundTag();
        f.entity.saveUserData(tag);
        return tag;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "NexusRegression"));
        TeamManagerImpl.INSTANCE.playerLoggedIn(null, id, "NexusRegression");
        return player;
    }

    private static ItemTask itemTask(ServerQuestFile file, Quest quest, net.minecraft.world.item.Item item) {
        ItemTask task = new ItemTask(file.newID(), quest).setStackAndCount(new ItemStack(item), 10);
        task.setConsumeItems(Tristate.TRUE);
        quest.addTask(task);
        return task;
    }

    private static void withSubmitter(GameTestHelper helper, Consumer<Fixture> test) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, NexusContent.SUBMITTER.get());
        helper.runAfterDelay(20, () -> {
            ServerQuestFile previous = ServerQuestFile.INSTANCE;
            SubmitterBlockEntity entity = (SubmitterBlockEntity) helper.getBlockEntity(pos);
            ServerPlayer owner = player(helper);
            ServerPlayer teammate = player(helper);
            owner.setPos(helper.absolutePos(pos).getX() + .5, helper.absolutePos(pos).getY() + .5,
                    helper.absolutePos(pos).getZ() + .5);
            TestQuestFile file = new TestQuestFile(helper.getLevel().getServer());
            Chapter chapter = new Chapter(file.newID(), file, file.getDefaultChapterGroup());
            file.getDefaultChapterGroup().addChapter(chapter);
            Quest quest = new Quest(file.newID(), chapter);
            chapter.addQuest(quest);
            ItemTask task = itemTask(file, quest, Items.IRON_INGOT);
            UUID team = TeamManagerImpl.INSTANCE.getTeamForPlayerID(owner.getUUID()).orElseThrow().getTeamId();
            List<ServerPlayer> onlineMembers = new ArrayList<>(List.of(owner, teammate));
            TeamData data = new TeamData(team, file) {
                @Override public Collection<ServerPlayer> getOnlineMembers() { return onlineMembers; }
            };
            file.addData(data, false);
            file.refreshIDMap();
            MemoryStorage storage = new MemoryStorage();
            storage.items.add(AEItemKey.of(Items.IRON_INGOT), 64);
            IStorageProvider provider = mounts -> mounts.mount(storage);
            var storageService = entity.getMainNode().getGrid().getStorageService();
            storageService.addGlobalStorageProvider(provider);
            Fixture f = new Fixture(file, quest, task, data, entity, owner, teammate, storage, onlineMembers);
            try {
                ServerQuestFile.INSTANCE = file;
                helper.assertTrue(entity.applyAction(owner, 0), "Fixture binding failed");
                helper.assertTrue(SubmitterBlockEntity.queueTask(owner,
                        UUID.fromString(entity.getSubmitterIdText()), task.id), "Fixture registration failed");
                test.accept(f);
                helper.succeed();
            } finally {
                ServerQuestFile.INSTANCE = previous;
                storageService.removeGlobalStorageProvider(provider);
            }
        });
    }

    private static final class TestQuestFile extends ServerQuestFile {
        private int questScans;

        TestQuestFile(net.minecraft.server.MinecraftServer server) { super(server); }

        @Override public void forAllQuests(Consumer<Quest> consumer) {
            questScans++;
            super.forAllQuests(consumer);
        }

        @Override public void forAllChapters(Consumer<Chapter> consumer) {
            questScans++;
            super.forAllChapters(consumer);
        }
    }

    private record Fixture(TestQuestFile file, Quest quest, ItemTask task, TeamData data,
                           SubmitterBlockEntity entity, ServerPlayer owner, ServerPlayer teammate,
                           MemoryStorage storage, List<ServerPlayer> onlineMembers) {}

    @FunctionalInterface private interface ReflectAction { void run() throws ReflectiveOperationException; }
    private static void helperReflect(ReflectAction action) {
        try { action.run(); }
        catch (InvocationTargetException e) { throw new AssertionError(e.getCause()); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static final class MemoryStorage implements MEStorage {
        private final KeyCounter items = new KeyCounter();
        private long capacity = Long.MAX_VALUE;
        @Override public Component getDescription() { return Component.empty(); }
        @Override public void getAvailableStacks(KeyCounter target) { target.addAll(items); }
        @Override public long extract(AEKey key, long amount, Actionable action, IActionSource source) {
            long extracted = Math.min(amount, items.get(key));
            if (action == Actionable.MODULATE) items.remove(key, extracted);
            return extracted;
        }
        @Override public long insert(AEKey key, long amount, Actionable action, IActionSource source) {
            long stored = 0;
            for (var entry : items) stored += entry.getLongValue();
            long inserted = Math.min(amount, Math.max(0, capacity - stored));
            if (action == Actionable.MODULATE) items.add(key, inserted);
            return inserted;
        }
    }
}
