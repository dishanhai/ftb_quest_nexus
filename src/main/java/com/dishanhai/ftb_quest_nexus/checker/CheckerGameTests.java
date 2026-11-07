package com.dishanhai.ftb_quest_nexus.checker;

import appeng.api.config.Actionable;
import appeng.api.features.IPlayerRegistry;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftblibrary.config.Tristate;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.FluidTask;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbteams.data.TeamManagerImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Author: dishanhai. Real FTBQ goals and AE key snapshots, with mutation forbidden by the storage fixture. */
@GameTestHolder(QuestNexus.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CheckerGameTests {
    private CheckerGameTests() {}

    @GameTest(template = "empty")
    public static void stockMustMeetFullRequirementAndNeverAccumulates(GameTestHelper helper) {
        Fixture f = fixture(helper);
        MemoryStorage storage = new MemoryStorage();
        AEItemKey iron = AEItemKey.of(Items.IRON_INGOT);
        storage.stacks.add(iron, 9);
        for (int scan = 0; scan < 3; scan++) {
            InventoryCheck snapshot = new InventoryCheck(storage);
            helper.assertTrue(!InventoryCheck.complete(f.data, f.task, snapshot.available(f.task)),
                    "Partial stock completed the goal");
        }
        helper.assertTrue(f.data.getProgress(f.task) == 0, "Repeated snapshots accumulated progress");
        storage.stacks.add(iron, 1);
        InventoryCheck snapshot = new InventoryCheck(storage);
        helper.assertTrue(InventoryCheck.complete(f.data, f.task, snapshot.available(f.task)), "Enough stock did not complete");
        helper.assertTrue(f.data.isCompleted(f.task), "FTBQ did not mark the item target complete");
        helper.assertTrue(storage.stacks.get(iron) == 10, "Checker consumed items");
        helper.assertTrue(!InventoryCheck.complete(f.data, f.task, 10), "Completed target was processed again");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fluidsCombineAeStockAndBucketItemsWithoutConsumption(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.task.setConsumeItems(Tristate.TRUE);
        FluidTask fluid = fluidTask(f, Fluids.WATER, 2500);
        ItemTask bucket = new ItemTask(f.file.newID(), f.quest).setStackAndCount(new ItemStack(Items.WATER_BUCKET), 2);
        f.quest.addTask(bucket);
        MemoryStorage storage = new MemoryStorage();
        storage.stacks.add(AEItemKey.of(Items.IRON_INGOT), 10);
        storage.stacks.add(AEFluidKey.of(Fluids.WATER), 500);
        storage.stacks.add(AEItemKey.of(Items.WATER_BUCKET), 2);
        storage.stacks.add(AEItemKey.of(Items.LAVA_BUCKET), 8);
        storage.stacks.add(AEItemKey.of(Items.BUCKET), 64);
        InventoryCheck snapshot = new InventoryCheck(storage);
        helper.assertTrue(AEFluidKey.AMOUNT_BUCKET == 1000, "AE fluid unit is not mB");
        helper.assertTrue(snapshot.available(fluid) == 2500, "AE fluid and bucket volume were not combined");
        helper.assertTrue(snapshot.available(bucket) == 2, "Filled buckets stopped matching item goals");
        CheckerBlockEntity entity = create(helper);
        entity.scanInventory(f.file, f.data, snapshot);
        helper.assertTrue(f.data.getProgress(f.task) == 0 && f.data.getProgress(fluid) == 0,
                "Checker scan bypassed a consuming item or fluid goal");
        helper.assertTrue(!InventoryCheck.complete(f.data, fluid, snapshot.available(fluid)),
                "Checker bypassed a consuming fluid goal");
        helper.assertTrue(f.data.getProgress(fluid) == 0, "Checker advanced a consuming fluid goal");
        helper.assertTrue(f.data.isCompleted(bucket), "Non-consuming bucket item goal did not complete");
        helper.assertTrue(storage.stacks.get(AEFluidKey.of(Fluids.WATER)) == 500
                && storage.stacks.get(AEItemKey.of(Items.WATER_BUCKET)) == 2, "Fluid completion consumed stock");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bucketItemGoalsUseWholeBucketsOfLooseFluid(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.task.setStackAndCount(new ItemStack(Items.WATER_BUCKET), 3);
        MemoryStorage storage = new MemoryStorage();
        CheckerBlockEntity entity = create(helper);
        long previous = 0;
        for (long amount : new long[]{999, 1000, 2999, 3000}) {
            storage.stacks.add(AEFluidKey.of(Fluids.WATER), amount - previous);
            previous = amount;
            InventoryCheck snapshot = new InventoryCheck(storage);
            helper.assertTrue(snapshot.available(f.task) == amount / AEFluidKey.AMOUNT_BUCKET,
                    "Loose fluid was not converted to whole buckets");
            for (int scan = 0; scan < 3; scan++) {
                entity.scanInventory(f.file, f.data, snapshot);
            }
            helper.assertTrue(f.data.getProgress(f.task) == (amount >= 3000 ? 3 : 0),
                    "Bucket goal completed below its full requirement or accumulated repeated scans");
            helper.assertTrue(entity.getRows().get(0).available() == amount / AEFluidKey.AMOUNT_BUCKET,
                    "Checker row did not report the bucket count");
        }
        helper.assertTrue(f.data.isCompleted(f.task), "Loose fluid did not complete the bucket item goal");
        helper.assertTrue(storage.stacks.get(AEFluidKey.of(Fluids.WATER)) == 3000,
                "Bucket item completion consumed loose fluid");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bucketItemGoalsCombinePhysicalBucketsAndLooseFluidOnce(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.task.setStackAndCount(new ItemStack(Items.WATER_BUCKET), 3);
        MemoryStorage storage = new MemoryStorage();
        storage.stacks.add(AEItemKey.of(Items.WATER_BUCKET), 1);
        storage.stacks.add(AEItemKey.of(Items.LAVA_BUCKET), 8);
        storage.stacks.add(AEItemKey.of(Items.BUCKET), 64);
        storage.stacks.add(AEFluidKey.of(Fluids.WATER), 1999);
        InventoryCheck snapshot = new InventoryCheck(storage);
        helper.assertTrue(snapshot.available(f.task) == 2,
                "Physical buckets were counted twice or partial fluid counted as a whole bucket");
        helper.assertTrue(!InventoryCheck.complete(f.data, f.task, snapshot.available(f.task)),
                "Insufficient combined bucket stock completed");
        storage.stacks.add(AEFluidKey.of(Fluids.WATER), 1);
        snapshot = new InventoryCheck(storage);
        helper.assertTrue(snapshot.available(f.task) == 3
                && InventoryCheck.complete(f.data, f.task, snapshot.available(f.task)),
                "Matching physical and loose fluid stock did not complete the bucket goal");
        helper.assertTrue(storage.stacks.get(AEItemKey.of(Items.WATER_BUCKET)) == 1
                && storage.stacks.get(AEFluidKey.of(Fluids.WATER)) == 2000, "Combined bucket stock was consumed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void looseWaterCannotSupplyEmptyOrMobBuckets(GameTestHelper helper) {
        Fixture f = fixture(helper);
        MemoryStorage storage = new MemoryStorage();
        storage.stacks.add(AEFluidKey.of(Fluids.WATER), Long.MAX_VALUE);
        InventoryCheck snapshot = new InventoryCheck(storage);
        f.task.setStackAndCount(new ItemStack(Items.BUCKET), 1);
        helper.assertTrue(snapshot.available(f.task) == 0, "Loose water supplied an empty bucket");
        f.task.setStackAndCount(new ItemStack(Items.COD_BUCKET), 1);
        helper.assertTrue(snapshot.available(f.task) == 0, "Loose water supplied a fish bucket");
        f.task.setStackAndCount(new ItemStack(Items.LAVA_BUCKET), 1);
        helper.assertTrue(snapshot.available(f.task) == 0, "Loose water supplied a different fluid bucket");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fluidTagsAreMatchedExactly(GameTestHelper helper) {
        Fixture f = fixture(helper);
        FluidTask fluid = fluidTask(f, Fluids.WATER, 1000);
        CompoundTag variant = new CompoundTag();
        variant.putInt("variant", 1);
        MemoryStorage storage = new MemoryStorage();
        storage.stacks.add(AEFluidKey.of(Fluids.WATER, variant), 2000);
        helper.assertTrue(new InventoryCheck(storage).available(fluid) == 0, "Different fluid NBT matched");
        f.task.setStackAndCount(new ItemStack(Items.WATER_BUCKET), 2);
        helper.assertTrue(new InventoryCheck(storage).available(f.task) == 0, "Different fluid data supplied plain buckets");
        CompoundTag config = new CompoundTag();
        fluid.writeData(config);
        config.put("nbt", variant);
        fluid.readData(config);
        helper.assertTrue(new InventoryCheck(storage).available(fluid) == 2000, "Matching fluid NBT did not match");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hugeBucketCountsSaturateWithoutOverflow(GameTestHelper helper) {
        Fixture f = fixture(helper);
        FluidTask fluid = fluidTask(f, Fluids.WATER, 1000);
        MemoryStorage storage = new MemoryStorage();
        storage.stacks.add(AEFluidKey.of(Fluids.WATER), 42);
        storage.stacks.add(AEItemKey.of(Items.WATER_BUCKET), Long.MAX_VALUE);
        helper.assertTrue(new InventoryCheck(storage).available(fluid) == Long.MAX_VALUE, "Bucket conversion overflowed");
        f.task.setStackAndCount(new ItemStack(Items.WATER_BUCKET), 1);
        storage.stacks.add(AEFluidKey.of(Fluids.WATER), AEFluidKey.AMOUNT_BUCKET);
        helper.assertTrue(new InventoryCheck(storage).available(f.task) == Long.MAX_VALUE,
                "Combined physical and loose bucket count overflowed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void consumingAndNonConsumingItemGoalsBothUseReadOnlyStock(GameTestHelper helper) {
        Fixture f = fixture(helper);
        f.task.setConsumeItems(Tristate.FALSE);
        MemoryStorage storage = new MemoryStorage();
        storage.stacks.add(AEItemKey.of(Items.IRON_INGOT), 12);
        helper.assertTrue(InventoryCheck.complete(f.data, f.task, new InventoryCheck(storage).available(f.task)),
                "Non-consuming item task was rejected");
        helper.assertTrue(storage.stacks.get(AEItemKey.of(Items.IRON_INGOT)) == 12, "Read-only stock changed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void craftingOnlyAndLockedTasksCannotBeCompleted(GameTestHelper helper) {
        Fixture f = fixture(helper);
        ItemTask crafting = new ItemTask(f.file.newID(), f.quest) {
            @Override public boolean isOnlyFromCrafting() { return true; }
        };
        crafting.setStackAndCount(new ItemStack(Items.IRON_INGOT), 10);
        f.quest.addTask(crafting);
        helper.assertTrue(!InventoryCheck.supported(crafting) && !InventoryCheck.complete(f.data, crafting, 100),
                "Crafting-only task was completed");
        f.data.setLocked(true);
        helper.assertTrue(!InventoryCheck.complete(f.data, f.task, 100) && f.data.getProgress(f.task) == 0,
                "Locked team gained progress");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void flexibleProgressionStillRequiresDependenciesForCompletion(GameTestHelper helper) {
        Fixture f = fixture(helper);
        Quest prerequisite = new Quest(f.file.newID(), f.quest.getChapter());
        f.quest.getChapter().addQuest(prerequisite);
        prerequisite.addTask(new ItemTask(f.file.newID(), prerequisite).setStackAndCount(new ItemStack(Items.DIAMOND), 1));
        CompoundTag config = new CompoundTag();
        f.quest.writeData(config);
        config.putString("progression_mode", "flexible");
        f.quest.readData(config);
        f.quest.addDependency(prerequisite);
        f.file.refreshIDMap();
        helper.assertTrue(f.data.canStartTasks(f.quest), "Fixture did not select FLEXIBLE progression");
        helper.assertTrue(!f.data.areDependenciesComplete(f.quest), "Fixture dependency is already complete");
        helper.assertTrue(!InventoryCheck.complete(f.data, f.task, 100) && f.data.getProgress(f.task) == 0,
                "Locked flexible goal gained progress or reported completion");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sequentialTaskOrderIsRespected(GameTestHelper helper) {
        Fixture f = fixture(helper);
        ItemTask second = new ItemTask(f.file.newID(), f.quest).setStackAndCount(new ItemStack(Items.GOLD_INGOT), 5);
        f.quest.addTask(second);
        CompoundTag config = new CompoundTag();
        f.quest.writeData(config);
        config.putBoolean("require_sequential_tasks", true);
        f.quest.readData(config);
        helper.assertTrue(f.quest.getRequireSequentialTasks(), "Fixture did not enable task sequencing");
        helper.assertTrue(!InventoryCheck.complete(f.data, second, 5), "Later task bypassed the earlier goal");
        helper.assertTrue(InventoryCheck.complete(f.data, f.task, 10), "First goal failed");
        helper.assertTrue(InventoryCheck.complete(f.data, second, 5), "Unlocked second goal failed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wholeCatalogScansAndPagesPastTheFirst128Goals(GameTestHelper helper) {
        Fixture f = fixture(helper);
        for (int i = 1; i < 131; i++) {
            f.quest.addTask(new ItemTask(f.file.newID(), f.quest).setStackAndCount(new ItemStack(Items.IRON_INGOT), 10));
        }
        f.file.refreshIDMap();
        MemoryStorage storage = new MemoryStorage();
        storage.stacks.add(AEItemKey.of(Items.IRON_INGOT), 10);
        CheckerBlockEntity entity = create(helper);
        entity.scanInventory(f.file, f.data, new InventoryCheck(storage));
        helper.assertTrue(entity.getItemTaskCount() == 131 && entity.getCompletedLastScan() == 131,
                "Some goals were truncated or limited to one completion per quest");
        helper.assertTrue(entity.getPageCount() == 22, "Catalog was truncated to a fixed row limit");
        CompoundTag first = entity.writeClientState(0);
        CompoundTag last = entity.writeClientState(9999);
        helper.assertTrue(first.getList("rows", 10).size() == 6, "First page is not six rows");
        helper.assertTrue(last.getInt("page") == 21 && last.getList("rows", 10).size() == 5,
                "Last page cannot reach the final goals");
        CheckerBlockEntity client = create(helper);
        client.readClientState(last);
        helper.assertTrue(client.getRows().size() == 5 && client.getRows().get(4).completed(),
                "Client page lost row completion state");
        helper.assertTrue(storage.stacks.get(AEItemKey.of(Items.IRON_INGOT)) == 10, "Catalog scan consumed stock");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void placementBindsUuidAndRejectsForeignTeamManagement(GameTestHelper helper) {
        withBoundChecker(helper, (entity, owner) -> {
            helper.assertTrue(owner.getUUID().toString().equals(entity.getPlayerIdText()), "Placer UUID was not bound");
            String team = TeamManagerImpl.INSTANCE.getTeamForPlayerID(owner.getUUID()).orElseThrow().getTeamId().toString();
            helper.assertTrue(team.equals(entity.getTeamIdText()), "FTBQ team UUID was not bound");
            ServerPlayer outsider = player(helper);
            outsider.setPos(owner.position());
            helper.assertTrue(!entity.applyAction(outsider, 0) && !entity.applyAction(outsider, 1),
                    "Foreign team changed checker binding or pause state");
            helper.assertTrue(entity.applyAction(owner, 0) && owner.getUUID().toString().equals(entity.getPlayerIdText()),
                    "Owner could not refresh their own binding");
            helper.assertTrue(entity.applyAction(owner, 1) && entity.isPaused(), "Owner could not pause");
            helper.assertTrue(entity.applyAction(owner, 1) && !entity.isPaused(), "Owner could not resume");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void worldSaveRetainsBindingButDropsRebindToTheNextPlacer(GameTestHelper helper) {
        withBoundChecker(helper, (entity, owner) -> {
            entity.applyAction(owner, 1);
            CompoundTag saved = entity.saveWithoutMetadata();
            CheckerBlockEntity restored = create(helper);
            restored.load(saved);
            helper.assertTrue(restored.getPlayerIdText().equals(owner.getUUID().toString()) && restored.isPaused(),
                    "World save lost player binding or pause state");
            var drops = Block.getDrops(entity.getBlockState(), helper.getLevel(), entity.getBlockPos(), entity);
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(NexusContent.CHECKER_ITEM.get()),
                    "Checker loot did not drop itself");
            helper.assertTrue(drops.get(0).getTagElement("BlockEntityTag") == null, "Drop copied binding or node identity");
            ServerPlayer nextPlacer = player(helper);
            restored.bindPlacer(nextPlacer);
            helper.assertTrue(restored.getPlayerIdText().equals(owner.getUUID().toString()),
                    "Placement overwrote an existing owner without consent");
            CheckerBlockEntity replacement = create(helper);
            replacement.bindPlacer(nextPlacer);
            helper.assertTrue(replacement.getPlayerIdText().equals(nextPlacer.getUUID().toString()),
                    "Fresh checker did not bind its placer");
        });
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void placedCheckerExposesSixFacesAndDestroysItsNode(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, NexusContent.CHECKER.get());
        helper.runAfterDelay(20, () -> {
            CheckerBlockEntity entity = (CheckerBlockEntity) helper.getBlockEntity(pos);
            helper.assertTrue(entity.getMainNode().getNode() != null, "Checker AE node was not created");
            for (Direction side : Direction.values()) {
                helper.assertTrue(entity.getGridNode(side) == entity.getMainNode().getNode(), "A face cannot join the AE grid");
            }
            helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
            helper.assertTrue(entity.getMainNode().getNode() == null, "Removed checker retained its node");
            helper.succeed();
        });
    }


    @GameTest(template = "empty", timeoutTicks = 100)
    public static void checkerJoinsFluxNetworkWithoutPointBlock(GameTestHelper helper) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("fluxnetworks")) {
            helper.succeed();
            return;
        }
        FluxFixture.verifyDirectTransfer(helper);
    }

    private static final class FluxFixture {
        private static void verifyDirectTransfer(GameTestHelper helper) {
            BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, NexusContent.CHECKER.get());
            helper.setBlock(pos.east(), sonar.fluxnetworks.register.RegistryBlocks.BASIC_FLUX_STORAGE.get());
            helper.runAfterDelay(20, () -> {
                var checker = (CheckerBlockEntity) helper.getBlockEntity(pos);
                var storage = (sonar.fluxnetworks.common.device.TileFluxStorage) helper.getBlockEntity(pos.east());
                var owner = player(helper);
                var outsider = player(helper);
                owner.setPos(checker.getBlockPos().getCenter());
                UUID team = TeamManagerImpl.INSTANCE.getTeamForPlayerID(owner.getUUID()).orElseThrow().getTeamId();
                Fixture f = fixture(helper, team);
                var task = new dev.ftb.mods.ftbquests.quest.task.forge.ForgeEnergyTask(f.file.newID(), f.quest);
                task.setValue(2300);
                f.quest.addTask(task);
                f.file.refreshIDMap();
                var networks = sonar.fluxnetworks.common.connection.FluxNetworkData.getInstance();
                var network = networks.createNetwork(owner, "Nexus GameTest " + owner.getUUID(), 0,
                        sonar.fluxnetworks.api.network.SecurityLevel.ENCRYPTED, "test");
                var previous = ServerQuestFile.INSTANCE;
                try {
                    ServerQuestFile.INSTANCE = f.file;
                    checker.bindPlacer(owner);
                    runScan(checker);
                    int id = network.getNetworkID();
                    helper.assertTrue(!checker.getMainNode().isOnline(), "Checker unexpectedly has ME power");
                    helper.assertTrue(!checker.selectFluxNetwork(outsider, id, "test"),
                            "A different player changed the checker network");
                    helper.assertTrue(checker.fluxOptions(owner, 512).stream().anyMatch(option -> option.id() == id)
                                    && checker.selectFluxNetwork(owner, id, ""),
                            "Owner could not select the Flux network");
                    helper.assertTrue(storage.connect(network), "Storage could not join the Flux network");
                    storage.fillUp();
                    long before = storage.getTransferBuffer();
                    for (int cycle = 0; cycle < 3; cycle++) network.onEndServerTick();
                    helper.assertTrue(f.data.getProgress(task) == 2300 && before - storage.getTransferBuffer() == 2300,
                            "Flux consumption did not match the energy task progress");
                    helper.assertTrue(checker.saveWithoutMetadata().getInt("checkerFluxNetwork") == id,
                            "Selected network was not persisted");
                    helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
                    network.onEndServerTick();
                    helper.assertTrue(network.getConnectionByPos(net.minecraft.core.GlobalPos.of(
                                    helper.getLevel().dimension(), helper.absolutePos(pos))) == null,
                            "Removed checker was still registered in the Flux network");
                } finally {
                    ServerQuestFile.INSTANCE = previous;
                    networks.deleteNetwork(network);
                }
                helper.succeed();
            });
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void forgeEnergyInputWorksWithoutAePower(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, NexusContent.CHECKER.get());
        helper.runAfterDelay(20, () -> {
            CheckerBlockEntity entity = (CheckerBlockEntity) helper.getBlockEntity(pos);
            helper.assertTrue(!entity.getMainNode().isOnline(), "Checker unexpectedly has ME power");
            var capability = net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY;
            var input = entity.getCapability(capability, Direction.UP);
            var energy = input.orElseThrow(() -> new AssertionError("Missing top-side FE input"));
            for (Direction side : Direction.values()) {
                helper.assertTrue(entity.getCapability(capability, side).orElse(null) == energy,
                        "Missing sided FE input");
            }
            helper.assertTrue(energy.canReceive() && !energy.canExtract() && energy.receiveEnergy(4000, false) == 0,
                    "Unbound checker did not expose an input-only FE capability");
            ServerQuestFile previous = ServerQuestFile.INSTANCE;
            ServerPlayer owner = player(helper);
            owner.setPos(entity.getBlockPos().getCenter());
            UUID team = TeamManagerImpl.INSTANCE.getTeamForPlayerID(owner.getUUID()).orElseThrow().getTeamId();
            Fixture f = fixture(helper, team);
            var task = new dev.ftb.mods.ftbquests.quest.task.forge.ForgeEnergyTask(f.file.newID(), f.quest);
            task.setValue(2300);
            f.quest.addTask(task);
            f.file.refreshIDMap();
            try {
                ServerQuestFile.INSTANCE = f.file;
                entity.bindPlacer(owner);
                entity.addMenuViewer();
                runScan(entity);
                helper.assertTrue(energy.receiveEnergy(4000, true) == 1000 && f.data.getProgress(task) == 0,
                        "FE simulation changed progress or ignored the input limit");
                helper.assertTrue(energy.receiveEnergy(4000, false) == 1000 && f.data.getProgress(task) == 1000,
                        "Accepted FE did not advance the task");
                helper.assertTrue(entity.applyAction(owner, 1) && energy.receiveEnergy(4000, false) == 0,
                        "Paused checker accepted FE");
                helper.assertTrue(entity.applyAction(owner, 1), "Checker did not resume");
                runScan(entity);
                helper.assertTrue(energy.receiveEnergy(4000, false) == 1000
                                && energy.receiveEnergy(4000, false) == 300 && f.data.isCompleted(task)
                                && energy.receiveEnergy(4000, false) == 0,
                        "FE input did not stop exactly at completion");
                runScan(entity);
                var row = entity.getRows().stream().filter(entry -> entry.taskId() == task.id).findFirst().orElseThrow();
                helper.assertTrue(row.energy() && row.completed() && row.available() == 2300,
                        "Catalog did not show completed FE");
                helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
                helper.assertTrue(!input.isPresent(), "Removed checker retained its FE capability");
            } finally {
                entity.removeMenuViewer();
                ServerQuestFile.INSTANCE = previous;
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void boundNetworkUsesRealAeStorageAndPausesWhenOwnerLeaves(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, NexusContent.CHECKER.get());
        helper.runAfterDelay(20, () -> {
            CheckerBlockEntity entity = (CheckerBlockEntity) helper.getBlockEntity(pos);
            ServerQuestFile previous = ServerQuestFile.INSTANCE;
            ServerPlayer owner = player(helper);
            owner.setPos(entity.getBlockPos().getCenter());
            UUID team = TeamManagerImpl.INSTANCE.getTeamForPlayerID(owner.getUUID()).orElseThrow().getTeamId();
            Fixture f = fixture(helper, team);
            MemoryStorage storage = new MemoryStorage();
            storage.stacks.add(AEItemKey.of(Items.IRON_INGOT), 10);
            appeng.api.storage.IStorageProvider provider = mounts -> mounts.mount(storage);
            var service = entity.getMainNode().getGrid().getStorageService();
            service.addGlobalStorageProvider(provider);
            try {
                ServerQuestFile.INSTANCE = f.file;
                entity.bindPlacer(owner);
                helper.assertTrue(entity.applyAction(owner, 1), "Could not pause the checker");
                runNetworkScan(entity);
                helper.assertTrue(f.data.getProgress(f.task) == 0, "Paused checker advanced progress");
                entity.applyAction(owner, 1);
                runNetworkScan(entity);
                helper.assertTrue(f.data.isCompleted(f.task), "Actual grid inventory did not complete the bound team's goal");
                helper.assertTrue(storage.stacks.get(AEItemKey.of(Items.IRON_INGOT)) == 10, "Real AE route consumed items");
                helper.assertTrue(entity.getRows().isEmpty(), "An unwatched checker built menu rows");
                int reads = storage.reads;
                runNetworkScan(entity);
                helper.assertTrue(storage.reads == reads, "An unwatched checker scanned stock with no pending goal");
                var previousMenu = owner.containerMenu;
                var menu = entity.createMenu(1, owner.getInventory(), owner);
                owner.containerMenu = menu;
                try {
                    menu.broadcastChanges();
                    runNetworkScan(entity);
                    helper.assertTrue(!entity.getRows().isEmpty(), "Opening the checker did not restore catalog rows");
                } finally {
                    menu.removed(owner);
                    owner.containerMenu = previousMenu;
                }
                reads = storage.reads;
                runNetworkScan(entity);
                helper.assertTrue(entity.getRows().isEmpty() && storage.reads == reads,
                        "Closing the checker kept the catalog scan active");
                CompoundTag saved = entity.saveWithoutMetadata();
                UUID departedOwner = UUID.randomUUID();
                saved.putUUID("checkerPlayer", departedOwner);
                entity.load(saved);
                runNetworkScan(entity);
                helper.assertTrue(entity.getRows().isEmpty()
                        && entity.writeClientState(0).getString("status").equals("player_left"), "Departed owner did not pause scanning");
                helper.assertTrue(entity.canManage(owner) && !entity.applyAction(owner, 0)
                                && departedOwner.toString().equals(entity.getPlayerIdText()),
                        "Original team replaced an offline owner without consent");
                helper.succeed();
            } finally {
                ServerQuestFile.INSTANCE = previous;
                service.removeGlobalStorageProvider(provider);
            }
        });
    }

    private static void runNetworkScan(CheckerBlockEntity entity) {
        try {
            // Only channel power is bypassed. Binding, team checks and AE storage use their real paths.
            var online = CheckerBlockEntity.class.getDeclaredField("online");
            online.setAccessible(true);
            online.setBoolean(entity, true);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
        runScan(entity);
    }

    private static void runScan(CheckerBlockEntity entity) {
        try {
            var scan = CheckerBlockEntity.class.getDeclaredMethod("scan");
            scan.setAccessible(true);
            scan.invoke(entity);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }


    private static Fixture fixture(GameTestHelper helper) {
        return fixture(helper, UUID.randomUUID());
    }

    private static Fixture fixture(GameTestHelper helper, UUID teamId) {
        ServerQuestFile file = new ServerQuestFile(helper.getLevel().getServer());
        Chapter chapter = new Chapter(file.newID(), file, file.getDefaultChapterGroup());
        file.getDefaultChapterGroup().addChapter(chapter);
        Quest quest = new Quest(file.newID(), chapter);
        chapter.addQuest(quest);
        ItemTask task = new ItemTask(file.newID(), quest).setStackAndCount(new ItemStack(Items.IRON_INGOT), 10);
        task.setConsumeItems(Tristate.FALSE);
        quest.addTask(task);
        file.refreshIDMap();
        TeamData data = new TeamData(teamId, file);
        file.addData(data, false);
        return new Fixture(file, quest, task, data);
    }

    private static FluidTask fluidTask(Fixture f, net.minecraft.world.level.material.Fluid type, long amount) {
        FluidTask task = new FluidTask(f.file.newID(), f.quest).setFluid(type);
        CompoundTag config = new CompoundTag();
        task.writeData(config);
        config.putLong("amount", amount);
        task.readData(config);
        f.quest.addTask(task);
        f.file.refreshIDMap();
        return task;
    }

@GameTest(template = "empty")
    public static void catalogChunksPublishAllGoalsAtomically(GameTestHelper helper) {
        Fixture f = fixture(helper);
        for (int i = 1; i < 131; i++) {
            f.quest.addTask(new ItemTask(f.file.newID(), f.quest).setStackAndCount(new ItemStack(Items.IRON_INGOT), 10));
        }
        f.file.refreshIDMap();
        MemoryStorage storage = new MemoryStorage();
        storage.stacks.add(AEItemKey.of(Items.IRON_INGOT), 10);
        CheckerBlockEntity source = create(helper);
        source.scanInventory(f.file, f.data, new InventoryCheck(storage));
        CheckerMenu menu = new CheckerMenu(1, player(helper).getInventory(), create(helper));
        try {
            var catalog = CheckerMenu.class.getMethod("getCatalogRows");
            int chunks = (source.getRows().size() + CheckerBlockEntity.CATALOG_CHUNK_SIZE - 1)
                    / CheckerBlockEntity.CATALOG_CHUNK_SIZE;
            for (int chunk = 0; chunk < chunks; chunk++) {
                CompoundTag state = source.writeClientState(chunk, CheckerBlockEntity.CATALOG_CHUNK_SIZE);
                helper.assertTrue(state.getList("rows", 10).size() <= CheckerBlockEntity.CATALOG_CHUNK_SIZE,
                        "Catalog packet exceeded its row limit");
                state.putBoolean("catalog", true);
                state.putLong("revision", 1);
                state.putInt("chunk", chunk);
                state.putInt("chunks", chunks);
                menu.readState(state);
                var rows = (java.util.List<?>) catalog.invoke(menu);
                helper.assertTrue(rows.size() == (chunk + 1 == chunks ? 131 : 0),
                        "A partial catalog was published or goals beyond the first page were lost");
            }
            CompoundTag empty = create(helper).writeClientState(0);
            empty.putBoolean("catalog", true);
            empty.putLong("revision", 2);
            empty.putInt("chunk", 0);
            empty.putInt("chunks", 1);
            menu.readState(empty);
            helper.assertTrue(((java.util.List<?>) catalog.invoke(menu)).isEmpty(), "Empty snapshot retained stale goals");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Checker menu has no complete catalog for search", exception);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stockDeltaUpdatesPublishedRowsWithoutReplacingCatalog(GameTestHelper helper) {
        Fixture f = fixture(helper);
        MemoryStorage storage = new MemoryStorage();
        CheckerBlockEntity source = create(helper);
        source.scanInventory(f.file, f.data, new InventoryCheck(storage));
        CheckerMenu menu = new CheckerMenu(1, player(helper).getInventory(), create(helper));
        CompoundTag full = source.writeClientState(0);
        full.putBoolean("catalog", true);
        full.putLong("revision", 1);
        full.putInt("chunk", 0);
        full.putInt("chunks", 1);
        menu.readState(full);
        long catalogRevision = menu.getCatalogRevision();

        CompoundTag delta = source.writeClientState(0);
        delta.putBoolean("delta", true);
        delta.putLong("revision", 2);
        delta.putLong("base", 1);
        delta.putInt("chunk", 0);
        delta.putInt("chunks", 1);
        var changes = new net.minecraft.nbt.ListTag();
        CompoundTag change = new CompoundTag();
        change.putInt("index", 0);
        change.putLong("available", 10);
        change.putBoolean("completed", true);
        changes.add(change);
        delta.put("rows", changes);
        menu.readState(delta);
        var row = menu.getCatalogRows().getFirst();
        helper.assertTrue(row.taskId() == f.task.id && row.available() == 10 && row.completed(),
                "Stock delta did not update the published row");
        helper.assertTrue(menu.getCatalogRevision() == catalogRevision, "Stock delta replaced the search catalog");

        delta.putLong("revision", 4);
        delta.putLong("base", 3);
        change.putLong("available", 0);
        menu.readState(delta);
        helper.assertTrue(menu.getCatalogRows().getFirst().available() == 10,
                "Delta with an unknown base mutated the published rows");
        helper.assertTrue(menu.takeResyncRequest() && !menu.takeResyncRequest(),
                "An unknown base did not request one full refresh");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void catalogEditsSendOnlyChangedDefinitions(GameTestHelper helper) {
        Fixture f = fixture(helper);
        var edited = new ItemTask(f.file.newID(), f.quest).setStackAndCount(new ItemStack(Items.IRON_INGOT), 10);
        var removed = new ItemTask(f.file.newID(), f.quest).setStackAndCount(new ItemStack(Items.IRON_INGOT), 10);
        f.quest.addTask(edited);
        f.quest.addTask(removed);
        for (int i = 0; i < 8; i++) {
            f.quest.addTask(new ItemTask(f.file.newID(), f.quest).setStackAndCount(new ItemStack(Items.IRON_INGOT), 10));
        }
        f.file.refreshIDMap();
        MemoryStorage storage = new MemoryStorage();
        CheckerBlockEntity source = create(helper);
        var player = player(helper);
        CheckerMenu sending = new CheckerMenu(1, player.getInventory(), source);
        CheckerMenu receiving = new CheckerMenu(1, player.getInventory(), create(helper));
        source.scanInventory(f.file, f.data, new InventoryCheck(storage));
        sending.writeStateChanges(false).forEach(receiving::readState);
        var oldCatalog = receiving.getCatalogRows();
        long oldRevision = receiving.getCatalogRevision();

        f.quest.removeTask(f.task);
        f.quest.removeTask(removed);
        f.quest.addTask(f.task);
        f.quest.addTask(new ItemTask(f.file.newID(), f.quest).setStackAndCount(new ItemStack(Items.GOLD_INGOT), 10));
        edited.setStackAndCount(new ItemStack(Items.IRON_INGOT), 11);
        f.file.refreshIDMap();
        source.scanInventory(f.file, f.data, new InventoryCheck(storage));
        var updates = sending.writeStateChanges(false);
        helper.assertTrue(updates.size() == 1 && updates.getFirst().getBoolean("delta")
                        && updates.getFirst().getList("rows", 10).size() == 5,
                "Add/remove/reorder/definition edit resent the whole catalog");
        updates.forEach(receiving::readState);
        helper.assertTrue(receiving.getCatalogRows().equals(source.getRows())
                        && receiving.getCatalogRevision() == oldRevision + 1 && oldCatalog.getFirst().taskId() == f.task.id,
                "Catalog patch lost ordering, metadata, or its atomic revision");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void largeStockDeltaPublishesOnlyAfterFinalChunk(GameTestHelper helper) {
        Fixture f = fixture(helper);
        for (int i = 1; i <= CheckerBlockEntity.CATALOG_CHUNK_SIZE; i++) {
            f.quest.addTask(new ItemTask(f.file.newID(), f.quest).setStackAndCount(new ItemStack(Items.IRON_INGOT), 10));
        }
        f.file.refreshIDMap();
        MemoryStorage storage = new MemoryStorage();
        CheckerBlockEntity source = create(helper);
        var player = player(helper);
        CheckerMenu sending = new CheckerMenu(1, player.getInventory(), source);
        CheckerMenu receiving = new CheckerMenu(1, player.getInventory(), create(helper));
        source.scanInventory(f.file, f.data, new InventoryCheck(storage));
        sending.writeStateChanges(false).forEach(receiving::readState);
        long oldRevision = receiving.getCatalogRevision();

        storage.stacks.add(AEItemKey.of(Items.IRON_INGOT), 5);
        source.scanInventory(f.file, f.data, new InventoryCheck(storage));
        var updates = sending.writeStateChanges(false);
        helper.assertTrue(updates.size() == 2 && updates.getFirst().getBoolean("delta")
                        && updates.getFirst().getList("rows", 10).size() == CheckerBlockEntity.CATALOG_CHUNK_SIZE
                        && !updates.getFirst().getList("rows", 10).getCompound(0).contains("title"),
                "Stock update was unbounded or included catalog metadata");
        receiving.readState(updates.getFirst());
        helper.assertTrue(receiving.getCatalogRows().getFirst().available() == 0,
                "A partial stock delta became visible");
        receiving.readState(updates.getLast());
        helper.assertTrue(receiving.getCatalogRows().equals(source.getRows())
                        && receiving.getCatalogRevision() == oldRevision && receiving.getStockRevision() == 1,
                "Stock chunks did not publish together with the original catalog revision");
        helper.succeed();
    }

    private static CheckerBlockEntity create(GameTestHelper helper) {
        CheckerBlockEntity entity = new CheckerBlockEntity(BlockPos.ZERO, NexusContent.CHECKER.get().defaultBlockState());
        entity.setLevel(helper.getLevel());
        return entity;
    }

    private static ServerPlayer player(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, "CheckerRegression"));
        TeamManagerImpl.INSTANCE.playerLoggedIn(null, id, "CheckerRegression");
        return player;
    }

    private static void withBoundChecker(GameTestHelper helper, java.util.function.BiConsumer<CheckerBlockEntity, ServerPlayer> test) {
        ServerQuestFile previous = ServerQuestFile.INSTANCE;
        try {
            ServerQuestFile.INSTANCE = new ServerQuestFile(helper.getLevel().getServer());
            BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, NexusContent.CHECKER.get());
            CheckerBlockEntity entity = (CheckerBlockEntity) helper.getBlockEntity(pos);
            ServerPlayer owner = player(helper);
            owner.setPos(helper.absolutePos(pos).getCenter());
            entity.getBlockState().getBlock().setPlacedBy(helper.getLevel(), entity.getBlockPos(), entity.getBlockState(),
                    owner, new ItemStack(NexusContent.CHECKER_ITEM.get()));
            test.accept(entity, owner);
        } finally {
            ServerQuestFile.INSTANCE = previous;
        }
    }

    private record Fixture(ServerQuestFile file, Quest quest, ItemTask task, TeamData data) {}

    private static final class MemoryStorage implements MEStorage {
        private final KeyCounter stacks = new KeyCounter();
        private int reads;
        @Override public Component getDescription() { return Component.empty(); }
        @Override public void getAvailableStacks(KeyCounter output) {
            reads++;
            output.addAll(stacks);
        }
        @Override public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
            throw new AssertionError("Checker must never insert into AE");
        }
        @Override public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
            throw new AssertionError("Checker must never extract from AE");
        }
    }
}

