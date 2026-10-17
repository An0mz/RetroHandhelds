package dev.anomz.retrohandhelds.client.libretro;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The player's own PS2 BIOS in {@code system/pcsx2/bios/}, where LRPS2 looks. The mod never ships
 * or downloads one. LRPS2 checks the contents itself; this only picks a likely file to name in
 * the {@code pcsx2_bios} option, which the core needs answered to boot.
 */
public final class Lrps2Bios {
    private static final long MIN_SIZE = 4L * 1024 * 1024;
    private static final long MAX_SIZE = 8L * 1024 * 1024;

    private Lrps2Bios() {
    }

    public static Path folder(Path systemDir) {
        return systemDir.resolve("pcsx2").resolve("bios");
    }

    /** File name of the BIOS to use: the first {@code .bin} of BIOS size, else any file of BIOS size. */
    public static Optional<String> find(Path systemDir) {
        Path folder = folder(systemDir);
        if (!Files.isDirectory(folder)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(Lrps2Bios::biosSized)
                    .map(p -> p.getFileName().toString())
                    .min(Comparator.comparing((String n) -> !n.toLowerCase(Locale.ROOT).endsWith(".bin"))
                            .thenComparing(String.CASE_INSENSITIVE_ORDER));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static boolean biosSized(Path file) {
        try {
            long size = Files.size(file);
            return Files.isRegularFile(file) && size >= MIN_SIZE && size <= MAX_SIZE;
        } catch (IOException e) {
            return false;
        }
    }
}
