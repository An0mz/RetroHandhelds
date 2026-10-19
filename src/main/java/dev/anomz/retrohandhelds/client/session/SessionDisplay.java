package dev.anomz.retrohandhelds.client.session;

import com.mojang.blaze3d.platform.NativeImage;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import dev.anomz.retrohandhelds.client.emu.FrameBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * GPU texture showing a session's latest frame. Render thread only: created, updated and closed
 * there, so the emulator thread never touches it.
 */
public final class SessionDisplay implements ScreenImage, AutoCloseable {
    private static final ResourceLocation TEXTURE_ID =
            ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "dynamic/emulator_screen");

    private final EmulatorSession session;
    private final FrameBuffer frames;
    private final int[] pixels = new int[FrameBuffer.PIXELS];
    private final int[] size = { FrameBuffer.WIDTH, FrameBuffer.HEIGHT };
    private DynamicTexture texture;
    private long lastFrame;

    SessionDisplay(EmulatorSession session) {
        this.session = session;
        this.frames = session.frames();
        this.texture = new DynamicTexture(FrameBuffer.WIDTH, FrameBuffer.HEIGHT, true);
        // This constructor allocates GPU memory without uploading anything, and drivers may hand back
        // the previous session's freed texture, still showing its last frame. Start from black instead.
        texture.getPixels().fillRect(0, 0, FrameBuffer.WIDTH, FrameBuffer.HEIGHT, 0xFF000000);
        texture.upload();
        Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
    }

    public ResourceLocation textureId() {
        return TEXTURE_ID;
    }

    /** Width of the game's picture in the texture's top-left corner (240 for GBA, 160 for Game Boy, 320 for Genesis). */
    public int width() {
        return size[0];
    }

    public int height() {
        return size[1];
    }

    @Override
    public int textureWidth() {
        return FrameBuffer.WIDTH;
    }

    @Override
    public int textureHeight() {
        return FrameBuffer.HEIGHT;
    }

    @Override
    public float aspectRatio() {
        float core = session.aspectRatio();
        return core > 0 ? core : ScreenImage.super.aspectRatio();
    }

    /** Uploads the newest frame if there is one. Cheap to call several times per frame. */
    public void update() {
        if (texture == null) {
            return;
        }
        long frame = frames.copyIfNewer(pixels, lastFrame, size);
        if (frame == lastFrame) {
            return;
        }
        lastFrame = frame;
        NativeImage image = texture.getPixels();
        for (int y = 0; y < FrameBuffer.HEIGHT; y++) {
            for (int x = 0; x < FrameBuffer.WIDTH; x++) {
                image.setPixelRGBA(x, y, pixels[y * FrameBuffer.WIDTH + x]);
            }
        }
        texture.upload();
    }

    @Override
    public void close() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
            texture = null;
        }
    }
}
