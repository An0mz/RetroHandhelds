package dev.anomz.retrohandhelds;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The game systems the mod can run, picked by ROM file extension. Used on both sides: the server
 * only accepts cartridges for a known system, and the client picks the core from it.
 */
public enum RetroSystem {
    GAME_BOY_ADVANCE("Game Boy Advance", List.of("gba", "gbc", "gb"), "mgba", List.of("mgba")),
    NES("NES", List.of("nes"), "fceumm", List.of("nestopia", "fceumm", "mesen")),
    SNES("SNES", List.of("sfc", "smc"), "bsnes", List.of("snes9x", "bsnes")),
    GENESIS("Genesis", List.of("md", "gen", "smd"), "blastem", List.of("genesis_plus_gx", "picodrive", "blastem")),
    MASTER_SYSTEM("Master System / Game Gear", List.of("sms", "gg"), "gearsystem",
            List.of("genesis_plus_gx", "picodrive", "gearsystem"));

    private static final List<RetroSystem> ALL = List.of(values());

    private final String displayName;
    private final List<String> extensions;
    private final String bundledCore;
    private final List<String> dropInCores;

    RetroSystem(String displayName, List<String> extensions, String bundledCore, List<String> dropInCores) {
        this.displayName = displayName;
        this.extensions = extensions;
        this.bundledCore = bundledCore;
        this.dropInCores = dropInCores;
    }

    /** The system a ROM file belongs to, from its extension. */
    public static Optional<RetroSystem> forFileName(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return Optional.empty();
        }
        String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        for (RetroSystem system : ALL) {
            if (system.extensions.contains(extension)) {
                return Optional.of(system);
            }
        }
        return Optional.empty();
    }

    public String displayName() {
        return displayName;
    }

    public List<String> extensions() {
        return extensions;
    }

    /** Base name of the core shipped in the jar ({@code <name>_libretro.<ext>}). */
    public String bundledCore() {
        return bundledCore;
    }

    /** Base names of cores to look for in the cores folder, most preferred first. */
    public List<String> dropInCores() {
        return dropInCores;
    }
}
