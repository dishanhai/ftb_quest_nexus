package com.dishanhai.ftb_quest_nexus.checker;

import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.network.CheckerStatePacket;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import com.dishanhai.ftb_quest_nexus.network.NexusNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import net.minecraftforge.network.PacketDistributor;

/** Author: dishanhai. Bounded catalog and stock updates, published atomically for each viewer. */
public final class CheckerMenu extends AbstractContainerMenu {
    public static final int RESYNC_BUTTON = 4;
    private static final int CHUNK_SIZE = CheckerBlockEntity.CATALOG_CHUNK_SIZE;
    private static final int STOCK = 0;
    private static final int REMOVE = 1;
    private static final int INSERT = 2;
    private static final int REPLACE = 3;
    public final CheckerBlockEntity checker;
    private final Player viewer;
    private int page;
    private int pageCount = 1;
    private boolean watching;
    private CheckerBlockEntity.ClientSummary lastSummary;
    private List<CheckerBlockEntity.CheckRow> lastRows;
    private long snapshotRevision;
    private boolean fullRefreshRequested;
    private long nextResyncTick;
    private long receivedRevision;
    private long pendingRevision;
    private int pendingChunks;
    private int nextChunk;
    private boolean pendingCatalog;
    private boolean needsResync;
    private final List<CheckerBlockEntity.CheckRow> pendingRows = new ArrayList<>();
    private final BitSet pendingRemovals = new BitSet();
    private final List<CompoundTag> pendingChanges = new ArrayList<>();
    private int pendingInsertions;
    private boolean pendingDefinitionChanges;
    private List<CheckerBlockEntity.CheckRow> catalogRows = List.of();
    private List<CheckerBlockEntity.CheckRow> catalogView = List.of();
    private long catalogRevision;
    private long stockRevision;

