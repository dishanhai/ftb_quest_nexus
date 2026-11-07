package com.dishanhai.ftb_quest_nexus.checker;

import appeng.api.features.IPlayerRegistry;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.util.AECableType;
import com.dishanhai.ftb_quest_nexus.NexusContent;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Author: dishanhai. Binds its placer and completes resource goals from read-only ME stock. */
public final class CheckerBlockEntity extends BlockEntity implements MenuProvider, IInWorldGridNodeHost, IActionHost {
    public static final int PAGE_SIZE = 6;
    static final int CATALOG_CHUNK_SIZE = 64;
    private final IManagedGridNode mainNode;
    private UUID playerId;
    private UUID teamId;
    private boolean paused;
    private boolean online;
    private String status = "unbound";
    private int itemTaskCount;
    private int fluidTaskCount;
    private int completedLastScan;
    private List<CheckRow> rows = List.of();

    public CheckerBlockEntity(BlockPos pos, BlockState state) {
        super(NexusContent.CHECKER_BLOCK_ENTITY.get(), pos, state);
        mainNode = GridHelper.createManagedNode(this, new IGridNodeListener<CheckerBlockEntity>() {
            @Override
            public void onSaveChanges(CheckerBlockEntity owner, IGridNode node) { owner.setChanged(); }
        }).setTagName("aeNode").setInWorldNode(true).setFlags(GridFlags.REQUIRE_CHANNEL)
                .setExposedOnSides(java.util.EnumSet.allOf(Direction.class)).setIdlePowerUsage(1.0);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) return;
        GridHelper.onFirstTick(this, owner -> {
            if (owner.isRemoved() || !(owner.level instanceof ServerLevel)) return;
            owner.mainNode.setVisualRepresentation(NexusContent.CHECKER_ITEM.get());
            owner.mainNode.create(owner.level, owner.worldPosition);
            if (owner.playerId != null) {
                owner.mainNode.setOwningPlayerId(IPlayerRegistry.getMapping(owner.level).getPlayerId(owner.playerId));
            }
        });
    }

    public void bindPlacer(ServerPlayer player) {
        TeamData data = playerData(player);
        playerId = player.getUUID();
        teamId = data == null ? null : data.getTeamId();
        mainNode.setOwningPlayer(player);
        rows = List.of();
        itemTaskCount = 0;
        fluidTaskCount = 0;
        completedLastScan = 0;
        status = teamId == null ? "unbound" : "waiting";
        setChanged();
    }

    private static TeamData playerData(ServerPlayer player) {
        return ServerQuestFile.INSTANCE == null ? null : ServerQuestFile.INSTANCE.getOrCreateTeamData(player);
    }

    public boolean applyAction(ServerPlayer player, int action) {
        if (level == null || level.isClientSide || player.level() != level
                || !worldPosition.closerToCenterThan(player.position(), 8) || !canManage(player)) return false;
        if (action == 0) bindPlacer(player);
        else if (action == 1) {
            paused = !paused;
            status = paused ? "paused" : "waiting";
            setChanged();
        } else return false;
        return true;
    }

    public boolean canManage(ServerPlayer player) {
        TeamData data = playerData(player);
        return data != null && (teamId == null ? playerId == null || playerId.equals(player.getUUID())
                : teamId.equals(data.getTeamId()));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CheckerBlockEntity entity) {
        if (level.isClientSide || (level.getGameTime() + pos.asLong()) % 20 != 0) return;
        entity.online = entity.mainNode.isOnline();
        if (state.getValue(CheckerBlock.ONLINE) != entity.online) {
            level.setBlock(pos, state.setValue(CheckerBlock.ONLINE, entity.online), 2);
        }
        entity.scan();
    }

    private void scan() {
        completedLastScan = 0;
        rows = List.of();
        itemTaskCount = 0;
        fluidTaskCount = 0;
        if (paused) { status = "paused"; return; }
        if (!online || mainNode.getGrid() == null) { status = "offline"; return; }
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        if (file == null) { status = "quests_unavailable"; return; }
        if (playerId == null || teamId == null) { status = "unbound"; return; }
        UUID currentTeam = FTBTeamsAPI.api().getManager().getTeamForPlayerID(playerId)
                .map(team -> team.getTeamId()).orElse(null);
        if (!teamId.equals(currentTeam)) { status = "player_left"; return; }
        TeamData data = file.getOrCreateTeamData(teamId);
        if (data.isLocked()) { status = "locked"; return; }
        scanInventory(file, data, new InventoryCheck(mainNode.getGrid().getStorageService().getInventory()));
        status = "scanned";
    }

    // Shared by the normal server tick and the regression tests; no inventory mutation path.
    void scanInventory(ServerQuestFile file, TeamData data, InventoryCheck inventory) {
        List<CheckRow> result = new ArrayList<>();
        file.forAllQuests(quest -> {
            if (data.isLocked() || !data.canStartTasks(quest) || !data.areDependenciesComplete(quest)) return;
            // Capture the round before progress callbacks can reset a repeatable quest.
            Set<Task> incomplete = quest.getTasks().stream().filter(task -> !data.isCompleted(task))
                    .collect(Collectors.toSet());
            boolean roundReset = false;
            for (Task task : quest.getTasks()) {
                if (!InventoryCheck.supported(task)) continue;
                boolean fluid = task instanceof FluidTask;
                if (fluid) fluidTaskCount++; else itemTaskCount++;
                long amount = inventory.available(task);
                boolean wasCompleted = !incomplete.contains(task);
                boolean completedNow = !wasCompleted && !roundReset && InventoryCheck.complete(data, task, amount);
                if (completedNow) {
                    completedLastScan++;
                    roundReset = !data.isCompleted(task);
                }
                // FTBQ title APIs are client-only. Send a common-side fallback with the task ID.
                Component title = task instanceof dev.ftb.mods.ftbquests.quest.task.ItemTask item
                        ? item.getItemStack().getHoverName()
                        : Component.translatable(((FluidTask) task).getFluid().getFluidType().getDescriptionId());
                result.add(new CheckRow(task.id, title, fluid, amount, task.getMaxProgress(),
                        wasCompleted || completedNow));
            }
        });
        rows = List.copyOf(result);
    }

    @Override public IGridNode getGridNode(Direction side) { return mainNode.getNode(); }
    @Override public IGridNode getActionableNode() { return mainNode.getNode(); }
    @Override public AECableType getCableConnectionType(Direction side) { return AECableType.SMART; }
    public IManagedGridNode getMainNode() { return mainNode; }

    private void unloadNode() {
        if (level != null && !level.isClientSide) mainNode.destroy();
    }
    @Override public void onChunkUnloaded() { unloadNode(); super.onChunkUnloaded(); }
    @Override public void setRemoved() { unloadNode(); super.setRemoved(); }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (level != null && level.isClientSide) return;
        if (playerId != null) tag.putUUID("checkerPlayer", playerId);
        if (teamId != null) tag.putUUID("checkerTeam", teamId);
        tag.putBoolean("checkerPaused", paused);
        mainNode.saveToNBT(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        playerId = tag.hasUUID("checkerPlayer") ? tag.getUUID("checkerPlayer") : null;
        teamId = tag.hasUUID("checkerTeam") ? tag.getUUID("checkerTeam") : null;
        paused = tag.getBoolean("checkerPaused");
        if (level == null || !level.isClientSide) mainNode.loadFromNBT(tag);
    }

    @Override
    public void saveToItem(ItemStack stack) {
        stack.removeTagKey("BlockEntityTag");
    }

    public int getPageCount() { return Math.max(1, (rows.size() + PAGE_SIZE - 1) / PAGE_SIZE); }

    public CompoundTag writeClientState(int requestedPage) {
        return writeClientState(requestedPage, PAGE_SIZE);
    }

    CompoundTag writeClientState(int requestedPage, int pageSize) {
        CompoundTag tag = new CompoundTag();
        tag.putString("player", getPlayerIdText());
        tag.putString("team", getTeamIdText());
        tag.putBoolean("online", online);
        tag.putBoolean("paused", paused);
        tag.putString("status", status);
        tag.putInt("items", itemTaskCount);
        tag.putInt("fluids", fluidTaskCount);
        tag.putInt("completed", completedLastScan);
        int pages = Math.max(1, (rows.size() + pageSize - 1) / pageSize);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        tag.putInt("page", page);
        tag.putInt("pages", pages);
        ListTag entries = new ListTag();
        int end = Math.min(rows.size(), (page + 1) * pageSize);
        for (int i = page * pageSize; i < end; i++) {
            CheckRow row = rows.get(i);
            CompoundTag entry = new CompoundTag();
            entry.putLong("id", row.taskId());
            entry.putString("title", Component.Serializer.toJson(row.title()));
            entry.putBoolean("fluid", row.fluid());
            entry.putLong("available", row.available());
            entry.putLong("required", row.required());
            entry.putBoolean("completed", row.completed());
            entries.add(entry);
        }
        tag.put("rows", entries);
        return tag;
    }

    public void readClientState(CompoundTag tag) {
        playerId = parseUuid(tag.getString("player"));
        teamId = parseUuid(tag.getString("team"));
        online = tag.getBoolean("online");
        paused = tag.getBoolean("paused");
        status = tag.getString("status");
        itemTaskCount = tag.getInt("items");
        fluidTaskCount = tag.getInt("fluids");
        completedLastScan = tag.getInt("completed");
        List<CheckRow> result = new ArrayList<>();
        ListTag entries = tag.getList("rows", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(CATALOG_CHUNK_SIZE, entries.size()); i++) {
            CompoundTag entry = entries.getCompound(i);
            Component title = Component.Serializer.fromJson(entry.getString("title"));
            result.add(new CheckRow(entry.getLong("id"), title == null ? Component.empty() : title,
                    entry.getBoolean("fluid"), entry.getLong("available"), entry.getLong("required"),
                    entry.getBoolean("completed")));
        }
        rows = List.copyOf(result);
    }

    private static UUID parseUuid(String text) {
        try { return text.isBlank() ? null : UUID.fromString(text); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    @Override public Component getDisplayName() { return Component.translatable("block.ftb_quest_nexus.ftbq_ae_checker"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CheckerMenu(id, inventory, this);
    }
    public String getPlayerIdText() { return playerId == null ? "" : playerId.toString(); }
    public String getTeamIdText() { return teamId == null ? "" : teamId.toString(); }
    public boolean isOnline() { return online; }
    public boolean isPaused() { return paused; }
    public int getItemTaskCount() { return itemTaskCount; }
    public int getFluidTaskCount() { return fluidTaskCount; }
    public int getCompletedLastScan() { return completedLastScan; }
    public List<CheckRow> getRows() { return rows; }
    public Component getStatusComponent() { return Component.translatable("status.ftb_quest_nexus.checker." + status); }

    public record CheckRow(long taskId, Component title, boolean fluid, long available, long required, boolean completed) {}
}

