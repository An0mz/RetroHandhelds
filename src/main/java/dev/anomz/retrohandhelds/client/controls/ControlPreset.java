package dev.anomz.retrohandhelds.client.controls;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.anomz.retrohandhelds.RetroSystem;
import dev.anomz.retrohandhelds.client.input.MouseLook;

/**
 * The controls one console has, in the order the Controls screen lists them, each with that console's
 * own name for it (the N64's "A" is the RetroPad's B) and a default key. The RetroPad mapping of every
 * label follows the bundled core's own input descriptors.
 */
public final class ControlPreset {
    /** One control: what it does, what the console calls it, and its key until the player changes it. */
    public record Entry(ControlAction action, String label, Binding defaultBinding) {
    }

    private final List<Entry> entries;
    @Nullable
    private final MouseLook.Mode mouseMode;
    private final boolean mouseOn;
    private final boolean mouseVertical;

    private ControlPreset(List<Entry> entries, @Nullable MouseLook.Mode mouseMode, boolean mouseOn, boolean mouseVertical) {
        this.entries = List.copyOf(entries);
        this.mouseMode = mouseMode;
        this.mouseOn = mouseOn;
        this.mouseVertical = mouseVertical;
    }

    public List<Entry> entries() {
        return entries;
    }

    /** Whether the mouse moves the right stick unless the player turns it off. */
    public boolean mouseLook() {
        return mouseMode != null && mouseOn;
    }

    /** How the mouse moves the right stick, or null when it does not. */
    @Nullable
    public MouseLook.Mode mouseMode() {
        return mouseMode;
    }

    /** Whether up/down mouse movement counts too, unless the player changes it. */
    public boolean mouseVertical() {
        return mouseVertical;
    }

    public static ControlPreset forSystem(RetroSystem system) {
        Builder b = new Builder();
        switch (system) {
            case GAME_BOY_ADVANCE -> b.dpad().button(ControlAction.A, "A", GLFW_KEY_X).button(ControlAction.B, "B", GLFW_KEY_Z)
                    .button(ControlAction.L, "L", GLFW_KEY_A).button(ControlAction.R, "R", GLFW_KEY_S).startSelect("Start", "Select");
            case NES -> b.dpad().button(ControlAction.A, "A", GLFW_KEY_X).button(ControlAction.B, "B", GLFW_KEY_Z)
                    .startSelect("Start", "Select");
            case SNES, NINTENDO_DS -> b.dpad().button(ControlAction.A, "A", GLFW_KEY_X).button(ControlAction.B, "B", GLFW_KEY_Z)
                    .button(ControlAction.X, "X", GLFW_KEY_S).button(ControlAction.Y, "Y", GLFW_KEY_A)
                    .button(ControlAction.L, "L", GLFW_KEY_Q).button(ControlAction.R, "R", GLFW_KEY_W).startSelect("Start", "Select");
            case GENESIS, SEGA_32X -> b.dpad().button(ControlAction.B, "A", GLFW_KEY_Z).button(ControlAction.A, "B", GLFW_KEY_X)
                    .button(ControlAction.R, "C", GLFW_KEY_C).button(ControlAction.Y, "X", GLFW_KEY_A)
                    .button(ControlAction.X, "Y", GLFW_KEY_S).button(ControlAction.L, "Z", GLFW_KEY_D).startSelect("Start", "Mode");
            case MASTER_SYSTEM -> b.dpad().button(ControlAction.B, "1", GLFW_KEY_Z).button(ControlAction.A, "2", GLFW_KEY_X)
                    .startSelect("Start / Pause", "Reset");
            case N64 -> b.stick(false, "Stick", GLFW_KEY_W, GLFW_KEY_S, GLFW_KEY_A, GLFW_KEY_D)
                    .button(ControlAction.B, "A", GLFW_KEY_SPACE).button(ControlAction.Y, "B", GLFW_KEY_E)
                    .button(ControlAction.L2, "Z", GLFW_KEY_LEFT_SHIFT).button(ControlAction.R, "R", GLFW_KEY_R)
                    .button(ControlAction.L, "L", GLFW_KEY_Q).button(ControlAction.START, "Start", GLFW_KEY_ENTER)
                    .stick(true, "C", GLFW_KEY_UP, GLFW_KEY_DOWN, GLFW_KEY_LEFT, GLFW_KEY_RIGHT)
                    .button(ControlAction.UP, "D-Pad Up", GLFW_KEY_I).button(ControlAction.DOWN, "D-Pad Down", GLFW_KEY_K)
                    .button(ControlAction.LEFT, "D-Pad Left", GLFW_KEY_J).button(ControlAction.RIGHT, "D-Pad Right", GLFW_KEY_L)
                    // C-buttons are buttons: Super Mario 64 turns its camera one step per press, and C-Up
                    // is first-person view. The arrow keys press them directly, so the mouse is off unless
                    // the player turns it on: then a flick is a tap, left/right only.
                    .mouseLook(MouseLook.Mode.TAPS, false, false);
            case PLAYSTATION -> b.dpad().button(ControlAction.B, "Cross", GLFW_KEY_Z).button(ControlAction.A, "Circle", GLFW_KEY_X)
                    .button(ControlAction.Y, "Square", GLFW_KEY_A).button(ControlAction.X, "Triangle", GLFW_KEY_S)
                    .button(ControlAction.L, "L1", GLFW_KEY_Q).button(ControlAction.R, "R1", GLFW_KEY_W)
                    .button(ControlAction.L2, "L2", GLFW_KEY_1).button(ControlAction.R2, "R2", GLFW_KEY_3)
                    .button(ControlAction.L3, "L3", -1).button(ControlAction.R3, "R3", -1)
                    .startSelect("Start", "Select")
                    .stick(false, "Left Stick", GLFW_KEY_I, GLFW_KEY_K, GLFW_KEY_J, GLFW_KEY_L)
                    .stick(true, "Right Stick", GLFW_KEY_T, GLFW_KEY_G, GLFW_KEY_F, GLFW_KEY_H)
                    .mouseLook(MouseLook.Mode.STICK, true, true);
            case PC_ENGINE -> b.dpad().button(ControlAction.A, "I", GLFW_KEY_X).button(ControlAction.B, "II", GLFW_KEY_Z)
                    .startSelect("Run", "Select");
            case NEO_GEO_POCKET -> b.dpad().button(ControlAction.B, "A", GLFW_KEY_Z).button(ControlAction.A, "B", GLFW_KEY_X)
                    .button(ControlAction.START, "Option", GLFW_KEY_ENTER);
            case WONDERSWAN -> b.button(ControlAction.UP, "X1 (Up)", GLFW_KEY_UP).button(ControlAction.RIGHT, "X2 (Right)", GLFW_KEY_RIGHT)
                    .button(ControlAction.DOWN, "X3 (Down)", GLFW_KEY_DOWN).button(ControlAction.LEFT, "X4 (Left)", GLFW_KEY_LEFT)
                    .button(ControlAction.R2, "Y1 (Up)", GLFW_KEY_I).button(ControlAction.R, "Y2 (Right)", GLFW_KEY_L)
                    .button(ControlAction.L2, "Y3 (Down)", GLFW_KEY_K).button(ControlAction.L, "Y4 (Left)", GLFW_KEY_J)
                    .button(ControlAction.A, "A", GLFW_KEY_X).button(ControlAction.B, "B", GLFW_KEY_Z)
                    .startSelect("Start", "Rotate");
            case LYNX -> b.dpad().button(ControlAction.A, "A", GLFW_KEY_X).button(ControlAction.B, "B", GLFW_KEY_Z)
                    .button(ControlAction.L, "Option 1", GLFW_KEY_A).button(ControlAction.R, "Option 2", GLFW_KEY_S)
                    .startSelect("Pause", "Rotate");
            case ATARI_2600 -> b.dpad().button(ControlAction.B, "Fire", GLFW_KEY_X).startSelect("Reset", "Select")
                    .button(ControlAction.L, "Left Difficulty A", -1).button(ControlAction.L2, "Left Difficulty B", -1)
                    .button(ControlAction.R, "Right Difficulty A", -1).button(ControlAction.R2, "Right Difficulty B", -1);
            case ATARI_7800 -> b.dpad().button(ControlAction.B, "1", GLFW_KEY_Z).button(ControlAction.A, "2", GLFW_KEY_X)
                    .startSelect("Pause", "Select").button(ControlAction.X, "Reset", GLFW_KEY_R);
            case POKEMON_MINI -> b.dpad().button(ControlAction.A, "A", GLFW_KEY_X).button(ControlAction.B, "B", GLFW_KEY_Z)
                    .button(ControlAction.R, "C", GLFW_KEY_C).button(ControlAction.L, "Shake", GLFW_KEY_A)
                    .button(ControlAction.SELECT, "Power", GLFW_KEY_BACKSPACE);
        }
        return b.build();
    }

