package com.dishanhai.ftb_quest_nexus.submitter;

import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.network.SubmitterStatePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/** Author: dishanhai. Actions use vanilla's container-id checked button packet. */
public final class SubmitterMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE = 6;
    public final SubmitterBlockEntity submitter;
    private final Player viewer;
    private int page;
    private int pageCount = 1;
    private List<QueueRow> rows = List.of();
    private CompoundTag lastState;
    private List<Long> queuedTaskIds = List.of();
    private long queueRevision;
    private Set<Long> searchMatches;
    private int matchedCount;
    private boolean filtering;
    public SubmitterMenu(int id, Inventory inventory, FriendlyByteBuf data) {
        this(id, inventory, findSubmitter(inventory, data));
    }
    private static SubmitterBlockEntity findSubmitter(Inventory inventory, FriendlyByteBuf data) {
        var entity = inventory.player.level().getBlockEntity(data.readBlockPos());
        return entity instanceof SubmitterBlockEntity submitter ? submitter : null;
    }
    public SubmitterMenu(int id, Inventory inventory, SubmitterBlockEntity submitter) {
        super(NexusContent.MENU.get(), id);
        this.submitter = submitter;
        viewer = inventory.player;
    }
    @Override public boolean stillValid(Player player) {
        return submitter != null && !submitter.isRemoved() && player.level().getBlockEntity(submitter.getBlockPos()) == submitter
                && player.distanceToSqr(submitter.getBlockPos().getX() + .5, submitter.getBlockPos().getY() + .5, submitter.getBlockPos().getZ() + .5) <= 64;
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer serverPlayer) || button < 0 || button > 6 || !stillValid(player)) return false;
        if (button == 4 || button == 5) {
            page += button == 4 ? -1 : 1;
        } else if (!submitter.applyAction(serverPlayer, button)) return false;
        else if (button == 6) page = 0;
        broadcastChanges();
        return true;
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (!(viewer instanceof ServerPlayer serverPlayer) || !stillValid(viewer)) return;
        List<Long> currentIds = submitter.getQueuedTaskIds();
        if (!currentIds.equals(queuedTaskIds)) {
            queuedTaskIds = currentIds;
            queueRevision++;
        }
        CompoundTag state = submitter.writeQueueState(page, searchMatches);
        state.putLongArray("queueIds", queuedTaskIds.stream().mapToLong(Long::longValue).toArray());
        state.putLong("queueRevision", queueRevision);
        page = state.getInt("page");
        pageCount = state.getInt("pages");
        matchedCount = state.getInt("matched");
        filtering = state.getBoolean("filtering");
        if (!state.equals(lastState)) {
            lastState = state;
            PacketDistributor.sendToPlayer(serverPlayer,
                    new SubmitterStatePacket(containerId, submitter.getBlockPos(), state));
        }
    }
    public void readState(CompoundTag state) {
        submitter.readClientState(state);
        queuedTaskIds = Arrays.stream(state.getLongArray("queueIds")).boxed().toList();
        queueRevision = state.getLong("queueRevision");
        matchedCount = state.getInt("matched");
        filtering = state.getBoolean("filtering");
        page = state.getInt("page");
        pageCount = Math.max(1, state.getInt("pages"));
        List<QueueRow> result = new ArrayList<>();
        var entries = state.getList("rows", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(PAGE_SIZE, entries.size()); i++) {
            CompoundTag entry = entries.getCompound(i);
            Component title = Component.Serializer.fromJson(entry.getString("title"), viewer.registryAccess());
            result.add(new QueueRow(entry.getLong("id"), title == null ? Component.empty() : title,
                    entry.getBoolean("valid"), entry.getBoolean("fluid"),
                    entry.getLong("progress"), entry.getLong("required"),
                    entry.getBoolean("completed")));
        }
        rows = List.copyOf(result);
    }
    public int getPage() { return page; }
    public List<Long> getQueuedTaskIds() { return queuedTaskIds; }
    public long getQueueRevision() { return queueRevision; }
    public int getMatchedCount() { return matchedCount; }
    public boolean isFiltering() { return filtering; }

    /** Author: dishanhai. Mask positions refer only to the acknowledged queue snapshot. */
    public boolean applySearch(ServerPlayer player, long revision, boolean filtering, long[] mask) {
        if (player != viewer || !stillValid(player)) return false;
        broadcastChanges();
        if (revision != queueRevision) return false;
        if (filtering) {
            BitSet selected = BitSet.valueOf(mask);
            searchMatches = new HashSet<>();
            for (int i = selected.nextSetBit(0); i >= 0 && i < queuedTaskIds.size();
                 i = selected.nextSetBit(i + 1)) {
                searchMatches.add(queuedTaskIds.get(i));
            }
        } else searchMatches = null;
        page = 0;
        broadcastChanges();
        return true;
    }
    public int getPageCount() { return pageCount; }
    public List<QueueRow> getRows() { return rows; }
    public record QueueRow(long taskId, Component title, boolean valid, boolean fluid, long progress, long required,
                           boolean completed) {}
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
