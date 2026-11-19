package com.dishanhai.ftb_quest_nexus.compat.create;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftblibrary.icon.Icon;
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
import dev.ftb.mods.ftblibrary.util.TooltipList;

/** Non-consumable task, completed by one live reading, not accumulated samples. */
public final class CreateStressTask extends Task {
    static final String LANG = "task.ftb_quest_nexus.create_stress";
    private static final long DEFAULT_AMOUNT = 256;
    private static final int MAX_AMOUNT = Integer.MAX_VALUE;
    public static final TaskType TYPE = TaskTypes.register(
            ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID,"create_stress"), CreateStressTask::new,
            () -> Icon.getIcon(ResourceLocation.withDefaultNamespace("textures/item/compass_16.png")))
            .setDisplayName(Component.translatable(LANG));

    private static final NameMap<StressReading.Metric> METRICS = NameMap
            .of(StressReading.Metric.CAPACITY, StressReading.Metric.values())
            .nameKey(value -> LANG + "." + value.name().toLowerCase(java.util.Locale.ROOT))
            .create();
    private StressReading.Metric metric = StressReading.Metric.CAPACITY;
    private long amount = DEFAULT_AMOUNT;

    public CreateStressTask(long id, Quest quest) { super(id, quest); }
    public static void init() {}
    @Override public TaskType getType() { return TYPE; }
    @Override public long getMaxProgress() { return amount; }
    @Override public String formatMaxProgress() { return amount + " SU"; }
    @Override public String formatProgress(TeamData data, long progress) { return progress + " SU"; }
    @Override public Component getAltTitle() {
        return Component.translatable(LANG + ".target", Component.translatable(LANG + "." + metric.name().toLowerCase(java.util.Locale.ROOT)),
                formatMaxProgress());
    }

    @Override public boolean checkOnLogin() { return false; }
    @Override public void submitTask(TeamData data, ServerPlayer player, ItemStack changedStack) {
        if (changedStack.isEmpty()) CreateStressCheck.submit(player, data, this);
    }
    @Override public void addMouseOverText(TooltipList list, TeamData data) {
        list.add(Component.translatable(LANG + ".hint", Long.toUnsignedString(id, 16)));
    }

    StressReading.Metric metric() { return metric; }
    private void setAmount(long value) { amount = Math.max(1, Math.min(MAX_AMOUNT, value)); }
    private void setMetric(String value) {
        try { metric = StressReading.Metric.valueOf(value.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException invalid) { metric = StressReading.Metric.CAPACITY; }
    }

    @Override public void fillConfigGroup(ConfigGroup group) {
        super.fillConfigGroup(group);
        group.addLong("amount", amount, this::setAmount, DEFAULT_AMOUNT, 1, MAX_AMOUNT)
                .setNameKey(LANG + ".amount");
        group.addEnum("metric", metric, value -> metric = value, METRICS)
                .setNameKey(LANG + ".metric");
    }

    @Override public void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeData(tag, registries);
        tag.putString("metric", metric.name()); tag.putLong("amount", amount);
    }
    @Override public void readData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readData(tag, registries);
        setMetric(tag.getString("metric"));
        setAmount(tag.contains("amount") ? tag.getLong("amount") : DEFAULT_AMOUNT);
    }
    @Override public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeEnum(metric); buffer.writeVarLong(amount);
    }
    @Override public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        metric = buffer.readEnum(StressReading.Metric.class); setAmount(buffer.readVarLong());
    }
}
