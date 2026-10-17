package dev.anomz.retrohandhelds.client.session;

import com.mojang.blaze3d.platform.NativeImage;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.emu.FrameBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * GPU texture showing a session's latest frame. Render thread only: created, updated and closed
 * there, so the emulator thread never touches it.
 */
public final class SessionDisplay implements AutoCloseable {
    private static final ResourceLocation TEXTURE_ID =
            ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "dynamic/emulator_screen");

    private final FrameBuffer frames;
    private final int[] pixels = new int[FrameBuffer.PIXELS];
    private DynamicTexture texture;
    private long lastFrame;

    SessionDisplay(FrameBuffer frames) {
        this.frames = frames;
        this.texture = new DynamicTexture(FrameBuffer.WIDTH, FrameBuffer.HEIGHT, true);
        Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
    }

    public ResourceLocation textureId() {
        return TEXTURE_ID;
    }

    /** Uploads the newest frame if there is one. Cheap to call several times per frame. */
    public void update() {
        if (texture == null) {
            return;
        }
        long frame = frames.copyIfNewer(pixels, lastFrame);
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
