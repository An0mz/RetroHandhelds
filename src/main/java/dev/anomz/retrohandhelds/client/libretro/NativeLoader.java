package dev.anomz.retrohandhelds.client.libretro;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import com.sun.jna.Native;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.RetroPaths;

/**
 * Extracts the mGBA core for this OS/arch from {@code /natives/<os>-<arch>/} in the mod jar into
 * {@code retro_handhelds/natives/} and loads it. Any failure is logged and reported as empty.
 */
public final class NativeLoader {
    private static Optional<MgbaCore> cached;

    private NativeLoader() {
    }

    public static synchronized Optional<MgbaCore> load() {
        if (cached == null) {
            long start = System.nanoTime();
            cached = Optional.ofNullable(tryLoad());
            RetroHandhelds.LOGGER.info("Retro Handhelds: native core load took {} ms", (System.nanoTime() - start) / 1_000_000);
        }
        return cached;
    }

    /**
     * Loads the core on a background thread during startup. Reading the library from the jar and
     * loading it takes long enough to freeze the game for a moment if it first happens on a click.
     * A click while this is still running simply waits for it ({@link #load} is synchronized).
     */
    public static void preloadAsync() {
        Thread thread = new Thread(NativeLoader::load, "RetroHandhelds-CoreLoader");
        thread.setDaemon(true);
        thread.start();
    }

    private static MgbaCore tryLoad() {
        String os = osKey();
        String arch = archKey();
        if (os == null || arch == null) {
            RetroHandhelds.LOGGER.error("Retro Handhelds has no mGBA core for this platform ({} / {})",
                    System.getProperty("os.name"), System.getProperty("os.arch"));
            return null;
        }
        String platform = os + "-" + arch;
        String resource = "/natives/" + platform + "/" + libraryFileName(os);
        try (InputStream in = NativeLoader.class.getResourceAsStream(resource)) {
            if (in == null) {
                RetroHandhelds.LOGGER.error("Retro Handhelds: native core {} is missing from the mod jar", resource);
                return null;
            }
            Path target = extract(in.readAllBytes(), RetroPaths.natives().resolve(platform).resolve(libraryFileName(os)));
            MgbaCore core = Native.load(target.toAbsolutePath().toString(), MgbaCore.class);
            RetroHandhelds.LOGGER.info("Loaded mGBA core (libretro API {}) from {}", core.retro_api_version(), target);
            return core;
        } catch (Throwable t) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: failed to load native core {}", resource, t);
            return null;
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

    private static String libraryFileName(String os) {
        return switch (os) {
            case "windows" -> "mgba_libretro.dll";
            case "macos" -> "mgba_libretro.dylib";
            default -> "mgba_libretro.so";
        };
    }
}
