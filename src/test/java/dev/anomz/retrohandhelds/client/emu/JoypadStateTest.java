package dev.anomz.retrohandhelds.client.emu;

import static dev.anomz.retrohandhelds.client.libretro.LibretroConstants.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JoypadStateTest {
    private static final short MAX = JoypadState.AXIS_MAX;

    private static JoypadState.Snapshot snapshot(JoypadState state) {
        JoypadState.Snapshot out = new JoypadState.Snapshot();
        state.snapshot(out);
        return out;
    }

    @Test
    void keyboardAndPadButtonsCombine() {
        JoypadState state = new JoypadState();
        state.set(DEVICE_ID_JOYPAD_A, true);
        state.setPad(1 << DEVICE_ID_JOYPAD_L2);
        assertEquals(1 << DEVICE_ID_JOYPAD_A | 1 << DEVICE_ID_JOYPAD_L2, snapshot(state).buttons);
    }

    @Test
    void padAxesComeThroughSigned() {
        JoypadState state = new JoypadState();
        state.setPadAxes(-MAX, 1200, 0, MAX);
        assertArrayEquals(new short[] {-MAX, 1200, 0, MAX}, snapshot(state).axes);
    }

    @Test
    void stickKeysTiltFullyAndCancelOut() {
        JoypadState state = new JoypadState();
        state.setStickKey(JoypadState.STICK_LEFT, true);
        state.setStickKey(JoypadState.RIGHT_STICK + JoypadState.STICK_DOWN, true);
        assertArrayEquals(new short[] {-MAX, 0, 0, MAX}, snapshot(state).axes);
        state.setStickKey(JoypadState.STICK_RIGHT, true);
        assertEquals(0, snapshot(state).axes[0], "left and right together cancel out");
    }

    @Test
    void mouseMovesTheRightStickWhenItIsTiltedFurthest() {
        JoypadState state = new JoypadState();
        state.setMouseStick(-9000, 4000);
        assertArrayEquals(new short[] {0, 0, -9000, 4000}, snapshot(state).axes);
        state.setPadAxes(0, 0, 20000, 0);
        assertArrayEquals(new short[] {0, 0, 20000, 0}, snapshot(state).axes, "the pad's stick is further out");
        state.setPadAxes(0, 0, 0, 0);
        state.setMouseStick(0, 0);
        state.setStickKey(JoypadState.RIGHT_STICK + JoypadState.STICK_UP, true);
        assertArrayEquals(new short[] {0, 0, 0, -MAX}, snapshot(state).axes);
    }

    @Test
    void pointerPositionAndTouch() {
        JoypadState state = new JoypadState();
        state.setPointer(JoypadState.toPointer(0), JoypadState.toPointer(1), true);
        JoypadState.Snapshot out = snapshot(state);
        assertEquals(-0x7FFF, out.pointerX);
        assertEquals(0x7FFF, out.pointerY);
        assertTrue(out.pointerPressed);
        assertEquals(0, JoypadState.toPointer(0.5));
        assertEquals(-0x7FFF, JoypadState.toPointer(-3), "clamped");
        state.clear();
        assertFalse(snapshot(state).pointerPressed);
    }
}
