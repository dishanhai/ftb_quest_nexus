package com.dishanhai.ftb_quest_nexus.submitter;

// Author: dishanhai. Forked from gt_shanhai's FTBQ AE submitter; see SOURCE.md.
import appeng.api.config.Actionable;
import appeng.api.features.IPlayerRegistry;
import appeng.api.networking.*;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.util.AECableType;
import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqRewardBuffer;
import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqSubmitterToastSuppressor;
import com.dishanhai.ftb_quest_nexus.api.ftbq.TaskQueue;
import com.dishanhai.ftb_quest_nexus.NexusContent;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardClaimType;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public final class SubmitterBlockEntity extends BlockEntity implements MenuProvider, IInWorldGridNodeHost, IActionHost {
    private static final Map<UUID, SubmitterBlockEntity> LOADED = new HashMap<>();
    private final TaskQueue queue = new TaskQueue();
    private final FtbqRewardBuffer<AEItemKey> pendingRewards = new FtbqRewardBuffer<>();
    private final IManagedGridNode mainNode;
    private String teamIdText = "";
    private String rewardPlayerIdText = "";
    private String submitterIdText = UUID.randomUUID().toString();
    private boolean suppressCompletionToast;
    private boolean claimOnlyQueuedRewards = true;
    private boolean online;
    private long lastSubmitted;
    private long lastClaimedRewards;
    private int queueSize;
    private String statusKey = "unbound";
    private long[] statusArgs = new long[0];
    private CompoundTag lastClientState;

    public SubmitterBlockEntity(BlockPos pos, BlockState state) {
        super(NexusContent.BLOCK_ENTITY.get(), pos, state);
        mainNode = GridHelper.createManagedNode(this, new IGridNodeListener<SubmitterBlockEntity>() {
            @Override public void onSaveChanges(SubmitterBlockEntity owner, IGridNode node) { owner.setChanged(); }
        }).setTagName("aeNode").setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL)
                .setExposedOnSides(EnumSet.allOf(Direction.class)).setIdlePowerUsage(1.0);
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) return;
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
        if (level == null || level.isClientSide) return;
        UUID id = uuid(submitterIdText);
        if (id != null) LOADED.remove(id, this);
        mainNode.destroy();
    }

    @Override public void onChunkUnloaded() { unloadNode(); super.onChunkUnloaded(); }
    @Override public void setRemoved() { unloadNode(); super.setRemoved(); }
    public static void clearLoadedSubmitters() { LOADED.clear(); }
    public IManagedGridNode getMainNode() { return mainNode; }
    @Override public IGridNode getGridNode(Direction side) { return mainNode.getNode(); }
    @Override public IGridNode getActionableNode() { return mainNode.getNode(); }
    @Override public AECableType getCableConnectionType(Direction side) { return AECableType.SMART; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SubmitterBlockEntity entity) {
        if (level.isClientSide || level.getGameTime() % 20 != 0) return;
        entity.online = entity.mainNode.isOnline();
        if (state.hasProperty(SubmitterBlock.ONLINE) && state.getValue(SubmitterBlock.ONLINE) != entity.online) {
            level.setBlock(pos, state.setValue(SubmitterBlock.ONLINE, entity.online), 2);
        }
        entity.runQueuedWork();
        entity.syncState();
    }

    private void status(String key, long... args) { statusKey = key; statusArgs = args; }
    public Component getStatusComponent() {
        Object[] args = new Object[statusArgs.length];
        for (int i = 0; i < args.length; i++) args[i] = statusArgs[i];
        return Component.translatable("status.ftb_quest_nexus." + statusKey, args);
    }

    public boolean applyAction(ServerPlayer player, int action) {
        if (level == null || level.isClientSide || player.level() != level
                || !worldPosition.closerToCenterThan(player.position(), 8.0)
                || (action != 0 && !canManage(player))) return false;
        switch (action) {
            case 0 -> {
                // Rebinding uses the acting player's real team, including after leaving the old team.
                if (!pendingRewards.isEmpty()) { status("pending_rebind"); break; }
                TeamData data = playerData(player);
                if (data == null) { status("quests_unavailable"); break; }
                if (!data.getTeamId().equals(uuid(teamIdText))) queue.clear();
                teamIdText = data.getTeamId().toString();
                rewardPlayerIdText = player.getUUID().toString();
                mainNode.setOwningPlayer(player);
                status("bound");
            }
            case 1 -> { queue.clear(); status("cleared"); }
            case 2 -> { suppressCompletionToast = !suppressCompletionToast; status(suppressCompletionToast ? "toast_hidden" : "toast_shown"); }
            case 3 -> { claimOnlyQueuedRewards = !claimOnlyQueuedRewards; status(claimOnlyQueuedRewards ? "rewards_queued" : "rewards_all"); }
            default -> { return false; }
        }
        queueSize = queue.size();
        setChanged();
        syncState();
        return true;
    }

    public boolean canManage(ServerPlayer player) {
        UUID bound = uuid(teamIdText);
        TeamData data = playerData(player);
        return data != null && (bound == null || bound.equals(data.getTeamId()));
    }

    private static TeamData playerData(ServerPlayer player) {
        return player == null || ServerQuestFile.INSTANCE == null ? null : ServerQuestFile.INSTANCE.getOrCreateTeamData(player);
    }

    private void runQueuedWork() {
        lastSubmitted = 0;
        lastClaimedRewards = 0;
        if (!online) { status("offline"); return; }
        IGrid grid = mainNode.getGrid();
        if (grid == null) { status("no_network"); return; }
        ServerQuestFile file = ServerQuestFile.INSTANCE;
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
        for (Long taskId : queue.list()) {
            Task task = file.getTask(taskId);
            if (!(task instanceof ItemTask itemTask)) { queue.remove(taskId); setChanged(); continue; }
            if (data.isCompleted(itemTask)) continue;
            long submitted = QuestWork.extract(storage, data, itemTask, source);
            if (submitted <= 0) continue;
            if (suppressCompletionToast) FtbqSubmitterToastSuppressor.runSuppressed(() -> data.addProgress(itemTask, submitted));
            else data.addProgress(itemTask, submitted);
            lastSubmitted = submitted;
            setChanged();
            break;
        }
        claimRewards(file, storage, data, playerId, source);
        queueSize = queue.size();
        if (!pendingRewards.isEmpty()) status("pending_rewards");
        else if (lastSubmitted > 0 || lastClaimedRewards > 0) status("worked", lastSubmitted, lastClaimedRewards);
        else status(queue.isEmpty() ? "waiting" : "missing_items");
    }

    private void claimRewards(ServerQuestFile file, MEStorage storage, TeamData data, UUID playerId, IActionSource source) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        Set<Quest> quests = new LinkedHashSet<>();
        if (claimOnlyQueuedRewards) {
            for (Long id : queue.list()) {
                Task task = file.getTask(id);
                if (task != null && task.getQuest() != null) quests.add(task.getQuest());
            }
        } else file.forAllQuests(quests::add);
        for (Quest quest : quests) {
            if (!data.isCompleted(quest)) continue;
            for (Reward reward : quest.getRewards()) {
                if (data.getClaimType(playerId, reward) != RewardClaimType.CAN_CLAIM) continue;
                lastClaimedRewards += QuestWork.claim(storage, data, playerId, reward, serverLevel,
                        this, pendingRewards, source, this::setChanged);
                if (!pendingRewards.isEmpty()) return;
            }
        }
    }

    public static boolean queueTask(ServerPlayer player, UUID submitterId, long taskId) {
        if (taskId == 0 || submitterId == null || player == null) return false;
        SubmitterBlockEntity entity = LOADED.get(submitterId);
        if (entity == null || entity.isRemoved() || entity.level == null
                || entity.level.getServer() != player.getServer() || !entity.canManage(player)
                || uuid(entity.teamIdText) == null) return false;
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        Task task = file == null ? null : file.getTask(taskId);
        if (!(task instanceof ItemTask itemTask) || !itemTask.consumesResources() || itemTask.isOnlyFromCrafting()) return false;
        boolean added = entity.queue.add(taskId);
        entity.queueSize = entity.queue.size();
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
            if (id == null || entity.isRemoved() || entity.level == null || entity.level.getServer() != player.getServer()
                    || !data.getTeamId().equals(uuid(entity.teamIdText))) continue;
            entries.add(new SubmitterEntry(id, entity.level.dimension().location() + " " + entity.worldPosition.toShortString(),
                    entity.queue.size(), entity.online));
        }
        entries.sort(Comparator.comparing(entry -> entry.id().toString()));
        return entries;
    }

    public record SubmitterEntry(UUID id, String pos, int queueSize, boolean online) {}

    public void saveUserData(CompoundTag tag) {
        tag.putString("teamIdText", teamIdText);
        tag.putString("rewardPlayerIdText", rewardPlayerIdText);
        tag.putString("submitterIdText", submitterIdText);
        tag.putString("queuedTaskIdsText", queue.encode());
        tag.putBoolean("suppressCompletionToast", suppressCompletionToast);
        tag.putBoolean("claimOnlyQueuedRewards", claimOnlyQueuedRewards);
        tag.putBoolean("rewardClaimModeInitialized", true);
        ListTag pending = new ListTag();
        pendingRewards.snapshot().forEach((key, amount) -> {
            CompoundTag entry = new CompoundTag();
            entry.put("key", key.toTag()); entry.putLong("amount", amount); pending.add(entry);
        });
        tag.put("FtbqPendingRewards", pending);
    }

    public void loadUserData(CompoundTag tag) {
        teamIdText = tag.getString("teamIdText");
        rewardPlayerIdText = tag.getString("rewardPlayerIdText");
        if (uuid(tag.getString("submitterIdText")) != null) submitterIdText = tag.getString("submitterIdText");
        queue.load(tag.getString("queuedTaskIdsText"));
        queueSize = queue.size();
        suppressCompletionToast = tag.getBoolean("suppressCompletionToast");
        claimOnlyQueuedRewards = !tag.getBoolean("rewardClaimModeInitialized") || tag.getBoolean("claimOnlyQueuedRewards");
        pendingRewards.clear();
        ListTag pending = tag.getList("FtbqPendingRewards", Tag.TAG_COMPOUND);
        for (int i = 0; i < pending.size(); i++) {
            CompoundTag entry = pending.getCompound(i);
            AEItemKey key = AEItemKey.fromTag(entry.getCompound("key"));
            long amount = entry.getLong("amount");
            if (key != null && amount > 0) pendingRewards.add(key, amount);
        }
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // Ctrl+pick calls saveWithFullMetadata on the client, bypassing getCloneItemStack.
        // Client state is presentation only; never export binding, UUID or reward data from it.
        if (level != null && level.isClientSide) return;
        CompoundTag data = new CompoundTag(); saveUserData(data); tag.put("SubmitterData", data);
        mainNode.saveToNBT(tag);
    }

    @Override public void saveToItem(net.minecraft.world.item.ItemStack stack) {
        // Server Ctrl+pick rehydrates from block coordinates through this method.
        // Survival drops use the separate copy_nbt loot function and keep their data.
        stack.removeTagKey("BlockEntityTag");
    }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        loadUserData(tag.getCompound("SubmitterData"));
        if (level == null || !level.isClientSide) mainNode.loadFromNBT(tag);
    }

    public CompoundTag writeClientState() {
        CompoundTag tag = new CompoundTag();
        tag.putString("teamId", teamIdText); tag.putString("playerId", rewardPlayerIdText);
        tag.putString("submitterId", submitterIdText); tag.putBoolean("online", online);
        tag.putInt("queue", queueSize); tag.putLong("submitted", lastSubmitted); tag.putLong("rewards", lastClaimedRewards);
        tag.putBoolean("suppressToast", suppressCompletionToast); tag.putBoolean("queuedRewards", claimOnlyQueuedRewards);
        tag.putString("status", statusKey); tag.putLongArray("statusArgs", statusArgs);
        return tag;
    }

    public void readClientState(CompoundTag tag) {
        teamIdText = tag.getString("teamId"); rewardPlayerIdText = tag.getString("playerId");
        submitterIdText = tag.getString("submitterId"); online = tag.getBoolean("online");
        queueSize = tag.getInt("queue"); lastSubmitted = tag.getLong("submitted"); lastClaimedRewards = tag.getLong("rewards");
        suppressCompletionToast = tag.getBoolean("suppressToast"); claimOnlyQueuedRewards = tag.getBoolean("queuedRewards");
        statusKey = tag.getString("status"); statusArgs = tag.getLongArray("statusArgs");
    }

    private void syncState() {
        if (level == null || level.isClientSide) return;
        CompoundTag state = writeClientState();
        if (!state.equals(lastClientState)) {
            lastClientState = state;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override public CompoundTag getUpdateTag() { return writeClientState(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public void handleUpdateTag(CompoundTag tag) { readClientState(tag); }
    @Override public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet) {
        if (packet.getTag() != null) readClientState(packet.getTag());
    }
    @Override public Component getDisplayName() { return Component.translatable("block.ftb_quest_nexus.ftbq_ae_submitter"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SubmitterMenu(id, inventory, this);
    }
    public String getTeamIdText() { return teamIdText; }
    public Component getQueueDetailComponent() {
        String key = "gui.ftb_quest_nexus.submitter.queue_detail.";
        if (queue.isEmpty()) return Component.translatable(key + "empty");
        var detail = Component.empty();
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        List<Long> ids = queue.list();
        int shown = Math.min(3, ids.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) detail.append(", ");
            long id = ids.get(i);
            String idText = Long.toUnsignedString(id, 16);
            Task task = file == null ? null : file.getTask(id);
            if (file == null) detail.append(idText);
            else if (task == null) detail.append(Component.translatable(key + "invalid", idText));
            else if (task.getQuest() == null || task.getQuest().getTitle().getString().isBlank()) detail.append(idText);
            else detail.append(task.getQuest().getTitle());
        }
        if (ids.size() > shown) detail.append(Component.translatable(key + "more", ids.size() - shown));
        return detail;
    }
    public String getRewardPlayerIdText() { return rewardPlayerIdText; }
    public String getSubmitterIdText() { return submitterIdText; }
    public boolean isOnline() { return online; }
    public int getQueueSize() { return queueSize; }
    public long getLastSubmitted() { return lastSubmitted; }
    public long getLastClaimedRewards() { return lastClaimedRewards; }
    public boolean isSuppressCompletionToast() { return suppressCompletionToast; }
    public boolean isClaimOnlyQueuedRewards() { return claimOnlyQueuedRewards; }
    private static UUID uuid(String text) {
        try { return text == null || text.isBlank() ? null : UUID.fromString(text.trim()); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
