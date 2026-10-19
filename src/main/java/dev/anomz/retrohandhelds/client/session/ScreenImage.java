package dev.anomz.retrohandhelds.client.session;

import net.minecraft.resources.ResourceLocation;

/**
 * A game picture that can be drawn: this client's own running game or one shared by another player.
 * The picture fills the top-left {@link #width()} x {@link #height()} of a
 * {@link #textureWidth()} x {@link #textureHeight()} texture.
 * Render thread only.
 */
public interface ScreenImage {
    ResourceLocation textureId();

    int width();

    int height();

    int textureWidth();

    int textureHeight();

    /** Width / height to draw the picture at; its pixel shape unless the core asks for another. */
    default float aspectRatio() {
        return width() / (float) height();
    }

    /** Uploads the newest picture if there is one. Cheap to call several times per frame. */
    void update();
}
