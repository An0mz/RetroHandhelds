package dev.anomz.retrohandhelds.client.session;

import net.minecraft.resources.ResourceLocation;

/**
 * A game picture that can be drawn: this client's own running game or one shared by another player.
 * The texture is {@code 240x160}; the picture fills its top-left {@link #width()} x {@link #height()}.
 * Render thread only.
 */
public interface ScreenImage {
    ResourceLocation textureId();

    int width();

    int height();

    /** Uploads the newest picture if there is one. Cheap to call several times per frame. */
    void update();
}
