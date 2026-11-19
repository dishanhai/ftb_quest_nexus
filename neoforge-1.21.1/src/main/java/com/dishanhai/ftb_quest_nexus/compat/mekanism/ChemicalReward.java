package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterQuestIndex;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Author: dishanhai. A direct FTB Quests reward configured as a chemical ID and an amount in mB. */
public final class ChemicalReward extends Reward implements SubmitterQuestIndex.NetworkReward {
    public static final String LANG = "reward.ftb_quest_nexus.mek_chemical";
    public static final RewardType TYPE = RewardTypes.register(
            ResourceLocation.fromNamespaceAndPath(QuestNexus.MOD_ID, "mek_chemical"), ChemicalReward::new,
            ChemicalTask.TYPE::getIconSupplier, true).setDisplayName(Component.translatable(LANG));
    private static final long DEFAULT_AMOUNT = 1000;
    private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath("mekanism", "empty");

    private ResourceLocation chemical = EMPTY;
    private long amount = DEFAULT_AMOUNT;

    public static void init() {}

    public ChemicalReward(long id, Quest quest) {
        super(id, quest);
    }

    void setChemical(ResourceLocation chemical) { this.chemical = chemical; }
    ResourceLocation chemical() { return chemical; }
    long amount() { return amount; }

    @Override public RewardType getType() { return TYPE; }
    @Override public Icon getAltIcon() { return ChemicalTask.iconOf(chemical); }
    @Override public Component getAltTitle() {
        return Component.translatable(LANG + ".description", ChemicalTask.nameOf(chemical), amount);
    }

    @Override
    public void claim(ServerPlayer player, boolean notify) {
        ChemicalRewards.receive(player, chemical, amount);
    }

    @Override
    public void fillConfigGroup(ConfigGroup group) {
        super.fillConfigGroup(group);
        if (ChemicalTask.isMekanismAvailable()) ChemicalPicker.addChemicalConfig(group, chemical, this::setChemical);
        group.addLong("amount", amount, value -> amount = Math.max(1, value), DEFAULT_AMOUNT, 1, Long.MAX_VALUE)
                .setNameKey(LANG + ".amount");
    }

    @Override
    public void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeData(tag, registries);
        tag.putString("chemical", chemical.toString());
        tag.putLong("amount", amount);
    }

    @Override
    public void readData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readData(tag, registries);
        String storedId = tag.getString("chemical");
        ResourceLocation id = storedId.isEmpty() ? null : ResourceLocation.tryParse(storedId);
        chemical = id == null ? EMPTY : id;
        amount = tag.contains("amount") ? Math.max(1, tag.getLong("amount")) : DEFAULT_AMOUNT;
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeResourceLocation(chemical);
        buffer.writeVarLong(amount);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        chemical = buffer.readResourceLocation();
        amount = Math.max(1, buffer.readVarLong());
    }
}
