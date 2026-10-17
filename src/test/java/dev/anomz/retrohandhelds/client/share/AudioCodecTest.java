package dev.anomz.retrohandhelds.client.share;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AudioCodecTest {
    private static final int N = AudioCodec.PACKET_SAMPLES;

    private static short[] sine(int samples, double hz, double amplitude, int rate) {
        short[] out = new short[samples];
        for (int i = 0; i < samples; i++) {
            out[i] = (short) Math.round(amplitude * Math.sin(2 * Math.PI * hz * i / rate));
        }
        return out;
    }

    /** Signal-to-noise ratio of {@code decoded} against {@code original}, in dB. */
    private static double snr(short[] original, short[] decoded, int from, int to) {
        double signal = 0;
        double noise = 0;
        for (int i = from; i < to; i++) {
            signal += (double) original[i] * original[i];
            double e = original[i] - decoded[i];
            noise += e * e;
        }
        return 10 * Math.log10(signal / Math.max(noise, 1e-9));
    }

    @Test
    void packetSizeIsAsDocumented() {
        byte[] packet = new AudioCodec.Encoder().encode(new short[N], 0);
        assertEquals(AudioCodec.PACKET_BYTES, packet.length);
        assertEquals(3 + N / 2, packet.length);
    }

    @Test
    void sineSurvivesEncodingWithGoodQuality() {
        short[] pcm = sine(N * 3, 440, 12000, AudioCodec.SAMPLE_RATE);
        AudioCodec.Encoder encoder = new AudioCodec.Encoder();
        short[] decoded = new short[pcm.length];
        short[] chunk = new short[N];
        for (int p = 0; p < 3; p++) {
            assertTrue(AudioCodec.decode(encoder.encode(pcm, p * N), chunk));
            System.arraycopy(chunk, 0, decoded, p * N, N);
        }
        // Skip the first few hundred samples while the step size adapts.
        double snr = snr(pcm, decoded, 300, pcm.length);
        assertTrue(snr > 20, "SNR " + snr + " dB");
    }

    @Test
    void laterPacketDecodesOnItsOwn() {
        short[] pcm = sine(N * 2, 300, 10000, AudioCodec.SAMPLE_RATE);
        AudioCodec.Encoder encoder = new AudioCodec.Encoder();
        encoder.encode(pcm, 0);
        byte[] second = encoder.encode(pcm, N);
        short[] decoded = new short[N];
        assertTrue(AudioCodec.decode(second, decoded));
        short[] expected = new short[N];
        System.arraycopy(pcm, N, expected, 0, N);
        assertTrue(snr(expected, decoded, 0, N) > 20, "packet decoded without the previous one");
    }

    @Test
    void rejectsBadPackets() {
        short[] out = new short[N];
        assertFalse(AudioCodec.decode(new byte[10], out), "wrong size");
        byte[] packet = new AudioCodec.Encoder().encode(new short[N], 0);
        packet[2] = (byte) 200;
        assertFalse(AudioCodec.decode(packet, out), "step index out of range");
    }

    @Test
    void downmixerHitsTheTargetRate() {
        int inputRate = 65536;
        int frames = inputRate; // one second of stereo
        short[] stereo = new short[frames * 2];
        short[] mono = new short[AudioCodec.SAMPLE_RATE + 10];
        int produced = new AudioCodec.Downmixer(inputRate).process(stereo, stereo.length, mono, 0);
        assertTrue(Math.abs(produced - AudioCodec.SAMPLE_RATE) <= 1, "produced " + produced);
    }

    @Test
    void downmixerAveragesChannelsAndKeepsLevel() {
        short[] stereo = new short[6000];
        for (int i = 0; i < stereo.length; i += 2) {
            stereo[i] = 1000;
            stereo[i + 1] = 3000;
        }
        short[] mono = new short[3000];
        int produced = new AudioCodec.Downmixer(65536).process(stereo, stereo.length, mono, 0);
        assertTrue(produced > 0);
        for (int i = 0; i < produced; i++) {
            assertEquals(2000, mono[i]);
        }
    }

    @Test
    void downmixerCarriesStateAcrossCalls() {
        short[] stereo = new short[2000];
        AudioCodec.Downmixer whole = new AudioCodec.Downmixer(65536);
        AudioCodec.Downmixer split = new AudioCodec.Downmixer(65536);
        short[] a = new short[2000];
        short[] b = new short[2000];
        int n1 = whole.process(stereo, 2000, a, 0);
        int n2 = split.process(stereo, 998, b, 0);
        n2 += split.process(stereo, 1002, b, n2);
        assertEquals(n1, n2);
    }
}
