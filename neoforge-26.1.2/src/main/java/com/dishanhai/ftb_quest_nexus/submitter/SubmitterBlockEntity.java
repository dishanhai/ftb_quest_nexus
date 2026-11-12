package com.dishanhai.ftb_quest_nexus.submitter;

import net.minecraft.nbt.NbtOps;

import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.network.chat.ComponentSerialization;

// Author: dishanhai. Forked from gt_shanhai's FTBQ AE submitter; see SOURCE.md.
import appeng.api.config.Actionable;
import appeng.api.features.IPlayerRegistry;
import appeng.api.networking.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.api.util.AECableType;
import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqRewardBuffer;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ChoiceRewardNotices;
import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqAutoClaimDeferral;
import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqSubmitterToastSuppressor;
import com.dishanhai.ftb_quest_nexus.api.ftbq.TaskQueue;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ConsumableTask;
import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalTask;
import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalReward;
import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalRewards;
import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterQuestIndex;
import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.config.ServerConfig;
import com.dishanhai.ftb_quest_nexus.config.ServerOption;
import com.dishanhai.ftb_quest_nexus.api.BindingTransfer;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.reward.ChoiceReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardClaimType;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public final class SubmitterBlockEntity extends BlockEntity implements MenuProvider, IInWorldGridNodeHost, IActionHost {
    private static final Map<UUID, SubmitterBlockEntity> LOADED = new HashMap<>();
    private final TaskQueue queue = new TaskQueue();
    private final TaskQueue matchedTasks = new TaskQueue();
    private final Map<Long, DeferredAutoClaim> deferredAutoClaims = new LinkedHashMap<>();
    private boolean matching;
    private boolean restoredUserData;
    private final FtbqRewardBuffer<AEItemKey> pendingRewards = new FtbqRewardBuffer<>();
    private final IManagedGridNode mainNode;
    private String teamIdText = "";
    private String rewardPlayerIdText = "";
    private String submitterIdText = UUID.randomUUID().toString();
    private boolean suppressCompletionToast;
    private boolean claimOnlyQueuedRewards = true;
    private boolean online;
    private long lastSubmitted;
    private long lastSubmittedFluid;
    private long lastSubmittedChemical;
    private long lastClaimedRewards;
    private int queueSize;
    private String statusKey = "unbound";
    private long[] statusArgs = new long[0];
    private CompoundTag lastClientState;
    private List<Long> scopedTaskIds = List.of();
    private Set<Long> scopedRewardQuests = Set.of();
    private long scopeRevision;
    private int nextQueuedIndex;
    private int nextMatchingIndex;

    public SubmitterBlockEntity(BlockPos pos, BlockState state) {
        super(NexusContent.BLOCK_ENTITY.get(), pos, state);
        mainNode = GridHelper.createManagedNode(this, new IGridNodeListener<SubmitterBlockEntity>() {
            @Override public void onSaveChanges(SubmitterBlockEntity owner, IGridNode node) { owner.setChanged(); }
        }).setTagName("aeNode").setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL)
                .setExposedOnSides(EnumSet.allOf(Direction.class)).setIdlePowerUsage(1.0);
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide()) return;
        GridHelper.onFirstTick(this, owner -> {
            if (owner.isRemoved() || !(owner.level instanceof ServerLevel)) return;
            // BlockItem restores its data after onLoad; index the restored identity.
            UUID id = uuid(owner.submitterIdText);
            if (id == null || (LOADED.containsKey(id) && LOADED.get(id) != owner)) {
                owner.submitterIdText = UUID.randomUUID().toString();
                owner.setChanged();
            }
            LOADED.put(UUID.fromString(owner.submitterIdText), owner);
            owner.mainNode.setVisualRepresentation(NexusContent.ITEM.get());
            UUID playerId = uuid(owner.rewardPlayerIdText);
            owner.mainNode.create(owner.level, owner.worldPosition);
            // create() restores node NBT, which may reset owner for a freshly placed item.
            if (playerId != null) owner.mainNode.setOwningPlayerId(IPlayerRegistry.getMapping(owner.level).getPlayerId(playerId));
        });
    }

    private void unloadNode() {
        if (level == null || level.isClientSide()) return;
        UUID id = uuid(submitterIdText);
        if (id != null) LOADED.remove(id, this);
        mainNode.destroy();
    }

    @Override public void onChunkUnloaded() { unloadNode(); super.onChunkUnloaded(); }
    @Override public void setRemoved() { unloadNode(); super.setRemoved(); }
    public static void clearLoadedSubmitters() {
        LOADED.clear();
        SubmitterQuestIndex.clear();
        ChoiceRewardNotices.clear();
    }
    public IManagedGridNode getMainNode() { return mainNode; }
    @Override public IGridNode getGridNode(Direction side) { return mainNode.getNode(); }
    @Override public IGridNode getActionableNode() { return mainNode.getNode(); }
    @Override public AECableType getCableConnectionType(Direction side) { return AECableType.SMART; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SubmitterBlockEntity entity) {
        if (level.isClientSide() || Math.floorMod(level.getGameTime(), 20) != Math.floorMod(pos.hashCode(), 20)) return;
        entity.online = entity.mainNode.isOnline();
        if (state.hasProperty(SubmitterBlock.ONLINE) && state.getValue(SubmitterBlock.ONLINE) != entity.online) {
            level.setBlock(pos, state.setValue(SubmitterBlock.ONLINE, entity.online), 2);
        }
        entity.runQueuedWork();
        entity.syncState();
    }

    private void status(String key, long... args) { statusKey = key; statusArgs = args; }

    void applyPlacementDefaults() {
        if (restoredUserData) return;
        matching = ServerConfig.get(ServerOption.DEFAULT_MATCHING);
        claimOnlyQueuedRewards = ServerConfig.get(ServerOption.DEFAULT_QUEUED_REWARDS);
        suppressCompletionToast = !ServerConfig.get(ServerOption.DEFAULT_COMPLETION_TOAST);
        setChanged();
    }

    public Component getStatusComponent() {
        Object[] args = new Object[statusArgs.length];
        for (int i = 0; i < args.length; i++) args[i] = statusArgs[i];
        return Component.translatable("status.ftb_quest_nexus." + statusKey, args);
    }

    public boolean applyAction(ServerPlayer player, int action) {
        if (!canReach(player) || !canManage(player)) return false;
        switch (action) {
            case 0 -> {
                if (!pendingRewards.isEmpty()) { status("pending_rebind"); break; }
                UUID owner = uuid(rewardPlayerIdText);
                if (owner != null && !owner.equals(player.getUUID())) {
                    return BindingTransfer.request(this, player, () -> uuid(rewardPlayerIdText), this::bindPlayer);
                }
                return bindPlayer(player);
            }
            case 1 -> {
                if (matching) return false;
                queue.clear();
                nextQueuedIndex = 0;
                status("cleared");
            }
            case 2 -> { suppressCompletionToast = !suppressCompletionToast; status(suppressCompletionToast ? "toast_hidden" : "toast_shown"); }
            case 3 -> {
                if (matching) return false;
                claimOnlyQueuedRewards = !claimOnlyQueuedRewards;
                status(claimOnlyQueuedRewards ? "rewards_queued" : "rewards_all");
            }
            case 6 -> { matching = !matching; status(matching ? "match_enabled" : "match_disabled"); }
            default -> { return false; }
        }
        queueSize = displayedQueue().size();
        setChanged();
        syncState();
        return true;
    }

    public boolean canManage(ServerPlayer player) {
        UUID bound = uuid(teamIdText);
        TeamData data = playerData(player);
        return data != null && (bound == null || bound.equals(data.getTeamId()));
    }

    private boolean canReach(ServerPlayer player) {
        return !isRemoved() && level != null && !level.isClientSide() && player.level() == level
                && worldPosition.closerToCenterThan(player.position(), 8.0);
    }

    private boolean bindPlayer(ServerPlayer player) {
        if (!pendingRewards.isEmpty()) return false;
        TeamData data = playerData(player);
        UUID teamId = uuid(teamIdText);
        if (data == null || teamId != null && !teamId.equals(data.getTeamId())) return false;
        if (teamId == null) {
            queue.clear();
            matchedTasks.clear();
            deferredAutoClaims.clear();
            nextQueuedIndex = 0;
            nextMatchingIndex = 0;
            matching = false;
        }
        teamIdText = data.getTeamId().toString();
        rewardPlayerIdText = player.getUUID().toString();
        mainNode.setOwningPlayer(player);
        status("bound");
        queueSize = displayedQueue().size();
        setChanged();
        syncState();
        return true;
    }

    private static TeamData playerData(ServerPlayer player) {
        return player != null && ServerQuestFile.exists() ? ServerQuestFile.getInstance().getTeamData(player).orElse(null) : null;
    }

    private void runQueuedWork() {
        lastSubmitted = 0;
        lastSubmittedFluid = 0;
        lastSubmittedChemical = 0;
        lastClaimedRewards = 0;
        if (!online) { status("offline"); return; }
        IGrid grid = mainNode.getGrid();
        if (grid == null) { status("no_network"); return; }
        ServerQuestFile file = (ServerQuestFile.exists() ? ServerQuestFile.getInstance() : null);
        if (file == null) { status("quests_unavailable"); return; }
        UUID teamId = uuid(teamIdText);
        UUID playerId = uuid(rewardPlayerIdText);
        if (teamId == null || playerId == null) { status("unbound"); return; }
        MEStorage storage = grid.getStorageService().getInventory();
        IActionSource source = IActionSource.ofMachine(this);
        lastClaimedRewards = pendingRewards.flush(
                (key, amount) -> storage.insert(key, amount, Actionable.MODULATE, source), this::setChanged);
        if (!pendingRewards.isEmpty()) { status("pending_rewards"); return; }
        TeamData data = file.getOrCreateTeamData(teamId);
        if (data == null) { status("quests_unavailable"); return; }
        if (data.isLocked()) { status("locked"); return; }
        UUID currentTeam = FTBTeamsAPI.api().getManager().getTeamForPlayerID(playerId)
                .map(team -> team.getTeamId()).orElse(null);
        if (!teamId.equals(currentTeam)) { status("player_left"); return; }
        boolean allRewards = !matching && !claimOnlyQueuedRewards;
        List<Long> taskIds = displayedQueue().list();
        if (!matching && !allRewards && taskIds.isEmpty() && deferredAutoClaims.isEmpty()) {
            scopedTaskIds = List.of();
            scopedRewardQuests = Set.of();
            queueSize = 0;
            if (lastClaimedRewards > 0) status("worked", 0, lastClaimedRewards);
            else status("waiting");
            return;
        }
        SubmitterQuestIndex index = SubmitterQuestIndex.forFile(file);
        if (!deferredAutoClaims.isEmpty()) {
            resumeDeferredAutoClaims(index, storage, data, playerId, source);
            if (!pendingRewards.isEmpty()) { status("pending_rewards"); return; }
            index = SubmitterQuestIndex.forFile(file);
        }
        Set<Long> rewardQuests = allRewards ? Set.of() : queuedRewardQuests(file, taskIds, index.revision());
        boolean autoSubmit = ServerConfig.get(ServerOption.AUTO_SUBMIT);
        List<Task> candidates = matching || !autoSubmit ? List.of() : queuedCandidates(file, taskIds);
        QuestWork.Submission submission = null;
        if (autoSubmit) {
            QuestWork.ItemKeys itemKeys = new QuestWork.ItemKeys();
            submission = matching ? QuestWork.match(storage, data, source, itemKeys, index, nextMatchingIndex)
                    : QuestWork.firstSubmission(storage, data, source, itemKeys, candidates, nextQueuedIndex);
        }
        long newlyMatchedQuestId = 0;
        if (submission != null) {
            Task task = submission.task();
            long submitted = submission.amount();
            long matchedQuestId = matching ? task.getQuest().id : 0;
            if (matching) {
                matchedTasks.add(task.id);
                nextMatchingIndex = submission.nextIndex();
            } else {
                nextQueuedIndex = submission.nextIndex();
            }
            newlyMatchedQuestId = matchedQuestId;
            List<Quest> deferred = FtbqAutoClaimDeferral.run(data, reward -> {
                Quest quest = reward.getQuest();
                return quest != null && (allRewards || quest.id == matchedQuestId || rewardQuests.contains(quest.id))
                        && (reward instanceof ChoiceReward || SubmitterQuestIndex.isExperienceReward(reward)
                            || canClaimNetworkReward(reward) && scopeIncludesItems(quest));
            }, () -> {
                if (suppressCompletionToast) FtbqSubmitterToastSuppressor.runSuppressed(() -> data.addProgress(task, submitted));
                else data.addProgress(task, submitted);
            });
            for (Quest quest : deferred) {
                boolean claimItems = scopeIncludesItems(quest);
                deferredAutoClaims.compute(quest.id, (ignored, prior) ->
                        new DeferredAutoClaim(claimItems || prior != null && prior.claimItems(), true));
            }
            if (task instanceof FluidTask) lastSubmittedFluid = submitted;
            else if (task instanceof ChemicalTask) lastSubmittedChemical = submitted;
            else lastSubmitted = submitted;
            setChanged();
            // Progress callbacks can edit the quest definitions during this work pass.
            SubmitterQuestIndex updated = SubmitterQuestIndex.forFile(file);
            if (matching && index != updated) updated.matchQuests();
            index = updated;
        }
        Set<Long> questsToClaim = allRewards ? index.rewardQuestIds() : rewardQuests;
        claimRewards(index, questsToClaim, newlyMatchedQuestId, storage, data, playerId, source);
        if (submission != null && pendingRewards.isEmpty()) {
            resumeDeferredAutoClaims(index, storage, data, playerId, source);
        }
        ServerPlayer boundPlayer = level.getServer().getPlayerList().getPlayer(playerId);
        if (boundPlayer != null) {
            Map<Long, ChoiceReward> choices = new LinkedHashMap<>();
            collectChoices(index, questsToClaim, data, playerId, choices);
            if (newlyMatchedQuestId != 0 && !questsToClaim.contains(newlyMatchedQuestId)) {
                collectChoices(index, Set.of(newlyMatchedQuestId), data, playerId, choices);
            }
            ChoiceRewardNotices.notifyReady(boundPlayer, data, choices, SubmitterBlockEntity::availableChoiceRewards);
        }
        queueSize = displayedQueue().size();
        if (!pendingRewards.isEmpty()) status("pending_rewards");
        else if (lastSubmittedChemical > 0) status("worked_chemical", lastSubmittedChemical, lastClaimedRewards);
        else if (lastSubmittedFluid > 0) status("worked_fluid", lastSubmittedFluid, lastClaimedRewards);
        else if (lastSubmitted > 0 || lastClaimedRewards > 0) status("worked", lastSubmitted, lastClaimedRewards);
        else if (!autoSubmit && (matching || !queue.isEmpty())) status("server_submission_disabled");
        else if (matching) status("match_waiting");
        else if (queue.isEmpty()) status("waiting");
        else if (candidates.stream().allMatch(data::isCompleted)) status("queue_done");
        else if (candidates.stream().anyMatch(task -> ConsumableTask.supported(task) && !data.isCompleted(task))) {
            status("missing_items");
        } else status("manual_targets");
    }

    private List<Task> queuedCandidates(ServerQuestFile file, List<Long> taskIds) {
        if (taskIds.isEmpty()) return List.of();
        List<Task> candidates = new ArrayList<>();
        for (long taskId : taskIds) {
            Task task = file.getTask(taskId);
            if (SubmitterQuestIndex.supportsQueueTask(task)) candidates.add(task);
            else {
                queue.remove(taskId);
                setChanged();
            }
        }
        return candidates;
    }

    private Set<Long> queuedRewardQuests(ServerQuestFile file, List<Long> taskIds, long revision) {
        if (scopeRevision != revision || scopedTaskIds != taskIds) {
            Set<Long> quests = new LinkedHashSet<>();
            for (long taskId : taskIds) {
                Task task = file.getTask(taskId);
                if (SubmitterQuestIndex.supportsQueueTask(task)) quests.add(task.getQuest().id);
            }
            scopedRewardQuests = quests;
            scopedTaskIds = taskIds;
            scopeRevision = revision;
        }
        return scopedRewardQuests;
    }

    public static Map<Long, ChoiceReward> availableChoiceRewards(ServerPlayer player) {
        ServerQuestFile file = (ServerQuestFile.exists() ? ServerQuestFile.getInstance() : null);
        TeamData data = playerData(player);
        if (file == null || data == null || data.isLocked()) return Map.of();
        UUID playerId = player.getUUID();
        SubmitterQuestIndex index = SubmitterQuestIndex.forFile(file);
        Map<Long, ChoiceReward> choices = new LinkedHashMap<>();
        for (SubmitterBlockEntity entity : LOADED.values()) {
            if (entity.isRemoved() || entity.level == null || entity.level.getServer() != player.level().getServer()
                    || !playerId.equals(uuid(entity.rewardPlayerIdText))
                    || !data.getTeamId().equals(uuid(entity.teamIdText))) continue;
            Set<Long> quests = !entity.matching && !entity.claimOnlyQueuedRewards ? index.rewardQuestIds()
                    : entity.queuedRewardQuests(file, entity.displayedQueue().list(), index.revision());
            collectChoices(index, quests, data, playerId, choices);
        }
        return choices;
    }

    public static long insertChemicalReward(ServerPlayer player, AEKey key, long amount) {
        TeamData data = playerData(player);
        if (data == null || data.isLocked()) return 0;
        MEStorage selected = null;
        IActionSource selectedSource = null;
        UUID selectedId = null;
        long best = 0;
        for (var entry : LOADED.entrySet()) {
            var entity = entry.getValue();
            if (entity.isRemoved() || entity.level == null || entity.level.getServer() != player.level().getServer()
                    || !player.getUUID().equals(uuid(entity.rewardPlayerIdText))
                    || !data.getTeamId().equals(uuid(entity.teamIdText)) || !entity.mainNode.isActive()) continue;
            IGrid grid = entity.mainNode.getGrid();
            if (grid == null) continue;
            MEStorage storage = grid.getStorageService().getInventory();
            IActionSource source = IActionSource.ofMachine(entity);
            long possible = storage.insert(key, amount, Actionable.SIMULATE, source);
            if (possible > best || possible > 0 && possible == best && entry.getKey().compareTo(selectedId) < 0) {
                best = possible;
                selectedId = entry.getKey();
                selected = storage;
                selectedSource = source;
            }
        }
        return selected == null ? 0 : selected.insert(key, amount, Actionable.MODULATE, selectedSource);
    }

    private static void collectChoices(SubmitterQuestIndex index, Set<Long> quests, TeamData data, UUID playerId,
                                       Map<Long, ChoiceReward> choices) {
        for (long questId : quests) {
            SubmitterQuestIndex.RewardQuest quest = index.rewardQuest(questId);
            if (quest == null || quest.choices().isEmpty() || !data.isCompleted(quest.quest())) continue;
            for (ChoiceReward reward : quest.choices()) {
                if (ChoiceRewardNotices.canChoose(data, playerId, reward)) choices.putIfAbsent(reward.id, reward);
            }
        }
    }

    private void claimRewards(SubmitterQuestIndex index, Set<Long> quests, long newlyMatchedQuestId,
                              MEStorage storage, TeamData data, UUID playerId, IActionSource source) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        for (long questId : quests) {
            var quest = index.rewardQuest(questId);
            if (quest != null) claimQuestRewards(quest, storage, data, playerId, source, serverLevel,
                    scopeIncludesItems(quest.quest()));
            if (!pendingRewards.isEmpty()) return;
        }
        if (newlyMatchedQuestId != 0 && !quests.contains(newlyMatchedQuestId)) {
            var quest = index.rewardQuest(newlyMatchedQuestId);
            if (quest != null) claimQuestRewards(quest, storage, data, playerId, source, serverLevel,
                    scopeIncludesItems(quest.quest()));
        }
    }

    private void claimQuestRewards(SubmitterQuestIndex.RewardQuest quest, MEStorage storage, TeamData data,
                                   UUID playerId, IActionSource source, ServerLevel serverLevel, boolean claimItems) {
        if (quest.rewards().isEmpty() || !data.isCompleted(quest.quest())) return;
        for (Reward reward : quest.rewards()) {
            if (SubmitterQuestIndex.isExperienceReward(reward)) {
                QuestWork.claimExperience(data, playerId, reward);
                continue;
            }
            if (!claimItems || data.isRewardBlocked(reward)
                    || data.getClaimType(playerId, reward) != RewardClaimType.CAN_CLAIM) continue;
            if (reward instanceof ChemicalReward chemical) {
                ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(playerId);
                if (ChemicalRewards.claimFromSubmitter(player, data, chemical, storage, source)) lastClaimedRewards++;
                continue;
            }
            lastClaimedRewards += QuestWork.claim(storage, data, playerId, reward, serverLevel,
                    this, pendingRewards, source, this::setChanged);
            if (!pendingRewards.isEmpty()) return;
        }
    }

    private void resumeDeferredAutoClaims(SubmitterQuestIndex index, MEStorage storage, TeamData data,
                                          UUID playerId, IActionSource source) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        for (var entries = deferredAutoClaims.entrySet().iterator(); entries.hasNext();) {
            var entry = entries.next();
            var quest = index.rewardQuest(entry.getKey());
            if (quest == null || !data.isCompleted(quest.quest())) {
                entries.remove();
                setChanged();
                continue;
            }
            DeferredAutoClaim deferred = entry.getValue();
            boolean claimItems = deferred.claimItems() && ServerConfig.get(ServerOption.NETWORK_REWARDS);
            claimQuestRewards(quest, storage, data, playerId, source, serverLevel, claimItems);
            if (!pendingRewards.isEmpty()) return;
            boolean pendingItems = claimItems && quest.rewards().stream().anyMatch(reward ->
                    canClaimNetworkReward(reward) && data.getClaimType(playerId, reward) == RewardClaimType.CAN_CLAIM);
            boolean pendingExperience = quest.rewards().stream().anyMatch(reward -> isExperiencePending(data, playerId, reward));
            if (pendingItems || quest.quest().canBeRepeated() && pendingExperience) continue;

            if (!pendingExperience) {
                entries.remove();
                setChanged();
            } else if (deferred.replayNative()) {
                // A non-repeatable quest can resume other rewards while its owner's XP waits.
                entry.setValue(new DeferredAutoClaim(false, false));
                setChanged();
            }
            if (deferred.replayNative()) {
                FtbqAutoClaimDeferral.run(data, reward -> reward.getQuest() == quest.quest()
                        && (reward instanceof ChoiceReward || isExperiencePending(data, playerId, reward)),
                        () -> data.checkAutoCompletion(quest.quest()));
            }
        }
    }

    public static boolean shouldDeferPendingAutoClaim(TeamData data, Reward reward) {
        if (data.getFile() != (ServerQuestFile.exists() ? ServerQuestFile.getInstance() : null) || reward.getQuest() == null) return false;
        for (SubmitterBlockEntity entity : LOADED.values()) {
            DeferredAutoClaim deferred = entity.deferredAutoClaims.get(reward.getQuest().id);
            if (deferred == null || !data.getTeamId().equals(uuid(entity.teamIdText))) continue;
            if (deferred.replayNative() && (reward instanceof ChoiceReward
                    || deferred.claimItems() && canClaimNetworkReward(reward))) return true;
            UUID playerId = uuid(entity.rewardPlayerIdText);
            if (playerId != null && isExperiencePending(data, playerId, reward)) return true;
        }
        return false;
    }

    private record DeferredAutoClaim(boolean claimItems, boolean replayNative) {}

    private static boolean canClaimNetworkReward(Reward reward) {
        return reward instanceof ChemicalReward && ChemicalTask.isMekanismAvailable() || QuestWork.canClaimItem(reward);
    }

    private boolean scopeIncludesItems(Quest quest) {
        return ServerConfig.get(ServerOption.NETWORK_REWARDS)
                && (matching || !claimOnlyQueuedRewards || quest.getTasks().stream().anyMatch(ConsumableTask::supported));
    }

    private static boolean isExperiencePending(TeamData data, UUID playerId, Reward reward) {
        return SubmitterQuestIndex.isExperienceReward(reward)
                && data.getClaimType(playerId, reward) == RewardClaimType.CAN_CLAIM;
    }

    public static boolean queueTask(ServerPlayer player, UUID submitterId, long taskId) {
        if (taskId == 0 || submitterId == null || player == null) return false;
        SubmitterBlockEntity entity = LOADED.get(submitterId);
        if (entity == null || entity.isRemoved() || entity.level == null
                || entity.level.getServer() != player.level().getServer() || !entity.canManage(player)
                || uuid(entity.teamIdText) == null) return false;
        ServerQuestFile file = (ServerQuestFile.exists() ? ServerQuestFile.getInstance() : null);
        Task task = file == null ? null : file.getTask(taskId);
        if (!SubmitterQuestIndex.supportsQueueTask(task)) return false;
        Collection<Task> tasks = task.getQuest().getTasks();
        boolean added = false;
        boolean hasConsumable = false;
        for (Task sibling : tasks) {
            if (ConsumableTask.supported(sibling)) {
                hasConsumable = true;
                added |= entity.queue.add(sibling.id);
            }
        }
        if (!hasConsumable) {
            List<Long> queued = entity.queue.list();
            if (tasks.stream().noneMatch(sibling -> queued.contains(sibling.id))) {
                added = entity.queue.add(task.id);
            }
        }
        entity.queueSize = entity.displayedQueue().size();
        entity.status(added ? "queued" : "already_queued");
        entity.setChanged();
        entity.syncState();
        return true;
    }

    public static List<SubmitterEntry> listSubmitters(ServerPlayer player) {
        TeamData data = playerData(player);
        if (data == null) return List.of();
        List<SubmitterEntry> entries = new ArrayList<>();
        for (SubmitterBlockEntity entity : LOADED.values()) {
            UUID id = uuid(entity.submitterIdText);
            if (id == null || entity.isRemoved() || entity.level == null || entity.level.getServer() != player.level().getServer()
                    || !data.getTeamId().equals(uuid(entity.teamIdText))) continue;
            entries.add(new SubmitterEntry(id, entity.level.dimension().identifier() + " " + entity.worldPosition.toShortString(),
                    entity.queue.size(), entity.online));
        }
        entries.sort(Comparator.comparing(entry -> entry.id().toString()));
        return entries;
    }

    public record SubmitterEntry(UUID id, String pos, int queueSize, boolean online) {}

    public void saveUserData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("teamIdText", teamIdText);
        tag.putString("rewardPlayerIdText", rewardPlayerIdText);
        tag.putString("submitterIdText", submitterIdText);
        queue.save(tag, "queuedTaskIds");
        matchedTasks.save(tag, "matchedTaskIds");
        ListTag deferred = new ListTag();
        deferredAutoClaims.forEach((id, claim) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("questId", id);
            entry.putBoolean("claimItems", claim.claimItems());
            entry.putBoolean("replayNative", claim.replayNative());
            deferred.add(entry);
        });
        tag.put("deferredAutoClaims", deferred);
        tag.putBoolean("matching", matching);
        tag.putBoolean("suppressCompletionToast", suppressCompletionToast);
        tag.putBoolean("claimOnlyQueuedRewards", claimOnlyQueuedRewards);
        tag.putBoolean("rewardClaimModeInitialized", true);
        ListTag pending = new ListTag();
        pendingRewards.snapshot().forEach((key, amount) -> {
            CompoundTag entry = new CompoundTag();
            TagValueOutput keyOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
            key.toTag(keyOutput);
            entry.put("key", keyOutput.buildResult());
            entry.putLong("amount", amount);
            pending.add(entry);
        });
        tag.put("FtbqPendingRewards", pending);
    }

    public void loadUserData(CompoundTag tag, HolderLookup.Provider registries) {
        restoredUserData = !tag.isEmpty();
        lastClientState = null;
        teamIdText = tag.getStringOr("teamIdText", "");
        rewardPlayerIdText = tag.getStringOr("rewardPlayerIdText", "");
        if (uuid(tag.getStringOr("submitterIdText", "")) != null) submitterIdText = tag.getStringOr("submitterIdText", "");
        queue.load(tag, "queuedTaskIds");
        matchedTasks.load(tag, "matchedTaskIds");
        nextQueuedIndex = 0;
        nextMatchingIndex = 0;
        deferredAutoClaims.clear();
        ListTag deferred = tag.getListOrEmpty("deferredAutoClaims");
        for (int i = 0; i < deferred.size(); i++) {
            CompoundTag entry = deferred.getCompoundOrEmpty(i);
            long id = entry.getLongOr("questId", 0L);
            if (id != 0) deferredAutoClaims.put(id,
                    new DeferredAutoClaim(entry.getBooleanOr("claimItems", false), entry.getBooleanOr("replayNative", false)));
        }
        matching = tag.getBooleanOr("matching", false);
        queueSize = displayedQueue().size();
        suppressCompletionToast = tag.getBooleanOr("suppressCompletionToast", false);
        claimOnlyQueuedRewards = !tag.getBooleanOr("rewardClaimModeInitialized", false) || tag.getBooleanOr("claimOnlyQueuedRewards", false);
        pendingRewards.clear();
        ListTag pending = tag.getListOrEmpty("FtbqPendingRewards");
        for (int i = 0; i < pending.size(); i++) {
            CompoundTag entry = pending.getCompoundOrEmpty(i);
            AEItemKey key = AEItemKey.fromTag(TagValueInput.create(ProblemReporter.DISCARDING, registries,
                    entry.getCompoundOrEmpty("key")));
            long amount = entry.getLongOr("amount", 0L);
            if (key != null && amount > 0) pendingRewards.add(key, amount);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!(level instanceof ServerLevel serverLevel)) return;
        CompoundTag data = new CompoundTag();
        saveUserData(data, serverLevel.registryAccess());
        output.store("SubmitterData", CompoundTag.CODEC, data);
        mainNode.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadUserData(input.read("SubmitterData", CompoundTag.CODEC).orElseGet(CompoundTag::new), input.lookup());
        if (level == null || !level.isClientSide()) mainNode.deserialize(input);
    }

    @Override protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        // Minecraft's client Ctrl+pick copies this map directly. Never export client identity.
        if (level == null || level.isClientSide()) return;
        CompoundTag user = new CompoundTag();
        saveUserData(user, level.registryAccess());
        CompoundTag data = new CompoundTag(); data.put("SubmitterData", user);
        components.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
    }

    @Override protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        CustomData customData = components.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (level != null && customData.contains("SubmitterData")) {
            loadUserData(customData.copyTag().getCompoundOrEmpty("SubmitterData"), level.registryAccess());
        }
    }

    public CompoundTag writeClientState() {
        CompoundTag tag = new CompoundTag();
        tag.putString("teamId", teamIdText); tag.putString("playerId", rewardPlayerIdText);
        tag.putString("submitterId", submitterIdText); tag.putBoolean("online", online);
        tag.putInt("queue", queueSize); tag.putLong("submitted", lastSubmitted); tag.putLong("rewards", lastClaimedRewards);
        tag.putLong("submittedFluid", lastSubmittedFluid); tag.putBoolean("matching", matching);
        if (lastSubmittedChemical > 0) tag.putLong("submittedChemical", lastSubmittedChemical);
        tag.putBoolean("suppressToast", suppressCompletionToast); tag.putBoolean("queuedRewards", claimOnlyQueuedRewards);
        tag.putString("status", statusKey); tag.putLongArray("statusArgs", statusArgs);
        return tag;
    }

    /** Author: dishanhai. Only the open menu receives its requested queue page. */
    public CompoundTag writeQueueState(int requestedPage) {
        return writeQueueState(requestedPage, null);
    }

    private TaskQueue displayedQueue() { return matching ? matchedTasks : queue; }
    public List<Long> getQueuedTaskIds() { return displayedQueue().list(); }

    public CompoundTag writeQueueState(int requestedPage, Set<Long> searchMatches) {
        List<Long> ids = getQueuedTaskIds();
        if (searchMatches != null) ids = ids.stream().filter(searchMatches::contains).toList();
        return writeQueueState(writeClientState(), queuePage(requestedPage, ids, searchMatches != null));
    }

    CompoundTag menuSummary() {
        return lastClientState == null ? writeClientState() : lastClientState;
    }

    QueuePage queuePage(int requestedPage, List<Long> ids, boolean filtering) {
        int pages = Math.max(1, (ids.size() + SubmitterMenu.PAGE_SIZE - 1) / SubmitterMenu.PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        if (ids.isEmpty()) return new QueuePage(page, pages, 0, filtering, List.of());
        List<SubmitterMenu.QueueRow> rows = new ArrayList<>(SubmitterMenu.PAGE_SIZE);
        ServerQuestFile file = (ServerQuestFile.exists() ? ServerQuestFile.getInstance() : null);
        UUID teamId = uuid(teamIdText);
        TeamData data = file == null || teamId == null ? null : file.getOrCreateTeamData(teamId);
        int end = Math.min(ids.size(), (page + 1) * SubmitterMenu.PAGE_SIZE);
        for (int i = page * SubmitterMenu.PAGE_SIZE; i < end; i++) {
            long id = ids.get(i);
            Task task = file == null ? null : file.getTask(id);
            boolean valid = SubmitterQuestIndex.supportsQueueTask(task);
            Component title = valid ? targetName(task) : Component.translatable(
                    "gui.ftb_quest_nexus.submitter.queue_detail.invalid", Long.toUnsignedString(id, 16));
            rows.add(new SubmitterMenu.QueueRow(id, title, valid, task instanceof FluidTask, task instanceof ChemicalTask,
                    valid && data != null ? data.getProgress(task) : 0, valid ? task.getMaxProgress() : 0,
                    valid && data != null && data.isCompleted(task)));
        }
        return new QueuePage(page, pages, ids.size(), filtering, rows);
    }

    CompoundTag writeQueueState(CompoundTag tag, QueuePage page) {
        tag.putInt("matched", page.matched());
        tag.putBoolean("filtering", page.filtering());
        tag.putInt("page", page.page());
        tag.putInt("pages", page.pages());
        ListTag rows = new ListTag();
        for (var goal : page.rows()) {
            CompoundTag row = new CompoundTag();
            row.putLong("id", goal.taskId());
            row.store("title", ComponentSerialization.CODEC, level.registryAccess().createSerializationContext(NbtOps.INSTANCE), goal.title());
            row.putBoolean("valid", goal.valid());
            row.putBoolean("fluid", goal.fluid());
            if (goal.chemical()) row.putBoolean("chemical", true);
            row.putLong("progress", goal.progress());
            row.putLong("required", goal.required());
            row.putBoolean("completed", goal.completed());
            rows.add(row);
        }
        tag.put("rows", rows);
        return tag;
    }

    record QueuePage(int page, int pages, int matched, boolean filtering, List<SubmitterMenu.QueueRow> rows) {}

    public void readClientState(CompoundTag tag) {
        teamIdText = tag.getStringOr("teamId", ""); rewardPlayerIdText = tag.getStringOr("playerId", "");
        submitterIdText = tag.getStringOr("submitterId", ""); online = tag.getBooleanOr("online", false);
        queueSize = tag.getIntOr("queue", 0); lastSubmitted = tag.getLongOr("submitted", 0L); lastClaimedRewards = tag.getLongOr("rewards", 0L);
        lastSubmittedFluid = tag.getLongOr("submittedFluid", 0L); matching = tag.getBooleanOr("matching", false);
        lastSubmittedChemical = tag.getLongOr("submittedChemical", 0L);
        suppressCompletionToast = tag.getBooleanOr("suppressToast", false); claimOnlyQueuedRewards = tag.getBooleanOr("queuedRewards", false);
        statusKey = tag.getStringOr("status", ""); statusArgs = tag.getLongArray("statusArgs").orElseGet(() -> new long[0]);
    }

    private void syncState() {
        if (level == null || level.isClientSide()) return;
        CompoundTag state = writeClientState();
        if (!state.equals(lastClientState)) {
            lastClientState = state;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("NexusState", writeClientState());
        return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void handleUpdateTag(ValueInput input) {
        input.read("NexusState", CompoundTag.CODEC).ifPresent(this::readClientState);
    }
    @Override public void onDataPacket(net.minecraft.network.Connection connection, ValueInput input) {
        handleUpdateTag(input);
    }
    @Override public Component getDisplayName() { return Component.translatable("block.ftb_quest_nexus.ftbq_ae_submitter"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SubmitterMenu(id, inventory, this);
    }
    public String getTeamIdText() { return teamIdText; }
    public Component getQueueDetailComponent() {
        String key = "gui.ftb_quest_nexus.submitter.queue_detail.";
        if (displayedQueue().isEmpty()) return Component.translatable(key + "empty");
        var detail = Component.empty();
        ServerQuestFile file = (ServerQuestFile.exists() ? ServerQuestFile.getInstance() : null);
        List<Long> ids = getQueuedTaskIds();
        int shown = Math.min(3, ids.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) detail.append(", ");
            long id = ids.get(i);
            String idText = Long.toUnsignedString(id, 16);
            Task task = file == null ? null : file.getTask(id);
            if (file == null) detail.append(idText);
            else if (task == null) detail.append(Component.translatable(key + "invalid", idText));
            else if (SubmitterQuestIndex.supportsQueueTask(task)) detail.append(targetName(task));
            else detail.append(idText);
        }
        if (ids.size() > shown) detail.append(Component.translatable(key + "more", ids.size() - shown));
        return detail;
    }
    public String getRewardPlayerIdText() { return rewardPlayerIdText; }
    public String getSubmitterIdText() { return submitterIdText; }
    public boolean isOnline() { return online; }
    public int getQueueSize() { return queueSize; }
    public long getLastSubmitted() { return lastSubmitted; }
    public long getLastSubmittedFluid() { return lastSubmittedFluid; }
    public long getLastSubmittedChemical() { return lastSubmittedChemical; }
    public boolean isMatching() { return matching; }

    private static Component targetName(Task task) {
        if (task instanceof ChemicalTask chemical) return chemical.targetName();
        if (task instanceof FluidTask fluid) {
            return Component.translatable(fluid.getFluid().getFluidType().getDescriptionId());
        }
        if (task instanceof ItemTask item && ConsumableTask.supported(item)) {
            return item.getItemStack().getHoverName();
        }
        return task.getQuest().getTitle();
    }
    public long getLastClaimedRewards() { return lastClaimedRewards; }
    public boolean isSuppressCompletionToast() { return suppressCompletionToast; }
    public boolean isClaimOnlyQueuedRewards() { return claimOnlyQueuedRewards; }
    private static UUID uuid(String text) {
        try { return text == null || text.isBlank() ? null : UUID.fromString(text.trim()); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
