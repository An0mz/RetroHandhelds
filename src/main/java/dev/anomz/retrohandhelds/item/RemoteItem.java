package dev.anomz.retrohandhelds.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.anomz.retrohandhelds.RetroRegistries;
import dev.anomz.retrohandhelds.block.ConsoleBlockEntity;
import dev.anomz.retrohandhelds.block.TvBlockEntity;
import dev.anomz.retrohandhelds.block.TvLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Plays a console from a distance. Shift-right-click a console, or a TV screen touching one, to
 * link it (saved on the item, so it works on the server); right-click anywhere in range to play,
 * handled on the client.
 */
public class RemoteItem extends Item {
    public RemoteItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static GlobalPos link(ItemStack remote) {
        return remote.get(RetroRegistries.REMOTE_LINK);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos console = consoleAt(level, context.getClickedPos());
        if (console == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            context.getItemInHand().set(RetroRegistries.REMOTE_LINK, GlobalPos.of(level.dimension(), console));
            player.displayClientMessage(Component.translatable("message.retrohandhelds.remote_linked",
                    console.getX(), console.getY(), console.getZ()), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** The console at {@code pos}, or the one touching the TV screen at {@code pos}. */
    @Nullable
    private static BlockPos consoleAt(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ConsoleBlockEntity) {
            return pos;
        }
        if (level.getBlockEntity(pos) instanceof TvBlockEntity tv) {
            TvLayout layout = tv.layout(level.getGameTime());
            return layout.invalid() ? null : layout.findConsole(level);
        }
        return null;
    }

    /** Consume rather than success: same handling, but without the arm swing. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GlobalPos link = link(stack);
        if (link == null) {
            tooltip.add(Component.translatable("item.retrohandhelds.remote.unlinked").withStyle(ChatFormatting.GRAY));
        } else {
            BlockPos pos = link.pos();
            tooltip.add(Component.translatable("item.retrohandhelds.remote.linked", pos.getX(), pos.getY(), pos.getZ())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
