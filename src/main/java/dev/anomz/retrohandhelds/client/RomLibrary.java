package dev.anomz.retrohandhelds.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import dev.anomz.retrohandhelds.RomNames;

/** The player's local ROM folder. */
public final class RomLibrary {
    /** A CD holds at most about 900 MB; bigger images are DVD games (PS2 and later), which can't run. */
    private static final long MAX_CD_IMAGE_SIZE = 1_000_000_000L;
    /** Cue sheets and playlists are small text files; anything bigger isn't one. */
    private static final long MAX_LIST_FILE_SIZE = 64 * 1024;

    private RomLibrary() {
    }

    public static Path folder() {
        return RetroPaths.roms().toAbsolutePath().normalize();
    }

    /** The ROM with this file name, if it exists in the ROM folder. */
    public static Optional<Path> resolve(String name) {
        if (!RomNames.isValid(name)) {
            return Optional.empty();
        }
        Path folder = folder();
        try {
            Path rom = folder.resolve(name).normalize();
            // Belt and braces on top of RomNames: never leave the ROM folder.
            return rom.startsWith(folder) && Files.isRegularFile(rom) ? Optional.of(rom) : Optional.empty();
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    /** Whether {@code rom} is an uncompressed disc image too big for a CD, so not a PS1 game. */
    public static boolean isDvdImage(Path rom) {
        String name = rom.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            return (name.endsWith(".iso") || name.endsWith(".bin") || name.endsWith(".img"))
                    && Files.size(rom) > MAX_CD_IMAGE_SIZE;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Games to choose from. Files that a cue sheet (disc tracks) or an m3u playlist (the discs of a
     * multi-disc game) refers to are left out, so each game appears once.
     */
    public static List<String> list() {
        try (Stream<Path> files = Files.list(folder())) {
            List<Path> roms = files.filter(Files::isRegularFile)
                    .filter(p -> RomNames.isValid(p.getFileName().toString()))
                    .toList();
            Set<String> referenced = new HashSet<>();
            for (Path rom : roms) {
                String name = rom.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.endsWith(".cue") || name.endsWith(".m3u")) {
                    referenced.addAll(referencedFiles(rom));
                }
            }
            return roms.stream()
                    .map(p -> p.getFileName().toString())
                    .filter(name -> !referenced.contains(name.toLowerCase(Locale.ROOT)))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Lower-case file names a cue sheet ({@code FILE "x.bin" BINARY}) or m3u playlist (one per line) uses. */
    private static Set<String> referencedFiles(Path listFile) {
        Set<String> names = new HashSet<>();
        try {
            if (Files.size(listFile) > MAX_LIST_FILE_SIZE) {
                return names;
            }
            boolean cue = listFile.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".cue");
            for (String line : Files.readAllLines(listFile, StandardCharsets.UTF_8)) {
                String entry = cue ? cueFileName(line.strip()) : line.strip();
                if (entry != null && !entry.isEmpty() && !entry.startsWith("#")) {
                    names.add(Path.of(entry).getFileName().toString().toLowerCase(Locale.ROOT));
                }
            }
        } catch (IOException | RuntimeException e) {
            // Unreadable or not text: just list everything.
        }
        return names;
    }

    private static String cueFileName(String line) {
        if (!line.regionMatches(true, 0, "FILE ", 0, 5)) {
            return null;
        }
        int open = line.indexOf('"');
        int close = line.lastIndexOf('"');
        if (open >= 0 && close > open) {
            return line.substring(open + 1, close);
        }
        String[] parts = line.split("\\s+");
        return parts.length >= 2 ? parts[1] : null;
    }
}
