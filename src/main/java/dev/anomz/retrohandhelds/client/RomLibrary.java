package dev.anomz.retrohandhelds.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import dev.anomz.retrohandhelds.RomNames;

/** The player's local ROM folder. */
public final class RomLibrary {
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

    public static List<String> list() {
        try (Stream<Path> files = Files.list(folder())) {
            return files.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(RomNames::isValid)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
