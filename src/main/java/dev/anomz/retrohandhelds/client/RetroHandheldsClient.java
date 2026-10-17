package dev.anomz.retrohandhelds.client;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.command.TestCommand;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

/**
 * Client entry point. Only constructed on the physical client, so every class under
 * {@code client} is reached from here and never from common code.
 */
@Mod(value = RetroHandhelds.MODID, dist = Dist.CLIENT)
public final class RetroHandheldsClient {
    public RetroHandheldsClient(IEventBus modEventBus) {
        modEventBus.addListener(RetroHandheldsClient::onClientSetup);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onRegisterClientCommands);
        NeoForge.EVENT_BUS.addListener(RetroHandheldsClient::onShutdown);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        RetroPaths.roms();
    }

    private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        TestCommand.register(event.getDispatcher());
    }

    private static void onShutdown(GameShuttingDownEvent event) {
        EmulatorSession.stopActive();
    }
}