    private static final class Builder {
        private final List<Entry> entries = new ArrayList<>();
        private MouseLook.Mode mouseMode;
        private boolean mouseOn;
        private boolean mouseVertical;

        Builder button(ControlAction action, String label, int glfwKey) {
            entries.add(new Entry(action, label, Binding.key(glfwKey)));
            return this;
        }

        Builder dpad() {
            return button(ControlAction.UP, "Up", GLFW_KEY_UP).button(ControlAction.DOWN, "Down", GLFW_KEY_DOWN)
                    .button(ControlAction.LEFT, "Left", GLFW_KEY_LEFT).button(ControlAction.RIGHT, "Right", GLFW_KEY_RIGHT);
        }

        Builder startSelect(String start, String select) {
            return button(ControlAction.START, start, GLFW_KEY_ENTER).button(ControlAction.SELECT, select, GLFW_KEY_BACKSPACE);
        }

        Builder stick(boolean right, String name, int up, int down, int left, int right2) {
            if (right) {
                return button(ControlAction.RIGHT_STICK_UP, name + " Up", up).button(ControlAction.RIGHT_STICK_DOWN, name + " Down", down)
                        .button(ControlAction.RIGHT_STICK_LEFT, name + " Left", left).button(ControlAction.RIGHT_STICK_RIGHT, name + " Right", right2);
            }
            return button(ControlAction.LEFT_STICK_UP, name + " Up", up).button(ControlAction.LEFT_STICK_DOWN, name + " Down", down)
                    .button(ControlAction.LEFT_STICK_LEFT, name + " Left", left).button(ControlAction.LEFT_STICK_RIGHT, name + " Right", right2);
        }

        /** How the mouse can move the right stick, whether it does until changed, and whether up/down counts. */
        Builder mouseLook(MouseLook.Mode mode, boolean on, boolean vertical) {
            mouseMode = mode;
            mouseOn = on;
            mouseVertical = vertical;
            return this;
        }

        ControlPreset build() {
            return new ControlPreset(entries, mouseMode, mouseOn, mouseVertical);
        }
    }
}
