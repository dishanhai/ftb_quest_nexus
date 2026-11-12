package com.dishanhai.ftb_quest_nexus.checker;

import net.minecraft.world.level.LevelReader;
import java.util.function.Consumer;

import com.dishanhai.ftb_quest_nexus.NexusContent;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Author: dishanhai. Six-sided ME connection with automatic placer binding. */
public final class CheckerBlock extends BaseEntityBlock {
    public static final MapCodec<CheckerBlock> CODEC = simpleCodec(CheckerBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ONLINE = BooleanProperty.create("online");

    public CheckerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ONLINE, false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, ONLINE);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CheckerBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, NexusContent.CHECKER_BLOCK_ENTITY.get(), CheckerBlockEntity::serverTick);
    }
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof CheckerBlockEntity checker) {
            checker.bindPlacer(player);
        }
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CheckerBlockEntity checker) {
            serverPlayer.openMenu(checker, pos);
        }
        return InteractionResult.SUCCESS;
    }
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state,
                                       boolean includeData, Player player) {
        return new ItemStack(NexusContent.CHECKER_ITEM.get());
    }

    public static void appendTooltip(Consumer<Component> tooltip) {
        for (int i = 1; i <= 5; i++) {
            tooltip.accept(Component.translatable("tooltip.ftb_quest_nexus.checker." + (i == 3 ? "3_with_energy" : i)));
        }
        tooltip.accept(Component.translatable("tooltip.ftb_quest_nexus.checker.energy_input"));
    }
}
