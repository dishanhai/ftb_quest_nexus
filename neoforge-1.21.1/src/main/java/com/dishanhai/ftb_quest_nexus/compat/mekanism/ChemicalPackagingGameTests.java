package com.dishanhai.ftb_quest_nexus.compat.mekanism;

import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.mojang.authlib.GameProfile;
import mekanism.api.MekanismAPI;
import mekanism.api.radiation.IRadiationManager;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.registries.MekanismBlocks;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Author: dishanhai. Verifies the contents and delivery count of native chemical tank rewards. */
@GameTestHolder(QuestNexus.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChemicalPackagingGameTests {
    private static final ResourceLocation ETHENE = ResourceLocation.fromNamespaceAndPath("mekanism", "ethene");
    private static final ResourceLocation NUCLEAR_WASTE = ResourceLocation.fromNamespaceAndPath("mekanism", "nuclear_waste");

    private ChemicalPackagingGameTests() {}

    @GameTest(template = "empty")
    public static void packagingPaysOnlyForTanksDelivered(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PackagingTest"));
        if (ModList.get().isLoaded("mekanism")) {
            WithMekanism.verifyDelivery(helper, player);
        } else {
            helper.assertTrue(!ChemicalPackaging.canPackage(ETHENE) && ChemicalPackaging.give(player, ETHENE, 1200) == 0,
                    "Packaging was available without Mekanism");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void packagingRespectsMekanismChemicalRestrictions(GameTestHelper helper) {
        if (ModList.get().isLoaded("mekanism")) {
            WithMekanism.verifyRestrictions(helper);
        }
        helper.succeed();
    }

    private static final class WithMekanism {
        private static void verifyDelivery(GameTestHelper helper, ServerPlayer player) {
            var inventory = player.getInventory();
            for (int slot = 0; slot < inventory.items.size(); slot++) {
                inventory.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
            inventory.setItem(0, ItemStack.EMPTY);
            player.getAbilities().instabuild = true;

            var emptyTank = new ItemStack(MekanismBlocks.BASIC_CHEMICAL_TANK);
            var emptyHandler = emptyTank.getCapability(Capabilities.CHEMICAL.item());
            long capacity = emptyHandler.getChemicalTankCapacity(0);
            long tail = Math.min(1700, capacity);
            helper.assertTrue(ChemicalPackaging.canPackage(ETHENE), "Mekanism tank did not accept ethene");
            long delivered = ChemicalPackaging.give(player, ETHENE, capacity + tail);
            helper.assertTrue(delivered == capacity && contentsMatch(inventory.getItem(0), capacity),
                    "Single empty slot did not receive exactly one full tank of ethene");
            helper.assertTrue(ChemicalPackaging.give(player, ETHENE, tail) == 0,
                    "A full inventory reported an undelivered tank as paid");

            inventory.setItem(1, ItemStack.EMPTY);
            helper.assertTrue(ChemicalPackaging.give(player, ETHENE, tail) == tail && contentsMatch(inventory.getItem(1), tail),
                    "Available slot did not receive exactly the remaining ethene");
        }

        private static void verifyRestrictions(GameTestHelper helper) {
            var unknown = ResourceLocation.fromNamespaceAndPath("mekanism", "missing_for_packaging_test");
            helper.assertTrue(!ChemicalPackaging.canPackage(unknown), "An unknown chemical was accepted");
            if (IRadiationManager.INSTANCE.isRadiationEnabled()) {
                helper.assertTrue(!ChemicalPackaging.canPackage(NUCLEAR_WASTE), "A portable tank was offered for radioactive waste");
            }
        }

        private static boolean contentsMatch(ItemStack tank, long amount) {
            var handler = tank.getCapability(Capabilities.CHEMICAL.item());
            if (handler == null) return false;
            var stored = handler.getChemicalInTank(0);
            return stored.is(MekanismAPI.CHEMICAL_REGISTRY.get(ETHENE)) && stored.getAmount() == amount;
        }
    }
}
