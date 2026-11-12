package com.dishanhai.ftb_quest_nexus.gametest;

import appeng.api.stacks.AEItemKey;
import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.UUID;

/** Author: dishanhai. Exercises the 26.1.2 registry, block-entity codec and transfer boundaries. */
@EventBusSubscriber(modid = QuestNexus.MOD_ID)
public final class Nexus26GameTests {
    private static final BlockPos MACHINE = new BlockPos(1, 1, 1);

    private Nexus26GameTests() {}

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        if (!GameTestHooks.isGametestEnabled()) return;
        event.register(Registries.TEST_FUNCTION, registry -> {
            registry.register(id("submitter_save"), Nexus26GameTests::submitterSave);
            registry.register(id("submitter_drop_and_pick"), Nexus26GameTests::submitterDropAndPick);
            registry.register(id("checker_energy_and_recipes"), Nexus26GameTests::checkerEnergyAndRecipes);
        });
    }

    private static void submitterSave(GameTestHelper helper) {
        SubmitterBlockEntity entity = placeSubmitter(helper, MACHINE);
        CompoundTag user = userData(helper);
        var registries = helper.getLevel().registryAccess();
        entity.loadUserData(user, registries);
        CompoundTag saved = entity.saveWithoutMetadata(registries);

        SubmitterBlockEntity restored = placeSubmitter(helper, MACHINE.east());
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved));
        helper.assertValueEqual(restored.getTeamIdText(), user.getStringOr("teamIdText", ""), "team");
        helper.assertValueEqual(restored.getSubmitterIdText(), user.getStringOr("submitterIdText", ""), "identity");
        helper.assertValueEqual(restored.getQueueSize(), 2, "legacy queue entries");
        helper.assertTrue(restored.isSuppressCompletionToast(), "Completion-toast setting changed");

        CompoundTag restoredUser = new CompoundTag();
        restored.saveUserData(restoredUser, registries);
        CompoundTag pending = restoredUser.getListOrEmpty("FtbqPendingRewards").getCompoundOrEmpty(0);
        helper.assertValueEqual(pending.getLongOr("amount", 0), 37L, "pending reward amount");
        AEItemKey key = AEItemKey.fromTag(TagValueInput.create(ProblemReporter.DISCARDING, registries,
                pending.getCompoundOrEmpty("key")));
        helper.assertTrue(key != null && key.getReadOnlyStack().has(DataComponents.CUSTOM_NAME),
                "Reward key lost its item component");
        helper.succeed();
    }

    private static void submitterDropAndPick(GameTestHelper helper) {
        var level = helper.getLevel();
        SubmitterBlockEntity entity = placeSubmitter(helper, MACHINE);
        CompoundTag user = userData(helper);
        entity.loadUserData(user, level.registryAccess());
        var state = entity.getBlockState();
        var absolute = helper.absolutePos(MACHINE);
        var drops = Block.getDrops(state, level, absolute, entity);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(NexusContent.ITEM.get()), "Wrong survival drop");
        var drop = drops.getFirst();
        CompoundTag copied = drop.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        helper.assertValueEqual(copied.getCompoundOrEmpty("SubmitterData").getStringOr("submitterIdText", ""),
                entity.getSubmitterIdText(), "survival drop identity");
        helper.assertTrue(!drop.has(DataComponents.BLOCK_ENTITY_DATA), "Drop included block-entity data");

        ItemStack picked = state.getBlock().getCloneItemStack(level, absolute, state, true,
                helper.makeMockPlayer(GameType.CREATIVE));
        helper.assertTrue(picked.is(NexusContent.ITEM.get()), "Wrong creative pick item");
        helper.assertTrue(!picked.has(DataComponents.CUSTOM_DATA) && !picked.has(DataComponents.BLOCK_ENTITY_DATA),
                "Creative pick copied user data");
        helper.succeed();
    }

    private static void checkerEnergyAndRecipes(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(MACHINE, NexusContent.CHECKER.get());
        for (Direction side : Direction.values()) {
            var energy = level.getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(MACHINE), side);
            helper.assertTrue(energy != null, "Missing energy capability on " + side);
            try (var transaction = Transaction.openRoot()) {
                helper.assertValueEqual(energy.insert(1000, transaction), 0, "unbound checker input");
            }
        }
        for (String name : new String[]{"ftbq_ae_checker", "ftbq_ae_submitter"}) {
            helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, id(name))).isPresent(),
                    "Missing recipe " + name);
        }
        helper.succeed();
    }

    private static SubmitterBlockEntity placeSubmitter(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, NexusContent.SUBMITTER.get());
        return helper.getBlockEntity(pos, SubmitterBlockEntity.class);
    }

    private static CompoundTag userData(GameTestHelper helper) {
        CompoundTag user = new CompoundTag();
        user.putString("teamIdText", UUID.randomUUID().toString());
        user.putString("rewardPlayerIdText", UUID.randomUUID().toString());
        user.putString("submitterIdText", UUID.randomUUID().toString());
        user.putString("queuedTaskIdsText", "ffffffffffffffff,1,1,bad!,0");
        user.putBoolean("suppressCompletionToast", true);

        ItemStack stack = new ItemStack(Items.DIAMOND);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Nexus test"));
        var key = AEItemKey.of(stack);
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
        key.toTag(output);
        CompoundTag pending = new CompoundTag();
        pending.put("key", output.buildResult());
        pending.putLong("amount", 37);
        ListTag rewards = new ListTag();
        rewards.add(pending);
        user.put("FtbqPendingRewards", rewards);
        return user;
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(QuestNexus.MOD_ID, name);
    }
}
