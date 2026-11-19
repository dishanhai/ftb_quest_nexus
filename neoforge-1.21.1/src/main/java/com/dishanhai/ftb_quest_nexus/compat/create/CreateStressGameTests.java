package com.dishanhai.ftb_quest_nexus.compat.create;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tests the optional reflection boundary on a real ticking Create network. */
@GameTestHolder(QuestNexus.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreateStressGameTests {
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void probesRunningCreateNetwork(GameTestHelper helper) {
        if (!CreateStressometer.available()) {
            helper.assertTrue(CreateStressometer.read(null) == null, "Absent optional dependency exposed a reading");
            helper.succeed();
            return;
        }
        var motor = new BlockPos(0, 2, 1);
        var gauge = new BlockPos(1, 2, 1);
        var stressometer = block("create", "stressometer");
        var axisAlongFirst = (BooleanProperty) stressometer.getStateDefinition().getProperty("axis_along_first");
        helper.setBlock(gauge, stressometer.defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.NORTH)
                .setValue(axisAlongFirst, true));
        helper.assertTrue(read(helper, gauge) == null, "Stationary gauge qualified");
        helper.setBlock(motor, block("create", "creative_motor").defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.EAST));

        var provider = new BlockPos(2, 2, 1);
        boolean hasAddon = ModList.get().isLoaded("createappliedkinetics");
        if (hasAddon) {
            // Energy Provider receives shaft rotation at FACING and feeds AE2 opposite FACING.
            helper.setBlock(new BlockPos(3, 2, 1), block("ae2", "energy_acceptor"));
            helper.setBlock(provider, block("createappliedkinetics", "energy_provider")
                    .defaultBlockState().setValue(BlockStateProperties.FACING, Direction.WEST));
        }
        helper.succeedWhen(() -> {
            var value = read(helper, gauge);
            helper.assertTrue(value != null && value.reaches(StressReading.Metric.CAPACITY, 1),
                    "Running Stressometer did not expose non-zero capacity");
            if (hasAddon) {
                var entity = helper.getLevel().getBlockEntity(helper.absolutePos(provider));
                helper.assertTrue(entity != null, "Energy Provider block entity missing");
                try {
                    helper.assertTrue((float) entity.getClass().getMethod("getSpeed").invoke(entity) != 0,
                            "Energy Provider is not connected to the stress test network");
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Energy Provider did not expose Create kinetics", e);
                }
            }
        });
    }

    @GameTest(template = "empty")
    public static void preservesStressMetricAndThreshold(GameTestHelper helper) {
        var file = new ServerQuestFile(helper.getLevel().getServer());
        var chapter = new Chapter(file.newID(), file, file.getDefaultChapterGroup());
        var quest = new Quest(file.newID(), chapter);
        var registries = helper.getLevel().registryAccess();
        var input = new CompoundTag();
        input.putString("metric", "HEADROOM");
        input.putLong("amount", 768);
        var source = new CreateStressTask(file.newID(), quest);
        source.readData(input, registries);
        var saved = new CompoundTag();
        source.writeData(saved, registries);
        var restored = new CreateStressTask(file.newID(), quest);
        restored.readData(saved, registries);
        assertHeadroom(helper, restored);
        var network = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            source.writeNetData(network);
            var received = new CreateStressTask(file.newID(), quest);
            received.readNetData(network);
            assertHeadroom(helper, received);
        } finally {
            network.release();
        }
        helper.succeed();
    }

    private static void assertHeadroom(GameTestHelper helper, CreateStressTask task) {
        helper.assertTrue(task.metric() == StressReading.Metric.HEADROOM && task.getMaxProgress() == 768,
                "Stress measurement or threshold changed in a save/network round trip");
    }

    private static StressReading read(GameTestHelper helper, BlockPos gauge) {
        return CreateStressometer.read(helper.getLevel().getBlockEntity(helper.absolutePos(gauge)));
    }

    private static Block block(String namespace, String name) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(namespace, name));
    }

    private CreateStressGameTests() {}
}
