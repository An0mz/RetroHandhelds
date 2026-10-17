package dev.anomz.retrohandhelds.client;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.RetroRegistries;
import dev.anomz.retrohandhelds.RetroServerConfig;
import dev.anomz.retrohandhelds.block.ConsoleBlockEntity;
import dev.anomz.retrohandhelds.client.command.RetroHandheldsCommand;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import dev.anomz.retrohandhelds.client.input.EmulatorKeys;
import dev.anomz.retrohandhelds.client.input.GamepadInput;
import dev.anomz.retrohandhelds.client.libretro.NativeLoader;
import dev.anomz.retrohandhelds.client.input.JoypadButton;
import dev.anomz.retrohandhelds.client.render.ConsoleRenderer;
import dev.anomz.retrohandhelds.client.render.HandheldInHandRenderer;
import dev.anomz.retrohandhelds.client.render.TvRenderer;
import dev.anomz.retrohandhelds.client.screen.RomPickerScreen;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import dev.anomz.retrohandhelds.client.share.RemoteScreens;
import dev.anomz.retrohandhelds.client.share.ScreenBroadcaster;
import dev.anomz.retrohandhelds.item.CartridgeData;
import dev.anomz.retrohandhelds.item.HandheldItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Client entry point. Only constructed on the physical client, so every class under
 * {@code client} is reached from here and never from common code.
 */
@Mod(value = RetroHandhelds.MODID, dist = Dist.CLIENT)
public final class RetroHandheldsClient {
    public RetroHandheldsClient(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, RetroClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(RetroHandheldsClient::onClientSetup);
        modEventBus.addListener(JoypadButton::registerAll);
        modEventBus.addListener(EmulatorKeys::registerAll);
        modEventBus.addListener(RetroHandheldsClient::onRegisterRenderers);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onRegisterClientCommands);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onShutdown);
        NeoForge.EVENT_BUS.addListener(HandheldInHandRenderer::onRenderHand);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        RetroPaths.roms();
        NativeLoader.preloadAsync();
        // GLFW joystick calls must run on the main thread; enqueued work runs there during loading.
        event.enqueueWork(GamepadInput::warmUp);
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RetroRegistries.CONSOLE_BLOCK_ENTITY.get(), ConsoleRenderer::new);
        event.registerBlockEntityRenderer(RetroRegistries.TV_BLOCK_ENTITY.get(), TvRenderer::new);
    }

    private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        RetroHandheldsCommand.register(event.getDispatcher());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        EmulatorKeys.handleInWorld(minecraft);
        ConsoleSessions.tick(minecraft);
        ScreenBroadcaster.tick(minecraft);
        RemoteScreens.tick();
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ConsoleSessions.stop();
        RemoteScreens.clear();
    }

    /** Empty hand on a console powers it on. Inserting and ejecting are handled by the block on the server. */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || !player.getMainHandItem().isEmpty() || player.isShiftKeyDown()
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof ConsoleBlockEntity console)) {
            return;
        }
        if (!RetroServerConfig.consolesEnabled()) {
            player.displayClientMessage(Component.translatable("message.retrohandhelds.consoles_disabled"), true);
            return;
        }
        CartridgeData cartridge = console.cartridgeData();
        if (cartridge == null) {
            player.displayClientMessage(Component.translatable("message.retrohandhelds.no_cartridge"), true);
            return;
        }
        ConsoleSessions.powerOn(event.getPos(), cartridge);
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.is(RetroRegistries.CARTRIDGE)) {
            Minecraft.getInstance().setScreen(new RomPickerScreen(event.getHand()));
        } else if (stack.is(RetroRegistries.HANDHELD)) {
            HandheldInHandRenderer.noteUsed(event.getHand(), event.getLevel().getGameTime());
            CartridgeData cartridge = HandheldItem.cartridgeData(stack);
            if (!RetroServerConfig.handheldsEnabled()) {
                event.getEntity().displayClientMessage(Component.translatable("message.retrohandhelds.handhelds_disabled"), true);
            } else if (cartridge == null) {
                event.getEntity().displayClientMessage(Component.translatable("message.retrohandhelds.handheld_empty"), true);
            } else {
                ConsoleSessions.powerOnHandheld(cartridge);
            }
        }
    }

    private static void onShutdown(GameShuttingDownEvent event) {
        EmulatorSession.stopActive();
    }
}
