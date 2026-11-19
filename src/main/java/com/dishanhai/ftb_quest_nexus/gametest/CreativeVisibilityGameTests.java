package com.dishanhai.ftb_quest_nexus.gametest;

import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.QuestNexus;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.Collection;

/** Author: dishanhai. Exercises the native creative builder and the catalogs consumed by JEI. */
@GameTestHolder(QuestNexus.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreativeVisibilityGameTests {
    private CreativeVisibilityGameTests() {}

    @GameTest(template = "empty")
    public static void creativeAndRecipeViewerCatalogsIncludeBothBlocksWithoutOp(GameTestHelper helper) {
        verifyCatalogs(helper, false);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeAndRecipeViewerCatalogsIncludeBothBlocksWithOp(GameTestHelper helper) {
        verifyCatalogs(helper, true);
        helper.succeed();
    }

    private static void verifyCatalogs(GameTestHelper helper, boolean operator) {
        CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS, operator, helper.getLevel().registryAccess());
        CreativeModeTab functional = BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.FUNCTIONAL_BLOCKS);
        for (Item item : new Item[]{NexusContent.ITEM.get(), NexusContent.CHECKER_ITEM.get()}) {
            helper.assertTrue(contains(functional.getDisplayItems(), item), "Functional tab omitted a Nexus block");
            helper.assertTrue(contains(functional.getSearchTabDisplayItems(), item), "JEI search catalog omitted a Nexus block");
            helper.assertTrue(contains(CreativeModeTabs.searchTab().getDisplayItems(), item), "Creative search omitted a Nexus block");
            helper.assertTrue(functional.getDisplayItems().stream().filter(stack -> stack.is(item)).count() == 1,
                    "Functional tab duplicated a Nexus block");
        }
    }

    private static boolean contains(Collection<ItemStack> stacks, Item item) {
        return stacks.stream().anyMatch(stack -> stack.is(item));
    }

    @GameTest(template = "empty")
    public static void registersOnlyActiveFumoProvider(GameTestHelper helper) {
        boolean libraryLoaded = ModList.get().isLoaded("sh_common_lib");
        String active = libraryLoaded ? "sh_common_lib" : QuestNexus.MOD_ID;
        String inactive = libraryLoaded ? QuestNexus.MOD_ID : "sh_common_lib";
        for (String name : new String[]{"dishanhai_fumo", "beibeipuo_fumo", "fake_fumo"}) {
            var current = ResourceLocation.fromNamespaceAndPath(active, name);
            var alternate = ResourceLocation.fromNamespaceAndPath(inactive, name);
            var legacy = ResourceLocation.fromNamespaceAndPath("sh_better_ui", name);
            verifyFumoEntry(helper, BuiltInRegistries.BLOCK, current, alternate, legacy);
            verifyFumoEntry(helper, BuiltInRegistries.ITEM, current, alternate, legacy);
        }
        verifyFumoEntry(helper, BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(active, "dishanhai_fumo"),
                ResourceLocation.fromNamespaceAndPath(inactive, "dishanhai_fumo"),
                ResourceLocation.fromNamespaceAndPath("sh_better_ui", "dishanhai_fumo"));
        helper.succeed();
    }

    private static <T> void verifyFumoEntry(GameTestHelper helper, Registry<T> registry,
                                            ResourceLocation current, ResourceLocation alternate, ResourceLocation legacy) {
        helper.assertTrue(registry.keySet().contains(current), "Missing Fumo registry entry " + current);
        helper.assertTrue(!registry.keySet().contains(alternate) && !registry.keySet().contains(legacy),
                "A second Fumo provider registered " + current);
    }
}
