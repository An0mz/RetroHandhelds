package dev.anomz.retrohandhelds;

import java.util.Locale;

/** ROM file names as stored on cartridges. Checked on both sides, since the name arrives from a client. */
public final class RomNames {
    public static final int MAX_LENGTH = 128;

    private RomNames() {
    }

    /** A bare file name in the ROM folder: no separators, no hidden files, a known extension. */
    public static boolean isValid(String name) {
        return name != null
                && !name.isEmpty()
                && name.length() <= MAX_LENGTH
                && name.indexOf('/') < 0
                && name.indexOf('\\') < 0
                && name.indexOf(':') < 0
                && !name.startsWith(".")
                && hasRomExtension(name);
    }

    public static boolean hasRomExtension(String name) {
        return isCartridge(name) || isDisc(name);
    }

    /** GBA, Game Boy and Game Boy Color games. */
    public static boolean isCartridge(String name) {
        return name.toLowerCase(Locale.ROOT).matches(".*\\.(gba|gbc|gb)$");
    }

    /** Disc images (PS1, PS2): plain images, cue sheets, compressed images and multi-disc playlists. */
    public static boolean isDisc(String name) {
        return name.toLowerCase(Locale.ROOT).matches(".*\\.(iso|cue|bin|img|chd|pbp|m3u|cso|zso)$");
    }

    /** "Some Game (USA).gba" becomes "Some Game (USA)". */
    public static String displayName(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
