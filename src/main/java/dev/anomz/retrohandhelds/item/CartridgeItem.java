package dev.anomz.retrohandhelds.item;

import java.util.List;

import dev.anomz.retrohandhelds.RetroRegistries;
import dev.anomz.retrohandhelds.RomNames;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Right-clicking in the air opens the ROM picker (handled on the client). */
public class CartridgeItem extends Item {
    public CartridgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public Component getName(ItemStack stack) {
        CartridgeData data = stack.get(RetroRegistries.CARTRIDGE_DATA);
        return data == null
                ? super.getName(stack)
                : Component.translatable("item.retrohandhelds.cartridge.named", RomNames.displayName(data.romFile()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CartridgeData data = stack.get(RetroRegistries.CARTRIDGE_DATA);
        if (data == null) {
            tooltip.add(Component.translatable("item.retrohandhelds.cartridge.blank").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.literal(data.romFile()).withStyle(ChatFormatting.GRAY));
        }
    }
}
