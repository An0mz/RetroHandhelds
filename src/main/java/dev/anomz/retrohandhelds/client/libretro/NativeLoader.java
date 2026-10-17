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
import dev.anomz.retrohandhelds.client.RetroPaths;

/**
 * Extracts a core for this OS/arch from {@code /natives/<os>-<arch>/} in the mod jar into
 * {@code retro_handhelds/natives/} and loads it. Any failure is logged and reported as empty.
 */
public final class NativeLoader {
    private static final Map<CoreKind, Optional<LibretroCore>> CACHED = new EnumMap<>(CoreKind.class);

    private NativeLoader() {
    }

    public static synchronized Optional<LibretroCore> load(CoreKind kind) {
        Optional<LibretroCore> cached = CACHED.get(kind);
        if (cached == null) {
            long start = System.nanoTime();
            cached = Optional.ofNullable(tryLoad(kind));
            CACHED.put(kind, cached);
            RetroHandhelds.LOGGER.info("Retro Handhelds: {} load took {} ms", kind.library(), (System.nanoTime() - start) / 1_000_000);
        }
        return cached;
    }

    /**
     * Loads the cores on a background thread during startup. Reading a library from the jar and
     * loading it takes long enough to freeze the game for a moment if it first happens on a click.
     * A click while this is still running simply waits for it ({@link #load} is synchronized).
     */
    public static void preloadAsync() {
        Thread thread = new Thread(() -> {
            for (CoreKind kind : CoreKind.values()) {
                if (kind.preload()) {
                    load(kind);
                }
            }
        }, "RetroHandhelds-CoreLoader");
        thread.setDaemon(true);
        thread.start();
    }

    private static LibretroCore tryLoad(CoreKind kind) {
        try {
            Path nativesDir = RetroPaths.natives();
            LibretroCore core = loadInto(nativesDir, kind);
            RetroHandhelds.LOGGER.info("Loaded {} (libretro API {}) into {}", kind.library(), core.retro_api_version(), nativesDir);
            return core;
        } catch (Throwable t) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: failed to load the native core {}", kind.library(), t);
            return null;
        }
    }

    /**
     * Extracts this platform's core from the jar into {@code nativesDir/<os>-<arch>/} and loads it.
     * Public so tests can check the bundled cores on every platform without Minecraft.
     *
     * @throws UnsupportedOperationException if no core is bundled for this OS/architecture
     * @throws IOException if the core is missing from the jar or cannot be written
     */
    public static LibretroCore loadInto(Path nativesDir, CoreKind kind) throws IOException {
        String os = osKey();
        String arch = archKey();
        if (os == null || arch == null) {
            throw new UnsupportedOperationException("No " + kind.library() + " for this platform ("
                    + System.getProperty("os.name") + " / " + System.getProperty("os.arch") + ")");
        }
        String platform = os + "-" + arch;
        String fileName = kind.library() + librarySuffix(os);
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

    private static String librarySuffix(String os) {
        return switch (os) {
            case "windows" -> ".dll";
            case "macos" -> ".dylib";
            default -> ".so";
        };
    }
}
