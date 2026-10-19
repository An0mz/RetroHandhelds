package dev.anomz.retrohandhelds.client.libretro;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.sun.jna.Native;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.RetroSystem;
import dev.anomz.retrohandhelds.client.RetroPaths;

/**
 * Finds and loads the libretro core for a system. A core the player put in
 * {@code retro_handhelds/cores/} wins; otherwise the core bundled for this OS/arch is extracted from
 * {@code /natives/<os>-<arch>/} in the mod jar into {@code retro_handhelds/natives/} and loaded.
 * Any failure is logged and reported as empty.
 */
public final class NativeLoader {
    /** A loaded core and the base name it was found under ({@code mgba}, {@code snes9x}, ...). */
    public record LoadedCore(LibretroCore api, String name) {
    }

    // Only successes are kept, so a core dropped into the folder later is found on the next try.
    private static final Map<RetroSystem, LoadedCore> CACHE = new EnumMap<>(RetroSystem.class);

    private NativeLoader() {
    }

    public static synchronized Optional<LoadedCore> load(RetroSystem system) {
        LoadedCore cached = CACHE.get(system);
        if (cached == null) {
            long start = System.nanoTime();
            cached = tryLoad(system);
            RetroHandhelds.LOGGER.info("Retro Handhelds: {} core load took {} ms", system.displayName(),
                    (System.nanoTime() - start) / 1_000_000);
            if (cached != null) {
                CACHE.put(system, cached);
            }
        }
        return Optional.ofNullable(cached);
    }

    /**
     * Loads the GBA core on a background thread during startup. Reading the library from the jar and
     * loading it takes long enough to freeze the game for a moment if it first happens on a click.
     * A click while this is still running simply waits for it ({@link #load} is synchronized).
     */
    public static void preloadAsync() {
        Thread thread = new Thread(() -> load(RetroSystem.GAME_BOY_ADVANCE), "RetroHandhelds-CoreLoader");
        thread.setDaemon(true);
        thread.start();
    }

    private static LoadedCore tryLoad(RetroSystem system) {
        String os = osKey();
        for (String name : system.dropInCores()) {
            Path file = RetroPaths.cores().resolve(libraryFileName(os, name));
            if (!Files.isRegularFile(file)) {
                continue;
            }
            try {
                LibretroCore core = Native.load(file.toAbsolutePath().toString(), LibretroCore.class);
                RetroHandhelds.LOGGER.info("Loaded {} core {} (libretro API {}) from {}", system.displayName(), name,
                        core.retro_api_version(), file);
                return new LoadedCore(core, name);
            } catch (Throwable t) {
                RetroHandhelds.LOGGER.error("Retro Handhelds: failed to load the core {}", file, t);
            }
        }
        try {
            Path nativesDir = RetroPaths.natives();
            LibretroCore core = loadInto(nativesDir, system.bundledCore());
            RetroHandhelds.LOGGER.info("Loaded bundled {} core {} (libretro API {}) into {}", system.displayName(),
                    system.bundledCore(), core.retro_api_version(), nativesDir);
            return new LoadedCore(core, system.bundledCore());
        } catch (Throwable t) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: failed to load the bundled {} core", system.displayName(), t);
            return null;
        }
    }

    /**
     * Extracts this platform's bundled core {@code <coreName>_libretro} from the jar into
     * {@code nativesDir/<os>-<arch>/} and loads it. Public so tests can check the bundled cores on
     * every platform without Minecraft.
     *
     * @throws UnsupportedOperationException if no core is bundled for this OS/architecture
     * @throws IOException if the core is missing from the jar or cannot be written
     */
    public static LibretroCore loadInto(Path nativesDir, String coreName) throws IOException {
        String os = osKey();
        String arch = archKey();
        if (os == null || arch == null) {
            throw new UnsupportedOperationException("No bundled cores for this platform ("
                    + System.getProperty("os.name") + " / " + System.getProperty("os.arch") + ")");
        }
        String platform = os + "-" + arch;
        String fileName = libraryFileName(os, coreName);
        String resource = "/natives/" + platform + "/" + fileName;
        try (InputStream in = NativeLoader.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Native core " + resource + " is missing from the mod jar");
            }
            Path target = extract(in.readAllBytes(), nativesDir.resolve(platform).resolve(fileName));
            return Native.load(target.toAbsolutePath().toString(), LibretroCore.class);
        }
    }

    private static Path extract(byte[] bytes, Path target) throws IOException {
        if (Files.isRegularFile(target) && Arrays.equals(bytes, Files.readAllBytes(target))) {
            return target;
        }
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), "core", ".tmp");
        try {
            Files.write(tmp, bytes);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
        return target;
    }

    private static String osKey() {
        String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (name.startsWith("windows")) return "windows";
        if (name.startsWith("mac") || name.contains("darwin")) return "macos";
        if (name.contains("linux")) return "linux";
        return null;
    }

    private static String archKey() {
        return switch (System.getProperty("os.arch", "").toLowerCase(Locale.ROOT)) {
            case "amd64", "x86_64" -> "x64";
            case "aarch64", "arm64" -> "arm64";
            default -> null;
        };
    }

    /** {@code mgba} becomes {@code mgba_libretro.dll} on Windows, {@code .dylib} on macOS, {@code .so} elsewhere. */
    private static String libraryFileName(String os, String coreName) {
        String extension = os == null ? "so" : switch (os) {
            case "windows" -> "dll";
            case "macos" -> "dylib";
            default -> "so";
        };
        return coreName + "_libretro." + extension;
    }
}
