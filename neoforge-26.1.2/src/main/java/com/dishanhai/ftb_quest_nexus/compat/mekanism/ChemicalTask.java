package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import appeng.api.stacks.AEKey;
import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.api.ftbq.ConsumableTask;
import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.minecraft.core.HolderLookup;
import de.marhali.json5.Json5Object;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Author: dishanhai. FTBQ task for a single registered Mekanism chemical, measured in mB. */
public final class ChemicalTask extends Task implements ConsumableTask.ExactStorageTarget {
    public static final String LANG = "task.ftb_quest_nexus.mek_chemical";
    public static final Identifier ID = Identifier.fromNamespaceAndPath(QuestNexus.MOD_ID, "mek_chemical");
    // FTB Quests resolves saved and synchronized tasks by this type even without chemical storage.
    public static final TaskType TYPE = TaskTypes.register(ID, ChemicalTask::new, () -> Icon.getIcon(
            Identifier.fromNamespaceAndPath(QuestNexus.MOD_ID, "textures/gui/mek_chemical.png")));
    private static final boolean MEKANISM_AVAILABLE = ModList.get().isLoaded("mekanism");
    private static final boolean STORAGE_AVAILABLE = MEKANISM_AVAILABLE && ModList.get().isLoaded("appmek");
    private static final Identifier EMPTY = Identifier.fromNamespaceAndPath("mekanism", "empty");
    private static final long DEFAULT_AMOUNT = 1000;

    private Identifier chemicalId = EMPTY;
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

    public Identifier chemicalId() { return chemicalId; }
    public void setChemicalId(Identifier id) { chemicalId = id; }
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

    public static Component nameOf(Identifier id) {
        return isMekanismAvailable() ? ChemicalStorage.name(id) : missingName(id);
    }

    static Component missingName(Identifier id) {
        return Component.translatable(LANG + ".missing", id.toString());
    }

    static Icon iconOf(Identifier id) {
        return isMekanismAvailable() ? ChemicalStorage.icon(id) : TYPE.getIcon();
    }

    @Override public Component getAltTitle() { return targetName(); }
    @Override public Icon getAltIcon() { return iconOf(chemicalId); }
    @Override public String formatMaxProgress() { return FluidTask.getVolumeString(amount); }
    @Override public String formatProgress(TeamData data, long progress) { return FluidTask.getVolumeString(progress); }

    @Override
    public void fillConfigGroup(EditableConfigGroup group) {
        super.fillConfigGroup(group);
        if (isMekanismAvailable()) ChemicalPicker.addChemicalConfig(group, this);
        group.addLong("amount", amount, this::setAmount, DEFAULT_AMOUNT, 1, Long.MAX_VALUE)
                .setNameKey(LANG + ".amount");
        group.addBool("consume", consume, value -> consume = value, false).setNameKey(LANG + ".consume");
    }

    @Override
    public void writeData(Json5Object tag, HolderLookup.Provider registries) {
        super.writeData(tag, registries);
        tag.addProperty("chemical", chemicalId.toString());
        tag.addProperty("amount", amount);
        tag.addProperty("consume", consume);
    }

    @Override
    public void readData(Json5Object tag, HolderLookup.Provider registries) {
        super.readData(tag, registries);
        String storedId = tag.has("chemical") ? tag.get("chemical").getAsString() : "";
        Identifier id = storedId.isEmpty() ? null : Identifier.tryParse(storedId);
        chemicalId = id == null ? EMPTY : id;
        setAmount(tag.has("amount") ? tag.get("amount").getAsLong() : DEFAULT_AMOUNT);
        consume = tag.has("consume") && tag.get("consume").getAsBoolean();
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeIdentifier(chemicalId);
        buffer.writeVarLong(amount);
        buffer.writeBoolean(consume);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        chemicalId = buffer.readIdentifier();
        setAmount(buffer.readVarLong());
        consume = buffer.readBoolean();
    }
}