    public CheckerMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, findChecker(inventory, buffer));
    }
    private static CheckerBlockEntity findChecker(Inventory inventory, FriendlyByteBuf buffer) {
        var entity = inventory.player.level().getBlockEntity(buffer.readBlockPos());
        return entity instanceof CheckerBlockEntity checker ? checker : null;
    }
    public CheckerMenu(int id, Inventory inventory, CheckerBlockEntity checker) {
        super(NexusContent.CHECKER_MENU.get(), id);
        this.checker = checker;
        viewer = inventory.player;
    }
    @Override public boolean stillValid(Player player) {
        return checker != null && !checker.isRemoved() && player.level() == checker.getLevel()
                && player.level().getBlockEntity(checker.getBlockPos()) == checker
                && checker.getBlockPos().closerToCenterThan(player.position(), 8);
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player)) return false;
        if (button == RESYNC_BUTTON) {
            if (player != viewer || viewer.containerMenu != this) return false;
            fullRefreshRequested = true;
            return true;
        }
        if (button == 2 || button == 3) {
            page = Math.max(0, Math.min(page + (button == 2 ? -1 : 1), checker.getPageCount() - 1));
        } else if (!checker.applyAction(serverPlayer, button)) return false;
        broadcastChanges();
        return true;
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (!(viewer instanceof ServerPlayer serverPlayer) || !stillValid(viewer)) return;
        if (!watching && viewer.containerMenu == this) {
            checker.addMenuViewer();
            watching = true;
        }
        long now = viewer.level().getGameTime();
        boolean forceFull = fullRefreshRequested && now >= nextResyncTick;
        if (forceFull) {
            fullRefreshRequested = false;
            nextResyncTick = now + 20;
        }
        for (CompoundTag part : writeStateChanges(forceFull)) {
            NexusNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new CheckerStatePacket(containerId, checker.getBlockPos(), part));
        }
    }

    List<CompoundTag> writeStateChanges(boolean forceFull) {
        var summary = checker.clientSummary();
        var rows = checker.getRows();
        boolean full = forceFull || lastRows == null || rows.isEmpty() != lastRows.isEmpty();
        ListTag changed = full || rows == lastRows ? null : diffRows(rows);
        if (changed != null && changed.size() >= rows.size() && !rows.isEmpty()) {
            int definitions = 0;
            for (int i = 0; i < changed.size(); i++) {
                int op = changed.getCompound(i).getByte("op");
                if (op == INSERT || op == REPLACE) definitions++;
            }
            // A delta with no reusable definitions carries more metadata than a full catalog.
            full = definitions == rows.size();
        }
        lastRows = rows;
        if (!full && (changed == null || changed.isEmpty()) && summary.equals(lastSummary)) return List.of();
        lastSummary = summary;
        long base = snapshotRevision++;
        int count = full ? rows.size() : changed == null ? 0 : changed.size();
        int chunks = count == 0 ? 1 : (count - 1) / CHUNK_SIZE + 1;
        List<CompoundTag> packets = new ArrayList<>(chunks);
        for (int chunk = 0; chunk < chunks; chunk++) {
            CompoundTag part = chunk + 1 == chunks ? checker.writeClientSummary() : new CompoundTag();
            part.putBoolean(full ? "catalog" : "delta", true);
            part.putLong("revision", snapshotRevision);
            part.putInt("chunk", chunk);
            part.putInt("chunks", chunks);
            int start = chunk * CHUNK_SIZE;
            int end = start + Math.min(CHUNK_SIZE, count - start);
            if (full) {
                part.put("rows", checker.writeCatalogRows(start, end));
            } else {
                part.putLong("base", base);
                ListTag updates = new ListTag();
                for (int i = start; i < end; i++) {
                    updates.add(changed.getCompound(i));
                }
                part.put("rows", updates);
            }
            packets.add(part);
        }
        return packets;
    }

    private ListTag diffRows(List<CheckerBlockEntity.CheckRow> rows) {
        if (rows.size() != lastRows.size()) return diffOrder(rows);
        ListTag changes = new ListTag();
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            var previous = lastRows.get(i);
            if (row.taskId() != previous.taskId()) return diffOrder(rows);
            addUpdate(changes, i, row, previous);
        }
        return changes;
    }

    private ListTag diffOrder(List<CheckerBlockEntity.CheckRow> rows) {
        var oldIndices = new Long2IntOpenHashMap(lastRows.size());
        oldIndices.defaultReturnValue(-1);
        for (int i = 0; i < lastRows.size(); i++) oldIndices.put(lastRows.get(i).taskId(), i);

        // Keep the longest ordered subsequence of existing IDs. Moves become removals and insertions.
        int[] tailValues = new int[rows.size()];
        int[] tailIndices = new int[rows.size()];
        int[] links = new int[rows.size()];
        int length = 0;
        for (int i = 0; i < rows.size(); i++) {
            int old = oldIndices.get(rows.get(i).taskId());
            if (old < 0) continue;
            int found = Arrays.binarySearch(tailValues, 0, length, old);
            if (found >= 0) continue;
            int insertion = ~found;
            links[i] = insertion == 0 ? -1 : tailIndices[insertion - 1];
            tailValues[insertion] = old;
            tailIndices[insertion] = i;
            if (insertion == length) length++;
        }
        BitSet keptOld = new BitSet(lastRows.size());
        BitSet keptNew = new BitSet(rows.size());
        for (int i = length == 0 ? -1 : tailIndices[length - 1]; i >= 0; i = links[i]) {
            keptOld.set(oldIndices.get(rows.get(i).taskId()));
            keptNew.set(i);
        }

        ListTag changes = new ListTag();
        for (int i = keptOld.nextClearBit(0); i < lastRows.size(); i = keptOld.nextClearBit(i + 1)) {
            addChange(changes, REMOVE, i, new CompoundTag());
        }
        for (int i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            if (keptNew.get(i)) {
                addUpdate(changes, i, row, lastRows.get(oldIndices.get(row.taskId())));
            } else {
                addChange(changes, INSERT, i, checker.writeCatalogRow(row));
            }
        }
        return changes;
    }

    private void addUpdate(ListTag changes, int index, CheckerBlockEntity.CheckRow row,
                           CheckerBlockEntity.CheckRow previous) {
        if (!row.sameDefinition(previous)) {
            addChange(changes, REPLACE, index, checker.writeCatalogRow(row));
        } else if (row.available() != previous.available() || row.completed() != previous.completed()) {
            CompoundTag values = new CompoundTag();
            values.putLong("available", row.available());
            values.putBoolean("completed", row.completed());
            addChange(changes, STOCK, index, values);
        }
    }

    private static void addChange(ListTag changes, int op, int index, CompoundTag entry) {
        if (op != STOCK) entry.putByte("op", (byte) op);
        entry.putInt("index", index);
        changes.add(entry);
    }

    @Override public void removed(Player player) {
        if (watching) {
            checker.removeMenuViewer();
            watching = false;
        }
        super.removed(player);
    }

    public void readState(CompoundTag state) {
        boolean full = state.getBoolean("catalog");
        if (!full && !state.getBoolean("delta")) return;
        long revision = state.getLong("revision");
        if (revision <= receivedRevision || revision < pendingRevision) return;
        int chunk = state.getInt("chunk");
        int chunks = state.getInt("chunks");
        var entries = state.getList("rows", Tag.TAG_COMPOUND);
        if (chunks <= 0 || chunk < 0 || chunk >= chunks || entries.size() > CHUNK_SIZE
                || (chunk + 1 < chunks && entries.size() != CHUNK_SIZE)
                || (chunks > 1 && entries.isEmpty())
                || (!full && (receivedRevision == 0 || state.getLong("base") != receivedRevision))) {
            requestResync();
            return;
        }
        if (chunk == 0 && revision != pendingRevision) {
            clearPending();
            pendingRevision = revision;
            pendingChunks = chunks;
            pendingCatalog = full;
        }
        if (revision != pendingRevision || chunk != nextChunk || chunks != pendingChunks || full != pendingCatalog) {
            requestResync();
            return;
        }
        if (full) {
            pendingRows.addAll(checker.readCatalogRows(entries));
        } else if (!appendChanges(entries)) {
            requestResync();
            return;
        }
        if (++nextChunk != chunks) return;
        if (full) {
            publishCatalog(new ArrayList<>(pendingRows));
            needsResync = false;
        } else if (!applyChanges()) {
            requestResync();
            return;
        }
        checker.readClientSummary(state);
        receivedRevision = revision;
        clearPending();
    }

    private boolean appendChanges(ListTag entries) {
        int previous = pendingChanges.isEmpty() ? -1 : pendingChanges.getLast().getInt("index");
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            int index = entry.getInt("index");
            int op = entry.getByte("op");
            if (op < STOCK || op > REPLACE) return false;
            if (op == REMOVE) {
                if (previous != -1 || index < pendingRemovals.length() || index >= catalogRows.size()) return false;
                pendingRemovals.set(index);
            } else {
                if (index <= previous) return false;
                pendingChanges.add(entry);
                if (op == INSERT) pendingInsertions++;
                previous = index;
            }
            if (op != STOCK) pendingDefinitionChanges = true;
        }
        return true;
    }

    private boolean applyChanges() {
        int size = catalogRows.size() - pendingRemovals.cardinality() + pendingInsertions;
        if (!pendingChanges.isEmpty() && pendingChanges.getLast().getInt("index") >= size) return false;
        if (pendingDefinitionChanges) return applyCatalogChanges(size);
        if (!pendingChanges.isEmpty()) {
            for (CompoundTag entry : pendingChanges) {
                int index = entry.getInt("index");
                catalogRows.set(index, withStock(catalogRows.get(index), entry));
            }
            stockRevision++;
        }
        return true;
    }

    private boolean applyCatalogChanges(int size) {
        List<CheckerBlockEntity.CheckRow> result = new ArrayList<>(size);
        int old = 0;
        int edit = 0;
        for (int index = 0; index < size; index++) {
            CompoundTag change = edit < pendingChanges.size() ? pendingChanges.get(edit) : null;
            if (change != null && change.getInt("index") == index) edit++;
            else change = null;
            if (change != null && change.getByte("op") == INSERT) {
                result.add(checker.readCatalogRow(change));
                continue;
            }
            old = pendingRemovals.nextClearBit(old);
            if (old >= catalogRows.size()) return false;
            var row = catalogRows.get(old++);
            if (change != null) {
                if (change.getByte("op") == REPLACE) {
                    if (change.getLong("id") != row.taskId()) return false;
                    row = checker.readCatalogRow(change);
                } else {
                    row = withStock(row, change);
                }
            }
            result.add(row);
        }
        publishCatalog(result);
        return true;
    }

    private void publishCatalog(List<CheckerBlockEntity.CheckRow> rows) {
        catalogRows = rows;
        catalogView = Collections.unmodifiableList(catalogRows);
        catalogRevision++;
        pageCount = Math.max(1, (rows.size() + CheckerBlockEntity.PAGE_SIZE - 1) / CheckerBlockEntity.PAGE_SIZE);
        page = Math.min(page, pageCount - 1);
    }

    private static CheckerBlockEntity.CheckRow withStock(CheckerBlockEntity.CheckRow row, CompoundTag entry) {
        return new CheckerBlockEntity.CheckRow(row.taskId(), row.title(), row.fluid(), entry.getLong("available"),
                row.required(), entry.getBoolean("completed"));
    }

    private void clearPending() {
        pendingRevision = 0;
        nextChunk = 0;
        pendingRows.clear();
        pendingRemovals.clear();
        pendingChanges.clear();
        pendingInsertions = 0;
        pendingDefinitionChanges = false;
    }

    private void requestResync() {
        clearPending();
        needsResync = true;
    }

    public boolean takeResyncRequest() {
        boolean requested = needsResync;
        needsResync = false;
        return requested;
    }
    public int getPage() { return page; }
    public int getPageCount() { return pageCount; }
    public List<CheckerBlockEntity.CheckRow> getCatalogRows() { return catalogView; }
    public long getCatalogRevision() { return catalogRevision; }
    public long getStockRevision() { return stockRevision; }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}

