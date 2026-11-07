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
import com.dishanhai.ftb_quest_nexus.api.BindingTransfer;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ConsumableTask;
import com.dishanhai.ftb_quest_nexus.api.ftbq.FtbqQuestFileRevision;
import com.dishanhai.ftb_quest_nexus.network.CheckerFluxPackets.Option;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.forge.ForgeEnergyTask;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    private int menuViewers;
    private EnergyTarget energyTarget;
    private int fluxNetworkId = -1;
    private Runnable unloadFluxConnection;
    private long lastFluxSelectionTick = Long.MIN_VALUE;
    private final IEnergyStorage energyInput = new IEnergyStorage() {
        @Override public int receiveEnergy(int amount, boolean simulate) { return acceptEnergy(amount, simulate); }
        @Override public int extractEnergy(int amount, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return 0; }
        @Override public int getMaxEnergyStored() { return acceptEnergy(Integer.MAX_VALUE, true); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };
    private LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyInput);

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
        if (playerId == null) bindPlayer(player);
    }

    private void bindPlayer(ServerPlayer player) {
        TeamData data = playerData(player);
        if (!player.getUUID().equals(playerId) || data == null || !data.getTeamId().equals(teamId)) {
            unloadFluxDevice();
            setFluxNetworkId(-1);
        }
        playerId = player.getUUID();
        teamId = data == null ? null : data.getTeamId();
        mainNode.setOwningPlayer(player);
        energyTarget = null;
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
        if (action == 0) {
            if (playerId != null && !playerId.equals(player.getUUID())) {
                return BindingTransfer.request(this, player, () -> playerId, this::acceptBindingTransfer);
            }
            bindPlayer(player);
        } else if (action == 1) {
            paused = !paused;
            energyTarget = null;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, CheckerBlockEntity entity) {
        if (level.isClientSide || (level.getGameTime() + pos.asLong()) % 20 != 0) return;
        if (entity.fluxNetworkId >= 0 && entity.unloadFluxConnection == null
                && entity.isBoundTeamCurrent() && ModList.get().isLoaded("fluxnetworks")) {
            entity.unloadFluxConnection = CheckerFluxDevice.restore(entity, entity.fluxNetworkId);
        }
        entity.online = entity.mainNode.isOnline();
        if (state.getValue(CheckerBlock.ONLINE) != entity.online) {
            level.setBlock(pos, state.setValue(CheckerBlock.ONLINE, entity.online), 2);
        }
        entity.scan();
    }

    UUID boundPlayerId() { return playerId; }

    public boolean canManageFlux(ServerPlayer player) {
        return ModList.get().isLoaded("fluxnetworks") && player.getUUID().equals(playerId)
                && isBoundTeamCurrent();
    }

    public int fluxNetworkId() { return fluxNetworkId; }

    void setFluxNetworkId(int id) {
        if (fluxNetworkId == id) return;
        fluxNetworkId = id;
        setChanged();
    }

    public List<Option> fluxOptions(ServerPlayer player, int maxOptions) {
        return canManageFlux(player) ? CheckerFluxDevice.options(player, maxOptions) : List.of();
    }

    public boolean selectFluxNetwork(ServerPlayer player, int id, String password) {
        if (!canManageFlux(player) || !CheckerFluxDevice.canSelect(this, player, id, password)) return false;
        if (id == fluxNetworkId) return true;
        long tick = level.getGameTime();
        if (tick == lastFluxSelectionTick) return false;
        lastFluxSelectionTick = tick;
        unloadFluxDevice();
        setFluxNetworkId(-1);
        if (id < 0) return true;
        unloadFluxConnection = CheckerFluxDevice.restore(this, id);
        return unloadFluxConnection != null;
    }

    private void unloadFluxDevice() {
        if (unloadFluxConnection == null) return;
        unloadFluxConnection.run();
        unloadFluxConnection = null;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return capability == ForgeCapabilities.ENERGY ? energyCapability.cast() : super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energyCapability = LazyOptional.of(() -> energyInput);
    }

    private boolean isBoundTeamCurrent() {
        return playerId != null && teamId != null && FTBTeamsAPI.api().getManager().getTeamForPlayerID(playerId)
                .map(team -> teamId.equals(team.getTeamId())).orElse(false);
    }

    private int acceptEnergy(int offered, boolean simulate) {
        EnergyTarget target = energyTarget;
        if (offered <= 0 || target == null || paused || isRemoved()
                || !(level instanceof ServerLevel server) || !server.getServer().isSameThread()
                || !isBoundTeamCurrent()) return 0;
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        ForgeEnergyTask task = target.task();
        if (file == null || file != task.getQuestFile() || target.revision() != FtbqQuestFileRevision.get(file)
                || file.getTask(task.id) != task) return 0;
        TeamData data = file.getNullableTeamData(teamId);
        if (data == null || !ConsumableTask.canSubmit(data, task)) return 0;
        long remaining = task.getMaxProgress() - data.getProgress(task);
        int accepted = (int) Math.max(0, Math.min(offered, Math.min(remaining, task.getMaxInput())));
        if (accepted > 0 && !simulate) {
            // Progress callbacks can reset a repeatable quest immediately.
            if (accepted == remaining) energyTarget = null;
            data.addProgress(task, accepted);
        }
        return accepted;
    }

    private record EnergyTarget(ForgeEnergyTask task, long revision) {}

    private void scan() {
        completedLastScan = 0;
        rows = List.of();
        itemTaskCount = 0;
        fluidTaskCount = 0;
        energyTarget = null;
        if (paused) { status = "paused"; return; }
        ServerQuestFile file = ServerQuestFile.INSTANCE;
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
                if (catalog) result.add(new CheckRow(task.id, Component.translatable("ftbquests.task.ftbquests.forge_energy"),
                        false, true, data.getProgress(task), task.getMaxProgress(), data.isCompleted(task)));
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
            if (fluid) fluidTaskCount++; else itemTaskCount++;
            // FTBQ title APIs are client-only. Send a common-side fallback with the task ID.
            Component title = task instanceof dev.ftb.mods.ftbquests.quest.task.ItemTask item
                    ? item.getItemStack().getHoverName()
                    : Component.translatable(((FluidTask) task).getFluid().getFluidType().getDescriptionId());
            result.add(new CheckRow(task.id, title, fluid, false, amount, required, wasCompleted || completedNow));
        }
    }

    void addMenuViewer() { menuViewers++; }
    void removeMenuViewer() { menuViewers--; }

    ClientSummary clientSummary() {
        return new ClientSummary(playerId, teamId, online, paused, status, itemTaskCount, fluidTaskCount,
                completedLastScan);
    }

    record ClientSummary(UUID player, UUID team, boolean online, boolean paused, String status,
                         int items, int fluids, int completed) {}

    @Override public IGridNode getGridNode(Direction side) { return mainNode.getNode(); }
    @Override public IGridNode getActionableNode() { return mainNode.getNode(); }
    @Override public AECableType getCableConnectionType(Direction side) { return AECableType.SMART; }
    public IManagedGridNode getMainNode() { return mainNode; }

    private void unloadNode() {
        unloadFluxDevice();
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
        if (fluxNetworkId >= 0) tag.putInt("checkerFluxNetwork", fluxNetworkId);
        mainNode.saveToNBT(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        unloadFluxDevice();
        fluxNetworkId = tag.contains("checkerFluxNetwork", Tag.TAG_INT) ? tag.getInt("checkerFluxNetwork") : -1;
        energyTarget = null;
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
        entry.putString("title", Component.Serializer.toJson(row.title()));
        entry.putBoolean("fluid", row.fluid());
        entry.putBoolean("energy", row.energy());
        entry.putLong("available", row.available());
        entry.putLong("required", row.required());
        entry.putBoolean("completed", row.completed());
        return entry;
    }

    public void readClientState(CompoundTag tag) {
        readClientSummary(tag);
        rows = List.copyOf(readCatalogRows(tag.getList("rows", Tag.TAG_COMPOUND)));
    }

    void readClientSummary(CompoundTag tag) {
        playerId = parseUuid(tag.getString("player"));
        teamId = parseUuid(tag.getString("team"));
        online = tag.getBoolean("online");
        paused = tag.getBoolean("paused");
        status = tag.getString("status");
        itemTaskCount = tag.getInt("items");
        fluidTaskCount = tag.getInt("fluids");
        completedLastScan = tag.getInt("completed");
    }

    List<CheckRow> readCatalogRows(ListTag entries) {
        List<CheckRow> result = new ArrayList<>();
        for (int i = 0; i < Math.min(CATALOG_CHUNK_SIZE, entries.size()); i++) {
            result.add(readCatalogRow(entries.getCompound(i)));
        }
        return result;
    }

    CheckRow readCatalogRow(CompoundTag entry) {
        Component title = Component.Serializer.fromJson(entry.getString("title"));
        return new CheckRow(entry.getLong("id"), title == null ? Component.empty() : title,
                entry.getBoolean("fluid"), entry.getBoolean("energy"), entry.getLong("available"), entry.getLong("required"),
                entry.getBoolean("completed"));
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

    public record CheckRow(long taskId, Component title, boolean fluid, boolean energy,
                           long available, long required, boolean completed) {
        boolean sameDefinition(CheckRow other) {
            return taskId == other.taskId && fluid == other.fluid && energy == other.energy && required == other.required
                    && title.equals(other.title);
        }
    }
}

