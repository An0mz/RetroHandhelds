package dev.anomz.retrohandhelds.client.input;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/** Emulator controls that are not handheld buttons. GUI context, like {@link JoypadButton}. */
public final class EmulatorKeys {
    public static final KeyMapping SAVE_STATE = new KeyMapping("key.retrohandhelds.save_state",
            KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F5, JoypadButton.CATEGORY);
    public static final KeyMapping LOAD_STATE = new KeyMapping("key.retrohandhelds.load_state",
            KeyConflictContext.GUI, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F9, JoypadButton.CATEGORY);

    private EmulatorKeys() {
    }

    public static void registerAll(RegisterKeyMappingsEvent event) {
        event.register(SAVE_STATE);
        event.register(LOAD_STATE);
    }
}
