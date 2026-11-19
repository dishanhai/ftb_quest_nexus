package com.dishanhai.ftb_quest_nexus.compat.ars;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(QuestNexus.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArsSourceGameTests {
    @GameTest(template = "empty")
    public static void carriesRealSourceWithoutCountingManaOrDroppingJars(GameTestHelper helper) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "SourceJarTest"));
        var inventory = player.getInventory();
        if (!ModList.get().isLoaded("ars_nouveau")) {
            helper.assertTrue(SourceJars.submit(inventory, 99, true) == 0 && ArsSourceKey.key() == null,
                    "Invisible optional dependency exposed Source");
            helper.succeed();
            return;
        }
        var stack = jar(2, 1000);
        var title = Component.literal("Keeps custom components");
        stack.set(DataComponents.CUSTOM_NAME, title);
        inventory.items.set(0, stack);
        inventory.offhand.set(0, jar(1, 100));
        var creative = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ars_nouveau:creative_source_jar")));
        inventory.items.set(1, creative);
        helper.assertTrue(SourceJars.submit(inventory, 4000, false) == 2100
                && SourceJars.submit(inventory, 4000, false) == 2100, "Read-only checks drained or overstated Source");
        helper.assertTrue(SourceJars.submit(inventory, 1501, true) == 1501, "A split changed the debited amount");
        helper.assertTrue(SourceJars.submit(inventory, 4000, false) == 599
                && inventory.items.get(0).getCount() + inventory.items.get(2).getCount() == 2
                && title.equals(inventory.items.get(0).get(DataComponents.CUSTOM_NAME))
                && title.equals(inventory.items.get(2).get(DataComponents.CUSTOM_NAME)),
                "Split jars lost contents, count or unrelated components");
        for (int i = 0; i < inventory.items.size(); i++) inventory.items.set(i, new ItemStack(Items.COBBLESTONE, 64));
        inventory.items.set(0, jar(2, 1000));
        inventory.offhand.set(0, ItemStack.EMPTY);
        helper.assertTrue(SourceJars.submit(inventory, 1, true) == 0 && SourceJars.submit(inventory, 4000, false) == 2000,
                "Full inventory corrupted a stack that required splitting");
        helper.assertTrue(SourceJars.submit(inventory, 1001, true) == 1000 && SourceJars.submit(inventory, 4000, false) == 1000,
                "Full inventory could not debit safely equal per-jar amounts");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void persistsSourceTaskAndResolvesOnlyExactAeKey(GameTestHelper helper) {
        var file = new ServerQuestFile(helper.getLevel().getServer());
        var quest = new Quest(file.newID(), new Chapter(file.newID(), file, file.getDefaultChapterGroup()));
        var task = new ArsSourceTask(file.newID(), quest);
        var registries = helper.getLevel().registryAccess();
        var tag = new CompoundTag();
        tag.putBoolean("consume", true);
        tag.putLong("amount", 12345);
        task.readData(tag, registries);
        var clone = new ArsSourceTask(file.newID(), quest);
        var saved = new CompoundTag();
        task.writeData(saved, registries);
        clone.readData(saved, registries);
        var packet = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            task.writeNetData(packet);
            var synced = new ArsSourceTask(file.newID(), quest);
            synced.readNetData(packet);
            helper.assertTrue(clone.consumesResources() && synced.consumesResources()
                    && clone.getMaxProgress() == 12345 && synced.getMaxProgress() == 12345,
                    "Task write/read lost Source quantity or mode");
        } finally {
            packet.release();
        }
        var key = task.storageKey();
        helper.assertTrue(ModList.get().isLoaded("arseng")
                        ? key != null && key.getId().equals(ResourceLocation.parse("ars_nouveau:source"))
                        : key == null,
                "Task did not resolve the exact optional Source key");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static ItemStack jar(int count, int contents) {
        var item = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ars_nouveau:source_jar")), count);
        var type = (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(
                ResourceLocation.parse("ars_nouveau:block_fill_contents"));
        try {
            Object value = Class.forName("com.hollingsworth.arsnouveau.common.items.data.BlockFillContents")
                    .getConstructor(int.class).newInstance(contents);
            item.set(type, value);
            return item;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot build exact Ars Nouveau Source fixture", e);
        }
    }

    private ArsSourceGameTests() {}
}
