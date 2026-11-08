package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import appeng.api.stacks.AEKey;
import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ConsumableTask;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
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

/** Author: dishanhai. FTBQ task for a single registered Mekanism chemical, measured in mB. */
public final class ChemicalTask extends Task implements ConsumableTask.ExactStorageTarget {
    public static final String LANG = "task.ftb_quest_nexus.mek_chemical";
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "mek_chemical");
    // FTB Quests resolves saved and synchronized tasks by this type even without chemical storage.
    public static final TaskType TYPE = TaskTypes.register(ID, ChemicalTask::new, () -> Icon.getIcon(
            ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "textures/gui/mek_chemical.png")))
            .setDisplayName(Component.translatable(LANG));
    private static final boolean MEKANISM_AVAILABLE = ModList.get().isLoaded("mekanism");
    private static final boolean STORAGE_AVAILABLE = MEKANISM_AVAILABLE && ModList.get().isLoaded("appmek");
    private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath("mekanism", "empty");
    private static final long DEFAULT_AMOUNT = 1000;

    private ResourceLocation chemicalId = EMPTY;
    private long amount = DEFAULT_AMOUNT;
    private boolean consume;

    public static void init() {}

    public static boolean isAvailable() {
        return STORAGE_AVAILABLE;
    }

    public static boolean isMekanismAvailable() {
        return MEKANISM_AVAILABLE;
    }

    public ChemicalTask(long id, Quest quest) {
        super(id, quest);
    }

    @Override public TaskType getType() { return TYPE; }
    @Override public long getMaxProgress() { return amount; }
    @Override public boolean consumesResources() { return consume; }

    public ResourceLocation chemicalId() { return chemicalId; }
    public void setChemicalId(ResourceLocation id) { chemicalId = id; }
    public void setAmount(long amount) { this.amount = Math.max(1, amount); }

    @Override
    public AEKey storageKey() {
        return isAvailable() ? ChemicalStorage.key(chemicalId) : null;
    }

    @Override
    public void submitTask(TeamData data, ServerPlayer player, ItemStack changedStack) {
        if (!changedStack.isEmpty() || !isMekanismAvailable() || !ConsumableTask.canSubmit(data, this)) return;
        long required = consume ? amount - data.getProgress(this) : amount;
        if (required <= 0) return;
        long submitted = ChemicalStorage.submitFromInventory(player.getInventory(), chemicalId, required, consume);
        if (consume) {
            if (submitted > 0) data.addProgress(this, submitted);
        } else if (submitted >= required) {
            data.setProgress(this, amount);
        }
    }

    public Component targetName() {
        return nameOf(chemicalId);
    }

    public static Component nameOf(ResourceLocation id) {
        return isMekanismAvailable() ? ChemicalStorage.name(id) : missingName(id);
    }

    static Component missingName(ResourceLocation id) {
        return Component.translatable(LANG + ".missing", id.toString());
    }

    static Icon iconOf(ResourceLocation id) {
        return isMekanismAvailable() ? ChemicalStorage.icon(id) : TYPE.getIconSupplier();
    }

    @Override public Component getAltTitle() { return targetName(); }
    @Override public Icon getAltIcon() { return iconOf(chemicalId); }
    @Override public String formatMaxProgress() { return FluidTask.getVolumeString(amount); }
    @Override public String formatProgress(TeamData data, long progress) { return FluidTask.getVolumeString(progress); }

    @Override
    public void fillConfigGroup(ConfigGroup group) {
        super.fillConfigGroup(group);
        if (isMekanismAvailable()) ChemicalPicker.addChemicalConfig(group, this);
        group.addLong("amount", amount, this::setAmount, DEFAULT_AMOUNT, 1, Long.MAX_VALUE)
                .setNameKey(LANG + ".amount");
        group.addBool("consume", consume, value -> consume = value, false).setNameKey(LANG + ".consume");
    }

    @Override
    public void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeData(tag, registries);
        tag.putString("chemical", chemicalId.toString());
        tag.putLong("amount", amount);
        tag.putBoolean("consume", consume);
    }

    @Override
    public void readData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readData(tag, registries);
        String storedId = tag.getString("chemical");
        ResourceLocation id = storedId.isEmpty() ? null : ResourceLocation.tryParse(storedId);
        chemicalId = id == null ? EMPTY : id;
        setAmount(tag.contains("amount") ? tag.getLong("amount") : DEFAULT_AMOUNT);
        consume = tag.getBoolean("consume");
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeResourceLocation(chemicalId);
        buffer.writeVarLong(amount);
        buffer.writeBoolean(consume);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        chemicalId = buffer.readResourceLocation();
        setAmount(buffer.readVarLong());
        consume = buffer.readBoolean();
    }
}
