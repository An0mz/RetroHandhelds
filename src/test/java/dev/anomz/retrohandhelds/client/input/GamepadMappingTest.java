package dev.anomz.retrohandhelds.client.input;

import static dev.anomz.retrohandhelds.client.libretro.LibretroConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.glfw.GLFW.*;

import org.junit.jupiter.api.Test;

class GamepadMappingTest {
    private static final float DEADZONE = 0.35f;

    /** Builds a pad state with the given buttons held; sticks centred and triggers at rest (-1). */
    private static int map(boolean positional, float leftX, float leftY, float leftTrigger, float rightTrigger, int... held) {
        byte[] buttons = new byte[GLFW_GAMEPAD_BUTTON_LAST + 1];
        for (int button : held) {
            buttons[button] = GLFW_PRESS;
        }
        float[] axes = new float[GLFW_GAMEPAD_AXIS_LAST + 1];
        axes[GLFW_GAMEPAD_AXIS_LEFT_X] = leftX;
        axes[GLFW_GAMEPAD_AXIS_LEFT_Y] = leftY;
        axes[GLFW_GAMEPAD_AXIS_LEFT_TRIGGER] = leftTrigger;
        axes[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER] = rightTrigger;
        return GamepadMapping.toRetroMask(buttons, axes, DEADZONE, positional);
    }

    private static int map(int... held) {
        return map(false, 0, 0, -1, -1, held);
    }

    private static int bits(int... retroIds) {
        int mask = 0;
        for (int id : retroIds) {
            mask |= 1 << id;
        }
        return mask;
    }

    @Test
    void idlePadPressesNothing() {
        assertEquals(0, map());
    }

    @Test
    void labelLayoutMatchesPadLabels() {
        assertEquals(bits(DEVICE_ID_JOYPAD_A), map(GLFW_GAMEPAD_BUTTON_A));
        assertEquals(bits(DEVICE_ID_JOYPAD_B), map(GLFW_GAMEPAD_BUTTON_B));
    }

    @Test
    void positionalLayoutUsesNintendoPositions() {
        assertEquals(bits(DEVICE_ID_JOYPAD_B), map(true, 0, 0, -1, -1, GLFW_GAMEPAD_BUTTON_A));
        assertEquals(bits(DEVICE_ID_JOYPAD_A), map(true, 0, 0, -1, -1, GLFW_GAMEPAD_BUTTON_B));
    }

    @Test
    void shouldersFromBumpersOrTriggers() {
        assertEquals(bits(DEVICE_ID_JOYPAD_L, DEVICE_ID_JOYPAD_R),
                map(GLFW_GAMEPAD_BUTTON_LEFT_BUMPER, GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER));
        assertEquals(bits(DEVICE_ID_JOYPAD_L, DEVICE_ID_JOYPAD_R), map(false, 0, 0, 1, 1));
        assertEquals(0, map(false, 0, 0, 0, 0), "half-pressed triggers do not count");
    }

    @Test
    void discLayoutUsesPositionsForAllFaceButtons() {
        byte[] buttons = new byte[GLFW_GAMEPAD_BUTTON_LAST + 1];
        float[] axes = new float[GLFW_GAMEPAD_AXIS_LAST + 1];
        axes[GLFW_GAMEPAD_AXIS_LEFT_TRIGGER] = -1;
        axes[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER] = -1;
        buttons[GLFW_GAMEPAD_BUTTON_A] = GLFW_PRESS;
        buttons[GLFW_GAMEPAD_BUTTON_B] = GLFW_PRESS;
        buttons[GLFW_GAMEPAD_BUTTON_X] = GLFW_PRESS;
        buttons[GLFW_GAMEPAD_BUTTON_Y] = GLFW_PRESS;
        assertEquals(bits(DEVICE_ID_JOYPAD_B, DEVICE_ID_JOYPAD_A, DEVICE_ID_JOYPAD_Y, DEVICE_ID_JOYPAD_X),
                GamepadMapping.toRetroMask(buttons, axes, DEADZONE, false, true));
    }

    @Test
    void discLayoutTriggersAreL2AndR2() {
        byte[] buttons = new byte[GLFW_GAMEPAD_BUTTON_LAST + 1];
        float[] axes = new float[GLFW_GAMEPAD_AXIS_LAST + 1];
        axes[GLFW_GAMEPAD_AXIS_LEFT_TRIGGER] = 1;
        axes[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER] = 1;
        assertEquals(bits(DEVICE_ID_JOYPAD_L2, DEVICE_ID_JOYPAD_R2),
                GamepadMapping.toRetroMask(buttons, axes, DEADZONE, false, true));
        buttons[GLFW_GAMEPAD_BUTTON_LEFT_BUMPER] = GLFW_PRESS;
        assertEquals(bits(DEVICE_ID_JOYPAD_L, DEVICE_ID_JOYPAD_L2, DEVICE_ID_JOYPAD_R2),
                GamepadMapping.toRetroMask(buttons, axes, DEADZONE, false, true));
    }

    @Test
    void startAndSelect() {
        assertEquals(bits(DEVICE_ID_JOYPAD_START, DEVICE_ID_JOYPAD_SELECT),
                map(GLFW_GAMEPAD_BUTTON_START, GLFW_GAMEPAD_BUTTON_BACK));
    }

    @Test
    void dpadButtons() {
        assertEquals(bits(DEVICE_ID_JOYPAD_UP, DEVICE_ID_JOYPAD_LEFT),
                map(GLFW_GAMEPAD_BUTTON_DPAD_UP, GLFW_GAMEPAD_BUTTON_DPAD_LEFT));
    }

    @Test
    void leftStickRespectsDeadzone() {
        assertEquals(0, map(false, 0.2f, -0.3f, -1, -1), "small wobble");
        assertEquals(bits(DEVICE_ID_JOYPAD_UP, DEVICE_ID_JOYPAD_RIGHT), map(false, 0.7f, -0.9f, -1, -1));
        assertEquals(bits(DEVICE_ID_JOYPAD_DOWN, DEVICE_ID_JOYPAD_LEFT), map(false, -0.5f, 0.5f, -1, -1));
    }
}
