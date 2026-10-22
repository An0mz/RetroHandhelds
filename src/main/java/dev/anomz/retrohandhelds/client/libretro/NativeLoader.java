package dev.anomz.retrohandhelds.client.libretro;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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
    /** A loaded core, the base name it was found under ({@code mgba}, {@code snes9x}, ...) and its file. */
    public record LoadedCore(LibretroCore api, String name, Path file) {
    }

    /**
     * Cores that cannot run a second game in the same process: Mupen64Plus keeps global state that
     * retro_deinit does not reset (ParaLLEl-N64 never runs again after its first game stops). Each
     * session gets its own copy of the library instead. Old copies stay loaded, since the core's
     * worker threads may outlive retro_deinit; their files are deleted on the next start.
     */
    private static final Set<String> ONE_GAME_PER_LOAD = Set.of("parallel_n64");
    /** Libraries a session has already run on. */
    private static final Set<Path> USED = new HashSet<>();

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
     * A core ready for a new session: the shared one, or a fresh copy for cores that can only run
     * one game per load.
     */
    public static synchronized Optional<LoadedCore> loadForSession(RetroSystem system) {
        Optional<LoadedCore> loaded = load(system);
        if (loaded.isEmpty() || !ONE_GAME_PER_LOAD.contains(loaded.get().name()) || USED.add(loaded.get().file())) {
            return loaded;
        }
        LoadedCore shared = loaded.get();
        try {
            Path dir = Files.createDirectories(RetroPaths.natives().resolve("instances"));
            String fileName = shared.file().getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            Path copy = Files.createTempFile(dir, fileName.substring(0, dot) + "-", fileName.substring(dot));
            Files.copy(shared.file(), copy, StandardCopyOption.REPLACE_EXISTING);
            LibretroCore core = Native.load(copy.toAbsolutePath().toString(), LibretroCore.class);
            USED.add(copy);
            RetroHandhelds.LOGGER.info("Retro Handhelds: fresh copy of the {} core for this game: {}", shared.name(), copy);
            return Optional.of(new LoadedCore(core, shared.name(), copy));
        } catch (Throwable t) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: could not load a fresh copy of the {} core", shared.name(), t);
            return Optional.empty();
        }
    }

    /** Deletes the core copies of earlier runs. Nothing has loaded them yet in this process. */
    private static synchronized void deleteOldCopies() {
        Path dir = RetroPaths.natives().resolve("instances");
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(file -> {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    // Still loaded by another game instance sharing this folder; try again next time.
                }
            });
        } catch (IOException e) {
            RetroHandhelds.LOGGER.warn("Retro Handhelds: could not clean up {}", dir, e);
        }
    }

    /**
     * Loads the GBA core on a background thread during startup. Reading the library from the jar and
     * loading it takes long enough to freeze the game for a moment if it first happens on a click.
     * A click while this is still running simply waits for it ({@link #load} is synchronized).
     */
    public static void preloadAsync() {
        Thread thread = new Thread(() -> {
            deleteOldCopies();
            load(RetroSystem.GAME_BOY_ADVANCE);
        }, "RetroHandhelds-CoreLoader");
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
                return new LoadedCore(core, name, file);
            } catch (Throwable t) {
                RetroHandhelds.LOGGER.error("Retro Handhelds: failed to load the core {}", file, t);
            }
        }
        if (system.bundledCore() == null) {
            RetroHandhelds.LOGGER.warn("Retro Handhelds: no {} core in {} (none is bundled; looked for {})",
                    system.displayName(), RetroPaths.cores(), system.dropInCores());
            return null;
        }
        try {
            Path nativesDir = RetroPaths.natives();
            Path file = extractBundled(nativesDir, system.bundledCore());
            LibretroCore core = Native.load(file.toAbsolutePath().toString(), LibretroCore.class);
            RetroHandhelds.LOGGER.info("Loaded bundled {} core {} (libretro API {}) into {}", system.displayName(),
                    system.bundledCore(), core.retro_api_version(), nativesDir);
            return new LoadedCore(core, system.bundledCore(), file);
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
        return Native.load(extractBundled(nativesDir, coreName).toAbsolutePath().toString(), LibretroCore.class);
    }

    /** Extracts this platform's bundled core {@code <coreName>_libretro} and returns where it is. */
    private static Path extractBundled(Path nativesDir, String coreName) throws IOException {
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
            return extract(in.readAllBytes(), nativesDir.resolve(platform).resolve(fileName));
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

    /** The file name of core {@code coreName} on this computer, e.g. {@code parallel_n64_libretro.dll}. */
    public static String libraryFileName(String coreName) {
        return libraryFileName(osKey(), coreName);
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
