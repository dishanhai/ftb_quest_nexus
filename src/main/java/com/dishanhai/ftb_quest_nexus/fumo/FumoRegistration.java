// Author: dishanhai
package com.dishanhai.ftb_quest_nexus.fumo;

import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.dishanhai.ftb_quest_nexus.QuestNexus;
import com.dishanhai.ftb_quest_nexus.fumo.DishanhaiFumoBlock.DanceEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

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
        bus.addListener((RegisterEvent event) -> {
            if (!(event.getForgeRegistry() instanceof ForgeRegistry<?> registry)) return;
            if (event.getRegistryKey().equals(Registries.BLOCK) || event.getRegistryKey().equals(Registries.ITEM)) {
                addAlias(registry, source, target, DISHANHAI);
                addAlias(registry, source, target, BEIBEIPUO);
                if (libraryLoaded) addAlias(registry, source, target, REPLICA);
            } else if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
                addAlias(registry, source, target, DISHANHAI);
            }
        });
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
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> event.addListener(
                FumoRewards.reloadListener(FMLPaths.CONFIGDIR.get(), manager -> manager.getResource(pool),
                        item -> BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(item)).orElse(null))));
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> FumoRewards.resetPool());
    }

    private static void addAlias(ForgeRegistry<?> registry, String source, String target, String path) {
        registry.addAlias(ResourceLocation.fromNamespaceAndPath(source, path), ResourceLocation.fromNamespaceAndPath(target, path));
    }

    private static final class Fallback {
        private final RegistryObject<DishanhaiFumoBlock> dishanhaiBlock =
                NexusContent.BLOCKS.register(DISHANHAI,
                        () -> new DishanhaiFumoBlock(BlockBehaviour.Properties.of(), this::entityType));
        private final RegistryObject<DishanhaiFumoBlock> beibeipuoBlock =
                NexusContent.BLOCKS.register(BEIBEIPUO,
                        () -> new DishanhaiFumoBlock(BlockBehaviour.Properties.of(), this::entityType));
        private final RegistryObject<FakeFumoBlock> replicaBlock =
                NexusContent.BLOCKS.register(REPLICA, () -> new FakeFumoBlock(BlockBehaviour.Properties.of()));
        private final RegistryObject<BlockItem> dishanhaiItem = item(DISHANHAI, dishanhaiBlock);
        private final RegistryObject<BlockItem> beibeipuoItem = item(BEIBEIPUO, beibeipuoBlock);
        private final RegistryObject<BlockItem> replicaItem = item(REPLICA, replicaBlock);
        private final RegistryObject<BlockEntityType<DanceEntity>> entity =
                NexusContent.BLOCK_ENTITIES.register(DISHANHAI, () -> BlockEntityType.Builder.of(
                        (pos, state) -> new DanceEntity(entityType(), pos, state),
                        dishanhaiBlock.get(), beibeipuoBlock.get()).build(null));

        private static RegistryObject<BlockItem> item(String path, Supplier<? extends Block> block) {
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
