// Author: dishanhai
package com.dishanhai.ftb_quest_nexus.fumo;

import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.fumo.DishanhaiFumoBlock.DanceEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

/** Supplies the Nexus plushes when SH-Common-Lib is absent. */
public final class FumoRegistration {
    private static final String SH_COMMON_LIB = "sh_common_lib";
    private static final String SH_BETTER_UI = "sh_better_ui";
    private static final String DISHANHAI = "dishanhai_fumo";
    private static final String BEIBEIPUO = "beibeipuo_fumo";
    private static final String REPLICA = "fake_fumo";

    private FumoRegistration() {}

    public static void register(IEventBus bus) {
        boolean libraryLoaded = ModList.get().isLoaded(SH_COMMON_LIB);
        String source = libraryLoaded ? QuestNexus.MOD_ID : SH_BETTER_UI;
        String target = libraryLoaded ? SH_COMMON_LIB : QuestNexus.MOD_ID;
        addAliases(source, target, DISHANHAI);
        addAliases(source, target, BEIBEIPUO);
        if (libraryLoaded) addAliases(source, target, REPLICA);
        if (libraryLoaded) return;

        var fallback = new Fallback();
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.COLORED_BLOCKS)) {
                event.accept(fallback.dishanhaiItem.get());
                event.accept(fallback.beibeipuoItem.get());
                event.accept(fallback.replicaItem.get());
            }
        });
        if (FMLEnvironment.dist == Dist.CLIENT) Client.register(bus, fallback);
        var pool = ResourceLocation.parse(FumoRewards.POOL_RESOURCE);
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(
                FumoRewards.reloadListener(FMLPaths.CONFIGDIR.get(), manager -> manager.getResource(pool),
                        item -> BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(item)).orElse(null))));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> FumoRewards.resetPool());
    }

    private static void addAliases(String source, String target, String path) {
        var from = ResourceLocation.fromNamespaceAndPath(source, path);
        var to = ResourceLocation.fromNamespaceAndPath(target, path);
        NexusContent.BLOCKS.addAlias(from, to);
        NexusContent.ITEMS.addAlias(from, to);
        if (path.equals(DISHANHAI)) NexusContent.BLOCK_ENTITIES.addAlias(from, to);
    }

    private static final class Fallback {
        private final DeferredHolder<Block, DishanhaiFumoBlock> dishanhaiBlock =
                NexusContent.BLOCKS.register(DISHANHAI,
                        () -> new DishanhaiFumoBlock(BlockBehaviour.Properties.of(), this::entityType));
        private final DeferredHolder<Block, DishanhaiFumoBlock> beibeipuoBlock =
                NexusContent.BLOCKS.register(BEIBEIPUO,
                        () -> new DishanhaiFumoBlock(BlockBehaviour.Properties.of(), this::entityType));
        private final DeferredHolder<Block, FakeFumoBlock> replicaBlock =
                NexusContent.BLOCKS.register(REPLICA, () -> new FakeFumoBlock(BlockBehaviour.Properties.of()));
        private final DeferredHolder<Item, BlockItem> dishanhaiItem = item(DISHANHAI, dishanhaiBlock);
        private final DeferredHolder<Item, BlockItem> beibeipuoItem = item(BEIBEIPUO, beibeipuoBlock);
        private final DeferredHolder<Item, BlockItem> replicaItem = item(REPLICA, replicaBlock);
        private final DeferredHolder<BlockEntityType<?>, BlockEntityType<DanceEntity>> entity =
                NexusContent.BLOCK_ENTITIES.register(DISHANHAI, () -> BlockEntityType.Builder.of(
                        (pos, state) -> new DanceEntity(entityType(), pos, state),
                        dishanhaiBlock.get(), beibeipuoBlock.get()).build(null));

        private static DeferredHolder<Item, BlockItem> item(String path, Supplier<? extends Block> block) {
            return NexusContent.ITEMS.register(path, () -> new BlockItem(block.get(), new Item.Properties()));
        }

        private BlockEntityType<DanceEntity> entityType() {
            return entity.get();
        }
    }

    private static final class Client {
        private static void register(IEventBus bus, Fallback fallback) {
            bus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                    event.registerBlockEntityRenderer(fallback.entityType(), FumoRenderer::new));
        }
    }
}
