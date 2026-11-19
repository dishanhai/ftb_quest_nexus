package com.dishanhai.ftb_quest_nexus.compat.pneumatic;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;

@GameTestHolder(QuestNexus.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CompressedAirGameTests {
    @GameTest(template = "empty")
    public static void verifiesNativeCanisterAndExactAppliedKey(GameTestHelper helper) throws ReflectiveOperationException {
        if (!ModList.get().isLoaded("pneumaticcraft")) {
            helper.assertTrue(!AirStorage.available(), "Optional PneumaticCraft unexpectedly resolved");
            helper.succeed();
            return;
        }
        var original = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("pneumaticcraft:air_canister")));
        addAir(original,1500);
        var slots = new ArrayList<>(List.of(original));
        helper.assertTrue(AirStorage.transfer(slots, 500, false) == 500 && slots.getFirst() == original,
                "Non-consuming check changed real canister");
        helper.assertTrue(AirStorage.transfer(slots, 500, true) == 500 && slots.getFirst() != original,
                "Real canister was not debited on the copy");
        helper.assertTrue(AirStorage.transfer(slots, 5000, false) == 1000
                && AirStorage.transfer(List.of(original), 5000, false) == 1500, "Wrong actual air conservation");
        if (ModList.get().isLoaded("appliedpneumatics")) {
            var key = AirStorage.key();
            helper.assertTrue(key != null && key.getId().equals(ResourceLocation.parse("appliedpneumatics:air_key")),
                    "Applied Pneumatics did not expose its real AirKey");
            var cell = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("appliedpneumatics:portable_air_cell_1k")));
            addAir(cell,1000);
            var cells = new ArrayList<>(List.of(cell));
            helper.assertTrue(AirStorage.transfer(cells, 333, true) == 333
                    && AirStorage.transfer(cells, 2000, false) == 667, "Portable Air Cell debit did not conserve mL");
        }
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static void addAir(ItemStack stack,int amount) throws ReflectiveOperationException {
        var cap = (ItemCapability<Object, Void>) Class.forName("me.desht.pneumaticcraft.api.PNCCapabilities")
                .getField("AIR_HANDLER_ITEM").get(null);
        Object handler = stack.getCapability(cap);
        Class.forName("me.desht.pneumaticcraft.api.tileentity.IAirHandler").getMethod("addAir",int.class).invoke(handler,amount);
    }
}
