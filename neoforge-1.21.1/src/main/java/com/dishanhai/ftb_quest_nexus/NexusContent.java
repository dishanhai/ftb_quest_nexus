package com.dishanhai.ftb_quest_nexus;

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
    public static final DeferredHolder<Block, SubmitterBlock> SUBMITTER = BLOCKS.register("ftbq_ae_submitter", () -> new SubmitterBlock());
    public static final DeferredHolder<Item, Item> ITEM = ITEMS.register("ftbq_ae_submitter",
            () -> new BlockItem(SUBMITTER.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SubmitterBlockEntity>> BLOCK_ENTITY =
            BLOCK_ENTITIES.register("ftbq_ae_submitter",
                    () -> BlockEntityType.Builder.of(SubmitterBlockEntity::new, SUBMITTER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<SubmitterMenu>> MENU =
            MENUS.register("ftbq_ae_submitter", () -> IMenuTypeExtension.create(SubmitterMenu::new));
    public static final DeferredHolder<Block, CheckerBlock> CHECKER =
            BLOCKS.register("ftbq_ae_checker", () -> new CheckerBlock());
    public static final DeferredHolder<Item, Item> CHECKER_ITEM = ITEMS.register("ftbq_ae_checker",
            () -> new BlockItem(CHECKER.get(), new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CheckerBlockEntity>> CHECKER_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("ftbq_ae_checker",
                    () -> BlockEntityType.Builder.of(CheckerBlockEntity::new, CHECKER.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<CheckerMenu>> CHECKER_MENU =
            MENUS.register("ftbq_ae_checker", () -> IMenuTypeExtension.create(CheckerMenu::new));

    private NexusContent() {}
}
