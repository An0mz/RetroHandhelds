package dev.anomz.retrohandhelds.client.input;

import static dev.anomz.retrohandhelds.client.libretro.LibretroConstants.*;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * Handheld buttons and their key mappings, listed under "Retro Handhelds" in the Controls menu.
 * GUI context: they only apply inside the emulator screen, so defaults such as A/S do not
 * conflict with movement keys.
 */
public enum JoypadButton {
    UP("up", "Up", DEVICE_ID_JOYPAD_UP, GLFW.GLFW_KEY_UP),
    DOWN("down", "Down", DEVICE_ID_JOYPAD_DOWN, GLFW.GLFW_KEY_DOWN),
    LEFT("left", "Left", DEVICE_ID_JOYPAD_LEFT, GLFW.GLFW_KEY_LEFT),
    RIGHT("right", "Right", DEVICE_ID_JOYPAD_RIGHT, GLFW.GLFW_KEY_RIGHT),
    A("a", "A", DEVICE_ID_JOYPAD_A, GLFW.GLFW_KEY_X),
    B("b", "B", DEVICE_ID_JOYPAD_B, GLFW.GLFW_KEY_Z),
    L("l", "L", DEVICE_ID_JOYPAD_L, GLFW.GLFW_KEY_A),
    R("r", "R", DEVICE_ID_JOYPAD_R, GLFW.GLFW_KEY_S),
    START("start", "Start", DEVICE_ID_JOYPAD_START, GLFW.GLFW_KEY_ENTER),
    SELECT("select", "Select", DEVICE_ID_JOYPAD_SELECT, GLFW.GLFW_KEY_BACKSPACE);

    public static final String CATEGORY = "key.categories.retrohandhelds";
    public static final List<JoypadButton> ALL = List.of(values());

    private final String label;
    private final int retroId;
    private final KeyMapping mapping;

    JoypadButton(String name, String label, int retroId, int defaultKey) {
        this.label = label;
        this.retroId = retroId;
        this.mapping = new KeyMapping("key.retrohandhelds." + name, KeyConflictContext.GUI,
                InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    /** Short name for on-screen hints. */
    public String label() {
        return label;
    }

    public int retroId() {
        return retroId;
    }

    public KeyMapping mapping() {
        return mapping;
    }

    public static void registerAll(RegisterKeyMappingsEvent event) {
        for (JoypadButton button : ALL) {
            event.register(button.mapping);
        }
    }
}
