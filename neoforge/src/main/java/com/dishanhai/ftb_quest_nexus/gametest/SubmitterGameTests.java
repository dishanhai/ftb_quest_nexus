package com.dishanhai.ftb_quest_nexus.gametest;

// Author: dishanhai. Platform regression checks use actual 1.21.1 components and AE keys.
import appeng.api.stacks.AEItemKey;
import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(QuestNexus.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SubmitterGameTests {
    private SubmitterGameTests() {}

    @GameTest(template = "empty")
    public static void bindingQueueAndRewardComponentsSurviveSave(GameTestHelper helper) {
        SubmitterBlockEntity entity = create(helper);
        CompoundTag user = userWithReward(helper);
        user.putString("queuedTaskIdsText", "ffffffffffffffff,1,1,bad!,0");
        user.putBoolean("suppressCompletionToast", true);
        user.putBoolean("rewardClaimModeInitialized", true);
        user.putBoolean("claimOnlyQueuedRewards", false);
        entity.loadUserData(user, helper.getLevel().registryAccess());
        CompoundTag saved = entity.saveWithoutMetadata(helper.getLevel().registryAccess());
        SubmitterBlockEntity restored = create(helper);
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(user.getString("teamIdText").equals(restored.getTeamIdText()), "Team binding changed");
        helper.assertTrue(user.getString("rewardPlayerIdText").equals(restored.getRewardPlayerIdText()), "Reward player changed");
        helper.assertTrue(user.getString("submitterIdText").equals(restored.getSubmitterIdText()), "UUID changed");
        helper.assertTrue(restored.getQueueSize() == 2, "Invalid or duplicate task IDs survived");
        helper.assertTrue(restored.isSuppressCompletionToast() && !restored.isClaimOnlyQueuedRewards(), "Settings changed");
        CompoundTag after = new CompoundTag();
        restored.saveUserData(after, helper.getLevel().registryAccess());
        helper.assertTrue(after.getList("FtbqPendingRewards", 10).getCompound(0).getLong("amount") == 37,
                "Pending reward amount changed");
        AEItemKey restoredKey = AEItemKey.fromTag(helper.getLevel().registryAccess(),
                after.getList("FtbqPendingRewards", 10).getCompound(0).getCompound("key"));
        helper.assertTrue(restoredKey != null && restoredKey.getReadOnlyStack().has(DataComponents.CUSTOM_NAME),
                "AE reward key lost its data components");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void newSubmitterDefaultsToQueueRewards(GameTestHelper helper) {
        SubmitterBlockEntity entity = create(helper);
        entity.loadUserData(new CompoundTag(), helper.getLevel().registryAccess());
        helper.assertTrue(entity.isClaimOnlyQueuedRewards() && entity.getQueueSize() == 0, "Wrong default settings");
        helper.assertTrue(UUID.fromString(entity.getSubmitterIdText()) != null, "UUID missing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nativeLootCopiesOnlyUserDataComponents(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, NexusContent.SUBMITTER.get());
        SubmitterBlockEntity entity = (SubmitterBlockEntity) helper.getBlockEntity(pos);
        CompoundTag user = userWithReward(helper);
        entity.loadUserData(user, helper.getLevel().registryAccess());
        var drops = Block.getDrops(entity.getBlockState(), helper.getLevel(), helper.absolutePos(pos), entity);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(NexusContent.ITEM.get()), "Wrong loot");
        ItemStack drop = drops.getFirst();
        CompoundTag copied = drop.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        helper.assertTrue(copied.contains("SubmitterData"), "Drop lost user data component");
        helper.assertTrue(user.getString("submitterIdText").equals(copied.getCompound("SubmitterData").getString("submitterIdText")),
                "Drop lost submitter identity");
        helper.assertTrue(copied.getCompound("SubmitterData").getList("FtbqPendingRewards", 10).getCompound(0).getLong("amount") == 37,
                "Drop lost undelivered rewards");
        helper.assertTrue(!copied.contains("aeNode") && !drop.has(DataComponents.BLOCK_ENTITY_DATA), "Drop copied AE identity");
        helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        BlockPos destination = new BlockPos(3, 1, 1);
        BlockPos absolute = helper.absolutePos(destination);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        var result = ((BlockItem) drop.getItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, drop, hit));
        helper.assertTrue(result.consumesAction(), "Native BlockItem placement was refused");
        SubmitterBlockEntity restored = (SubmitterBlockEntity) helper.getBlockEntity(destination);
        helper.assertTrue(entity.getSubmitterIdText().equals(restored.getSubmitterIdText()), "Placement lost identity");
        helper.assertTrue(entity.getTeamIdText().equals(restored.getTeamIdText()), "Placement lost binding");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void ordinaryCreativePickReturnsFreshSubmitter(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, NexusContent.SUBMITTER.get());
        var state = helper.getBlockState(pos);
        var stack = state.getBlock().getCloneItemStack(helper.getLevel(), helper.absolutePos(pos), state);
        helper.assertTrue(stack.is(NexusContent.ITEM.get()), "Wrong creative pick item");
        helper.assertTrue(!stack.has(DataComponents.CUSTOM_DATA) && !stack.has(DataComponents.BLOCK_ENTITY_DATA),
                "Creative pick copied user components");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeServerSaveRemovesExistingUserComponents(GameTestHelper helper) {
        SubmitterBlockEntity entity = create(helper);
        CompoundTag user = userWithReward(helper);
        entity.loadUserData(user, helper.getLevel().registryAccess());
        CompoundTag existing = new CompoundTag(); existing.put("SubmitterData", user.copy());
        ItemStack stack = new ItemStack(NexusContent.ITEM.get());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(existing));
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(existing));
        entity.saveToItem(stack, helper.getLevel().registryAccess());
        helper.assertTrue(!stack.has(DataComponents.CUSTOM_DATA) && !stack.has(DataComponents.BLOCK_ENTITY_DATA),
                "Creative copy retained UUID or reward components");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void everyFaceExposesOneNodeAndRemovalDestroysIt(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, NexusContent.SUBMITTER.get());
        helper.runAfterDelay(20, () -> {
            SubmitterBlockEntity entity = (SubmitterBlockEntity) helper.getBlockEntity(pos);
            helper.assertTrue(entity.getMainNode().getNode() != null, "Node missing");
            for (Direction direction : Direction.values()) helper.assertTrue(entity.getGridNode(direction) == entity.getMainNode().getNode(),
                    "Face has wrong AE node");
            helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
            helper.assertTrue(entity.getMainNode().getNode() == null, "Removed node remained alive");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void identityCollisionIsResolvedAfterComponentsAreApplied(GameTestHelper helper) {
        BlockPos firstPos = new BlockPos(1, 1, 1), secondPos = new BlockPos(3, 1, 1);
        helper.setBlock(firstPos, NexusContent.SUBMITTER.get());
        helper.setBlock(secondPos, NexusContent.SUBMITTER.get());
        SubmitterBlockEntity first = (SubmitterBlockEntity) helper.getBlockEntity(firstPos);
        SubmitterBlockEntity second = (SubmitterBlockEntity) helper.getBlockEntity(secondPos);
        CompoundTag user = userWithReward(helper);
        first.loadUserData(user.copy(), helper.getLevel().registryAccess());
        second.loadUserData(user.copy(), helper.getLevel().registryAccess());
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(!first.getSubmitterIdText().equals(second.getSubmitterIdText()), "Restored identity collision remained");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void restoredPlayerOwnsCreatedNode(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, NexusContent.SUBMITTER.get());
        SubmitterBlockEntity entity = (SubmitterBlockEntity) helper.getBlockEntity(pos);
        CompoundTag user = new CompoundTag();
        UUID player = UUID.randomUUID(); user.putString("rewardPlayerIdText", player.toString());
        entity.loadUserData(user, helper.getLevel().registryAccess());
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(entity.getMainNode().getNode() != null, "Restored node missing");
            helper.assertTrue(player.equals(entity.getMainNode().getNode().getOwningPlayerProfileId()), "Node creation reset owner");
            helper.succeed();
        });
    }

    private static SubmitterBlockEntity create(GameTestHelper helper) {
        SubmitterBlockEntity entity = NexusContent.BLOCK_ENTITY.get().create(BlockPos.ZERO, NexusContent.SUBMITTER.get().defaultBlockState());
        entity.setLevel(helper.getLevel());
        return entity;
    }

    private static CompoundTag userWithReward(GameTestHelper helper) {
        CompoundTag user = new CompoundTag();
        user.putString("teamIdText", UUID.randomUUID().toString());
        user.putString("rewardPlayerIdText", UUID.randomUUID().toString());
        user.putString("submitterIdText", UUID.randomUUID().toString());
        CompoundTag reward = new CompoundTag();
        ItemStack named = new ItemStack(Items.IRON_INGOT);
        named.set(DataComponents.CUSTOM_NAME, Component.translatable("block.ftb_quest_nexus.ftbq_ae_submitter"));
        reward.put("key", AEItemKey.of(named).toTag(helper.getLevel().registryAccess()));
        reward.putLong("amount", 37L);
        ListTag pending = new ListTag(); pending.add(reward); user.put("FtbqPendingRewards", pending);
        return user;
    }
}
