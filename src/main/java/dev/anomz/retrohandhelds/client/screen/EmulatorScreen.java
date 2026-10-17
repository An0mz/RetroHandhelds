package dev.anomz.retrohandhelds.client.screen;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import dev.anomz.retrohandhelds.RomNames;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import dev.anomz.retrohandhelds.client.emu.FrameBuffer;
import dev.anomz.retrohandhelds.client.emu.StateResult;
import dev.anomz.retrohandhelds.client.input.EmulatorKeys;
import dev.anomz.retrohandhelds.client.input.JoypadButton;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Full-screen view of a running session, with keyboard input. A console's session keeps running
 * on the block after this closes; a standalone one (play command) is stopped.
 */
public final class EmulatorScreen extends Screen {
    private static final int W = FrameBuffer.WIDTH;
    private static final int H = FrameBuffer.HEIGHT;
    private static final long HINT_VISIBLE_MS = 5000;
    private static final long HINT_FADE_MS = 1000;
    private static final long STATUS_VISIBLE_MS = 2000;

    private final ConsoleSessions.Active active;
    private final EmulatorSession session;
    private final boolean stopOnClose;
    private final long openedAtMs = Util.getMillis();
    private List<Component> hintLines = List.of();
    private Component status;
    private long statusAtMs;

    public EmulatorScreen(ConsoleSessions.Active active, boolean stopOnClose) {
        super(Component.literal(RomNames.displayName(active.romFile())));
        this.active = active;
        this.session = active.session();
        this.stopOnClose = stopOnClose;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.retrohandhelds.power_off"), b -> powerOff())
                .bounds(width - 74, 4, 70, 20).build());
        // Built here, not per frame; bindings cannot change while this screen is open.
        hintLines = List.of(
                hint(JoypadButton.UP, JoypadButton.DOWN, JoypadButton.LEFT, JoypadButton.RIGHT),
                hint(JoypadButton.A, JoypadButton.B, JoypadButton.L, JoypadButton.R),
                hint(JoypadButton.START, JoypadButton.SELECT).copy().append("   Close: Esc"),
                Component.empty()
                        .append("Save state: ").append(EmulatorKeys.SAVE_STATE.getTranslatedKeyMessage())
                        .append("   Load state: ").append(EmulatorKeys.LOAD_STATE.getTranslatedKeyMessage()),
                Component.empty()
                        .append("Mute: ").append(EmulatorKeys.MUTE.getTranslatedKeyMessage())
                        .append("   Volume: ").append(EmulatorKeys.VOLUME_DOWN.getTranslatedKeyMessage())
                        .append(" / ").append(EmulatorKeys.VOLUME_UP.getTranslatedKeyMessage()));
    }

    private static Component hint(JoypadButton... buttons) {
        var line = Component.empty();
        for (int i = 0; i < buttons.length; i++) {
            if (i > 0) {
                line.append("   ");
            }
            line.append(buttons[i].label() + ": ").append(buttons[i].mapping().getTranslatedKeyMessage());
        }
        return line;
    }

    @Override
    public void tick() {
        // Key releases are lost while the window is unfocused, so drop everything instead of sticking.
        if (!minecraft.isWindowActive()) {
            session.joypad().clear();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        active.display().update();
        graphics.fill(0, 0, width, height, 0xFF000000);
        float scale = Math.min(width / (float) W, height / (float) H);
        int w = (int) (W * scale);
        int h = (int) (H * scale);
        graphics.blit(active.display().textureId(), (width - w) / 2, (height - h) / 2, w, h, 0, 0, W, H, W, H);

        renderHint(graphics);
        if (status != null && Util.getMillis() - statusAtMs < STATUS_VISIBLE_MS) {
            graphics.drawCenteredString(font, status, width / 2, 8, 0xFFFFFFFF);
        }

        String failure = session.failure();
        if (failure != null) {
            graphics.drawCenteredString(font, failure, width / 2, height / 2, 0xFFFF5555);
        }
        // Draws the widgets (the Power Off button); the background is ours, see renderBackground.
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Nothing: render() fills black and draws the game instead of Minecraft's blurred menu background.
    }

    /** Shuts the console down (the cartridge stays in) and goes back to the world. */
    private void powerOff() {
        ConsoleSessions.stop(active);
        onClose();
    }

    private void renderHint(GuiGraphics graphics) {
        long elapsed = Util.getMillis() - openedAtMs;
        if (elapsed >= HINT_VISIBLE_MS + HINT_FADE_MS) {
            return;
        }
        float fade = elapsed <= HINT_VISIBLE_MS ? 1f : 1f - (elapsed - HINT_VISIBLE_MS) / (float) HINT_FADE_MS;
        int alpha = (int) (fade * 255);
        // Font treats alpha below 4 as fully opaque, so stop drawing just before that.
        if (alpha < 8) {
            return;
        }
        int lineHeight = font.lineHeight + 2;
        int top = height - hintLines.size() * lineHeight - 6;
        graphics.fill(0, top - 4, width, height, (alpha * 3 / 5) << 24);
        for (int i = 0; i < hintLines.size(); i++) {
            graphics.drawCenteredString(font, hintLines.get(i), width / 2, top + i * lineHeight, alpha << 24 | 0xFFFFFF);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (matches(EmulatorKeys.SAVE_STATE, keyCode, scanCode)) {
            session.saveState().thenAccept(this::showStateResult);
            return true;
        }
        if (matches(EmulatorKeys.LOAD_STATE, keyCode, scanCode)) {
            session.loadState().thenAccept(this::showStateResult);
            return true;
        }
        if (matches(EmulatorKeys.MUTE, keyCode, scanCode)) {
            showStatus(ConsoleSessions.toggleMute());
            return true;
        }
        if (matches(EmulatorKeys.VOLUME_DOWN, keyCode, scanCode)) {
            showStatus(ConsoleSessions.changeVolume(-1));
            return true;
        }
        if (matches(EmulatorKeys.VOLUME_UP, keyCode, scanCode)) {
            showStatus(ConsoleSessions.changeVolume(1));
            return true;
        }
        if (keyCode != GLFW.GLFW_KEY_ESCAPE && setButtons(keyCode, scanCode, true)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return setButtons(keyCode, scanCode, false) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return setMouseButtons(button, true) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return setMouseButtons(button, false) || super.mouseReleased(mouseX, mouseY, button);
    }

    private static boolean matches(KeyMapping mapping, int keyCode, int scanCode) {
        return keyCode != GLFW.GLFW_KEY_ESCAPE && mapping.matches(keyCode, scanCode);
    }

    /** Called on the emulator thread; hops to the render thread before touching the screen. */
    private void showStateResult(StateResult result) {
        String key = switch (result) {
            case SAVED -> "message.retrohandhelds.state_saved";
            case LOADED -> "message.retrohandhelds.state_loaded";
            case NO_STATE -> "message.retrohandhelds.state_missing";
            case FAILED -> "message.retrohandhelds.state_failed";
        };
        minecraft.execute(() -> showStatus(Component.translatable(key)));
    }

    private void showStatus(Component message) {
        status = message;
        statusAtMs = Util.getMillis();
    }

    private boolean setButtons(int keyCode, int scanCode, boolean down) {
        boolean matched = false;
        for (JoypadButton button : JoypadButton.ALL) {
            if (button.mapping().matches(keyCode, scanCode)) {
                session.joypad().set(button.retroId(), down);
                matched = true;
            }
        }
        return matched;
    }

    private boolean setMouseButtons(int mouseButton, boolean down) {
        boolean matched = false;
        for (JoypadButton button : JoypadButton.ALL) {
            if (button.mapping().matchesMouse(mouseButton)) {
                session.joypad().set(button.retroId(), down);
                matched = true;
            }
        }
        return matched;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        session.joypad().clear();
        if (stopOnClose) {
            ConsoleSessions.stop(active);
        }
    }
}
