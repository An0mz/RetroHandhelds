package dev.anomz.retrohandhelds.client.libretro;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * JNA binding for the libretro API every core exports (mgba_libretro, fceumm_libretro, ...).
 *
 * <p>Type mapping: C {@code unsigned} is {@code int}; {@code size_t} is {@code long} (only 64-bit
 * platforms are supported); C99 {@code bool} is {@code byte}, because JNA's {@code boolean} reads a
 * full int and the upper bytes of a bool return register are not guaranteed to be zero.
 */
public interface LibretroCore extends Library {
    interface EnvironmentCallback extends Callback {
        byte callback(int cmd, Pointer data);
    }

    interface VideoRefreshCallback extends Callback {
        void callback(Pointer data, int width, int height, long pitch);
    }

    interface AudioSampleCallback extends Callback {
        void callback(short left, short right);
    }

    interface AudioSampleBatchCallback extends Callback {
        long callback(Pointer data, long frames);
    }

    interface InputPollCallback extends Callback {
        void callback();
    }

    interface InputStateCallback extends Callback {
        short callback(int port, int device, int index, int id);
    }

    /**
     * {@code retro_log_printf_t}, which is variadic: only the fixed level and format arguments are
     * read, which every supported calling convention passes the same way as for a plain function.
     */
    interface LogCallback extends Callback {
        void callback(int level, Pointer format);
    }

    int retro_api_version();

    void retro_set_environment(EnvironmentCallback callback);

    void retro_set_video_refresh(VideoRefreshCallback callback);

    void retro_set_audio_sample(AudioSampleCallback callback);

    void retro_set_audio_sample_batch(AudioSampleBatchCallback callback);

    void retro_set_input_poll(InputPollCallback callback);

    void retro_set_input_state(InputStateCallback callback);

    void retro_init();

    void retro_deinit();

    byte retro_load_game(RetroGameInfo game);

    void retro_unload_game();

    void retro_get_system_info(RetroSystemInfo info);

    void retro_set_controller_port_device(int port, int device);

    void retro_get_system_av_info(RetroSystemAvInfo info);

    void retro_reset();

    void retro_run();

    long retro_serialize_size();

    byte retro_serialize(Pointer data, long size);

    byte retro_unserialize(Pointer data, long size);

    Pointer retro_get_memory_data(int id);

    long retro_get_memory_size(int id);
}
