package dev.anomz.retrohandhelds.client.libretro;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CoreInstallerTest {
    private static final String CORE = "parallel_n64_libretro.dll";
    private static final byte[] LIBRARY = {1, 2, 3, 4};

    @TempDir
    Path temp;
    private Path cores;
    private Path downloads;

    @BeforeEach
    void folders() throws IOException {
        cores = Files.createDirectories(temp.resolve("cores"));
        downloads = Files.createDirectories(temp.resolve("Downloads"));
    }

    private CoreInstaller.Outcome install() {
        return CoreInstaller.install(CORE, cores, List.of(cores, downloads));
    }

    private static Path zip(Path file, String entry, byte[] data) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(file))) {
            out.putNextEntry(new ZipEntry(entry));
            out.write(data);
            out.closeEntry();
        }
        return file;
    }

    @Test
    void installsFromTheBuildbotZipInDownloads() throws IOException {
        Path zip = zip(downloads.resolve(CORE + ".zip"), CORE, LIBRARY);
        CoreInstaller.Outcome outcome = install();
        assertEquals(CoreInstaller.Status.INSTALLED, outcome.status());
        assertEquals(zip, outcome.source());
        assertArrayEquals(LIBRARY, Files.readAllBytes(cores.resolve(CORE)));
    }

    @Test
    void findsBrowserRenamedDuplicatesAndPicksTheNewest() throws IOException {
        Path old = zip(downloads.resolve(CORE + ".zip"), CORE, new byte[] {9});
        Path newer = zip(downloads.resolve(CORE + " (1).zip"), CORE, LIBRARY);
        Files.setLastModifiedTime(old, FileTime.fromMillis(1_000_000));
        Files.setLastModifiedTime(newer, FileTime.fromMillis(2_000_000));
        assertEquals(newer, install().source());
        assertArrayEquals(LIBRARY, Files.readAllBytes(cores.resolve(CORE)));
    }

    @Test
    void installsFromAZipInTheCoresFolderOrALooseLibrary() throws IOException {
        zip(cores.resolve(CORE + ".zip"), "some/folder/" + CORE, LIBRARY);
        assertEquals(CoreInstaller.Status.INSTALLED, install().status());

        Files.delete(cores.resolve(CORE));
        Files.delete(cores.resolve(CORE + ".zip"));
        Files.write(downloads.resolve(CORE), LIBRARY);
        assertEquals(CoreInstaller.Status.INSTALLED, install().status());
        assertArrayEquals(LIBRARY, Files.readAllBytes(cores.resolve(CORE)));
    }

    @Test
    void leavesAnInstalledCoreAloneUnlessTheDownloadIsNewer() throws IOException {
        Path zip = zip(downloads.resolve(CORE + ".zip"), CORE, LIBRARY);
        Files.write(cores.resolve(CORE), new byte[] {7});
        Files.setLastModifiedTime(zip, FileTime.fromMillis(1_000_000));
        Files.setLastModifiedTime(cores.resolve(CORE), FileTime.fromMillis(2_000_000));
        assertEquals(CoreInstaller.Status.NOT_FOUND, install().status());
        assertArrayEquals(new byte[] {7}, Files.readAllBytes(cores.resolve(CORE)));

        Files.setLastModifiedTime(zip, FileTime.fromMillis(3_000_000));
        assertEquals(CoreInstaller.Status.INSTALLED, install().status());
        assertArrayEquals(LIBRARY, Files.readAllBytes(cores.resolve(CORE)));
    }

    @Test
    void writesOnlyTheCoreItselfWhateverTheZipHolds() throws IOException {
        Path zip = downloads.resolve(CORE + ".zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("../../evil.dll"));
            out.write(LIBRARY);
            out.putNextEntry(new ZipEntry("../" + CORE));
            out.write(LIBRARY);
        }
        assertEquals(CoreInstaller.Status.INSTALLED, install().status());
        assertFalse(Files.exists(temp.resolve("evil.dll")));
        assertFalse(Files.exists(temp.resolve(CORE)));
        try (var files = Files.list(cores)) {
            assertEquals(List.of(cores.resolve(CORE)), files.filter(f -> !f.toString().endsWith(".zip")).toList());
        }
    }

    @Test
    void reportsAZipWithoutTheCoreAsFailedAndWritesNothing() throws IOException {
        zip(downloads.resolve(CORE + ".zip"), "readme.txt", LIBRARY);
        assertEquals(CoreInstaller.Status.FAILED, install().status());
        assertFalse(Files.exists(cores.resolve(CORE)));
    }

    @Test
    void rejectsAnOversizedEntryWithoutLeavingAPartialCore() throws IOException {
        Path zip = downloads.resolve(CORE + ".zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.setLevel(9);
            out.putNextEntry(new ZipEntry(CORE));
            byte[] chunk = new byte[1 << 20];
            for (long written = 0; written <= CoreInstaller.MAX_CORE_BYTES; written += chunk.length) {
                out.write(chunk);
            }
        }
        assertEquals(CoreInstaller.Status.FAILED, install().status());
        try (var files = Files.list(cores)) {
            assertTrue(files.findAny().isEmpty());
        }
    }

    @Test
    void ignoresOtherFiles() throws IOException {
        zip(downloads.resolve("snes9x_libretro.dll.zip"), "snes9x_libretro.dll", LIBRARY);
        Files.write(downloads.resolve("notes.txt"), LIBRARY);
        try (OutputStream out = Files.newOutputStream(downloads.resolve("x" + CORE + ".zip"))) {
            out.write(LIBRARY);
        }
        assertEquals(CoreInstaller.Status.NOT_FOUND, install().status());
    }
}
