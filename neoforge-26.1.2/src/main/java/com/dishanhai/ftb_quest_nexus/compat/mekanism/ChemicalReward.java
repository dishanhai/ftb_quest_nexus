package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.api.ftbq.SubmitterQuestIndex;
import dev.ftb.mods.ftblibrary.client.config.EditableConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import net.minecraft.core.HolderLookup;
import de.marhali.json5.Json5Object;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Author: dishanhai. A direct FTB Quests reward configured as a chemical ID and an amount in mB. */
public final class ChemicalReward extends Reward implements SubmitterQuestIndex.NetworkReward {
    public static final String LANG = "reward.ftb_quest_nexus.mek_chemical";
    public static final RewardType TYPE = RewardTypes.register(
            Identifier.fromNamespaceAndPath(QuestNexus.MOD_ID, "mek_chemical"), ChemicalReward::new,
            ChemicalTask.TYPE::getIcon, true);
    private static final long DEFAULT_AMOUNT = 1000;
    private static final Identifier EMPTY = Identifier.fromNamespaceAndPath("mekanism", "empty");

    private Identifier chemical = EMPTY;
    private long amount = DEFAULT_AMOUNT;

    public static void init() {}

    public ChemicalReward(long id, Quest quest) {
        super(id, quest);
    }

    void setChemical(Identifier chemical) { this.chemical = chemical; }
    Identifier chemical() { return chemical; }
    long amount() { return amount; }

    @Override public RewardType getType() { return TYPE; }
    @Override public Icon getAltIcon() { return ChemicalTask.iconOf(chemical); }
    @Override public Component getAltTitle() {
        return Component.translatable(LANG + ".description", ChemicalTask.nameOf(chemical), amount);
    }

    @Override
    public boolean claim(ServerPlayer player, boolean notify) {
        ChemicalRewards.receive(player, chemical, amount);
        return true;
    }

    @Override
    public void fillConfigGroup(EditableConfigGroup group) {
        super.fillConfigGroup(group);
        if (ChemicalTask.isMekanismAvailable()) ChemicalPicker.addChemicalConfig(group, chemical, this::setChemical);
        group.addLong("amount", amount, value -> amount = Math.max(1, value), DEFAULT_AMOUNT, 1, Long.MAX_VALUE)
                .setNameKey(LANG + ".amount");
    }

    @Override
    public void writeData(Json5Object tag, HolderLookup.Provider registries) {
        super.writeData(tag, registries);
        tag.addProperty("chemical", chemical.toString());
        tag.addProperty("amount", amount);
    }

    @Override
    public void readData(Json5Object tag, HolderLookup.Provider registries) {
        super.readData(tag, registries);
        String storedId = tag.has("chemical") ? tag.get("chemical").getAsString() : "";
        Identifier id = storedId.isEmpty() ? null : Identifier.tryParse(storedId);
        chemical = id == null ? EMPTY : id;
        amount = tag.has("amount") ? Math.max(1, tag.get("amount").getAsLong()) : DEFAULT_AMOUNT;
    }

    @Override
    public void writeNetData(RegistryFriendlyByteBuf buffer) {
        super.writeNetData(buffer);
        buffer.writeIdentifier(chemical);
        buffer.writeVarLong(amount);
    }

    @Override
    public void readNetData(RegistryFriendlyByteBuf buffer) {
        super.readNetData(buffer);
        chemical = buffer.readIdentifier();
        amount = Math.max(1, buffer.readVarLong());
    }
}
