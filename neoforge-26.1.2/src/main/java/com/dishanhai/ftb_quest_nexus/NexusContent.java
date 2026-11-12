package com.dishanhai.ftb_quest_nexus;

import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import java.util.function.Consumer;

import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlock;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterMenu;
import com.dishanhai.ftb_quest_nexus.checker.CheckerBlock;
import com.dishanhai.ftb_quest_nexus.checker.CheckerBlockEntity;
import com.dishanhai.ftb_quest_nexus.checker.CheckerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Author: dishanhai. Registration remains separate from feature logic. */
public final class NexusContent {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, QuestNexus.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, QuestNexus.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, QuestNexus.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, QuestNexus.MOD_ID);
    public static final DeferredHolder<Block, SubmitterBlock> SUBMITTER = BLOCKS.register("ftbq_ae_submitter", id -> new SubmitterBlock(blockProperties(id, MapColor.COLOR_CYAN)));
    public static final DeferredHolder<Item, Item> ITEM = ITEMS.register("ftbq_ae_submitter",
            id -> new BlockItem(SUBMITTER.get(), itemProperties(id)) {
                @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                                      Consumer<Component> tooltip, TooltipFlag flag) {
                    SubmitterBlock.appendTooltip(tooltip);
                }
            });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SubmitterBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register("ftbq_ae_submitter",
                    () -> new BlockEntityType<>(SubmitterBlockEntity::new, SUBMITTER.get()));
    public static final DeferredHolder<MenuType<?>, MenuType<SubmitterMenu>> MENU =
            MENUS.register("ftbq_ae_submitter", () -> IMenuTypeExtension.create(SubmitterMenu::new));
    public static final DeferredHolder<Block, CheckerBlock> CHECKER =
            BLOCKS.register("ftbq_ae_checker", id -> new CheckerBlock(blockProperties(id, MapColor.COLOR_LIGHT_GRAY)));
    public static final DeferredHolder<Item, Item> CHECKER_ITEM = ITEMS.register("ftbq_ae_checker",
            id -> new BlockItem(CHECKER.get(), itemProperties(id)) {
                @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                                      Consumer<Component> tooltip, TooltipFlag flag) {
                    CheckerBlock.appendTooltip(tooltip);
                }
            });
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CheckerBlockEntity>> CHECKER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("ftbq_ae_checker",
                    () -> new BlockEntityType<>(CheckerBlockEntity::new, CHECKER.get()));
    public static final DeferredHolder<MenuType<?>, MenuType<CheckerMenu>> CHECKER_MENU =
            MENUS.register("ftbq_ae_checker", () -> IMenuTypeExtension.create(CheckerMenu::new));

    private static BlockBehaviour.Properties blockProperties(net.minecraft.resources.Identifier id, MapColor color) {
        return BlockBehaviour.Properties.of().setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK, id))
                .mapColor(color).strength(3.5F).requiresCorrectToolForDrops();
    }

    private static Item.Properties itemProperties(net.minecraft.resources.Identifier id) {
        return new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM, id)).stacksTo(1);
    }

    private NexusContent() {}
}
