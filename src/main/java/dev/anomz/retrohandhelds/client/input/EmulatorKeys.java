package dev.anomz.retrohandhelds.client.input;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import dev.anomz.retrohandhelds.client.share.RemoteAudio;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * Emulator controls that are not handheld buttons. Save states only apply in the emulator screen;
 * the volume keys also work in the world: on this player's game while one runs, otherwise on the
 * games other players share nearby.
 */
public final class EmulatorKeys {
    public static final KeyMapping SAVE_STATE = new KeyMapping("key.retrohandhelds.save_state",
            KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F5, JoypadButton.CATEGORY);
    public static final KeyMapping LOAD_STATE = new KeyMapping("key.retrohandhelds.load_state",
            KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, JoypadButton.CATEGORY);
    public static final KeyMapping FAST_FORWARD = new KeyMapping("key.retrohandhelds.fast_forward",
            KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_TAB, JoypadButton.CATEGORY);
    public static final KeyMapping TOGGLE_VIEW = new KeyMapping("key.retrohandhelds.toggle_view",
            KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, JoypadButton.CATEGORY);
    public static final KeyMapping MUTE = new KeyMapping("key.retrohandhelds.mute",
            KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, JoypadButton.CATEGORY);
    public static final KeyMapping VOLUME_DOWN = new KeyMapping("key.retrohandhelds.volume_down",
            KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_MINUS, JoypadButton.CATEGORY);
    public static final KeyMapping VOLUME_UP = new KeyMapping("key.retrohandhelds.volume_up",
            KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_EQUAL, JoypadButton.CATEGORY);

    private EmulatorKeys() {
    }

    public static void registerAll(RegisterKeyMappingsEvent event) {
        event.register(SAVE_STATE);
        event.register(LOAD_STATE);
        event.register(FAST_FORWARD);
        event.register(TOGGLE_VIEW);
        event.register(MUTE);
        event.register(VOLUME_DOWN);
        event.register(VOLUME_UP);
    }

    /**
     * In-world handling, called every client tick. Minecraft only counts clicks while no screen is
     * open; the emulator screen handles these keys itself. Clicks are always drained, so presses
     * made while nothing plays do nothing, now or later.
     */
    public static void handleInWorld(Minecraft minecraft) {
        boolean own = ConsoleSessions.isRunning();
        boolean active = (own || RemoteAudio.isPlaying()) && minecraft.player != null;
        while (MUTE.consumeClick()) {
            if (active) {
                minecraft.player.displayClientMessage(ConsoleSessions.toggleMute(), true);
            }
        }
        while (VOLUME_DOWN.consumeClick()) {
            if (active) {
                minecraft.player.displayClientMessage(changeVolume(own, -1), true);
            }
        }
        while (VOLUME_UP.consumeClick()) {
            if (active) {
                minecraft.player.displayClientMessage(changeVolume(own, 1), true);
            }
        }
    }

    private static Component changeVolume(boolean own, int steps) {
        return own ? ConsoleSessions.changeVolume(steps) : ConsoleSessions.changeOtherPlayersVolume(steps);
    }
}
