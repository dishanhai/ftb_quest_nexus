package com.dishanhai.ftb_quest_nexus.compat.pneumatic;

import appeng.api.stacks.AEKey;
import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ConsumableTask;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayer;

/** FTB quest objective for compressed air, in raw mL. Mod absence does not invalidate saved quests. */
public final class CompressedAirTask extends Task implements ConsumableTask.ExactStorageTarget {
    private static final String LANG = "task.ftb_quest_nexus.compressed_air";
    private static final long DEFAULT_AMOUNT = 1000;
    private static final long MAX_AMOUNT = Integer.MAX_VALUE;
    private static final TaskType TYPE = TaskTypes.register(
            ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "compressed_air"),
            CompressedAirTask::new,
            () -> Icon.getIcon(ResourceLocation.withDefaultNamespace("textures/item/glass_bottle.png")))
            .setDisplayName(Component.translatable(LANG));
    private long amount = DEFAULT_AMOUNT;
    private boolean consume;

    public CompressedAirTask(long id, Quest quest) { super(id, quest); }
    public static void init() {}
    @Override public TaskType getType() { return TYPE; }
    @Override public long getMaxProgress() { return amount; }
    @Override public boolean consumesResources() { return consume; }
    @Override public boolean checkOnLogin() { return false; }
    @Override public String formatMaxProgress() { return amount + " mL"; }
    @Override public String formatProgress(TeamData data, long progress) { return progress + " mL"; }

    private static boolean available() { return ModList.get().isLoaded("pneumaticcraft") && AirStorage.available(); }
    @Override public AEKey storageKey() {
        return available() && ModList.get().isLoaded("appliedpneumatics") ? AirStorage.key() : null;
    }

    @Override public void submitTask(TeamData data, ServerPlayer player, ItemStack changedStack) {
        if (!changedStack.isEmpty() || player.isSpectator() || player instanceof FakePlayer
                || !available() || !ConsumableTask.canSubmit(data, this)) return;
        long required = consume ? amount - data.getProgress(this) : amount;
        if (required <= 0) return;
        long moved = AirStorage.submit(player.getInventory(), required, consume);
        if (consume && moved > 0) data.addProgress(this, moved);
        else if (!consume && moved >= required) data.setProgress(this, amount);
    }

    @Override public void addMouseOverText(TooltipList list, TeamData data) {
        list.add(Component.translatable(LANG + (available() ? ".hint" : ".unavailable")));
    }
    private void setAmount(long value) { amount = Math.max(1, Math.min(MAX_AMOUNT, value)); }
    @Override public void fillConfigGroup(ConfigGroup group) {
        super.fillConfigGroup(group);
        group.addLong("amount", amount, this::setAmount, DEFAULT_AMOUNT, 1, MAX_AMOUNT).setNameKey(LANG + ".amount");
        group.addBool("consume", consume, value -> consume = value, false).setNameKey(LANG + ".consume");
    }
    @Override public void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeData(tag, registries);
        tag.putLong("amount", amount);
        tag.putBoolean("consume", consume);
    }
    @Override public void readData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readData(tag, registries);
        setAmount(tag.contains("amount") ? tag.getLong("amount") : DEFAULT_AMOUNT);
        consume = tag.getBoolean("consume");
    }
    @Override public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeVarLong(amount);
        buffer.writeBoolean(consume);
    }
    @Override public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        setAmount(buffer.readVarLong());
        consume = buffer.readBoolean();
    }
}
