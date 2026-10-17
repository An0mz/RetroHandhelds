package dev.anomz.retrohandhelds.client.libretro;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Tells PS2 discs from PS1 discs: both systems use the same image formats, but a PS2 disc's
 * {@code SYSTEM.CNF} boots with {@code BOOT2}. Reads the ISO 9660 root directory of a plain
 * (2048-byte sectors) or raw (2352-byte sectors) image. Anything unreadable counts as not PS2.
 */
public final class DiscDetector {
    private static final int SECTOR = 2048;
    private static final int RAW_SECTOR = 2352;
    private static final int PRIMARY_VOLUME_DESCRIPTOR = 16;
    private static final int MAX_ROOT_DIRECTORY = 64 * 1024;
    private static final int MAX_SYSTEM_CNF = 4096;
    private static final byte[] RAW_SYNC = {0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0};

    private DiscDetector() {
    }

    public static boolean isPs2(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".cso") || name.endsWith(".zso")) {
                return true; // compressed formats only PS2 tools produce
            }
            if (name.endsWith(".cue") || name.endsWith(".m3u")) {
                Path first = firstListedFile(file);
                return first != null && isPs2(first);
            }
            if (name.endsWith(".iso") || name.endsWith(".bin") || name.endsWith(".img")) {
                try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
                    return isPs2Image(channel);
                }
            }
        } catch (IOException | RuntimeException e) {
            // Not a readable image: let the PS1 core have a go.
        }
        return false;
    }

    static boolean isPs2Image(FileChannel channel) throws IOException {
        byte[] head = read(channel, 0, 16);
        boolean raw = head != null && Arrays.equals(head, 0, RAW_SYNC.length, RAW_SYNC, 0, RAW_SYNC.length);
        // Raw sectors: 12 sync bytes, 4 header bytes (the last one is the mode), then data;
        // mode 2 sectors add an 8-byte subheader.
        int dataOffset = raw ? (head[15] == 2 ? 24 : 16) : 0;
        Image image = new Image(channel, raw ? RAW_SECTOR : SECTOR, dataOffset);

        byte[] pvd = image.sector(PRIMARY_VOLUME_DESCRIPTOR);
        if (pvd == null || pvd[0] != 1 || !"CD001".equals(new String(pvd, 1, 5, StandardCharsets.US_ASCII))) {
            return false;
        }
        int rootLba = le32(pvd, 156 + 2);
        int rootSize = Math.min(le32(pvd, 156 + 10), MAX_ROOT_DIRECTORY);
        byte[] root = image.extent(rootLba, rootSize);
        if (root == null) {
            return false;
        }
        for (int i = 0; i < root.length; ) {
            int length = root[i] & 0xFF;
            if (length == 0) {
                // Records never cross a sector boundary; the rest of this sector is padding.
                i = (i / SECTOR + 1) * SECTOR;
                continue;
            }
            if (i + 33 > root.length || i + length > root.length) {
                break;
            }
            int nameLength = root[i + 32] & 0xFF;
            String entry = new String(root, i + 33, Math.min(nameLength, length - 33), StandardCharsets.US_ASCII);
            if (entry.toUpperCase(Locale.ROOT).startsWith("SYSTEM.CNF")) {
                byte[] cnf = image.extent(le32(root, i + 2), Math.min(le32(root, i + 10), MAX_SYSTEM_CNF));
                return cnf != null && new String(cnf, StandardCharsets.US_ASCII).contains("BOOT2");
            }
            i += length;
        }
        return false;
    }

    private static Path firstListedFile(Path listFile) throws IOException {
        if (Files.size(listFile) > 64 * 1024) {
            return null;
        }
        boolean cue = listFile.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".cue");
        List<String> lines = Files.readAllLines(listFile, StandardCharsets.UTF_8);
        for (String line : lines) {
            String entry = line.strip();
            if (cue) {
                if (!entry.regionMatches(true, 0, "FILE ", 0, 5)) {
                    continue;
                }
                int open = entry.indexOf('"');
                int close = entry.lastIndexOf('"');
                entry = open >= 0 && close > open ? entry.substring(open + 1, close) : entry.split("\\s+")[1];
            }
            if (!entry.isEmpty() && !entry.startsWith("#")) {
                // Only files next to the list: never follow a path elsewhere.
                Path target = listFile.resolveSibling(Path.of(entry).getFileName());
                return Files.isRegularFile(target) ? target : null;
            }
        }
        return null;
    }

    private static int le32(byte[] b, int offset) {
        return ByteBuffer.wrap(b, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static byte[] read(FileChannel channel, long position, int length) throws IOException {
        if (position < 0 || position + length > channel.size()) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(length);
        while (buffer.hasRemaining()) {
            if (channel.read(buffer, position + buffer.position()) < 0) {
                return null;
            }
        }
        return buffer.array();
    }

    private record Image(FileChannel channel, int sectorSize, int dataOffset) {
        byte[] sector(int lba) throws IOException {
            return read(channel, (long) lba * sectorSize + dataOffset, SECTOR);
        }

        /** {@code size} bytes of user data starting at sector {@code lba}. */
        byte[] extent(int lba, int size) throws IOException {
            if (lba < 0 || size <= 0) {
                return null;
            }
            byte[] out = new byte[size];
            for (int done = 0; done < size; done += SECTOR) {
                byte[] sector = sector(lba + done / SECTOR);
                if (sector == null) {
                    return null;
                }
                System.arraycopy(sector, 0, out, done, Math.min(SECTOR, size - done));
            }
            return out;
        }
    }
}
