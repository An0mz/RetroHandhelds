package dev.anomz.retrohandhelds.client.emu;

import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.jna.Memory;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.libretro.LibretroCore;

/** One save-state slot backed by a file. Emulator thread only, between frames. */
final class SaveStates {
    private final LibretroCore core;
    private final Path file;
    private Memory buffer;

    SaveStates(LibretroCore core, Path file) {
        this.core = core;
        this.file = file;
    }

    StateResult save() {
        try {
            long size = core.retro_serialize_size();
            if (size <= 0 || size > Integer.MAX_VALUE) {
                return StateResult.FAILED;
            }
            Memory memory = buffer((int) size);
            if (core.retro_serialize(memory, size) == 0) {
                return StateResult.FAILED;
            }
            byte[] bytes = memory.getByteArray(0, (int) size);
            AtomicFiles.write(file, bytes, bytes.length);
            return StateResult.SAVED;
        } catch (Throwable t) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: could not save state to {}", file, t);
            return StateResult.FAILED;
        }
    }

    StateResult load() {
        try {
            if (!Files.isRegularFile(file)) {
                return StateResult.NO_STATE;
            }
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length == 0) {
                return StateResult.FAILED;
            }
            Memory memory = buffer(bytes.length);
            memory.write(0, bytes, 0, bytes.length);
            return core.retro_unserialize(memory, bytes.length) != 0 ? StateResult.LOADED : StateResult.FAILED;
        } catch (Throwable t) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: could not load state from {}", file, t);
            return StateResult.FAILED;
        }
    }

    private Memory buffer(int size) {
        if (buffer == null || buffer.size() < size) {
            buffer = new Memory(size);
        }
        return buffer;
    }
}
