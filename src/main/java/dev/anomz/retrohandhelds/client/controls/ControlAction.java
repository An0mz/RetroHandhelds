package dev.anomz.retrohandhelds.client.controls;

import static dev.anomz.retrohandhelds.client.libretro.LibretroConstants.*;

import dev.anomz.retrohandhelds.client.emu.JoypadState;

/**
 * Everything a key can do in a game: press a RetroPad button, or tilt an analog stick all the way.
 * What a button is called depends on the console ({@link ControlPreset} labels it).
 */
public enum ControlAction {
    UP(DEVICE_ID_JOYPAD_UP),
    DOWN(DEVICE_ID_JOYPAD_DOWN),
    LEFT(DEVICE_ID_JOYPAD_LEFT),
    RIGHT(DEVICE_ID_JOYPAD_RIGHT),
    A(DEVICE_ID_JOYPAD_A),
    B(DEVICE_ID_JOYPAD_B),
    X(DEVICE_ID_JOYPAD_X),
    Y(DEVICE_ID_JOYPAD_Y),
    L(DEVICE_ID_JOYPAD_L),
    R(DEVICE_ID_JOYPAD_R),
    L2(DEVICE_ID_JOYPAD_L2),
    R2(DEVICE_ID_JOYPAD_R2),
    L3(DEVICE_ID_JOYPAD_L3),
    R3(DEVICE_ID_JOYPAD_R3),
    START(DEVICE_ID_JOYPAD_START),
    SELECT(DEVICE_ID_JOYPAD_SELECT),
    LEFT_STICK_UP(-1, JoypadState.STICK_UP),
    LEFT_STICK_DOWN(-1, JoypadState.STICK_DOWN),
    LEFT_STICK_LEFT(-1, JoypadState.STICK_LEFT),
    LEFT_STICK_RIGHT(-1, JoypadState.STICK_RIGHT),
    RIGHT_STICK_UP(-1, JoypadState.RIGHT_STICK + JoypadState.STICK_UP),
    RIGHT_STICK_DOWN(-1, JoypadState.RIGHT_STICK + JoypadState.STICK_DOWN),
    RIGHT_STICK_LEFT(-1, JoypadState.RIGHT_STICK + JoypadState.STICK_LEFT),
    RIGHT_STICK_RIGHT(-1, JoypadState.RIGHT_STICK + JoypadState.STICK_RIGHT);

    private final int retroId;
    private final int stickKey;

    ControlAction(int retroId) {
        this(retroId, -1);
    }

    ControlAction(int retroId, int stickKey) {
        this.retroId = retroId;
        this.stickKey = stickKey;
    }

    /** Presses or releases this control. */
    public void apply(JoypadState joypad, boolean down) {
        if (retroId >= 0) {
            joypad.set(retroId, down);
        } else {
            joypad.setStickKey(stickKey, down);
        }
    }
}
