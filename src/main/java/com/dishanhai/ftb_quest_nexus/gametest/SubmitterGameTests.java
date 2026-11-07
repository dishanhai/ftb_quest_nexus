package com.dishanhai.ftb_quest_nexus.gametest;

import appeng.api.networking.GridHelper;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import com.mojang.authlib.GameProfile;
import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

/** Author: dishanhai. Real registry/NBT/node checks in a dedicated test server. */
@GameTestHolder(QuestNexus.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SubmitterGameTests {
    private SubmitterGameTests() {}

    @GameTest(template = "empty")
    public static void boundQueueAndRewardOutboxSurviveSave(GameTestHelper helper) {
        SubmitterBlockEntity entity = create();
        CompoundTag user = new CompoundTag();
        String team = UUID.randomUUID().toString();
        String player = UUID.randomUUID().toString();
        String id = UUID.randomUUID().toString();
        user.putString("teamIdText", team);
        user.putString("rewardPlayerIdText", player);
        user.putString("submitterIdText", id);
        user.putString("queuedTaskIdsText", "ffffffffffffffff,1,1,bad!,0");
        user.putBoolean("suppressCompletionToast", true);
        user.putBoolean("claimOnlyQueuedRewards", false);
        user.putBoolean("rewardClaimModeInitialized", true);
        CompoundTag entry = new CompoundTag();
        entry.put("key", AEItemKey.of(Items.IRON_INGOT).toTag());
        entry.putLong("amount", 123L);
        ListTag pending = new ListTag();
        pending.add(entry);
        user.put("FtbqPendingRewards", pending);
        entity.loadUserData(user);
        CompoundTag saved = entity.saveWithoutMetadata();
        CompoundTag savedUser = saved.getCompound("SubmitterData");
        helper.assertTrue(!savedUser.contains("aeNode"), "Item data must not retain AE node identity");
        helper.assertTrue(savedUser.getList("FtbqPendingRewards", 10).getCompound(0).getLong("amount") == 123L,
                "Uninserted reward amount changed while saving");
        SubmitterBlockEntity restored = create();
        restored.load(saved);
        helper.assertTrue(team.equals(restored.getTeamIdText()), "Team binding did not round trip");
        helper.assertTrue(player.equals(restored.getRewardPlayerIdText()), "Reward player did not round trip");
        helper.assertTrue(id.equals(restored.getSubmitterIdText()), "Submitter UUID did not round trip");
        helper.assertTrue(restored.getQueueSize() == 2, "Invalid/duplicate task IDs survived loading");
        helper.assertTrue(restored.isSuppressCompletionToast(), "Toast setting did not round trip");
        helper.assertTrue(!restored.isClaimOnlyQueuedRewards(), "Reward mode did not round trip");
        CompoundTag after = new CompoundTag();
        restored.saveUserData(after);
        helper.assertTrue(after.getList("FtbqPendingRewards", 10).getCompound(0).getLong("amount") == 123L,
                "Pending rewards disappeared after loading");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void freshSubmitterDefaultsToQueueRewards(GameTestHelper helper) {
        SubmitterBlockEntity entity = create();
        entity.loadUserData(new CompoundTag());
        helper.assertTrue(entity.isClaimOnlyQueuedRewards(), "New submitter must default to queued rewards");
        helper.assertTrue(entity.getQueueSize() == 0, "New submitter queue must be empty");
        helper.assertTrue(UUID.fromString(entity.getSubmitterIdText()) != null, "Submitter UUID missing");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void placedSubmitterCreatesAndDestroysAeNode(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, NexusContent.SUBMITTER.get());
        helper.runAfterDelay(20, () -> {
            SubmitterBlockEntity entity = (SubmitterBlockEntity) helper.getBlockEntity(pos);
            helper.assertTrue(entity.getMainNode().getNode() != null, "Placed submitter has no AE node");
            for (Direction direction : Direction.values()) {
                helper.assertTrue(entity.getGridNode(direction) == entity.getMainNode().getNode(),
                        "Every block face must expose the same AE node");
            }
            helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
            helper.assertTrue(entity.getMainNode().getNode() == null, "Removed submitter retained AE node");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void rightClickWithAeCablePlacesItAgainstSubmitter(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, NexusContent.SUBMITTER.get());
        BlockPos absolute = helper.absolutePos(pos);
        var level = helper.getLevel();
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "NexusCableTest"));
        var cableItem = AEParts.SMART_DENSE_CABLE.item(AEColor.TRANSPARENT);
        var cable = new ItemStack(cableItem);
        player.setItemInHand(InteractionHand.MAIN_HAND, cable);
        Direction side = Direction.EAST;
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0.5, 0, 0), side, absolute, false);
        player.gameMode.useItemOn(player, level, cable, InteractionHand.MAIN_HAND, hit);
        var host = PartHelper.getPartHost(level, absolute.relative(side));
        helper.assertTrue(host != null && host.getPart(null) != null
                && host.getPart(null).getPartItem() == cableItem, "Submitter consumed the cable placement click");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void existingSubmitterConnectsToAeCablesOnEveryFace(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, NexusContent.SUBMITTER.get());
        helper.setBlock(pos.relative(Direction.EAST, 2), AEBlocks.CREATIVE_ENERGY_CELL.block());
        var cables = List.of(AEParts.GLASS_CABLE, AEParts.COVERED_CABLE, AEParts.SMART_CABLE,
                AEParts.COVERED_DENSE_CABLE, AEParts.SMART_DENSE_CABLE, AEParts.SMART_CABLE);
        var level = helper.getLevel();
        BlockPos absolute = helper.absolutePos(pos);
        helper.runAfterDelay(20, () -> {
            for (Direction direction : Direction.values()) {
                var host = PartHelper.placePartHost(null, level, absolute.relative(direction));
                helper.assertTrue(host != null && host.addPart(cables.get(direction.ordinal())
                        .item(AEColor.TRANSPARENT), null, null) != null, "Could not place AE cable on " + direction);
            }
        });
        helper.succeedWhen(() -> {
            var entity = (SubmitterBlockEntity) helper.getBlockEntity(pos);
            helper.assertTrue(GridHelper.getNodeHost(level, absolute) == entity, "AE cannot discover submitter host");
            var node = entity.getMainNode().getNode();
            helper.assertTrue(node != null, "Submitter node missing");
            for (Direction direction : Direction.values()) {
                var cableNode = GridHelper.getExposedNode(level, absolute.relative(direction), direction.getOpposite());
                helper.assertTrue(cableNode != null && node.getConnections().stream().anyMatch(connection ->
                        connection.getDirection(node) == direction && connection.getOtherSide(node) == cableNode),
                        "Missing AE cable connection on " + direction);
            }
            helper.assertTrue(entity.getMainNode().isOnline(), "Submitter did not get power and a channel through the cable");
        });
    }

    @GameTest(template = "empty")
    public static void actualLootPreservesUserDataWithoutCopyingAeIdentity(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, NexusContent.SUBMITTER.get());
        SubmitterBlockEntity entity = (SubmitterBlockEntity) helper.getBlockEntity(pos);
        CompoundTag user = new CompoundTag();
        String id = UUID.randomUUID().toString();
        user.putString("submitterIdText", id);
        user.putString("queuedTaskIdsText", "1,ffffffffffffffff");
        CompoundTag entry = new CompoundTag();
        entry.put("key", AEItemKey.of(Items.IRON_INGOT).toTag());
        entry.putLong("amount", 37L);
        ListTag pending = new ListTag(); pending.add(entry);
        user.put("FtbqPendingRewards", pending);
        entity.loadUserData(user);
        var drops = Block.getDrops(entity.getBlockState(), helper.getLevel(), helper.absolutePos(pos), entity);
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(NexusContent.ITEM.get()), "Wrong native block loot");
        CompoundTag itemData = drops.get(0).getTagElement("BlockEntityTag");
        helper.assertTrue(itemData != null && itemData.contains("SubmitterData"), "Drop lost its user data");
        CompoundTag saved = itemData.getCompound("SubmitterData");
        helper.assertTrue(id.equals(saved.getString("submitterIdText")), "Drop lost submitter UUID");
        helper.assertTrue(saved.getList("FtbqPendingRewards", 10).getCompound(0).getLong("amount") == 37L,
                "Drop lost undelivered rewards");
        helper.assertTrue(!itemData.contains("aeNode") && !saved.contains("aeNode"), "Drop copied AE identity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativePickDoesNotClonePendingRewardsOrUuid(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, NexusContent.SUBMITTER.get());
        var state = helper.getBlockState(pos);
        var stack = state.getBlock().getCloneItemStack(helper.getLevel(), helper.absolutePos(pos), state);
        helper.assertTrue(stack.is(NexusContent.ITEM.get()), "Creative pick returned wrong item");
        helper.assertTrue(stack.getTagElement("BlockEntityTag") == null, "Creative pick cloned submitter data");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void serverCreativeSaveToItemRemovesExistingUserData(GameTestHelper helper) {
        SubmitterBlockEntity entity = create();
        CompoundTag user = new CompoundTag();
        user.putString("submitterIdText", UUID.randomUUID().toString());
        CompoundTag entry = new CompoundTag();
        entry.put("key", AEItemKey.of(Items.IRON_INGOT).toTag());
        entry.putLong("amount", 37L);
        ListTag pending = new ListTag(); pending.add(entry);
        user.put("FtbqPendingRewards", pending);
        entity.loadUserData(user);
        var stack = new net.minecraft.world.item.ItemStack(NexusContent.ITEM.get());
        CompoundTag existing = new CompoundTag();
        existing.put("SubmitterData", user.copy());
        stack.addTagElement("BlockEntityTag", existing);
        entity.saveToItem(stack);
        helper.assertTrue(stack.getTagElement("BlockEntityTag") == null,
                "Server creative slot sanitization copied UUID or pending rewards");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void identityCollisionIsResolvedAfterPlacementNbtIsLoaded(GameTestHelper helper) {
        BlockPos firstPos = new BlockPos(1, 1, 1);
        BlockPos secondPos = new BlockPos(3, 1, 1);
        helper.setBlock(firstPos, NexusContent.SUBMITTER.get());
        helper.setBlock(secondPos, NexusContent.SUBMITTER.get());
        SubmitterBlockEntity first = (SubmitterBlockEntity) helper.getBlockEntity(firstPos);
        SubmitterBlockEntity second = (SubmitterBlockEntity) helper.getBlockEntity(secondPos);
        CompoundTag user = new CompoundTag();
        user.putString("submitterIdText", UUID.randomUUID().toString());
        // BlockItem applies its NBT after the newly placed entity's onLoad callback.
        first.loadUserData(user.copy());
        second.loadUserData(user.copy());
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(!first.getSubmitterIdText().equals(second.getSubmitterIdText()),
                    "Post-placement item NBT bypassed submitter UUID collision detection");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void restoredRewardPlayerOwnsCreatedAeNode(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, NexusContent.SUBMITTER.get());
        SubmitterBlockEntity entity = (SubmitterBlockEntity) helper.getBlockEntity(pos);
        UUID playerId = UUID.randomUUID();
        CompoundTag user = new CompoundTag();
        user.putString("rewardPlayerIdText", playerId.toString());
        entity.loadUserData(user);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(entity.getMainNode().getNode() != null, "Restored submitter has no node");
            helper.assertTrue(playerId.equals(entity.getMainNode().getNode().getOwningPlayerProfileId()),
                    "Node creation overwrote the restored reward player owner");
            helper.succeed();
        });
    }

    private static SubmitterBlockEntity create() {
        return NexusContent.BLOCK_ENTITY.get().create(BlockPos.ZERO, NexusContent.SUBMITTER.get().defaultBlockState());
    }
}
