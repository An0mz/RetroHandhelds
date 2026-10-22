package dev.anomz.retrohandhelds.client.input;

import static dev.anomz.retrohandhelds.client.libretro.LibretroConstants.*;
import static org.lwjgl.glfw.GLFW.*;

import dev.anomz.retrohandhelds.client.emu.JoypadState;

/** Turns a GLFW gamepad state (standard Xbox-style layout) into a libretro joypad bitmask and analog sticks. */
public final class GamepadMapping {
    private static final float TRIGGER_THRESHOLD = 0.5f;

    private GamepadMapping() {
    }

    /** {@link #toRetroMask(byte[], float[], float, boolean, boolean, boolean)} for a plain joypad system. */
    public static int toRetroMask(byte[] buttons, float[] axes, float deadzone, boolean positionalAB) {
        return toRetroMask(buttons, axes, deadzone, positionalAB, true, false);
    }

    /**
     * @param buttons GLFW_PRESS (1) or GLFW_RELEASE (0) per {@code GLFW_GAMEPAD_BUTTON_*}
     * @param axes values per {@code GLFW_GAMEPAD_AXIS_*}: sticks -1..1 (y down), triggers -1..1
     * @param deadzone how far the left stick must move (0..1) to count as a D-pad press
     * @param positionalAB false: pad A/B/X/Y labels map to the same-named buttons. true: Nintendo
     *     positions, so the east button (Xbox B) is A, south (Xbox A) is B, north (Xbox Y) is X and
     *     west (Xbox X) is Y
     * @param stickPressesDpad whether the left stick also presses the D-pad (not on the N64, whose
     *     stick is analog only)
     * @param triggersAreL2R2 whether the triggers are L2/R2; otherwise they press L/R like the bumpers
     */
    public static int toRetroMask(byte[] buttons, float[] axes, float deadzone, boolean positionalAB,
            boolean stickPressesDpad, boolean triggersAreL2R2) {
        int mask = 0;
        boolean south = pressed(buttons, GLFW_GAMEPAD_BUTTON_A);
        boolean east = pressed(buttons, GLFW_GAMEPAD_BUTTON_B);
        mask |= bit(positionalAB ? east : south, DEVICE_ID_JOYPAD_A);
        mask |= bit(positionalAB ? south : east, DEVICE_ID_JOYPAD_B);
        boolean west = pressed(buttons, GLFW_GAMEPAD_BUTTON_X);
        boolean north = pressed(buttons, GLFW_GAMEPAD_BUTTON_Y);
        mask |= bit(positionalAB ? north : west, DEVICE_ID_JOYPAD_X);
        mask |= bit(positionalAB ? west : north, DEVICE_ID_JOYPAD_Y);
        boolean leftTrigger = axes[GLFW_GAMEPAD_AXIS_LEFT_TRIGGER] > TRIGGER_THRESHOLD;
        boolean rightTrigger = axes[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER] > TRIGGER_THRESHOLD;
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_LEFT_BUMPER) || !triggersAreL2R2 && leftTrigger, DEVICE_ID_JOYPAD_L);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER) || !triggersAreL2R2 && rightTrigger, DEVICE_ID_JOYPAD_R);
        mask |= bit(triggersAreL2R2 && leftTrigger, DEVICE_ID_JOYPAD_L2);
        mask |= bit(triggersAreL2R2 && rightTrigger, DEVICE_ID_JOYPAD_R2);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_LEFT_THUMB), DEVICE_ID_JOYPAD_L3);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_RIGHT_THUMB), DEVICE_ID_JOYPAD_R3);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_START), DEVICE_ID_JOYPAD_START);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_BACK), DEVICE_ID_JOYPAD_SELECT);

        float x = stickPressesDpad ? axes[GLFW_GAMEPAD_AXIS_LEFT_X] : 0;
        float y = stickPressesDpad ? axes[GLFW_GAMEPAD_AXIS_LEFT_Y] : 0;
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_DPAD_UP) || y < -deadzone, DEVICE_ID_JOYPAD_UP);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_DPAD_DOWN) || y > deadzone, DEVICE_ID_JOYPAD_DOWN);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_DPAD_LEFT) || x < -deadzone, DEVICE_ID_JOYPAD_LEFT);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_DPAD_RIGHT) || x > deadzone, DEVICE_ID_JOYPAD_RIGHT);
        return mask;
    }

    /**
     * One stick axis (-1..1) as a libretro analog value (-0x7FFF..0x7FFF): zero inside the deadzone,
     * then rescaled so the stick still reaches full tilt.
     */
    public static int toAxis(float value, float deadzone) {
        float magnitude = Math.abs(value);
        if (magnitude <= deadzone || deadzone >= 1) {
            return 0;
        }
        float scaled = Math.min(1, (magnitude - deadzone) / (1 - deadzone));
        return Math.round(Math.copySign(scaled, value) * JoypadState.AXIS_MAX);
    }

    private static boolean pressed(byte[] buttons, int button) {
        return buttons[button] == GLFW_PRESS;
    }

    private static int bit(boolean on, int retroId) {
        return on ? 1 << retroId : 0;
    }
}
