package dev.anomz.retrohandhelds.client.share;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.client.RetroClientConfig;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import dev.anomz.retrohandhelds.client.session.ScreenImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Pictures other players share: from consoles, keyed by console position, and from the handhelds in
 * their hands, keyed by player entity id. Main/render thread only.
 */
public final class RemoteScreens {
    /** Caps texture memory if many consoles are being played nearby. */
    private static final int MAX_SCREENS = 16;
    /** A screen disappears this long after its last strip arrived. */
    private static final long EXPIRE_MS = 3000;

    private static final Map<BlockPos, RemoteScreen> SCREENS = new LinkedHashMap<>();
    private static final Int2ObjectMap<RemoteScreen> HANDHELDS = new Int2ObjectOpenHashMap<>();
    private static final ScreenStrips.Decoder DECODER = new ScreenStrips.Decoder();
    private static int nextId;

    private RemoteScreens() {
    }

    /** A strip arrived from the server: from a console, or from the handheld of {@code player} if it isn't -1. */
    public static void receive(int player, BlockPos pos, int width, int height, int strip, byte[] data) {
        if (!RetroClientConfig.showOtherScreens()) {
            return;
        }
        RemoteScreen screen;
        if (player >= 0) {
            screen = HANDHELDS.get(player);
            if (screen == null) {
                screen = newScreen();
                HANDHELDS.put(player, screen);
            }
        } else {
            // Our own game on that console is already drawn from the real thing.
            if (ConsoleSessions.displayAt(pos) != null) {
                return;
            }
            screen = SCREENS.get(pos);
            if (screen == null) {
                screen = newScreen();
                SCREENS.put(pos.immutable(), screen);
            }
        }
        if (DECODER.decode(data, strip, width, height, screen.pixels)) {
            screen.width = width;
            screen.height = height;
            screen.dirty = true;
            screen.lastUpdate = Util.getMillis();
        }
    }

    private static RemoteScreen newScreen() {
        if (SCREENS.size() + HANDHELDS.size() >= MAX_SCREENS) {
            removeOldest();
        }
        return new RemoteScreen(nextId++);
    }

    @Nullable
    public static ScreenImage at(BlockPos pos) {
        return SCREENS.get(pos);
    }

    /** The game shared from the handheld held by the player with entity id {@code player}. */
    @Nullable
    public static ScreenImage forPlayer(int player) {
        return HANDHELDS.get(player);
    }

    /** Consoles currently shown from other players, for TVs to check. */
    public static Collection<BlockPos> positions() {
        return SCREENS.keySet();
    }

    /** Client tick: drop screens whose player stopped sharing. */
    public static void tick() {
        expire(SCREENS.values(), Util.getMillis());
        expire(HANDHELDS.values(), Util.getMillis());
    }

    private static void expire(Collection<RemoteScreen> screens, long now) {
        Iterator<RemoteScreen> it = screens.iterator();
        while (it.hasNext()) {
            RemoteScreen screen = it.next();
            if (now - screen.lastUpdate > EXPIRE_MS || !RetroClientConfig.showOtherScreens()) {
                screen.close();
                it.remove();
            }
        }
    }

    public static void clear() {
        SCREENS.values().forEach(RemoteScreen::close);
        SCREENS.clear();
        HANDHELDS.values().forEach(RemoteScreen::close);
        HANDHELDS.clear();
    }

    private static void removeOldest() {
        RemoteScreen oldest = null;
        for (RemoteScreen screen : SCREENS.values()) {
            oldest = older(oldest, screen);
        }
        for (RemoteScreen screen : HANDHELDS.values()) {
            oldest = older(oldest, screen);
        }
        if (oldest != null) {
            oldest.close();
            SCREENS.values().remove(oldest);
            HANDHELDS.values().remove(oldest);
        }
    }

    private static RemoteScreen older(@Nullable RemoteScreen a, RemoteScreen b) {
        return a == null || b.lastUpdate < a.lastUpdate ? b : a;
    }

    /** What to draw for the console at {@code pos}: this client's own game, else one shared by another player. */
    @Nullable
    public static ScreenImage imageFor(BlockPos pos) {
        ScreenImage local = ConsoleSessions.displayAt(pos);
        return local != null ? local : SCREENS.get(pos);
    }

    private static final class RemoteScreen implements ScreenImage {
        private final ResourceLocation textureId;
        private final int[] pixels = new int[ScreenStrips.MAX_WIDTH * ScreenStrips.MAX_HEIGHT];
        private DynamicTexture texture;
        private int width = ScreenStrips.MAX_WIDTH;
        private int height = ScreenStrips.MAX_HEIGHT;
        private boolean dirty = true;
        private long lastUpdate = Util.getMillis();

        RemoteScreen(int id) {
            this.textureId = ResourceLocation.fromNamespaceAndPath(RetroHandhelds.MODID, "dynamic/remote_screen_" + id);
            Arrays.fill(pixels, 0xFF000000);
        }

        @Override
        public ResourceLocation textureId() {
            return textureId;
        }

        @Override
        public int width() {
            return width;
        }

        @Override
        public int height() {
            return height;
        }

        @Override
        public int textureWidth() {
            return ScreenStrips.MAX_WIDTH;
        }

        @Override
        public int textureHeight() {
            return ScreenStrips.MAX_HEIGHT;
        }

        @Override
        public void update() {
            if (texture == null) {
                texture = new DynamicTexture(ScreenStrips.MAX_WIDTH, ScreenStrips.MAX_HEIGHT, true);
                Minecraft.getInstance().getTextureManager().register(textureId, texture);
            }
            if (!dirty) {
                return;
            }
            dirty = false;
            NativeImage image = texture.getPixels();
            for (int y = 0; y < ScreenStrips.MAX_HEIGHT; y++) {
                for (int x = 0; x < ScreenStrips.MAX_WIDTH; x++) {
                    image.setPixelRGBA(x, y, pixels[y * ScreenStrips.MAX_WIDTH + x]);
                }
            }
            texture.upload();
        }

        void close() {
            if (texture != null) {
                Minecraft.getInstance().getTextureManager().release(textureId);
                texture = null;
            }
        }
    }
}
