package com.dishanhai.ftb_quest_nexus.checker;

import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.network.CheckerStatePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.network.PacketDistributor;

/** Author: dishanhai. Bounded catalog chunks, published atomically for each open viewer. */
public final class CheckerMenu extends AbstractContainerMenu {
    public final CheckerBlockEntity checker;
    private final Player viewer;
    private int page;
    private int pageCount = 1;
    private CompoundTag lastState;
    private List<CheckerBlockEntity.CheckRow> lastRows;
    private long snapshotRevision;
    private long pendingRevision;
    private int nextChunk;
    private final List<CheckerBlockEntity.CheckRow> pendingRows = new ArrayList<>();
    private List<CheckerBlockEntity.CheckRow> catalogRows = List.of();
    private long catalogRevision;

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
        if (button == 2 || button == 3) {
            page = Math.max(0, Math.min(page + (button == 2 ? -1 : 1), checker.getPageCount() - 1));
        } else if (!checker.applyAction(serverPlayer, button)) return false;
        broadcastChanges();
        return true;
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (!(viewer instanceof ServerPlayer serverPlayer) || !stillValid(viewer)) return;
        CompoundTag state = checker.writeClientState(0);
        if (state.equals(lastState) && checker.getRows().equals(lastRows)) return;
        lastState = state;
        lastRows = checker.getRows();
        snapshotRevision++;
        int chunks = Math.max(1, (lastRows.size() + CheckerBlockEntity.CATALOG_CHUNK_SIZE - 1)
                / CheckerBlockEntity.CATALOG_CHUNK_SIZE);
        for (int chunk = 0; chunk < chunks; chunk++) {
            CompoundTag part = checker.writeClientState(chunk, CheckerBlockEntity.CATALOG_CHUNK_SIZE);
            part.putBoolean("catalog", true);
            part.putLong("revision", snapshotRevision);
            part.putInt("chunk", chunk);
            part.putInt("chunks", chunks);
            PacketDistributor.sendToPlayer(serverPlayer,
                    new CheckerStatePacket(containerId, checker.getBlockPos(), part));
        }
    }
    public void readState(CompoundTag state) {
        if (state.getBoolean("catalog")) {
            int chunk = state.getInt("chunk");
            int chunks = state.getInt("chunks");
            long revision = state.getLong("revision");
            if (chunk == 0) {
                pendingRows.clear();
                pendingRevision = revision;
                nextChunk = 0;
            }
            if (chunks <= 0 || chunk < 0 || chunk >= chunks
                    || revision != pendingRevision || chunk != nextChunk) return;
            checker.readClientState(state);
            pendingRows.addAll(checker.getRows());
            nextChunk++;
            if (nextChunk == chunks) {
                catalogRows = List.copyOf(pendingRows);
                catalogRevision++;
                pendingRows.clear();
            }
            return;
        }
        checker.readClientState(state);
        page = state.getInt("page");
        pageCount = Math.max(1, state.getInt("pages"));
    }
    public int getPage() { return page; }
    public int getPageCount() { return pageCount; }
    public List<CheckerBlockEntity.CheckRow> getCatalogRows() { return catalogRows; }
    public long getCatalogRevision() { return catalogRevision; }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}



