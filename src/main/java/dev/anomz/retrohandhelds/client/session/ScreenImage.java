package dev.anomz.retrohandhelds.client.session;

import net.minecraft.resources.ResourceLocation;

/**
 * A game picture that can be drawn: this client's own running game or one shared by another player.
 * The picture fills the top-left {@link #width()} x {@link #height()} of a
 * {@link #textureWidth()} x {@link #textureHeight()} texture. Render thread only.
 */
public interface ScreenImage {
    ResourceLocation textureId();

    int width();

    int height();

    int textureWidth();

    int textureHeight();

    /** How wide the picture should be drawn for its height; not always width/height (PS1 pixels aren't square). */
    default float aspect() {
        return width() / (float) height();
    }

    /** Right edge of the picture in texture coordinates. */
    default float u1() {
        return width() / (float) textureWidth();
    }

    /** Bottom edge of the picture in texture coordinates. */
    default float v1() {
        return height() / (float) textureHeight();
    }

    /** Uploads the newest picture if there is one. Cheap to call several times per frame. */
    void update();
}
