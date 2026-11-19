package com.dishanhai.ftb_quest_nexus.compat.ars;

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

/** FTB Quests Source objective; Source is neither fluid nor the player's spell mana. */
public final class ArsSourceTask extends Task implements ConsumableTask.ExactStorageTarget {
    public static final String LANG = "task.ftb_quest_nexus.ars_source";
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "ars_source");
    public static final TaskType TYPE = TaskTypes.register(ID, ArsSourceTask::new,
            () -> Icon.getIcon(ResourceLocation.withDefaultNamespace("textures/item/amethyst_shard.png")))
            .setDisplayName(Component.translatable(LANG));
    private static final long DEFAULT_AMOUNT = 1000;
    private long amount = DEFAULT_AMOUNT;
    private boolean consume;

    public ArsSourceTask(long id, Quest quest) { super(id, quest); }
    public static void init() {}
    @Override public TaskType getType() { return TYPE; }
    @Override public long getMaxProgress() { return amount; }
    @Override public boolean consumesResources() { return consume; }
    @Override public boolean checkOnLogin() { return false; }
    @Override public String formatMaxProgress() { return amount + " Source"; }
    @Override public String formatProgress(TeamData data, long progress) { return progress + " Source"; }
    @Override public Component getAltTitle() { return Component.translatable(LANG); }
    @Override public AEKey storageKey() {
        return ModList.get().isLoaded("ars_nouveau") ? ArsSourceKey.key() : null;
    }

    @Override public void addMouseOverText(TooltipList list, TeamData data) {
        list.add(Component.translatable(LANG + (consume ? ".consume_hint" : ".check_hint")));
    }

    @Override public void submitTask(TeamData data, ServerPlayer player, ItemStack changedStack) {
        if (!changedStack.isEmpty() || player.isSpectator() || player instanceof FakePlayer
                || !ConsumableTask.canSubmit(data, this)) return;
        if (!ModList.get().isLoaded("ars_nouveau")) {
            player.displayClientMessage(Component.translatable(LANG + ".missing"), true);
            return;
        }
        long required = consume ? amount - data.getProgress(this) : amount;
        long received = SourceJars.submit(player.getInventory(), required, consume);
        if (consume ? received > 0 : received >= required) {
            if (consume) data.addProgress(this, received);
            else data.setProgress(this, amount);
        } else {
            player.displayClientMessage(Component.translatable(LANG + ".not_enough"), true);
        }
    }

    @Override public void fillConfigGroup(ConfigGroup group) {
        super.fillConfigGroup(group);
        group.addLong("amount", amount, this::setAmount, DEFAULT_AMOUNT, 1, Long.MAX_VALUE)
                .setNameKey(LANG + ".amount");
        group.addBool("consume", consume, value -> consume = value, false)
                .setNameKey(LANG + ".consume");
    }

    private void setAmount(long value) { amount = Math.max(1, value); }
    @Override public void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeData(tag, registries);
        tag.putLong("amount", amount); tag.putBoolean("consume", consume);
    }
    @Override public void readData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readData(tag, registries);
        setAmount(tag.contains("amount") ? tag.getLong("amount") : DEFAULT_AMOUNT);
        consume = tag.getBoolean("consume");
    }
    @Override public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeVarLong(amount); buffer.writeBoolean(consume);
    }
    @Override public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        setAmount(buffer.readVarLong()); consume = buffer.readBoolean();
    }
}
