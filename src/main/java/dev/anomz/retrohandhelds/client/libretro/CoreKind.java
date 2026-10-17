package dev.anomz.retrohandhelds.client.libretro;

import dev.anomz.retrohandhelds.RomNames;

/** The bundled libretro cores. Which one runs a game depends only on its file extension. */
public enum CoreKind {
    /** mGBA: GBA, Game Boy and Game Boy Color. Up to 256x224 (Super Game Boy borders). */
    MGBA("mgba_libretro", 256, 224, false),
    /** PCSX ReARMed: PS1 discs, read from disk by the core itself. Up to 1024x512. */
    PCSX("pcsx_rearmed_libretro", 1024, 512, true);

    private final String library;
    private final int maxWidth;
    private final int maxHeight;
    private final boolean readsFromDisk;

    CoreKind(String library, int maxWidth, int maxHeight, boolean readsFromDisk) {
        this.library = library;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.readsFromDisk = readsFromDisk;
    }

    public static CoreKind forRom(String fileName) {
        return RomNames.isDisc(fileName) ? PCSX : MGBA;
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
}
