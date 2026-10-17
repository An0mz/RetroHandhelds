package dev.anomz.retrohandhelds.client.input;

import static dev.anomz.retrohandhelds.client.libretro.LibretroConstants.*;
import static org.lwjgl.glfw.GLFW.*;

/** Turns a GLFW gamepad state (standard Xbox-style layout) into a libretro joypad bitmask. */
public final class GamepadMapping {
    private static final float TRIGGER_THRESHOLD = 0.5f;
    /** Sticks used as sticks (not as a D-pad) ignore this much drift around the centre. */
    private static final float STICK_DEADZONE = 0.12f;

    private GamepadMapping() {
    }

    /**
     * @param buttons GLFW_PRESS (1) or GLFW_RELEASE (0) per {@code GLFW_GAMEPAD_BUTTON_*}
     * @param axes values per {@code GLFW_GAMEPAD_AXIS_*}: sticks -1..1 (y down), triggers -1..1
     * @param deadzone how far the left stick must move (0..1) to count as a D-pad press
     * @param positionalAB false: pad A/B labels map to GBA A/B. true: Nintendo positions, so the
     *     east button (Xbox B) is GBA A and the south button (Xbox A) is GBA B
     */
    public static int toRetroMask(byte[] buttons, float[] axes, float deadzone, boolean positionalAB) {
        return toRetroMask(buttons, axes, deadzone, positionalAB, false);
    }

    /**
     * @param discLayout for disc games (PS1): all four face buttons by position (south B, east A,
     *     west Y, north X, like the original pad) and the triggers as L2/R2 instead of L/R
     */
    public static int toRetroMask(byte[] buttons, float[] axes, float deadzone, boolean positionalAB,
            boolean discLayout) {
        int mask = 0;
        boolean south = pressed(buttons, GLFW_GAMEPAD_BUTTON_A);
        boolean east = pressed(buttons, GLFW_GAMEPAD_BUTTON_B);
        boolean positional = positionalAB || discLayout;
        mask |= bit(positional ? east : south, DEVICE_ID_JOYPAD_A);
        mask |= bit(positional ? south : east, DEVICE_ID_JOYPAD_B);
        boolean leftTrigger = axes[GLFW_GAMEPAD_AXIS_LEFT_TRIGGER] > TRIGGER_THRESHOLD;
        boolean rightTrigger = axes[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER] > TRIGGER_THRESHOLD;
        if (discLayout) {
            mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_X), DEVICE_ID_JOYPAD_Y);
            mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_Y), DEVICE_ID_JOYPAD_X);
            mask |= bit(leftTrigger, DEVICE_ID_JOYPAD_L2);
            mask |= bit(rightTrigger, DEVICE_ID_JOYPAD_R2);
            mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_LEFT_THUMB), DEVICE_ID_JOYPAD_L3);
            mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_RIGHT_THUMB), DEVICE_ID_JOYPAD_R3);
            leftTrigger = false;
            rightTrigger = false;
        }
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_LEFT_BUMPER) || leftTrigger, DEVICE_ID_JOYPAD_L);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER) || rightTrigger, DEVICE_ID_JOYPAD_R);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_START), DEVICE_ID_JOYPAD_START);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_BACK), DEVICE_ID_JOYPAD_SELECT);

        float x = axes[GLFW_GAMEPAD_AXIS_LEFT_X];
        float y = axes[GLFW_GAMEPAD_AXIS_LEFT_Y];
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_DPAD_UP) || y < -deadzone, DEVICE_ID_JOYPAD_UP);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_DPAD_DOWN) || y > deadzone, DEVICE_ID_JOYPAD_DOWN);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_DPAD_LEFT) || x < -deadzone, DEVICE_ID_JOYPAD_LEFT);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_DPAD_RIGHT) || x > deadzone, DEVICE_ID_JOYPAD_RIGHT);
        return mask;
    }

    /** A stick axis (-1..1) as a libretro analog value, rescaled past a small deadzone so slow movement still works. */
    public static short stickValue(float axis) {
        float magnitude = Math.abs(axis);
        if (magnitude <= STICK_DEADZONE) {
            return 0;
        }
        float scaled = Math.min(1, (magnitude - STICK_DEADZONE) / (1 - STICK_DEADZONE));
        return (short) Math.round(Math.copySign(scaled, axis) * Short.MAX_VALUE);
    }

    private static boolean pressed(byte[] buttons, int button) {
        return buttons[button] == GLFW_PRESS;
    }

    private static int bit(boolean on, int retroId) {
        return on ? 1 << retroId : 0;
    }
}
