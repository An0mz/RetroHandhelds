package dev.anomz.retrohandhelds.client.command;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.mojang.blaze3d.platform.NativeImage;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.RetroPaths;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import dev.anomz.retrohandhelds.client.emu.FrameBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Lets a session run headless for a few seconds, then dumps its last frame to PNG. */
final class DebugFrameTest {
    static final long RUN_SECONDS = 5;

    private DebugFrameTest() {
    }

    static void scheduleFinish(EmulatorSession session) {
        CompletableFuture.delayedExecutor(RUN_SECONDS, TimeUnit.SECONDS).execute(() -> finish(session));
    }

    /** Runs off the render thread so stopping the session never blocks a frame. */
    private static void finish(EmulatorSession session) {
        session.stop();
        int[] pixels = new int[FrameBuffer.PIXELS];
        long frameCount = session.frames().copyLatest(pixels);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> writeFrame(minecraft, session, pixels, frameCount));
    }

    private static void writeFrame(Minecraft minecraft, EmulatorSession session, int[] pixels, long frameCount) {
        if (frameCount == 0) {
            String reason = session.failure() != null ? session.failure() : "the core produced no video";
            chat(minecraft, "Test failed: " + reason);
            return;
        }
        Path out = RetroPaths.debugFrame();
        try (NativeImage image = new NativeImage(FrameBuffer.WIDTH, FrameBuffer.HEIGHT, false)) {
            for (int y = 0; y < FrameBuffer.HEIGHT; y++) {
                for (int x = 0; x < FrameBuffer.WIDTH; x++) {
                    image.setPixelRGBA(x, y, pixels[y * FrameBuffer.WIDTH + x]);
                }
            }
            image.writeToFile(out);
            chat(minecraft, "Ran " + frameCount + " frames. Last frame written to " + out);
        } catch (IOException e) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: could not write {}", out, e);
            chat(minecraft, "Could not write " + out + ": " + e.getMessage());
        }
    }

    private static void chat(Minecraft minecraft, String message) {
        minecraft.gui.getChat().addMessage(Component.literal(message));
    }
}
