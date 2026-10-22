package dev.anomz.retrohandhelds.client.emu;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Memory;

/**
 * The core options this mod sets: mGBA's two player settings, plus fixed values per system (such as
 * ParaLLEl-N64's software renderer). Any thread may call {@link #set}; the emulator thread answers
 * the core's GET_VARIABLE / GET_VARIABLE_UPDATE requests from it. Unlisted options keep the core's defaults.
 */
public final class CoreOptions {
    static final String COLOR_CORRECTION = "mgba_color_correction";
    static final String INTERFRAME_BLENDING = "mgba_interframe_blending";

    // Native strings handed to the core; they must outlive every call that returns them.
    private static final Memory OFF = nativeString("OFF");
    private static final Memory AUTO = nativeString("Auto");
    private static final Memory LCD_GHOSTING = nativeString("lcd_ghosting");

    /** Fixed values, as native strings built once; they must outlive every call that returns them. */
    private final Map<String, Memory> fixed;
    private volatile boolean colorCorrection;
    private volatile boolean lcdGhosting;
    private volatile boolean changed;

    public CoreOptions(boolean colorCorrection, boolean lcdGhosting) {
        this(colorCorrection, lcdGhosting, Map.of());
    }

    /** @param fixed option values that never change while the game runs, by option key */
    public CoreOptions(boolean colorCorrection, boolean lcdGhosting, Map<String, String> fixed) {
        Map<String, Memory> strings = new HashMap<>();
        fixed.forEach((key, value) -> strings.put(key, nativeString(value)));
        this.fixed = Map.copyOf(strings);
        this.colorCorrection = colorCorrection;
        this.lcdGhosting = lcdGhosting;
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
