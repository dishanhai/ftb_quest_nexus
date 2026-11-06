package com.dishanhai.ftb_quest_nexus;

import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlock;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterBlockEntity;
import com.dishanhai.ftb_quest_nexus.submitter.SubmitterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Author: dishanhai. Registration is kept separate from individual features. */
public final class NexusContent {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, QuestNexus.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, QuestNexus.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, QuestNexus.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, QuestNexus.MOD_ID);
    public static final RegistryObject<SubmitterBlock> SUBMITTER = BLOCKS.register("ftbq_ae_submitter", SubmitterBlock::new);
    public static final RegistryObject<Item> ITEM = ITEMS.register("ftbq_ae_submitter", () -> new BlockItem(SUBMITTER.get(), new Item.Properties().stacksTo(1)));
    public static final RegistryObject<BlockEntityType<SubmitterBlockEntity>> BLOCK_ENTITY = BLOCK_ENTITIES.register("ftbq_ae_submitter",
            () -> BlockEntityType.Builder.of(SubmitterBlockEntity::new, SUBMITTER.get()).build(null));
    public static final RegistryObject<MenuType<SubmitterMenu>> MENU = MENUS.register("ftbq_ae_submitter", () -> IForgeMenuType.create(SubmitterMenu::new));
    private NexusContent() {}
}
