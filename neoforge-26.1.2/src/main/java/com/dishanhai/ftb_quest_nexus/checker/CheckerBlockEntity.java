package com.dishanhai.ftb_quest_nexus.checker;

import net.minecraft.nbt.NbtOps;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.network.chat.ComponentSerialization;

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
import com.dishanhai.ftb_quest_nexus.config.ServerConfig;
import com.dishanhai.ftb_quest_nexus.config.ServerOption;
import com.dishanhai.ftb_quest_nexus.api.BindingTransfer;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ConsumableTask;
import com.dishanhai.ftb_quest_nexus.compat.mekanism.ChemicalTask;
import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqQuestFileRevision;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.neoforge.ForgeEnergyTask;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
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
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Author: dishanhai. Checks ME stock and accepts FE for its bound team's quests. */
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
    private int energyTaskCount;
    private int chemicalTaskCount;
    private int completedLastScan;
    private List<CheckRow> rows = List.of();
    private int menuViewers;
    private EnergyTarget energyTarget;
    private final EnergyHandler energyInput = new EnergyInput();

    private final class EnergyInput extends SnapshotJournal<Integer> implements EnergyHandler {
        private int pending;

        @Override public long getAmountAsLong() { return pending; }
        @Override public long getCapacityAsLong() { return acceptEnergy(Integer.MAX_VALUE, true); }

        @Override
        public int insert(int offered, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(offered);
            int accepted = Math.max(0, acceptEnergy((int) Math.min(Integer.MAX_VALUE, (long) offered + pending),
                    true) - pending);
            if (accepted > 0) {
                updateSnapshots(transaction);
                pending += accepted;
            }
            return accepted;
        }

        @Override public int extract(int offered, TransactionContext transaction) {
            TransferPreconditions.checkNonNegative(offered);
            return 0;
        }

        @Override protected Integer createSnapshot() { return pending; }
        @Override protected void revertToSnapshot(Integer snapshot) { pending = snapshot; }

        @Override
        protected void onRootCommit(Integer snapshot) {
            int committed = pending;
            pending = 0;
            acceptEnergy(committed, false);
        }
    }

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
        if (level == null || level.isClientSide()) return;
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
        if (playerId == null) bindPlayer(player);
    }

    private void bindPlayer(ServerPlayer player) {
        TeamData data = playerData(player);
        playerId = player.getUUID();
        teamId = data == null ? null : data.getTeamId();
        mainNode.setOwningPlayer(player);
        rows = List.of();
        itemTaskCount = 0;
        fluidTaskCount = 0;
        energyTaskCount = 0;
        chemicalTaskCount = 0;
        energyTarget = null;
        completedLastScan = 0;
        status = teamId == null ? "unbound" : "waiting";
        setChanged();
    }

    private static TeamData playerData(ServerPlayer player) {
        return ServerQuestFile.exists() ? ServerQuestFile.getInstance().getTeamData(player).orElse(null) : null;
    }

    public boolean applyAction(ServerPlayer player, int action) {
        if (level == null || level.isClientSide() || player.level() != level
                || !worldPosition.closerToCenterThan(player.position(), 8) || !canManage(player)) return false;
        if (action == 0) {
            if (playerId != null && !playerId.equals(player.getUUID())) {
                return BindingTransfer.request(this, player, () -> playerId, this::acceptBindingTransfer);
            }
            bindPlayer(player);
        } else if (action == 1) {
            paused = !paused;
            status = paused ? "paused" : "waiting";
            setChanged();
        } else return false;
        return true;
    }

    private boolean acceptBindingTransfer(ServerPlayer player) {
        if (isRemoved() || !(level instanceof ServerLevel) || !canManage(player)) return false;
        bindPlayer(player);
        return true;
    }

    public boolean canManage(ServerPlayer player) {
        TeamData data = playerData(player);
        return data != null && (teamId == null ? playerId == null || playerId.equals(player.getUUID())
                : teamId.equals(data.getTeamId()));
    }

    private boolean isBoundTeamCurrent() {
        return playerId != null && teamId != null && FTBTeamsAPI.api().getManager().getTeamForPlayerID(playerId)
                .map(team -> teamId.equals(team.getTeamId())).orElse(false);
    }

    public EnergyHandler energyInput() { return energyInput; }

    private int acceptEnergy(int offered, boolean simulate) {
        EnergyTarget target = energyTarget;
        if (offered <= 0 || target == null || paused || isRemoved()
                || !(level instanceof ServerLevel server) || !server.getServer().isSameThread()
                || !ServerConfig.get(ServerOption.AUTO_CHECK) || !isBoundTeamCurrent()) return 0;
        ServerQuestFile file = (ServerQuestFile.exists() ? ServerQuestFile.getInstance() : null);
        ForgeEnergyTask task = target.task();
        if (file == null || file != task.getQuestFile() || target.revision() != FtbqQuestFileRevision.get(file)
                || file.getTask(task.id) != task) return 0;
        TeamData data = file.getNullableTeamData(teamId);
        if (data == null || !ConsumableTask.canSubmit(data, task)) return 0;
        long remaining = task.getMaxProgress() - data.getProgress(task);
        int accepted = (int) Math.max(0, Math.min(offered, Math.min(remaining, task.getMaxInput())));
        if (accepted > 0 && !simulate) {
            // Completion can reset a repeatable quest inside FTBQ's progress callbacks.
            if (accepted == remaining) energyTarget = null;
            data.addProgress(task, accepted);
        }
        return accepted;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CheckerBlockEntity entity) {
        if (level.isClientSide() || (level.getGameTime() + pos.asLong()) % 20 != 0) return;
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
        energyTaskCount = 0;
        chemicalTaskCount = 0;
        energyTarget = null;
        if (!ServerConfig.get(ServerOption.AUTO_CHECK)) { status = "server_disabled"; return; }
        if (paused) { status = "paused"; return; }
        ServerQuestFile file = (ServerQuestFile.exists() ? ServerQuestFile.getInstance() : null);
        if (file == null) { status = "quests_unavailable"; return; }
        if (playerId == null || teamId == null) { status = "unbound"; return; }
        if (!isBoundTeamCurrent()) { status = "player_left"; return; }
        TeamData data = file.getOrCreateTeamData(teamId);
        if (data.isLocked()) { status = "locked"; return; }
        var grid = online ? mainNode.getGrid() : null;
        var inventory = grid == null ? null : new InventoryCheck(grid.getStorageService().getInventory());
        scanInventory(file, data, inventory, menuViewers > 0);
        status = inventory != null ? "scanned" : energyTarget != null ? "energy_ready" : "offline";
    }

    void scanInventory(ServerQuestFile file, TeamData data, InventoryCheck inventory) {
        scanInventory(file, data, inventory, true);
    }

    private void scanInventory(ServerQuestFile file, TeamData data, InventoryCheck inventory, boolean catalog) {
        List<CheckRow> result = catalog ? new ArrayList<>() : List.of();
        file.forAllChapters(chapter -> chapter.getQuests().forEach(
                quest -> scanQuest(quest, data, inventory, catalog, result)));
        rows = List.copyOf(result);
    }

    private void scanQuest(Quest quest, TeamData data, InventoryCheck inventory, boolean catalog,
                           List<CheckRow> result) {
        if (data.isLocked() || !data.canStartTasks(quest) || !data.areDependenciesComplete(quest)) return;
        // Capture the round before progress callbacks can reset a repeatable quest.
        Set<Task> incomplete = null;
        for (Task task : quest.getTasks()) {
            if (task instanceof ForgeEnergyTask energy) {
                if (energyTarget == null && energy.getMaxInput() > 0
                        && data.getProgress(energy) < energy.getMaxProgress() && ConsumableTask.canSubmit(data, energy)) {
                    energyTarget = new EnergyTarget(energy, FtbqQuestFileRevision.get(quest.getQuestFile()));
                }
                continue;
            }
            if (inventory == null || !InventoryCheck.supported(task) || (!catalog && task.consumesResources())
                    || data.isCompleted(task)) continue;
            if (incomplete == null) incomplete = new HashSet<>();
            incomplete.add(task);
        }
        if (!catalog && incomplete == null) return;
        boolean roundReset = false;
        for (Task task : quest.getTasks()) {
            if (task instanceof ForgeEnergyTask) {
                if (catalog) {
                    energyTaskCount++;
                    result.add(new CheckRow(task.id, Component.translatable("ftbquests.task.ftbquests.forge_energy"),
                            false, true, false, data.getProgress(task), task.getMaxProgress(), data.isCompleted(task)));
                }
                continue;
            }
            if (inventory == null || !InventoryCheck.supported(task)) continue;
            boolean wasCompleted = incomplete == null || !incomplete.contains(task);
            boolean canComplete = !wasCompleted && !roundReset && !task.consumesResources();
            long required = task.getMaxProgress();
            if (!catalog && (!canComplete || required <= 0 || !ConsumableTask.canSubmit(data, task))) continue;
            long amount = catalog ? inventory.available(task) : inventory.countUpTo(task, required);
            boolean completedNow = canComplete && InventoryCheck.complete(data, task, amount);
            if (completedNow) {
                completedLastScan++;
                roundReset = !data.isCompleted(task);
            }
            if (!catalog) continue;
            boolean fluid = task instanceof FluidTask;
            boolean chemical = task instanceof ChemicalTask;
            Component title;
            if (task instanceof ChemicalTask target) {
                chemicalTaskCount++;
                title = target.targetName();
            } else if (task instanceof FluidTask target) {
                fluidTaskCount++;
                title = Component.translatable(target.getFluid().getFluidType().getDescriptionId());
            } else {
                itemTaskCount++;
                title = ((dev.ftb.mods.ftbquests.quest.task.ItemTask) task).getItemStack().getHoverName();
            }
            result.add(new CheckRow(task.id, title, fluid, false, chemical, amount, required, wasCompleted || completedNow));
        }
    }

    void addMenuViewer() { menuViewers++; }
    void removeMenuViewer() { menuViewers--; }

    ClientSummary clientSummary() {
        return new ClientSummary(playerId, teamId, online, paused, status, itemTaskCount, fluidTaskCount,
                energyTaskCount, chemicalTaskCount, completedLastScan);
    }

    record ClientSummary(UUID player, UUID team, boolean online, boolean paused, String status,
                         int items, int fluids, int energy, int chemicals, int completed) {}

    private record EnergyTarget(ForgeEnergyTask task, long revision) {}

    @Override public IGridNode getGridNode(Direction side) { return mainNode.getNode(); }
    @Override public IGridNode getActionableNode() { return mainNode.getNode(); }
    @Override public AECableType getCableConnectionType(Direction side) { return AECableType.SMART; }
    public IManagedGridNode getMainNode() { return mainNode; }

    private void unloadNode() {
        if (level != null && !level.isClientSide()) mainNode.destroy();
    }
    @Override public void onChunkUnloaded() { unloadNode(); super.onChunkUnloaded(); }
    @Override public void setRemoved() { unloadNode(); super.setRemoved(); }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (level != null && level.isClientSide()) return;
        output.storeNullable("checkerPlayer", UUIDUtil.CODEC, playerId);
        output.storeNullable("checkerTeam", UUIDUtil.CODEC, teamId);
        output.putBoolean("checkerPaused", paused);
        mainNode.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        playerId = input.read("checkerPlayer", UUIDUtil.CODEC).orElse(null);
        teamId = input.read("checkerTeam", UUIDUtil.CODEC).orElse(null);
        paused = input.getBooleanOr("checkerPaused", false);
        energyTarget = null;
        if (level == null || !level.isClientSide()) mainNode.deserialize(input);
    }

    public int getPageCount() { return Math.max(1, (rows.size() + PAGE_SIZE - 1) / PAGE_SIZE); }

    public CompoundTag writeClientState(int requestedPage) {
        return writeClientState(requestedPage, PAGE_SIZE);
    }

    CompoundTag writeClientState(int requestedPage, int pageSize) {
        CompoundTag tag = writeClientSummary();
        int pages = Math.max(1, (rows.size() + pageSize - 1) / pageSize);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        tag.putInt("page", page);
        tag.putInt("pages", pages);
        tag.put("rows", writeCatalogRows(page * pageSize, Math.min(rows.size(), (page + 1) * pageSize)));
        return tag;
    }

    CompoundTag writeClientSummary() {
        CompoundTag tag = new CompoundTag();
        tag.putString("player", getPlayerIdText());
        tag.putString("team", getTeamIdText());
        tag.putBoolean("online", online);
        tag.putBoolean("paused", paused);
        tag.putString("status", status);
        tag.putInt("items", itemTaskCount);
        tag.putInt("fluids", fluidTaskCount);
        tag.putInt("energy", energyTaskCount);
        if (chemicalTaskCount > 0) tag.putInt("chemicals", chemicalTaskCount);
        tag.putInt("completed", completedLastScan);
        return tag;
    }

    ListTag writeCatalogRows(int start, int end) {
        ListTag entries = new ListTag();
        for (int i = start; i < end; i++) {
            entries.add(writeCatalogRow(rows.get(i)));
        }
        return entries;
    }

    CompoundTag writeCatalogRow(CheckRow row) {
        CompoundTag entry = new CompoundTag();
        entry.putLong("id", row.taskId());
        entry.store("title", ComponentSerialization.CODEC, level.registryAccess().createSerializationContext(NbtOps.INSTANCE), row.title());
        entry.putBoolean("fluid", row.fluid());
        if (row.energy()) entry.putBoolean("energy", true);
        if (row.chemical()) entry.putBoolean("chemical", true);
        entry.putLong("available", row.available());
        entry.putLong("required", row.required());
        entry.putBoolean("completed", row.completed());
        return entry;
    }

    public void readClientState(CompoundTag tag) {
        readClientSummary(tag);
        rows = List.copyOf(readCatalogRows(tag.getListOrEmpty("rows")));
    }

    void readClientSummary(CompoundTag tag) {
        playerId = parseUuid(tag.getStringOr("player", ""));
        teamId = parseUuid(tag.getStringOr("team", ""));
        online = tag.getBooleanOr("online", false);
        paused = tag.getBooleanOr("paused", false);
        status = tag.getStringOr("status", "");
        itemTaskCount = tag.getIntOr("items", 0);
        fluidTaskCount = tag.getIntOr("fluids", 0);
        energyTaskCount = tag.getIntOr("energy", 0);
        chemicalTaskCount = tag.getIntOr("chemicals", 0);
        completedLastScan = tag.getIntOr("completed", 0);
    }

    List<CheckRow> readCatalogRows(ListTag entries) {
        List<CheckRow> result = new ArrayList<>();
        for (int i = 0; i < Math.min(CATALOG_CHUNK_SIZE, entries.size()); i++) {
            result.add(readCatalogRow(entries.getCompoundOrEmpty(i)));
        }
        return result;
    }

    CheckRow readCatalogRow(CompoundTag entry) {
        Component title = entry.read("title", ComponentSerialization.CODEC, level.registryAccess().createSerializationContext(NbtOps.INSTANCE)).orElse(null);
        return new CheckRow(entry.getLongOr("id", 0L), title == null ? Component.empty() : title,
                entry.getBooleanOr("fluid", false), entry.getBooleanOr("energy", false), entry.getBooleanOr("chemical", false),
                entry.getLongOr("available", 0L), entry.getLongOr("required", 0L), entry.getBooleanOr("completed", false));
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
    public int getEnergyTaskCount() { return energyTaskCount; }
    public int getChemicalTaskCount() { return chemicalTaskCount; }
    public int getCompletedLastScan() { return completedLastScan; }
    public List<CheckRow> getRows() { return rows; }
    public Component getStatusComponent() { return Component.translatable("status.ftb_quest_nexus.checker." + status); }

    public record CheckRow(long taskId, Component title, boolean fluid, boolean energy, boolean chemical,
                           long available, long required, boolean completed) {
        boolean sameDefinition(CheckRow other) {
            return taskId == other.taskId && fluid == other.fluid && energy == other.energy && chemical == other.chemical
                    && required == other.required && title.equals(other.title);
        }
    }
}
