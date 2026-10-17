package dev.anomz.retrohandhelds.client.emu;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Memory;

/**
 * The core options this mod sets: mGBA's display options, which the player can change while playing
 * (any thread may call {@link #set}), plus fixed values for other cores, set before the game starts.
 * The emulator thread answers the core's GET_VARIABLE / GET_VARIABLE_UPDATE requests from it.
 * Unlisted options keep the core's defaults.
 */
public final class CoreOptions {
    static final String COLOR_CORRECTION = "mgba_color_correction";
    static final String INTERFRAME_BLENDING = "mgba_interframe_blending";

    // Native strings handed to the core; they must outlive every call that returns them.
    private static final Memory OFF = nativeString("OFF");
    private static final Memory AUTO = nativeString("Auto");
    private static final Memory LCD_GHOSTING = nativeString("lcd_ghosting");

    private volatile boolean colorCorrection;
    private volatile boolean lcdGhosting;
    private volatile boolean changed;
    private final Map<String, Memory> fixed = new HashMap<>();

    public CoreOptions(boolean colorCorrection, boolean lcdGhosting) {
        this.colorCorrection = colorCorrection;
        this.lcdGhosting = lcdGhosting;
    }

    /** Answers {@code key} with {@code value}. Call before the session starts. */
    public CoreOptions fixed(String key, String value) {
        fixed.put(key, nativeString(value));
        return this;
    }

    /** Applies new settings; the core picks them up on its next frame. */
    public void set(boolean colorCorrection, boolean lcdGhosting) {
        if (colorCorrection != this.colorCorrection || lcdGhosting != this.lcdGhosting) {
            this.colorCorrection = colorCorrection;
            this.lcdGhosting = lcdGhosting;
            changed = true;
        }
    }

    /** The value to report for {@code key}, or null to let the core use its default. */
    Memory valueFor(String key) {
        return switch (key) {
            // "Auto" picks GBA or GBC correction to match the game.
            case COLOR_CORRECTION -> colorCorrection ? AUTO : OFF;
            case INTERFRAME_BLENDING -> lcdGhosting ? LCD_GHOSTING : OFF;
            default -> fixed.get(key);
        };
    }

    /** Whether settings changed since the last call; clears the flag. Called every frame, allocation-free. */
    boolean consumeChanged() {
        if (!changed) {
            return false;
        }
        changed = false;
        return true;
    }

    private static Memory nativeString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        Memory memory = new Memory(bytes.length + 1L);
        memory.write(0, bytes, 0, bytes.length);
        memory.setByte(bytes.length, (byte) 0);
        return memory;
    }
}
