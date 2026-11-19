package com.dishanhai.ftb_quest_nexus.compat.create;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

/** Optional Create adapter. Its public 1.21.1 Stressometer accessors are read-only. */
final class CreateStressometer {
    private static final ResourceLocation STRESSOMETER = ResourceLocation.fromNamespaceAndPath("create", "stressometer");
    private static final String GAUGE = "com.simibubi.create.content.kinetics.gauge.StressGaugeBlockEntity";
    private static final String STRESS_IMPACT = "com.simibubi.create.content.kinetics.base.IRotate$StressImpact";

    static boolean available() { return ModList.get().isLoaded("create"); }

    static StressReading read(BlockEntity block) {
        if (block == null || block.isRemoved() || !available()
                || !STRESSOMETER.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(block.getType()))) return null;
        try {
            var loader = block.getClass().getClassLoader();
            var type = Class.forName(GAUGE, false, loader);
            if (!type.isInstance(block)) return null;
            var impact = Class.forName(STRESS_IMPACT, false, loader);
            if (!(boolean) impact.getMethod("isEnabled").invoke(null)) return null;
            return StressReading.active(
                    (float) type.getMethod("getSpeed").invoke(block),
                    (float) type.getMethod("getNetworkCapacity").invoke(block),
                    (float) type.getMethod("getNetworkStress").invoke(block),
                    (boolean) type.getMethod("hasNetwork").invoke(block),
                    type.getField("networkDirty").getBoolean(block),
                    (boolean) type.getMethod("isOverStressed").invoke(block));
        } catch (ReflectiveOperationException | LinkageError incompatible) {
            // Unsupported Create versions do not prevent FTB Quests from loading the task type.
            return null;
        }
    }

    private CreateStressometer() {}
}
