package dev.anomz.retrohandhelds.client.input;

import static dev.anomz.retrohandhelds.client.libretro.LibretroConstants.*;
import static org.lwjgl.glfw.GLFW.*;

/** Turns a GLFW gamepad state (standard Xbox-style layout) into a libretro joypad bitmask. */
public final class GamepadMapping {
    private static final float TRIGGER_THRESHOLD = 0.5f;

    private GamepadMapping() {
    }

    /**
     * @param buttons GLFW_PRESS (1) or GLFW_RELEASE (0) per {@code GLFW_GAMEPAD_BUTTON_*}
     * @param axes values per {@code GLFW_GAMEPAD_AXIS_*}: sticks -1..1 (y down), triggers -1..1
     * @param deadzone how far the left stick must move (0..1) to count as a D-pad press
     * @param positionalAB false: pad A/B/X/Y labels map to the same-named buttons. true: Nintendo
     *     positions, so the east button (Xbox B) is A, south (Xbox A) is B, north (Xbox Y) is X and
     *     west (Xbox X) is Y
     */
    public static int toRetroMask(byte[] buttons, float[] axes, float deadzone, boolean positionalAB) {
        int mask = 0;
        boolean south = pressed(buttons, GLFW_GAMEPAD_BUTTON_A);
        boolean east = pressed(buttons, GLFW_GAMEPAD_BUTTON_B);
        mask |= bit(positionalAB ? east : south, DEVICE_ID_JOYPAD_A);
        mask |= bit(positionalAB ? south : east, DEVICE_ID_JOYPAD_B);
        boolean west = pressed(buttons, GLFW_GAMEPAD_BUTTON_X);
        boolean north = pressed(buttons, GLFW_GAMEPAD_BUTTON_Y);
        mask |= bit(positionalAB ? north : west, DEVICE_ID_JOYPAD_X);
        mask |= bit(positionalAB ? west : north, DEVICE_ID_JOYPAD_Y);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_LEFT_BUMPER)
                || axes[GLFW_GAMEPAD_AXIS_LEFT_TRIGGER] > TRIGGER_THRESHOLD, DEVICE_ID_JOYPAD_L);
        mask |= bit(pressed(buttons, GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER)
                || axes[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER] > TRIGGER_THRESHOLD, DEVICE_ID_JOYPAD_R);
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

    private static boolean pressed(byte[] buttons, int button) {
        return buttons[button] == GLFW_PRESS;
    }

    private static int bit(boolean on, int retroId) {
        return on ? 1 << retroId : 0;
    }
}
