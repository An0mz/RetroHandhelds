package dev.anomz.retrohandhelds.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.anomz.retrohandhelds.RetroHandhelds;
import net.neoforged.fml.loading.FMLPaths;

/** Folders under {@code <minecraft folder>/retro_handhelds/}. Each getter creates its folder if missing. */
public final class RetroPaths {
    private RetroPaths() {
    }

    public static Path root() {
        return ensure(FMLPaths.GAMEDIR.get().resolve("retro_handhelds"));
    }

    public static Path roms() {
        return ensure(root().resolve("roms"));
    }

    public static Path natives() {
        return ensure(root().resolve("natives"));
    }

    /** Where players can put extra libretro cores; one found here wins over the bundled core. */
    public static Path cores() {
        return ensure(root().resolve("cores"));
    }

    public static Path saves() {
        return ensure(root().resolve("saves"));
    }

    /** Where the core looks for an optional BIOS. Never populated by the mod. */
    public static Path system() {
        return ensure(root().resolve("system"));
    }

    /** Every console's key bindings and mouse look settings. */
    public static Path controls() {
        return root().resolve("controls.json");
    }

    public static Path debugFrame() {
        return root().resolve("debug_frame.png");
    }

    private static Path ensure(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            RetroHandhelds.LOGGER.error("Could not create folder {}", dir, e);
        }
        return dir;
    }
}
