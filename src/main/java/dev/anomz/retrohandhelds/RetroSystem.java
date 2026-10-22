package dev.anomz.retrohandhelds;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The game systems the mod can run, picked by ROM file extension. Used on both sides: the server
 * only accepts cartridges for a known system, and the client picks the core from it.
 *
 * <p>Cores built on libco (bsnes and its forks, Mupen64Plus-Next, melonDS) cannot be used: they call
 * back into Java from coroutine stacks the JVM does not know, which breaks its locking and stack
 * checks. Cores that need a GL or Vulkan context cannot be used either; ParaLLEl-N64 is told to use
 * its software renderer.
 */
public enum RetroSystem {
    GAME_BOY_ADVANCE("Game Boy Advance", List.of("gba", "gbc", "gb"), "mgba", List.of("mgba")),
    NES("NES", List.of("nes"), "fceumm", List.of("nestopia", "fceumm", "mesen")),
    SNES("SNES", List.of("sfc", "smc"), "mesen-s", List.of("snes9x", "mesen-s")),
    GENESIS("Genesis", List.of("md", "gen", "smd"), "blastem", List.of("genesis_plus_gx", "picodrive", "blastem")),
    SEGA_32X("32X", List.of("32x"), "blastem", List.of("picodrive", "blastem")),
    MASTER_SYSTEM("Master System / Game Gear", List.of("sms", "gg", "sg"), "gearsystem",
            List.of("genesis_plus_gx", "picodrive", "gearsystem")),
    N64("Nintendo 64", List.of("n64", "v64", "z64"), "parallel_n64", List.of("parallel_n64"),
            Controls.N64, 0,
            // Software rendering (no GL context here) with the high-level RSP, on all cores.
            Map.of("parallel-n64-gfxplugin", "angrylion",
                    "parallel-n64-rspplugin", "hle",
                    "parallel-n64-angrylion-multithread", "all threads"),
            List.of()),
    PLAYSTATION("PlayStation", List.of("cue", "chd", "m3u", "pbp"), "pcsx_rearmed",
            List.of("pcsx_rearmed", "mednafen_psx"),
            // RETRO_DEVICE_SUBCLASS(RETRO_DEVICE_ANALOG, 1): PCSX ReARMed's DualShock. Digital games
            // still work, since a DualShock starts in digital mode.
            Controls.DUALSHOCK, (1 + 1) << 8 | 5, Map.of(),
            List.of("scph5501.bin (optional: PCSX ReARMed has a built-in BIOS)")),
    NINTENDO_DS("Nintendo DS", List.of("nds"), "desmume", List.of("desmume"),
            Controls.TOUCH, 0,
            // Both screens stacked, touched with the mouse as an absolute pointer.
            Map.of("desmume_screens_layout", "top/bottom",
                    "desmume_pointer_mouse", "enabled",
                    "desmume_pointer_type", "touch"),
            List.of("bios7.bin, bios9.bin, firmware.bin (optional)")),
    PC_ENGINE("PC Engine / TurboGrafx-16", List.of("pce"), "mednafen_pce_fast",
            List.of("mednafen_pce", "mednafen_pce_fast")),
    NEO_GEO_POCKET("Neo Geo Pocket / Color", List.of("ngp", "ngc"), "mednafen_ngp", List.of("mednafen_ngp")),
    WONDERSWAN("WonderSwan / Color", List.of("ws", "wsc"), "mednafen_wswan", List.of("mednafen_wswan")),
    LYNX("Atari Lynx", List.of("lnx"), "handy", List.of("handy")),
    ATARI_2600("Atari 2600", List.of("a26"), "stella2014", List.of("stella", "stella2014")),
    ATARI_7800("Atari 7800", List.of("a78"), "prosystem", List.of("prosystem")),
    POKEMON_MINI("Pokemon Mini", List.of("min"), "pokemini", List.of("pokemini"));

    /** How the system is played, beyond the plain joypad every system has. */
    public enum Controls {
        BUTTONS(false, true, false),
        /** Control stick is the left stick, C-buttons the right one, Z is L2. */
        N64(true, false, true),
        DUALSHOCK(true, true, true),
        /** A touchscreen, played with the mouse in the full-screen view. */
        TOUCH(false, true, false);

        /** Sends the gamepad's sticks and the keyboard stick keys as analog sticks. */
        public final boolean analog;
        /** The gamepad's left stick also presses the D-pad. */
        public final boolean stickPressesDpad;
        /** The gamepad's triggers are L2/R2 instead of a second way to press L/R. */
        public final boolean triggersAreL2R2;

        Controls(boolean analog, boolean stickPressesDpad, boolean triggersAreL2R2) {
            this.analog = analog;
            this.stickPressesDpad = stickPressesDpad;
            this.triggersAreL2R2 = triggersAreL2R2;
        }

        public boolean touch() {
            return this == TOUCH;
        }
    }

    private static final List<RetroSystem> ALL = List.of(values());

    private final String displayName;
    private final List<String> extensions;
    private final String bundledCore;
    private final List<String> dropInCores;
    private final Controls controls;
    private final int controllerDevice;
    private final Map<String, String> coreOptions;
    private final List<String> biosFiles;

    RetroSystem(String displayName, List<String> extensions, String bundledCore, List<String> dropInCores) {
        this(displayName, extensions, bundledCore, dropInCores, Controls.BUTTONS, 0, Map.of(), List.of());
    }

    RetroSystem(String displayName, List<String> extensions, String bundledCore, List<String> dropInCores,
            Controls controls, int controllerDevice, Map<String, String> coreOptions, List<String> biosFiles) {
        this.displayName = displayName;
        this.extensions = extensions;
        this.bundledCore = bundledCore;
        this.dropInCores = dropInCores;
        this.controls = controls;
        this.controllerDevice = controllerDevice;
        this.coreOptions = coreOptions;
        this.biosFiles = biosFiles;
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

    public Controls controls() {
        return controls;
    }

    /** The libretro device to plug into port 1, or 0 to leave the core's default. */
    public int controllerDevice() {
        return controllerDevice;
    }

    /** Core option values this mod sets; keys are prefixed by core, so one map serves every core of the system. */
    public Map<String, String> coreOptions() {
        return coreOptions;
    }

    /**
     * Whether the picture is drawn smoothed (linear filtering) instead of with sharp pixels: 3D
     * systems looked soft on the TVs they were made for, and their low resolutions look blocky blown up.
     */
    public boolean smoothPicture() {
        return this == N64 || this == PLAYSTATION;
    }

    /** BIOS files the player may put in the system folder, for messages when a game does not start. */
    public List<String> biosFiles() {
        return biosFiles;
    }
}
