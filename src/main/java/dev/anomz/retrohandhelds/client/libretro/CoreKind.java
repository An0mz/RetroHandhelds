package dev.anomz.retrohandhelds.client.libretro;

import java.nio.file.Path;

import dev.anomz.retrohandhelds.RomNames;

/** The bundled libretro cores. Which one runs a game depends on its file (see {@link #forRom}). */
public enum CoreKind {
    /** mGBA: GBA, Game Boy and Game Boy Color. Up to 256x224 (Super Game Boy borders). */
    MGBA("mgba_libretro", 256, 224, false, true),
    /** PCSX ReARMed: PS1 discs, read from disk by the core itself. Up to 1024x512. */
    PCSX("pcsx_rearmed_libretro", 1024, 512, true, true),
    /**
     * LRPS2: PS2 discs, run with its all-CPU software renderer (no graphics context needed).
     * Needs the player's own BIOS. Big, so only loaded when a PS2 game starts.
     */
    LRPS2("pcsx2_libretro", 1280, 1024, true, false);

    private final String library;
    private final int maxWidth;
    private final int maxHeight;
    private final boolean readsFromDisk;
    private final boolean preload;

    CoreKind(String library, int maxWidth, int maxHeight, boolean readsFromDisk, boolean preload) {
        this.library = library;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.readsFromDisk = readsFromDisk;
        this.preload = preload;
    }

    /** Cartridges go to mGBA; discs to LRPS2 if they boot like a PS2 disc, else to PCSX ReARMed. */
    public static CoreKind forRom(Path rom) {
        String name = rom.getFileName().toString();
        if (!RomNames.isDisc(name)) {
            return MGBA;
        }
        return DiscDetector.isPs2(rom) ? LRPS2 : PCSX;
    }

    /** Library file name without the platform suffix. */
    public String library() {
        return library;
    }

    /** Largest picture the core produces. */
    public int maxWidth() {
        return maxWidth;
    }

    public int maxHeight() {
        return maxHeight;
    }

    /**
     * Whether the core opens the game file itself (libretro "need_fullpath"). Discs are hundreds of
     * megabytes and come with companion files, so they are never read into memory.
     */
    public boolean readsFromDisk() {
        return readsFromDisk;
    }

    /** Whether to load the library in the background at startup rather than on first use. */
    public boolean preload() {
        return preload;
    }

    /** Disc systems: four face buttons, L2/R2 and (PS2) analog sticks. */
    public boolean discLayout() {
        return this != MGBA;
    }
}
