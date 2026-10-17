package dev.anomz.retrohandhelds.client.input;

import static org.lwjgl.glfw.GLFW.*;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFWGamepadState;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.RetroClientConfig;

/**
 * Reads the first connected gamepad through GLFW (bundled with Minecraft). GLFW requires the main
 * thread, so this is only called from the render thread. Allocation-free after the first call.
 */
public final class GamepadInput {
    private static final int BUTTON_COUNT = GLFW_GAMEPAD_BUTTON_LAST + 1;
    private static final int AXIS_COUNT = GLFW_GAMEPAD_AXIS_LAST + 1;

    private static GLFWGamepadState state;
    private static final byte[] buttons = new byte[BUTTON_COUNT];
    private static final float[] axes = new float[AXIS_COUNT];
    private static int joystick = -1;
    private static boolean errorLogged;

    private GamepadInput() {
    }

    /** The current libretro joypad bitmask from the gamepad, or 0 if none is connected or it is disabled. */
    public static int poll(boolean discLayout) {
        // A pad that went away must not leave its sticks pushed.
        Arrays.fill(axes, 0);
        if (!RetroClientConfig.gamepadEnabled()) {
            return 0;
        }
        try {
            if (joystick < 0 || !glfwJoystickIsGamepad(joystick)) {
                joystick = findGamepad();
                if (joystick < 0) {
                    return 0;
                }
            }
            if (state == null) {
                state = GLFWGamepadState.create();
            }
            if (!glfwGetGamepadState(joystick, state)) {
                joystick = -1;
                return 0;
            }
            for (int i = 0; i < BUTTON_COUNT; i++) {
                buttons[i] = state.buttons(i);
            }
            for (int i = 0; i < AXIS_COUNT; i++) {
                axes[i] = state.axes(i);
            }
            return GamepadMapping.toRetroMask(buttons, axes, RetroClientConfig.gamepadDeadzone(),
                    RetroClientConfig.gamepadPositionalAB(), discLayout);
        } catch (Throwable t) {
            if (!errorLogged) {
                errorLogged = true;
                RetroHandhelds.LOGGER.error("Retro Handhelds: could not read the gamepad", t);
            }
            return 0;
        }
    }

    /** One stick axis from the last {@link #poll}, as a libretro analog value (y down, like GLFW). */
    public static short stick(int glfwAxis) {
        return GamepadMapping.stickValue(axes[glfwAxis]);
    }

    /**
     * The first joystick query in a process makes GLFW enumerate input devices, which can take seconds
     * on Windows. Doing it here, on the main thread during the loading screen, keeps that pause out of
     * the game. Skipped when gamepads are disabled.
     */
    public static void warmUp() {
        if (!RetroClientConfig.gamepadEnabled()) {
            return;
        }
        long start = System.nanoTime();
        String name = connectedName();
        RetroHandhelds.LOGGER.info("Retro Handhelds: gamepad detection took {} ms ({})",
                (System.nanoTime() - start) / 1_000_000, name == null ? "no gamepad connected" : name);
    }

    /** Name of the gamepad that would be used, or null if none is connected (or gamepads are disabled). */
    @Nullable
    public static String connectedName() {
        if (!RetroClientConfig.gamepadEnabled()) {
            return null;
        }
        try {
            int found = findGamepad();
            return found < 0 ? null : glfwGetGamepadName(found);
        } catch (Throwable t) {
            return null;
        }
    }

    private static int findGamepad() {
        for (int jid = GLFW_JOYSTICK_1; jid <= GLFW_JOYSTICK_LAST; jid++) {
            if (glfwJoystickIsGamepad(jid)) {
                return jid;
            }
        }
        return -1;
    }
}
