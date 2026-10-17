package dev.anomz.retrohandhelds.client.share;

import dev.anomz.retrohandhelds.network.ScreenShare;

/**
 * Wire format for sharing a game's sound: mono, {@link #SAMPLE_RATE} Hz, IMA ADPCM (4 bits per sample).
 * Each packet carries {@link #PACKET_SAMPLES} samples and starts with the decoder state, so packets
 * decode independently and a lost one only leaves a short gap. About 11 KB/s.
 */
public final class AudioCodec {
    public static final int SAMPLE_RATE = 22050;
    /** About 93 ms per packet. Even, so it always fits the sample ring's stereo-pair rule. */
    public static final int PACKET_SAMPLES = ScreenShare.AUDIO_PACKET_SAMPLES;
    /** Header (2-byte predictor, 1-byte step index) plus two samples per byte. */
    public static final int PACKET_BYTES = ScreenShare.AUDIO_PACKET_BYTES;

    private static final int[] STEPS = {
            7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55, 60, 66, 73, 80,
            88, 97, 107, 118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449, 494, 544, 598,
            658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024, 3327,
            3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899, 15289,
            16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767};
    private static final int[] INDEX_ADJUST = {-1, -1, -1, -1, 2, 4, 6, 8};

    private AudioCodec() {
    }

    /**
     * Turns the emulator's interleaved stereo into mono at {@link #SAMPLE_RATE}, averaging the input
     * samples that fall into each output sample (a cheap low-pass that keeps aliasing down). Stateful.
     */
    public static final class Downmixer {
        private final double step;
        private double phase;
        private double sum;
        private int count;

        public Downmixer(int inputRate) {
            this.step = inputRate / (double) SAMPLE_RATE;
        }

        public void reset() {
            phase = 0;
            sum = 0;
            count = 0;
        }

        /** Converts {@code samples} interleaved stereo samples; returns how many mono samples went into {@code out}. */
        public int process(short[] stereo, int samples, short[] out, int outOffset) {
            int o = outOffset;
            for (int i = 0; i + 1 < samples; i += 2) {
                sum += (stereo[i] + stereo[i + 1]) * 0.5;
                count++;
                phase += 1;
                if (phase >= step) {
                    phase -= step;
                    out[o++] = clamp(Math.round(sum / count));
                    sum = 0;
                    count = 0;
                }
            }
            return o - outOffset;
        }
    }

    /** Encoder state carries over between packets, so consecutive packets join without clicks. */
    public static final class Encoder {
        private int predictor;
        private int index;

        public void reset() {
            predictor = 0;
            index = 0;
        }

        /** Encodes exactly {@link #PACKET_SAMPLES} samples from {@code pcm} into a new packet. */
        public byte[] encode(short[] pcm, int offset) {
            byte[] out = new byte[PACKET_BYTES];
            out[0] = (byte) predictor;
            out[1] = (byte) (predictor >> 8);
            out[2] = (byte) index;
            for (int i = 0; i < PACKET_SAMPLES; i++) {
                int nibble = encodeSample(pcm[offset + i]);
                if ((i & 1) == 0) {
                    out[3 + i / 2] = (byte) nibble;
                } else {
                    out[3 + i / 2] |= (byte) (nibble << 4);
                }
            }
            return out;
        }

        private int encodeSample(int sample) {
            int step = STEPS[index];
            int diff = sample - predictor;
            int nibble = 0;
            if (diff < 0) {
                nibble = 8;
                diff = -diff;
            }
            int delta = step >> 3;
            if (diff >= step) {
                nibble |= 4;
                diff -= step;
                delta += step;
            }
            if (diff >= step >> 1) {
                nibble |= 2;
                diff -= step >> 1;
                delta += step >> 1;
            }
            if (diff >= step >> 2) {
                nibble |= 1;
                delta += step >> 2;
            }
            predictor = clamp((nibble & 8) != 0 ? predictor - delta : predictor + delta);
            index = Math.clamp(index + INDEX_ADJUST[nibble & 7], 0, STEPS.length - 1);
            return nibble;
        }
    }

    /**
     * Decodes one packet into {@code out}. Returns false, writing nothing useful, if the packet is not
     * exactly the expected size or its header is invalid.
     */
    public static boolean decode(byte[] data, short[] out) {
        if (data.length != PACKET_BYTES || out.length < PACKET_SAMPLES) {
            return false;
        }
        int predictor = (short) ((data[0] & 0xFF) | (data[1] & 0xFF) << 8);
        int index = data[2] & 0xFF;
        if (index >= STEPS.length) {
            return false;
        }
        for (int i = 0; i < PACKET_SAMPLES; i++) {
            int b = data[3 + i / 2] & 0xFF;
            int nibble = (i & 1) == 0 ? b & 0x0F : b >> 4;
            int step = STEPS[index];
            int delta = step >> 3;
            if ((nibble & 4) != 0) {
                delta += step;
            }
            if ((nibble & 2) != 0) {
                delta += step >> 1;
            }
            if ((nibble & 1) != 0) {
                delta += step >> 2;
            }
            predictor = clamp((nibble & 8) != 0 ? predictor - delta : predictor + delta);
            index = Math.clamp(index + INDEX_ADJUST[nibble & 7], 0, STEPS.length - 1);
            out[i] = (short) predictor;
        }
        return true;
    }

    private static short clamp(long value) {
        return (short) Math.clamp(value, Short.MIN_VALUE, Short.MAX_VALUE);
    }
}
