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
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Pictures other players share from their consoles, keyed by console position. Main/render thread only. */
public final class RemoteScreens {
    /** Caps texture memory if many consoles are being played nearby. */
    private static final int MAX_SCREENS = 16;
    /** A screen disappears this long after its last strip arrived. */
    private static final long EXPIRE_MS = 3000;

    private static final Map<BlockPos, RemoteScreen> SCREENS = new LinkedHashMap<>();
    private static final ScreenStrips.Decoder DECODER = new ScreenStrips.Decoder();
    private static int nextId;

    private RemoteScreens() {
    }

    /** A strip arrived from the server. */
    public static void receive(BlockPos pos, int width, int height, int strip, byte[] data) {
        // Our own game on that console is already drawn from the real thing.
        if (!RetroClientConfig.showOtherScreens() || ConsoleSessions.displayAt(pos) != null) {
            return;
        }
        RemoteScreen screen = SCREENS.get(pos);
        if (screen == null) {
            if (SCREENS.size() >= MAX_SCREENS) {
                removeOldest();
            }
            screen = new RemoteScreen(nextId++);
            SCREENS.put(pos.immutable(), screen);
        }
        if (DECODER.decode(data, strip, width, height, screen.pixels)) {
            screen.width = width;
            screen.height = height;
            screen.dirty = true;
            screen.lastUpdate = Util.getMillis();
        }
    }

    @Nullable
    public static ScreenImage at(BlockPos pos) {
        return SCREENS.get(pos);
    }

    /** Consoles currently shown from other players, for TVs to check. */
    public static Collection<BlockPos> positions() {
        return SCREENS.keySet();
    }

    /** Client tick: drop screens whose player stopped sharing. */
    public static void tick() {
        long now = Util.getMillis();
        Iterator<RemoteScreen> it = SCREENS.values().iterator();
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
    }

    private static void removeOldest() {
        BlockPos oldest = null;
        for (Map.Entry<BlockPos, RemoteScreen> entry : SCREENS.entrySet()) {
            if (oldest == null || entry.getValue().lastUpdate < SCREENS.get(oldest).lastUpdate) {
                oldest = entry.getKey();
            }
        }
        if (oldest != null) {
            SCREENS.remove(oldest).close();
        }
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
