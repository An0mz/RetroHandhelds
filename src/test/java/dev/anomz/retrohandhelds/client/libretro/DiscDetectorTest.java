package dev.anomz.retrohandhelds.client.libretro;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiscDetectorTest {
    private static final int SECTOR = 2048;
    private static final int ROOT_LBA = 20;
    private static final int CNF_LBA = 21;

    @TempDir
    Path dir;

    @Test
    void ps2IsoBootsWithBoot2() throws IOException {
        assertTrue(DiscDetector.isPs2(write("game.iso", image("BOOT2 = cdrom0:\\SLUS_203.12;1\r\nVER = 1.00\r\n"))));
    }

    @Test
    void ps1IsoIsNotPs2() throws IOException {
        assertFalse(DiscDetector.isPs2(write("game.iso", image("BOOT = cdrom:\\SLUS_000.67;1\r\nTCB = 4\r\n"))));
    }

    @Test
    void rawCdSectorsAreRead() throws IOException {
        // A PS2 game on CD, dumped as raw mode 2 sectors (2352 bytes, data at 24).
        assertTrue(DiscDetector.isPs2(write("game.bin", raw(image("BOOT2 = cdrom0:\\SLUS_200.01;1\r\n"), 2, 24))));
        assertFalse(DiscDetector.isPs2(write("ps1.bin", raw(image("BOOT = cdrom:\\SCUS_944.26;1\r\n"), 2, 24))));
    }

    @Test
    void cueSheetChecksItsTrack() throws IOException {
        write("game.bin", raw(image("BOOT2 = cdrom0:\\SLUS_200.01;1\r\n"), 2, 24));
        Path cue = write("game.cue", "FILE \"game.bin\" BINARY\r\n  TRACK 01 MODE2/2352\r\n".getBytes(StandardCharsets.US_ASCII));
        assertTrue(DiscDetector.isPs2(cue));
    }

    @Test
    void compressedPs2FormatsArePs2AndJunkIsNot() throws IOException {
        assertTrue(DiscDetector.isPs2(write("game.cso", new byte[16])));
        assertFalse(DiscDetector.isPs2(write("junk.iso", new byte[100])));
        assertFalse(DiscDetector.isPs2(dir.resolve("missing.iso")));
    }

    private Path write(String name, byte[] bytes) throws IOException {
        return Files.write(dir.resolve(name), bytes);
    }

    /** A minimal ISO 9660 image: volume descriptor at 16, root directory at 20, SYSTEM.CNF at 21. */
    private static byte[] image(String systemCnf) {
        byte[] cnf = systemCnf.getBytes(StandardCharsets.US_ASCII);
        ByteBuffer iso = ByteBuffer.allocate(SECTOR * (CNF_LBA + 1)).order(ByteOrder.LITTLE_ENDIAN);

        int pvd = 16 * SECTOR;
        iso.put(pvd, (byte) 1);
        iso.put(pvd + 1, "CD001".getBytes(StandardCharsets.US_ASCII));
        iso.putInt(pvd + 156 + 2, ROOT_LBA);
        iso.putInt(pvd + 156 + 10, SECTOR);

        int root = ROOT_LBA * SECTOR;
        root = record(iso, root, ".", ROOT_LBA, SECTOR);   // "." and ".." entries come first
        root = record(iso, root, "..", ROOT_LBA, SECTOR);
        record(iso, root, "SYSTEM.CNF;1", CNF_LBA, cnf.length);

        iso.put(CNF_LBA * SECTOR, cnf);
        return iso.array();
    }

    private static int record(ByteBuffer iso, int at, String name, int lba, int size) {
        byte[] bytes = name.getBytes(StandardCharsets.US_ASCII);
        int length = 33 + bytes.length + (bytes.length % 2 == 0 ? 1 : 0);
        iso.put(at, (byte) length);
        iso.putInt(at + 2, lba);
        iso.putInt(at + 10, size);
        iso.put(at + 32, (byte) bytes.length);
        iso.put(at + 33, bytes);
        return at + length;
    }

    /** The same image as raw CD sectors: sync, header with {@code mode}, data at {@code dataOffset}. */
    private static byte[] raw(byte[] image, int mode, int dataOffset) {
        int sectors = image.length / SECTOR;
        byte[] out = new byte[sectors * 2352];
        for (int s = 0; s < sectors; s++) {
            int base = s * 2352;
            for (int i = 1; i <= 10; i++) {
                out[base + i] = (byte) 0xFF;
            }
            out[base + 15] = (byte) mode;
            System.arraycopy(image, s * SECTOR, out, base + dataOffset, SECTOR);
        }
        return out;
    }
}
