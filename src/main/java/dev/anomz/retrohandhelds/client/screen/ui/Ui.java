package dev.anomz.retrohandhelds.client.screen.ui;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.anomz.retrohandhelds.RetroHandhelds;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The look of Retro Handhelds' screens: dark panels with one retro-red accent, like a handheld's power light. */
public final class Ui {
    public static final int PANEL = 0xF2141519;
    public static final int PANEL_BORDER = 0xFF2A2D36;
    public static final int DIVIDER = 0xFF23252D;
    public static final int SURFACE = 0xFF1D1F26;
    public static final int SURFACE_HOVER = 0xFF272A34;
    public static final int TRACK = 0xFF343743;
    public static final int ACCENT = 0xFFE5484D;
    public static final int ACCENT_HOVER = 0xFFF26A6E;
    public static final int ACCENT_SOFT = 0x40E5484D;
    public static final int TEXT = 0xFFEDEDF2;
    public static final int TEXT_DIM = 0xFF9A9CAA;
    public static final int TEXT_FAINT = 0xFF5E606C;
    public static final int DANGER = 0xFFEF5D60;
    public static final int WARNING = 0xFFF5B83D;

    public static final int PAD = 10;
    public static final int HEADER = 36;
    /** Height of a key cap drawn by {@link #keyCap}. */
    public static final int KEY_CAP_HEIGHT = 13;

    private static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "textures/gui/icons.png");
    private static final int ICON_SOURCE = 32;
    private static final int ATLAS_COLUMNS = 8;
    private static final int ATLAS_ROWS = 2;

    private Ui() {
    }

    /** {@code color} with its alpha multiplied by {@code alpha} (0..1), for fading things out. */
    public static int fade(int color, float alpha) {
        int a = Math.round((color >>> 24) * Math.max(0, Math.min(1, alpha)));
        return a << 24 | color & 0xFFFFFF;
    }

    /** A rectangle with its corners cut by two pixels, which reads as rounded at GUI scale. */
    public static void round(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w < 4 || h < 4) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + 2, y, x + w - 2, y + 1, color);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, color);
        g.fill(x, y + 2, x + w, y + h - 2, color);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, color);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
    }

    /** A rounded box with a one-pixel border. */
    public static void box(GuiGraphics g, int x, int y, int w, int h, int fill, int border) {
        round(g, x, y, w, h, border);
        round(g, x + 1, y + 1, w - 2, h - 2, fill);
    }

    /** The screen's main panel, with a soft shadow. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        round(g, x - 2, y - 1, w + 4, h + 4, 0x40000000);
        box(g, x, y, w, h, PANEL, PANEL_BORDER);
    }

    /** Width of {@link #keyCap} for {@code text}. */
    public static int keyCapWidth(Font font, Component text) {
        return font.width(text) + 8;
    }

    /**
     * A key drawn as a small cap, e.g. "Space": the way keys are shown everywhere in the mod. Returns
     * its width.
     */
    public static int keyCap(GuiGraphics g, Font font, Component text, int x, int y, int textColor, int border, float alpha) {
        int w = keyCapWidth(font, text);
        box(g, x, y, w, KEY_CAP_HEIGHT, fade(SURFACE, alpha), fade(border, alpha));
        g.drawString(font, text, x + 4, y + 3, fade(textColor, alpha), false);
        return w;
    }

    /** One of the {@link Icon}s, {@code size} GUI pixels square, tinted {@code color} (ARGB). */
    public static void icon(GuiGraphics g, Icon icon, int x, int y, int size, int color) {
        int index = icon.ordinal();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f, (color >>> 24) / 255f);
        g.blit(ICONS, x, y, size, size, index % ATLAS_COLUMNS * ICON_SOURCE, index / ATLAS_COLUMNS * ICON_SOURCE,
                ICON_SOURCE, ICON_SOURCE, ATLAS_COLUMNS * ICON_SOURCE, ATLAS_ROWS * ICON_SOURCE);
        g.setColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
    }
}
