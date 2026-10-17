package dev.anomz.retrohandhelds.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.anomz.retrohandhelds.RetroRegistries;
import dev.anomz.retrohandhelds.RomNames;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/**
 * A portable console holding one cartridge (in the vanilla container component). Like a bundle:
 * right-click a cartridge onto it in the inventory to insert, right-click it with an empty cursor
 * to take the cartridge out. Right-clicking in hand to play is handled on the client.
 */
public class HandheldItem extends Item {
    public HandheldItem(Properties properties) {
        super(properties);
    }

    public static ItemStack cartridge(ItemStack handheld) {
        return handheld.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
    }

    @Nullable
    public static CartridgeData cartridgeData(ItemStack handheld) {
        return cartridge(handheld).get(RetroRegistries.CARTRIDGE_DATA);
    }

    private static void setCartridge(ItemStack handheld, ItemStack cartridge) {
        handheld.set(DataComponents.CONTAINER,
                cartridge.isEmpty() ? ItemContainerContents.EMPTY : ItemContainerContents.fromItems(List.of(cartridge)));
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack handheld, ItemStack carried, Slot slot, ClickAction action,
            Player player, SlotAccess access) {
        if (handheld.getCount() != 1 || action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false;
        }
        ItemStack inside = cartridge(handheld);
        if (carried.isEmpty()) {
            if (inside.isEmpty()) {
                return false;
            }
            setCartridge(handheld, ItemStack.EMPTY);
            access.set(inside);
            player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
            return true;
        }
        // Only cartridges with a ROM chosen; a blank one would have nothing to play.
        if (!carried.is(RetroRegistries.CARTRIDGE) || !carried.has(RetroRegistries.CARTRIDGE_DATA)) {
            return false;
        }
        setCartridge(handheld, carried.split(1));
        // Swap: the cartridge that was inside goes onto the cursor (cartridges stack to 1, so it is free).
        if (!inside.isEmpty()) {
            access.set(inside);
        }
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public Component getName(ItemStack stack) {
        CartridgeData data = cartridgeData(stack);
        return data == null
                ? super.getName(stack)
                : Component.translatable("item.retrohandhelds.handheld.named", RomNames.displayName(data.romFile()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CartridgeData data = cartridgeData(stack);
        if (data == null) {
            tooltip.add(Component.translatable("item.retrohandhelds.handheld.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.literal(data.romFile()).withStyle(ChatFormatting.GRAY));
        }
    }
}
