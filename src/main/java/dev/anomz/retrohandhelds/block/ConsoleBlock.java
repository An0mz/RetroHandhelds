package dev.anomz.retrohandhelds.block;

import com.mojang.serialization.MapCodec;

import dev.anomz.retrohandhelds.RetroRegistries;
import dev.anomz.retrohandhelds.RetroServerConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Right-click with a cartridge to insert it (swapping out any cartridge already inside), sneak-right-click
 * with an empty hand to eject it. Powering on with an empty hand is handled on the client, since the
 * emulator only exists there.
 */
public class ConsoleBlock extends BaseEntityBlock {
    public static final MapCodec<ConsoleBlock> CODEC = simpleCodec(ConsoleBlock::new);
    /** Whether a cartridge is inserted; the model shows it sticking out of the top. */
    public static final BooleanProperty CARTRIDGE = BooleanProperty.create("cartridge");

    public ConsoleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(CARTRIDGE, false));
    }

    @Override
    protected MapCodec<ConsoleBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, CARTRIDGE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConsoleBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(RetroRegistries.CARTRIDGE) || stack.get(RetroRegistries.CARTRIDGE_DATA) == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!RetroServerConfig.consolesEnabled()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.retrohandhelds.consoles_disabled"), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ConsoleBlockEntity console) {
            ItemStack inserted = player.getAbilities().instabuild ? stack.copyWithCount(1) : stack.split(1);
            giveBack(player, console.swapCartridge(inserted));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() && !level.isClientSide() && level.getBlockEntity(pos) instanceof ConsoleBlockEntity console) {
            giveBack(player, console.swapCartridge(ItemStack.EMPTY));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ConsoleBlockEntity console) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, console.cartridge());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static void giveBack(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
