package dev.anomz.retrohandhelds.client.emu;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import com.sun.jna.Pointer;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.libretro.LibretroConstants;
import dev.anomz.retrohandhelds.client.libretro.MgbaCore;

/**
 * Keeps the core's battery save RAM in sync with a {@code .sav} file. Emulator thread only.
 * The file is written only when the memory actually changed since the last write.
 */
final class SaveRam {
    private final MgbaCore core;
    private final Path file;
    private byte[] current = new byte[0];
    private byte[] written = new byte[0];
    private boolean errorLogged;

    SaveRam(MgbaCore core, Path file) {
        this.core = core;
        this.file = file;
    }

    /** Call right after {@code retro_load_game}, before the first frame. */
    void load() {
        try {
            Pointer memory = core.retro_get_memory_data(LibretroConstants.MEMORY_SAVE_RAM);
            int size = size();
            if (memory == null || size == 0) {
                return;
            }
            if (Files.isRegularFile(file)) {
                byte[] saved = Files.readAllBytes(file);
                memory.write(0, saved, 0, Math.min(saved.length, size));
                RetroHandhelds.LOGGER.info("Retro Handhelds: loaded save RAM from {}", file);
            }
            // Remember what is in memory now, so an untouched save is never written out.
            ensureCapacity(size);
            memory.read(0, written, 0, size);
        } catch (Throwable t) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: could not load save RAM from {}", file, t);
        }
    }

    /** Writes the file if save memory changed since the last write. */
    void flushIfChanged() {
        try {
            Pointer memory = core.retro_get_memory_data(LibretroConstants.MEMORY_SAVE_RAM);
            int size = size();
            if (memory == null || size == 0) {
                return;
            }
            ensureCapacity(size);
            memory.read(0, current, 0, size);
            if (Arrays.equals(current, 0, size, written, 0, size)) {
                return;
            }
            AtomicFiles.write(file, current, size);
            byte[] swap = written;
            written = current;
            current = swap;
        } catch (Throwable t) {
            if (!errorLogged) {
                errorLogged = true;
                RetroHandhelds.LOGGER.error("Retro Handhelds: could not write save RAM to {}", file, t);
            }
        }
    }

    private int size() {
        long size = core.retro_get_memory_size(LibretroConstants.MEMORY_SAVE_RAM);
        return size > 0 && size <= Integer.MAX_VALUE ? (int) size : 0;
    }

    // The size can change once the core detects the game's save type; reallocation is rare.
    private void ensureCapacity(int size) {
        if (current.length < size) {
            current = Arrays.copyOf(current, size);
        }
        if (written.length < size) {
            written = Arrays.copyOf(written, size);
        }
    }
}
