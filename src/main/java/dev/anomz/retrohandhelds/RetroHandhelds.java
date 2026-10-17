package dev.anomz.retrohandhelds;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.anomz.retrohandhelds.network.AssignRomPayload;
import dev.anomz.retrohandhelds.network.AudioChunkPayload;
import dev.anomz.retrohandhelds.network.ShareAudioPayload;
import dev.anomz.retrohandhelds.network.ScreenStripPayload;
import dev.anomz.retrohandhelds.network.ShareStripPayload;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(RetroHandhelds.MODID)
public final class RetroHandhelds {
    public static final String MODID = "retrohandhelds";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RetroHandhelds(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, RetroServerConfig.SPEC);
        RetroRegistries.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(RetroHandhelds::onLoggedOut);
        modEventBus.addListener(RetroHandhelds::onRegisterPayloads);
        modEventBus.addListener(RetroHandhelds::onBuildCreativeTabs);
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        AssignRomPayload.forget(event.getEntity());
        ShareStripPayload.forget(event.getEntity());
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("2")
                .playToServer(AssignRomPayload.TYPE, AssignRomPayload.STREAM_CODEC, AssignRomPayload::handle)
                .playToServer(ShareStripPayload.TYPE, ShareStripPayload.STREAM_CODEC, ShareStripPayload::handle)
                .playToClient(ScreenStripPayload.TYPE, ScreenStripPayload.STREAM_CODEC, ScreenStripPayload::handle)
                .playToServer(ShareAudioPayload.TYPE, ShareAudioPayload.STREAM_CODEC, ShareAudioPayload::handle)
                .playToClient(AudioChunkPayload.TYPE, AudioChunkPayload.STREAM_CODEC, AudioChunkPayload::handle);
    }

    private static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(RetroRegistries.CONSOLE_ITEM);
            event.accept(RetroRegistries.TV_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(RetroRegistries.CARTRIDGE);
            event.accept(RetroRegistries.HANDHELD);
        }
    }
}
