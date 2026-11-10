package dev.anomz.retrohandhelds.client.libretro;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.anomz.retrohandhelds.RetroSystem;
import dev.anomz.retrohandhelds.client.RetroPaths;

/**
 * Installs a core the player downloaded themselves (the N64's) into {@code retro_handhelds/cores/}:
 * finds the buildbot zip, or the bare library, in the cores folder or the player's Downloads folder,
 * and copies the library out of it. The mod never downloads anything; it only saves the player the
 * unzip and the move. Only the one library file of the expected name is ever written, so a zip can't
 * place files anywhere else.
 */
public final class CoreInstaller {
    /** Larger than any libretro core; a bigger entry isn't the core and isn't written. */
    static final long MAX_CORE_BYTES = 256L << 20;
    // Its own logger, not RetroHandhelds.LOGGER, so the tests run without NeoForge.
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreInstaller.class);

    public enum Status {
        /** The library was copied into the cores folder from {@link Outcome#source()}. */
        INSTALLED,
        /** No zip or library newer than the one already in the cores folder. */
        NOT_FOUND,
        /** A candidate was found but none could be installed; the log says why. */
        FAILED
    }

    public record Outcome(Status status, @Nullable Path source) {
    }

    private CoreInstaller() {
    }

    /**
     * For a system with no bundled core: installs its core from the cores or Downloads folder if a
     * download newer than the installed core is there.
     */
    public static Outcome installFor(RetroSystem system) {
        if (system.bundledCore() != null) {
            return new Outcome(Status.NOT_FOUND, null);
        }
        List<Path> searchDirs = new ArrayList<>();
        searchDirs.add(RetroPaths.cores());
        Path downloads = downloadsFolder();
        if (downloads != null) {
            searchDirs.add(downloads);
        }
        return install(NativeLoader.libraryFileName(system.dropInCores().getFirst()), RetroPaths.cores(), searchDirs);
    }

    /** The player's Downloads folder, or null if it isn't where it usually is. */
    @Nullable
    public static Path downloadsFolder() {
        String home = System.getProperty("user.home");
        if (home == null || home.isEmpty()) {
            return null;
        }
        Path downloads = Path.of(home, "Downloads");
        return Files.isDirectory(downloads) ? downloads : null;
    }

    /**
     * Installs library {@code fileName} into {@code coresDir} from the newest zip of it, or copy of it,
     * in {@code searchDirs}, if that is newer than the library already there. A browser's renamed
     * duplicates ({@code name (1).zip}, {@code name(2).zip}) count too.
     */
    public static Outcome install(String fileName, Path coresDir, List<Path> searchDirs) {
        Path target = coresDir.resolve(fileName);
        FileTime installed = modified(target);
        List<Path> candidates = new ArrayList<>();
        Pattern pattern = Pattern.compile(Pattern.quote(fileName) + "(\\s*\\(\\d+\\))?(\\.zip)?", Pattern.CASE_INSENSITIVE);
        for (Path dir : searchDirs) {
            try (Stream<Path> files = Files.list(dir)) {
                files.filter(file -> pattern.matcher(file.getFileName().toString()).matches())
                        .filter(file -> !file.equals(target) && Files.isRegularFile(file))
                        .filter(file -> installed == null || modified(file) != null && modified(file).compareTo(installed) > 0)
                        .forEach(candidates::add);
            } catch (IOException e) {
                LOGGER.warn("Retro Handhelds: could not look for {} in {}", fileName, dir, e);
            }
        }
        candidates.sort(Comparator.comparing(CoreInstaller::modified, Comparator.nullsLast(Comparator.reverseOrder())));
        for (Path candidate : candidates) {
            try {
                boolean zip = candidate.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
                if (zip ? extract(candidate, fileName, target) : copy(candidate, target)) {
                    LOGGER.info("Retro Handhelds: installed {} from {}", target, candidate);
                    return new Outcome(Status.INSTALLED, candidate);
                }
                LOGGER.warn("Retro Handhelds: {} has no usable {} in it", candidate, fileName);
            } catch (IOException e) {
                LOGGER.warn("Retro Handhelds: could not install {} from {}", fileName, candidate, e);
            }
        }
        return new Outcome(candidates.isEmpty() ? Status.NOT_FOUND : Status.FAILED, null);
    }

    /** Copies the entry named {@code fileName} (in any folder of the zip) to {@code target}. */
    private static boolean extract(Path zip, String fileName, Path target) throws IOException {
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            for (ZipEntry entry; (entry = in.getNextEntry()) != null;) {
                String name = entry.getName();
                String base = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
                if (!entry.isDirectory() && base.equalsIgnoreCase(fileName)) {
                    return write(in, target);
                }
            }
        }
        return false;
    }

    private static boolean copy(Path source, Path target) throws IOException {
        if (Files.size(source) > MAX_CORE_BYTES) {
            return false;
        }
        try (InputStream in = Files.newInputStream(source)) {
            return write(in, target);
        }
    }

    /**
     * Writes at most {@link #MAX_CORE_BYTES} to a temporary file next to {@code target}, then moves it
     * into place, so a failed or oversized copy never leaves a broken core behind.
     */
    private static boolean write(InputStream in, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), "install", ".tmp");
        try {
            long total = 0;
            byte[] buffer = new byte[64 * 1024];
            try (OutputStream out = Files.newOutputStream(tmp)) {
                for (int read; (read = in.read(buffer)) > 0;) {
                    total += read;
                    if (total > MAX_CORE_BYTES) {
                        return false;
                    }
                    out.write(buffer, 0, read);
                }
            }
            if (total == 0) {
                return false;
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /**
     * On macOS, lets a core the player put in the cores folder load: a file downloaded with a browser
     * or unzipped with Archive Utility is quarantined, and macOS refuses to load it. Same as the
     * {@code xattr -d com.apple.quarantine} the README used to ask for. Does nothing elsewhere.
     */
    static void clearQuarantine(Path file) {
        if (!"macos".equals(NativeLoader.osKey())) {
            return;
        }
        try {
            Process process = new ProcessBuilder("xattr", "-d", "com.apple.quarantine", file.toAbsolutePath().toString())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroy();
            }
        } catch (IOException e) {
            LOGGER.warn("Retro Handhelds: could not clear the quarantine flag of {}", file, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Nullable
    private static FileTime modified(Path file) {
        try {
            return Files.getLastModifiedTime(file);
        } catch (IOException e) {
            return null;
        }
    }
}
