package dev.anomz.retrohandhelds.client;

import org.jetbrains.annotations.Nullable;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.RetroRegistries;
import dev.anomz.retrohandhelds.RetroServerConfig;
import dev.anomz.retrohandhelds.block.ConsoleBlockEntity;
import dev.anomz.retrohandhelds.block.TvBlockEntity;
import dev.anomz.retrohandhelds.block.TvLayout;
import dev.anomz.retrohandhelds.client.command.RetroHandheldsCommand;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import dev.anomz.retrohandhelds.client.input.EmulatorKeys;
import dev.anomz.retrohandhelds.client.input.GamepadInput;
import dev.anomz.retrohandhelds.client.libretro.NativeLoader;
import dev.anomz.retrohandhelds.client.input.JoypadButton;
import dev.anomz.retrohandhelds.client.render.ConsoleRenderer;
import dev.anomz.retrohandhelds.client.render.HandheldArmPose;
import dev.anomz.retrohandhelds.client.render.HandheldInHandRenderer;
import dev.anomz.retrohandhelds.client.render.HeldHandheldLayer;
import dev.anomz.retrohandhelds.client.render.TvRenderer;
import dev.anomz.retrohandhelds.client.screen.EmulatorScreen;
import dev.anomz.retrohandhelds.client.screen.RomPickerScreen;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import dev.anomz.retrohandhelds.client.share.RemoteAudio;
import dev.anomz.retrohandhelds.client.share.RemoteScreens;
import dev.anomz.retrohandhelds.client.share.ScreenBroadcaster;
import dev.anomz.retrohandhelds.item.CartridgeData;
import dev.anomz.retrohandhelds.item.HandheldItem;
import dev.anomz.retrohandhelds.item.RemoteItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
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
        modEventBus.addListener(RetroHandheldsClient::onAddLayers);
        modEventBus.addListener(RetroHandheldsClient::onRegisterClientExtensions);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onRegisterClientCommands);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onRightClickItem);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onShutdown);
        NeoForge.EVENT_BUS.addListener(HandheldInHandRenderer::onRenderHand);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onRenderGui);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onRenderHandHidden);
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

    /** The game on handhelds held by players seen from outside. */
    private static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new HeldHandheldLayer(renderer));
            }
        }
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(HandheldArmPose.EXTENSIONS, RetroRegistries.HANDHELD.get());
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
        RemoteAudio.tick(minecraft);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ConsoleSessions.stop();
        RemoteScreens.clear();
        RemoteAudio.clear();
    }

    /**
     * Empty hand (or a remote) on a console, or on a TV screen touching one, powers it on.
     * Inserting and ejecting are handled by the block on the server.
     */
    private static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (!event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND
                || !(held.isEmpty() || held.is(RetroRegistries.REMOTE)) || player.isShiftKeyDown()) {
            return;
        }
        BlockPos pos = event.getPos();
        if (event.getLevel().getBlockEntity(pos) instanceof TvBlockEntity tv) {
            if (!held.isEmpty()) {
                // The TV doesn't take the click, so the remote's own right-click handles it.
                return;
            }
            TvLayout layout = tv.layout(event.getLevel().getGameTime());
            BlockPos found = layout.tooBig() ? null : layout.findConsole(event.getLevel());
            if (found == null) {
                player.displayClientMessage(Component.translatable("message.retrohandhelds.no_console"), true);
                return;
            }
            pos = found;
        }
        if (event.getLevel().getBlockEntity(pos) instanceof ConsoleBlockEntity) {
            powerOnConsole(player, event.getLevel(), pos);
        }
    }

    private static void powerOnConsole(Player player, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof ConsoleBlockEntity console)) {
            player.displayClientMessage(Component.translatable("message.retrohandhelds.remote_lost"), true);
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
        ConsoleSessions.powerOn(pos, cartridge);
    }

    private static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        if (stack.is(RetroRegistries.CARTRIDGE)) {
            Minecraft.getInstance().setScreen(new RomPickerScreen(event.getHand()));
        } else if (stack.is(RetroRegistries.HANDHELD)) {
            HandheldInHandRenderer.noteUsed(event.getHand(), event.getLevel().getGameTime());
            CartridgeData cartridge = HandheldItem.cartridgeData(stack);
            if (!RetroServerConfig.handheldsEnabled()) {
                player.displayClientMessage(Component.translatable("message.retrohandhelds.handhelds_disabled"), true);
            } else if (cartridge == null) {
                player.displayClientMessage(Component.translatable("message.retrohandhelds.handheld_empty"), true);
            } else {
                ConsoleSessions.powerOnHandheld(cartridge);
            }
        } else if (stack.is(RetroRegistries.REMOTE) && !player.isSecondaryUseActive()) {
            useRemote(player, event.getLevel(), RemoteItem.link(stack));
        }
    }

    private static void useRemote(Player player, Level level, @Nullable GlobalPos link) {
        if (link == null) {
            player.displayClientMessage(Component.translatable("item.retrohandhelds.remote.unlinked"), true);
        } else if (!link.dimension().equals(level.dimension())
                || player.distanceToSqr(Vec3.atCenterOf(link.pos())) > ConsoleSessions.STOP_RANGE * ConsoleSessions.STOP_RANGE
                || !level.isLoaded(link.pos())) {
            player.displayClientMessage(Component.translatable("message.retrohandhelds.remote_too_far"), true);
        } else {
            powerOnConsole(player, level, link.pos());
        }
    }

    /** While the game is watched in the world, hide the HUD (like F1) so nothing covers it. */
    private static void onRenderGui(RenderGuiEvent.Pre event) {
        if (worldView() != null) {
            event.setCanceled(true);
        }
    }

    /** No empty hand in front of a console or TV being watched; a handheld is still drawn (it has the game). */
    private static void onRenderHandHidden(RenderHandEvent event) {
        EmulatorScreen screen = worldView();
        if (screen != null && !screen.showsHandheld()) {
            event.setCanceled(true);
        }
    }

    @Nullable
    private static EmulatorScreen worldView() {
        return Minecraft.getInstance().screen instanceof EmulatorScreen screen && screen.inWorldView() ? screen : null;
    }

    private static void onShutdown(GameShuttingDownEvent event) {
        EmulatorSession.stopActive();
    }
}
