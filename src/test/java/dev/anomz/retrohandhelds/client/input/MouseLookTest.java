package dev.anomz.retrohandhelds.client.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.anomz.retrohandhelds.client.emu.JoypadState;
import dev.anomz.retrohandhelds.client.input.MouseLook.Mode;

class MouseLookTest {
    private static final double FRAME = 1 / 60.0;
    private static final long FRAME_NANOS = 16_666_667L;
    private static final int MAX = JoypadState.AXIS_MAX;

    /** Moves the mouse at {@code speed} pixels per second for {@code frames} frames. */
    private static MouseLook moved(double speed, int frames) {
        MouseLook look = new MouseLook();
        for (int i = 0; i < frames; i++) {
            look.update(speed * FRAME, 0, FRAME);
        }
        return look;
    }

    @Test
    void steadyMovementSettlesOnItsSpeed() {
        MouseLook look = moved(MouseLook.FULL_TILT_SPEED / 2, 60);
        assertEquals(MAX / 2, look.stickX(Mode.STICK, 1), 50);
        assertEquals(0, look.stickY(Mode.STICK, 1));
    }

    @Test
    void fastMovementIsClampedToFullTilt() {
        assertEquals(-MAX, moved(-MouseLook.FULL_TILT_SPEED * 5, 60).stickX(Mode.STICK, 1));
    }

    @Test
    void sensitivityScales() {
        MouseLook look = moved(MouseLook.FULL_TILT_SPEED / 4, 60);
        assertEquals(look.stickX(Mode.STICK, 1) * 2, look.stickX(Mode.STICK, 2), 2);
    }

    @Test
    void stickReturnsToCentreWhenTheMouseStops() {
        MouseLook look = moved(MouseLook.FULL_TILT_SPEED, 30);
        for (int i = 0; i < 30; i++) {
            look.update(0, 0, FRAME);
        }
        assertTrue(Math.abs(look.stickX(Mode.STICK, 1)) < 100, "back near centre within half a second");
        look.reset();
        assertEquals(0, look.stickX(Mode.STICK, 1));
    }

    @Test
    void smallMovementMakesNoTap() {
        MouseLook look = new MouseLook();
        look.updateTaps(MouseLook.TAP_PIXELS / 2, 0, 0, 1);
        assertEquals(0, look.stickX(Mode.TAPS, 1));
    }

    @Test
    void aFlickIsOneShortFullTap() {
        MouseLook look = new MouseLook();
        long now = 0;
        look.updateTaps(-MouseLook.TAP_PIXELS * 1.2, 0, now, 1);
        assertEquals(-MAX, look.stickX(Mode.TAPS, 1), "tapping left");
        int pressedFrames = 0;
        for (int i = 0; i < 60; i++) {
            now += FRAME_NANOS;
            look.updateTaps(0, 0, now, 1);
            if (look.stickX(Mode.TAPS, 1) != 0) {
                pressedFrames++;
            }
        }
        assertTrue(pressedFrames >= 3 && pressedFrames <= 6, "one tap of a few frames, then released: " + pressedFrames);
    }

    @Test
    void aLongSwipeQueuesAtMostTwoTapsWithReleasesBetween() {
        MouseLook look = new MouseLook();
        long now = 0;
        look.updateTaps(MouseLook.TAP_PIXELS * 10, 0, now, 1);
        int taps = 0;
        boolean wasPressed = false;
        for (int i = 0; i < 120; i++) {
            boolean pressed = look.stickX(Mode.TAPS, 1) != 0;
            if (pressed && !wasPressed) {
                taps++;
            }
            wasPressed = pressed;
            now += FRAME_NANOS;
            look.updateTaps(0, 0, now, 1);
        }
        assertEquals(2, taps);
    }

    @Test
    void sensitivityShortensTheDistanceForATap() {
        MouseLook look = new MouseLook();
        look.updateTaps(MouseLook.TAP_PIXELS * 0.6, 0, 0, 2);
        assertEquals(MAX, look.stickX(Mode.TAPS, 2));
    }

    @Test
    void verticalTapsWhenThereIsNoHorizontalOne() {
        MouseLook look = new MouseLook();
        look.updateTaps(0, MouseLook.TAP_PIXELS, 0, 1);
        assertEquals(0, look.stickX(Mode.TAPS, 1));
        assertEquals(MAX, look.stickY(Mode.TAPS, 1));
    }
}
