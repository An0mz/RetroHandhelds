package dev.anomz.retrohandhelds.client.screen;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import org.lwjgl.glfw.GLFW;

import dev.anomz.retrohandhelds.RetroSystem;
import dev.anomz.retrohandhelds.RomNames;
import dev.anomz.retrohandhelds.client.RetroClientConfig;
import dev.anomz.retrohandhelds.client.emu.EmulatorSession;
import dev.anomz.retrohandhelds.client.emu.FrameBuffer;
import dev.anomz.retrohandhelds.client.emu.JoypadState;
import dev.anomz.retrohandhelds.client.emu.StateResult;
import dev.anomz.retrohandhelds.client.controls.Binding;
import dev.anomz.retrohandhelds.client.controls.ControlAction;
import dev.anomz.retrohandhelds.client.controls.ControlPreset;
import dev.anomz.retrohandhelds.client.controls.ControlProfile;
import dev.anomz.retrohandhelds.client.controls.ControlProfiles;
import dev.anomz.retrohandhelds.client.input.EmulatorKeys;
import dev.anomz.retrohandhelds.client.input.GamepadInput;
import dev.anomz.retrohandhelds.client.input.MouseLook;
import dev.anomz.retrohandhelds.client.session.ConsoleSessions;
import dev.anomz.retrohandhelds.client.session.SessionDisplay;
import net.minecraft.Util;
import dev.anomz.retrohandhelds.client.screen.ui.Icon;
import dev.anomz.retrohandhelds.client.screen.ui.IconButton;
import dev.anomz.retrohandhelds.client.screen.ui.Ui;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

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
    /** The top bar hides this long after the mouse last moved. */
    private static final long BAR_IDLE_MS = 2500;
    private static final int BAR_BUTTON = 18;
    private static final int HINT_MAX_WIDTH = 460;

    private final ConsoleSessions.Active active;
    private final EmulatorSession session;
    private final boolean stopOnClose;
    private final RetroSystem.Controls controls;
    private final ControlProfile profile;
    private final MouseLook mouseLook = new MouseLook();
    /** The player freed the cursor with the free-mouse key (mouse look off until pressed again). */
    private boolean cursorFree;
    /** Whether this screen has hidden the cursor for mouse look. */
    private boolean captured;
    private final double[] cursorX = new double[1];
    private final double[] cursorY = new double[1];
    private double lastCursorX;
    private double lastCursorY;
    private long lastLookNanos;
    /** Where the picture is drawn in the full-screen view, for touch input; zero size when not drawn. */
    private int pictureX;
    private int pictureY;
    private int pictureWidth;
    private int pictureHeight;
    private boolean touching;
    /** A screen of ours is open on top (Controls, restart confirmation): this one comes back, so the game keeps running. */
    private boolean childOpen;
    /** Whether there is a console or handheld in the world to watch instead of the full-screen picture. */
    private final boolean hasWorldView;
    private boolean inWorldView;
    private final long openedAtMs = Util.getMillis();
    /** One hint: a label and the key that does it, or a whole line of text when the key is null. */
    private record Hint(Component label, @Nullable Component key) {
    }

    private List<Hint> hints = List.of();
    private final List<IconButton> barButtons = new ArrayList<>();
    private long lastMouseMoveMs = Util.getMillis();
    private Component status;
    private long statusAtMs;

    public EmulatorScreen(ConsoleSessions.Active active, boolean stopOnClose) {
        super(Component.literal(RomNames.displayName(active.romFile())));
        this.active = active;
        this.session = active.session();
        this.stopOnClose = stopOnClose;
        RetroSystem system = RetroSystem.forFileName(active.romFile()).orElse(RetroSystem.GAME_BOY_ADVANCE);
        this.controls = system.controls();
        this.profile = ControlProfiles.get(system);
        this.hasWorldView = active.pos() != null || active.handheld();
        this.inWorldView = hasWorldView && RetroClientConfig.inWorldView();
    }

    @Override
    protected void init() {
        childOpen = false;
        barButtons.clear();
        List<IconButton> buttons = new ArrayList<>();
        buttons.add(new IconButton(0, 0, BAR_BUTTON, Icon.GAMEPAD, Component.translatable("screen.retrohandhelds.controls"),
                IconButton.Style.GHOST, this::openControls));
        if (hasWorldView) {
            buttons.add(new IconButton(0, 0, BAR_BUTTON, Icon.FULLSCREEN, Component.translatable("screen.retrohandhelds.view.toggle"),
                    IconButton.Style.GHOST, this::toggleView));
        }
        buttons.add(new IconButton(0, 0, BAR_BUTTON, Icon.RESTART, Component.translatable("screen.retrohandhelds.restart"),
                IconButton.Style.GHOST, this::confirmRestart));
        buttons.add(new IconButton(0, 0, BAR_BUTTON, Icon.POWER, Component.translatable("screen.retrohandhelds.power_off"),
                IconButton.Style.DANGER, this::powerOff));
        int x = width - 10 - buttons.size() * (BAR_BUTTON + 2);
        for (IconButton button : buttons) {
            button.setPosition(x, 10);
            x += BAR_BUTTON + 2;
            barButtons.add(addRenderableWidget(button));
        }
        String pad = GamepadInput.connectedName();
        if (pad != null && status == null) {
            showStatus(Component.translatable("screen.retrohandhelds.gamepad", pad));
        }
        // Built here, not per frame: bindings only change in the Controls screen, which runs init() again.
        hints = buildHints();
    }

    /** The console's controls with their keys, then touch or mouse look, then the emulator keys. */
    private List<Hint> buildHints() {
        List<Hint> list = new ArrayList<>();
        for (ControlPreset.Entry entry : profile.preset().entries()) {
            list.add(new Hint(Component.literal(entry.label()), ControlsScreen.keyName(profile.binding(entry.action()))));
        }
        if (controls.touch()) {
            list.add(new Hint(Component.literal("Touchscreen: left mouse button on the picture (full-screen view)"), null));
        } else if (profile.mouseLook()) {
            list.add(new Hint(Component.literal(controls == RetroSystem.Controls.N64
                    ? "Mouse: flick to tap the C-buttons (camera)" : "Mouse: right stick"), null));
            list.add(new Hint(Component.literal("Free the cursor"), EmulatorKeys.FREE_MOUSE.getTranslatedKeyMessage()));
        }
        list.add(new Hint(Component.literal("Save state"), EmulatorKeys.SAVE_STATE.getTranslatedKeyMessage()));
        list.add(new Hint(Component.literal("Load state"), EmulatorKeys.LOAD_STATE.getTranslatedKeyMessage()));
        list.add(new Hint(Component.literal("Fast-forward (hold)"), EmulatorKeys.FAST_FORWARD.getTranslatedKeyMessage()));
        list.add(new Hint(Component.literal("View"), EmulatorKeys.TOGGLE_VIEW.getTranslatedKeyMessage()));
        list.add(new Hint(Component.literal("Mute"), EmulatorKeys.MUTE.getTranslatedKeyMessage()));
        list.add(new Hint(Component.literal("Volume"), Component.empty().append(EmulatorKeys.VOLUME_DOWN.getTranslatedKeyMessage())
                .append(" / ").append(EmulatorKeys.VOLUME_UP.getTranslatedKeyMessage())));
        list.add(new Hint(Component.literal("Close"), Component.literal("Esc")));
        return list;
    }

    @Override
    public void tick() {
        if (session.consumeResumed()) {
            showStatus(Component.translatable("screen.retrohandhelds.resumed"));
        }
        // Key releases are lost while the window is unfocused, so drop everything instead of sticking.
        if (!minecraft.isWindowActive()) {
            session.joypad().clear();
            session.setSpeed(1);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Polled every frame here: GLFW needs the main thread, and a 20 Hz tick would add input lag.
        if (minecraft.isWindowActive()) {
            GamepadInput.poll(session.joypad(), controls);
        } else {
            session.joypad().setPad(0);
            session.joypad().setPadAxes(0, 0, 0, 0);
        }
        SessionDisplay display = active.display();
        display.update();
        // In-world view draws nothing here: the world renders behind this screen, showing the game on
        // the console block or the handheld in the player's hands.
        if (!inWorldView) {
            graphics.fill(0, 0, width, height, 0xFF000000);
            int frameWidth = display.width();
            int frameHeight = display.height();
            float aspect = display.aspectRatio();
            int w = (int) Math.min(width, height * aspect);
            int h = (int) (w / aspect);
            pictureX = (width - w) / 2;
            pictureY = (height - h) / 2;
            pictureWidth = w;
            pictureHeight = h;
            graphics.blit(display.textureId(), pictureX, pictureY, w, h, 0, 0, frameWidth, frameHeight, W, H);
        } else {
            pictureWidth = 0;
            pictureHeight = 0;
        }
        if (controls.touch()) {
            updatePointer(mouseX, mouseY);
        }
        updateMouseLook();

        renderHint(graphics);
        if (session.speed() > 1) {
            pill(graphics, Component.translatable("screen.retrohandhelds.fast_forward", session.speed()), 10, 10, Ui.ACCENT);
        }
        if (status != null && Util.getMillis() - statusAtMs < STATUS_VISIBLE_MS) {
            int w = font.width(status) + 16;
            pill(graphics, status, (width - w) / 2, 10, Ui.TEXT);
        }
        String failure = session.failure();
        if (failure != null) {
            renderFailure(graphics, failure);
        }
        renderBar(graphics, mouseX, mouseY);
        // Draws the widgets (the Power Off button); the background is ours, see renderBackground.
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Nothing: render() fills black and draws the game instead of Minecraft's blurred menu background.
    }

    /** Switches between the full-screen picture and watching the game in the world; remembered in the config. */
    private void toggleView() {
        if (!hasWorldView) {
            showStatus(Component.translatable("screen.retrohandhelds.view.unavailable"));
            return;
        }
        inWorldView = !inWorldView;
        RetroClientConfig.setInWorldView(inWorldView);
        showStatus(Component.translatable(inWorldView ? "screen.retrohandhelds.view.world" : "screen.retrohandhelds.view.fullscreen"));
    }

    /** Asks first: restarting loses everything since the game's last own save. */
    private void confirmRestart() {
        childOpen = true;
        minecraft.setScreen(new ConfirmPanel(this, Component.translatable("screen.retrohandhelds.restart.title"),
                Component.translatable("screen.retrohandhelds.restart.message"),
                Component.translatable("screen.retrohandhelds.restart"), () -> {
                    session.reset();
                    showStatus(Component.translatable("screen.retrohandhelds.restarted"));
                }));
    }

    /** A short rounded label, e.g. a status message: returns its width. */
    private int pill(GuiGraphics graphics, Component text, int x, int y, int textColor) {
        int w = font.width(text) + 16;
        Ui.round(graphics, x, y, w, 18, Ui.PANEL);
        graphics.drawString(font, text, x + 8, y + 5, textColor, false);
        return w;
    }

    /** Why the game stopped, in a panel in the middle. */
    private void renderFailure(GuiGraphics graphics, String failure) {
        int w = Math.min(320, width - 40);
        List<FormattedCharSequence> lines = font.split(Component.literal(failure), w - 2 * Ui.PAD - 30);
        int h = Math.max(40, 2 * Ui.PAD + lines.size() * (font.lineHeight + 2) + 12);
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        Ui.panel(graphics, x, y, w, h);
        Ui.icon(graphics, Icon.WARNING, x + Ui.PAD, y + Ui.PAD, 20, Ui.DANGER);
        graphics.drawString(font, Component.translatable("screen.retrohandhelds.failed"), x + Ui.PAD + 28, y + Ui.PAD, Ui.TEXT, false);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), x + Ui.PAD + 28, y + Ui.PAD + 12 + i * (font.lineHeight + 2), Ui.TEXT_DIM, false);
        }
    }

    /**
     * The buttons at the top right, on a dark rounded bar: shown while the mouse moves or is over them,
     * hidden when it rests and whenever mouse look has the cursor.
     */
    private void renderBar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (barButtons.isEmpty()) {
            return;
        }
        int left = barButtons.getFirst().getX() - 4;
        int top = 6;
        int right = barButtons.getLast().getX() + BAR_BUTTON + 4;
        int bottom = top + BAR_BUTTON + 8;
        boolean over = mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
        boolean shown = !captured && (over || Util.getMillis() - lastMouseMoveMs < BAR_IDLE_MS
                || Util.getMillis() - openedAtMs < HINT_VISIBLE_MS);
        for (IconButton button : barButtons) {
            button.visible = shown;
        }
        if (shown) {
            Ui.round(graphics, left, top, right - left, bottom - top, Ui.PANEL);
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        lastMouseMoveMs = Util.getMillis();
        super.mouseMoved(mouseX, mouseY);
    }

    /** This console's keys and mouse look; the game keeps running behind. */
    private void openControls() {
        childOpen = true;
        minecraft.setScreen(new ControlsScreen(this, profile));
    }

    /** Shuts the console down (the cartridge stays in) and goes back to the world. */
    private void powerOff() {
        ConsoleSessions.stop(active);
        onClose();
    }

    /** The controls, as a card of labels and key caps at the bottom, for a few seconds after opening. */
    private void renderHint(GuiGraphics graphics) {
        long elapsed = Util.getMillis() - openedAtMs;
        if (elapsed >= HINT_VISIBLE_MS + HINT_FADE_MS) {
            return;
        }
        float fade = elapsed <= HINT_VISIBLE_MS ? 1f : 1f - (elapsed - HINT_VISIBLE_MS) / (float) HINT_FADE_MS;
        // Font treats alpha below 4 as fully opaque, so stop drawing just before that.
        if (fade * 255 < 8) {
            return;
        }
        int maxWidth = Math.min(HINT_MAX_WIDTH, width - 40);
        int rowHeight = Ui.KEY_CAP_HEIGHT + 4;
        // Lay out first: hints flow left to right and wrap; a line of text gets a row of its own.
        List<List<Hint>> rows = new ArrayList<>();
        List<Hint> row = new ArrayList<>();
        int rowWidth = 0;
        int widest = 0;
        for (Hint hint : hints) {
            int w = hintWidth(hint);
            if (!row.isEmpty() && (hint.key() == null || rowWidth + w > maxWidth)) {
                rows.add(row);
                widest = Math.max(widest, rowWidth);
                row = new ArrayList<>();
                rowWidth = 0;
            }
            row.add(hint);
            rowWidth += w;
            if (hint.key() == null) {
                rows.add(row);
                widest = Math.max(widest, rowWidth);
                row = new ArrayList<>();
                rowWidth = 0;
            }
        }
        if (!row.isEmpty()) {
            rows.add(row);
            widest = Math.max(widest, rowWidth);
        }
        int cardW = widest + 2 * Ui.PAD;
        int cardH = rows.size() * rowHeight + 2 * Ui.PAD - 4;
        int cardX = (width - cardW) / 2;
        int cardY = height - cardH - 12;
        Ui.round(graphics, cardX, cardY, cardW, cardH, Ui.fade(Ui.PANEL, fade));
        for (int r = 0; r < rows.size(); r++) {
            int x = cardX + Ui.PAD;
            int y = cardY + Ui.PAD + r * rowHeight;
            for (Hint hint : rows.get(r)) {
                graphics.drawString(font, hint.label(), x, y + 3, Ui.fade(hint.key() == null ? Ui.TEXT : Ui.TEXT_DIM, fade), false);
                if (hint.key() != null) {
                    int capX = x + font.width(hint.label()) + 4;
                    Ui.keyCap(graphics, font, hint.key(), capX, y, Ui.TEXT, Ui.PANEL_BORDER, fade);
                }
                x += hintWidth(hint);
            }
        }
    }

    private int hintWidth(Hint hint) {
        int w = font.width(hint.label());
        return hint.key() == null ? w : w + 4 + Ui.keyCapWidth(font, hint.key()) + 12;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (matches(EmulatorKeys.FREE_MOUSE, keyCode, scanCode) && profile.mouseLook()) {
            cursorFree = !cursorFree;
            showStatus(Component.translatable(cursorFree ? "screen.retrohandhelds.mouse.freed" : "screen.retrohandhelds.mouse.captured"));
            return true;
        }
        if (matches(EmulatorKeys.SAVE_STATE, keyCode, scanCode)) {
            session.saveState().thenAccept(this::showStateResult);
            return true;
        }
        if (matches(EmulatorKeys.LOAD_STATE, keyCode, scanCode)) {
            session.loadState().thenAccept(this::showStateResult);
            return true;
        }
        if (matches(EmulatorKeys.TOGGLE_VIEW, keyCode, scanCode)) {
            toggleView();
            return true;
        }
        if (matches(EmulatorKeys.FAST_FORWARD, keyCode, scanCode)) {
            session.setSpeed(RetroClientConfig.fastForwardSpeed());
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
        if (matches(EmulatorKeys.MUTE_OTHERS, keyCode, scanCode)) {
            showStatus(ConsoleSessions.toggleOtherPlayersMute());
            return true;
        }
        if (matches(EmulatorKeys.OTHERS_VOLUME_DOWN, keyCode, scanCode)) {
            showStatus(ConsoleSessions.changeOtherPlayersVolume(-1));
            return true;
        }
        if (matches(EmulatorKeys.OTHERS_VOLUME_UP, keyCode, scanCode)) {
            showStatus(ConsoleSessions.changeOtherPlayersVolume(1));
            return true;
        }
        if (keyCode != GLFW.GLFW_KEY_ESCAPE && setControls(Binding.key(keyCode), true)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (matches(EmulatorKeys.FAST_FORWARD, keyCode, scanCode)) {
            session.setSpeed(1);
            return true;
        }
        return setControls(Binding.key(keyCode), false) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // While the mouse looks around the cursor is hidden: clicks only press bound controls, never
        // a button the hidden cursor happens to be over. Otherwise widgets (Power Off) first, then the
        // touchscreen, then bound controls.
        if (captured) {
            setControls(Binding.mouseButton(button), true);
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (controls.touch() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && overPicture(mouseX, mouseY)) {
            touching = true;
            return true;
        }
        return setControls(Binding.mouseButton(button), true);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (controls.touch() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && touching) {
            touching = false;
            return true;
        }
        return setControls(Binding.mouseButton(button), false) || super.mouseReleased(mouseX, mouseY, button);
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

    /** The mouse over the picture as a libretro pointer; touching only while held over the picture. */
    private void updatePointer(double mouseX, double mouseY) {
        if (pictureWidth <= 0 || pictureHeight <= 0) {
            session.joypad().setPointer(0, 0, false);
            return;
        }
        double u = (mouseX - pictureX) / pictureWidth;
        double v = (mouseY - pictureY) / pictureHeight;
        boolean inside = u >= 0 && u < 1 && v >= 0 && v < 1;
        session.joypad().setPointer(JoypadState.toPointer(u), JoypadState.toPointer(v), touching && inside);
    }

    private boolean overPicture(double mouseX, double mouseY) {
        return mouseX >= pictureX && mouseX < pictureX + pictureWidth && mouseY >= pictureY && mouseY < pictureY + pictureHeight;
    }

    /** Presses or releases every control of this console bound to {@code binding}. */
    private boolean setControls(Binding binding, boolean down) {
        List<ControlAction> actions = profile.actionsFor(binding);
        for (ControlAction action : actions) {
            action.apply(session.joypad(), down);
        }
        return !actions.isEmpty();
    }

    /**
     * Mouse look: while on, the cursor is hidden and its movement tilts the right stick. Off while
     * the player freed the cursor, a screen of ours is on top or the window is in the background.
     */
    private void updateMouseLook() {
        boolean wanted = profile.mouseLook() && !cursorFree && !childOpen && minecraft.isWindowActive();
        if (wanted != captured) {
            setCaptured(wanted);
        }
        if (!captured) {
            session.joypad().setMouseStick(0, 0);
            return;
        }
        GLFW.glfwGetCursorPos(minecraft.getWindow().getWindow(), cursorX, cursorY);
        long now = System.nanoTime();
        double dx = cursorX[0] - lastCursorX;
        double dy = profile.mouseVertical() ? cursorY[0] - lastCursorY : 0;
        float sensitivity = profile.sensitivity();
        MouseLook.Mode mode = profile.preset().mouseMode();
        if (mode == MouseLook.Mode.TAPS) {
            mouseLook.updateTaps(dx, dy, now, sensitivity);
        } else {
            mouseLook.update(dx, dy, (now - lastLookNanos) / 1e9);
        }
        lastCursorX = cursorX[0];
        lastCursorY = cursorY[0];
        lastLookNanos = now;
        session.joypad().setMouseStick(mouseLook.stickX(mode, sensitivity), mouseLook.stickY(mode, sensitivity));
    }

    /**
     * Hides or shows the cursor through GLFW directly: Minecraft's own mouse grab would also turn the
     * player behind this screen.
     */
    private void setCaptured(boolean capture) {
        long window = minecraft.getWindow().getWindow();
        captured = capture;
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, capture ? GLFW.GLFW_CURSOR_DISABLED : GLFW.GLFW_CURSOR_NORMAL);
        if (!capture) {
            // Back in the middle, as Minecraft does when it lets go of the mouse.
            GLFW.glfwSetCursorPos(window, minecraft.getWindow().getScreenWidth() / 2.0, minecraft.getWindow().getScreenHeight() / 2.0);
        }
        GLFW.glfwGetCursorPos(window, cursorX, cursorY);
        lastCursorX = cursorX[0];
        lastCursorY = cursorY[0];
        lastLookNanos = System.nanoTime();
        mouseLook.reset();
    }

    /** Whether this screen shows {@code session}. */
    public boolean shows(ConsoleSessions.Active session) {
        return active == session;
    }

    /** Whether this screen plays a handheld (drawn in the player's hands in the in-world view). */
    public boolean showsHandheld() {
        return active.handheld();
    }

    /** Whether the game is watched in the world rather than full screen. */
    public boolean inWorldView() {
        return inWorldView;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (captured) {
            setCaptured(false);
        }
        session.joypad().clear();
        session.setSpeed(1);
        if (stopOnClose && !childOpen) {
            ConsoleSessions.stop(active);
        }
    }
}
