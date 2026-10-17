package dev.anomz.retrohandhelds.client.session;

import com.mojang.blaze3d.platform.NativeImage;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.emu.FrameBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * GPU texture showing a session's latest frame. Render thread only: created, updated and closed
 * there, so the emulator thread never touches it. The texture is as big as the core's largest
 * picture; only the part in use is copied and uploaded each frame.
 */
public final class SessionDisplay implements ScreenImage, AutoCloseable {
    private static final ResourceLocation TEXTURE_ID =
            ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "dynamic/emulator_screen");

    private final FrameBuffer frames;
    private final int textureWidth;
    private final int textureHeight;
    private final int[] pixels;
    private final int[] size;
    private DynamicTexture texture;
    private long lastFrame;

    SessionDisplay(FrameBuffer frames) {
        this.frames = frames;
        this.textureWidth = frames.stride();
        this.textureHeight = frames.maxHeight();
        this.pixels = new int[frames.capacity()];
        // Until the first frame: a small black picture rather than the whole (mostly unused) texture.
        this.size = new int[] { Math.min(240, textureWidth), Math.min(160, textureHeight) };
        this.texture = new DynamicTexture(textureWidth, textureHeight, true);
        // This constructor allocates GPU memory without uploading anything, and drivers may hand back
        // the previous session's freed texture, still showing its last frame. Start from black instead.
        texture.getPixels().fillRect(0, 0, textureWidth, textureHeight, 0xFF000000);
        texture.upload();
        Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
    }

    public ResourceLocation textureId() {
        return TEXTURE_ID;
    }

    /** Width of the game's picture in the texture's top-left corner (240 for GBA, 160 for Game Boy). */
    public int width() {
        return size[0];
    }

    public int height() {
        return size[1];
    }

    @Override
    public int textureWidth() {
        return textureWidth;
    }

    @Override
    public int textureHeight() {
        return textureHeight;
    }

    @Override
    public float aspect() {
        return frames.aspect(size[0], size[1]);
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
        int width = size[0];
        int height = size[1];
        NativeImage image = texture.getPixels();
        for (int y = 0; y < height; y++) {
            int row = y * textureWidth;
            for (int x = 0; x < width; x++) {
                image.setPixelRGBA(x, y, pixels[row + x]);
            }
        }
        texture.bind();
        image.upload(0, 0, 0, 0, 0, width, height, false, false);
    }

    @Override
    public void close() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE_ID);
            texture = null;
        }
    }
}
