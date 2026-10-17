package dev.anomz.retrohandhelds;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.anomz.retrohandhelds.network.AssignRomPayload;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(RetroHandhelds.MODID)
public final class RetroHandhelds {
    public static final String MODID = "retrohandhelds";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RetroHandhelds(IEventBus modEventBus) {
        RetroRegistries.register(modEventBus);
        modEventBus.addListener(RetroHandhelds::onRegisterPayloads);
        modEventBus.addListener(RetroHandhelds::onBuildCreativeTabs);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(AssignRomPayload.TYPE, AssignRomPayload.STREAM_CODEC, AssignRomPayload::handle);
    }

    private static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(RetroRegistries.CONSOLE_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(RetroRegistries.CARTRIDGE);
            event.accept(RetroRegistries.HANDHELD);
        }
    }
}
