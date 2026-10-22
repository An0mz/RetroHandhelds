package dev.anomz.retrohandhelds.client.input;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import dev.anomz.retrohandhelds.client.share.RemoteAudio;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * Emulator controls that are not handheld buttons. Save states only apply in the emulator screen;
 * the volume keys also work in the world. Your own game and other players' games have separate
 * mute and volume keys, so silencing one never touches the other.
 */
public final class EmulatorKeys {
    public static final String CATEGORY = "key.categories.retrohandhelds";
    /** Turns mouse look off (and on again) while a game is open, to click the screen's buttons. */
    public static final KeyMapping FREE_MOUSE = new KeyMapping("key.retrohandhelds.free_mouse",
            KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    public static final KeyMapping SAVE_STATE = new KeyMapping("key.retrohandhelds.save_state",
            KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F5, CATEGORY);
    public static final KeyMapping LOAD_STATE = new KeyMapping("key.retrohandhelds.load_state",
            KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, CATEGORY);
    public static final KeyMapping FAST_FORWARD = new KeyMapping("key.retrohandhelds.fast_forward",
            KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_TAB, CATEGORY);
    public static final KeyMapping TOGGLE_VIEW = new KeyMapping("key.retrohandhelds.toggle_view",
            KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping MUTE = new KeyMapping("key.retrohandhelds.mute",
            KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
    public static final KeyMapping VOLUME_DOWN = new KeyMapping("key.retrohandhelds.volume_down",
            KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, CATEGORY);
    public static final KeyMapping VOLUME_UP = new KeyMapping("key.retrohandhelds.volume_up",
            KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, CATEGORY);
    public static final KeyMapping MUTE_OTHERS = new KeyMapping("key.retrohandhelds.mute_others",
            KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);
    public static final KeyMapping OTHERS_VOLUME_DOWN = new KeyMapping("key.retrohandhelds.others_volume_down",
            KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, CATEGORY);
    public static final KeyMapping OTHERS_VOLUME_UP = new KeyMapping("key.retrohandhelds.others_volume_up",
            KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, CATEGORY);

    private EmulatorKeys() {
    }

    public static void registerAll(RegisterKeyMappingsEvent event) {
        event.register(SAVE_STATE);
        event.register(LOAD_STATE);
        event.register(FAST_FORWARD);
        event.register(TOGGLE_VIEW);
        event.register(FREE_MOUSE);
        event.register(MUTE);
        event.register(VOLUME_DOWN);
        event.register(VOLUME_UP);
        event.register(MUTE_OTHERS);
        event.register(OTHERS_VOLUME_DOWN);
        event.register(OTHERS_VOLUME_UP);
    }

    /**
     * In-world handling, called every client tick. Minecraft only counts clicks while no screen is
     * open; the emulator screen handles these keys itself. Clicks are always drained, so presses
     * made while nothing plays do nothing, now or later.
     */
    public static void handleInWorld(Minecraft minecraft) {
        boolean own = ConsoleSessions.isRunning() && minecraft.player != null;
        boolean others = RemoteAudio.isPlaying() && minecraft.player != null;
        while (MUTE.consumeClick()) {
            if (own) {
                minecraft.player.displayClientMessage(ConsoleSessions.toggleMute(), true);
            }
        }
        while (VOLUME_DOWN.consumeClick()) {
            if (own) {
                minecraft.player.displayClientMessage(ConsoleSessions.changeVolume(-1), true);
            }
        }
        while (VOLUME_UP.consumeClick()) {
            if (own) {
                minecraft.player.displayClientMessage(ConsoleSessions.changeVolume(1), true);
            }
        }
        while (MUTE_OTHERS.consumeClick()) {
            if (others) {
                minecraft.player.displayClientMessage(ConsoleSessions.toggleOtherPlayersMute(), true);
            }
        }
        while (OTHERS_VOLUME_DOWN.consumeClick()) {
            if (others) {
                minecraft.player.displayClientMessage(ConsoleSessions.changeOtherPlayersVolume(-1), true);
            }
        }
        while (OTHERS_VOLUME_UP.consumeClick()) {
            if (others) {
                minecraft.player.displayClientMessage(ConsoleSessions.changeOtherPlayersVolume(1), true);
            }
        }
    }
}
