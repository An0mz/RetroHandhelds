package dev.anomz.retrohandhelds.client.screen;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import dev.anomz.retrohandhelds.client.controls.Binding;
import dev.anomz.retrohandhelds.client.controls.ControlAction;
import dev.anomz.retrohandhelds.client.controls.ControlPreset;
import dev.anomz.retrohandhelds.client.controls.ControlProfile;
import dev.anomz.retrohandhelds.client.controls.ControlProfiles;
import dev.anomz.retrohandhelds.client.input.EmulatorKeys;
import dev.anomz.retrohandhelds.client.screen.ui.PanelScreen;
import dev.anomz.retrohandhelds.client.screen.ui.ScrollList;
import dev.anomz.retrohandhelds.client.screen.ui.TextButton;
import dev.anomz.retrohandhelds.client.screen.ui.Toggle;
import dev.anomz.retrohandhelds.client.screen.ui.Ui;
import dev.anomz.retrohandhelds.client.screen.ui.UiSlider;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * One console's keys: click a control, then press a key or mouse button; Esc or a right click
 * unbinds. A key shown in red does something else too, and the line under the list says what.
 * Saved to {@code controls.json} when the screen closes.
 */
public final class ControlsScreen extends PanelScreen {
    private static final int ROW_HEIGHT = 20;
    /** Emulator keys checked before any console control in the emulator screen. */
    private static final List<KeyMapping> EMULATOR_KEYS = List.of(EmulatorKeys.SAVE_STATE, EmulatorKeys.LOAD_STATE,
            EmulatorKeys.FAST_FORWARD, EmulatorKeys.TOGGLE_VIEW, EmulatorKeys.FREE_MOUSE, EmulatorKeys.MUTE,
            EmulatorKeys.VOLUME_DOWN, EmulatorKeys.VOLUME_UP, EmulatorKeys.MUTE_OTHERS, EmulatorKeys.OTHERS_VOLUME_DOWN,
            EmulatorKeys.OTHERS_VOLUME_UP);

    private final ControlProfile profile;
    /** The control waiting for its new key, or null. */
    @Nullable
    private ControlAction waitingFor;
    private int clashY;

    ControlsScreen(Screen parent, ControlProfile profile) {
        super(Component.translatable("screen.retrohandhelds.controls.title", profile.system().displayName()), parent);
        this.profile = profile;
        subtitle = Component.translatable("screen.retrohandhelds.controls.help");
    }

    /** How a binding reads on screen: the key's own name, a mouse button, or "-" when unbound. */
    public static Component keyName(Binding binding) {
        if (!binding.isBound()) {
            return Component.literal("-");
        }
        InputConstants.Type type = binding.mouse() ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM;
        return type.getOrCreate(binding.code()).getDisplayName();
    }

    private boolean analog() {
        return profile.system().controls().analog;
    }

    @Override
    protected int panelWidth() {
        return 360;
    }

    @Override
    protected int panelHeight() {
        return 340;
    }

    @Override
    protected void initContent() {
        int x = contentLeft();
        int w = contentWidth();
        int bottom = contentBottom();
        int buttonsY = bottom - 20;
        clashY = buttonsY - 14;
        int footerTop = clashY;
        if (analog()) {
            footerTop -= 48;
            int half = (w - 6) / 2;
            addRenderableWidget(new Toggle(x, footerTop, half, 20, Component.translatable("screen.retrohandhelds.controls.mouse_look"),
                    profile.mouseLook(), profile::setMouseLook));
            addRenderableWidget(new Toggle(x + half + 6, footerTop, half, 20, Component.translatable("screen.retrohandhelds.controls.mouse_vertical"),
                    profile.mouseVertical(), profile::setMouseVertical));
            addRenderableWidget(new SensitivitySlider(x + 110, footerTop + 24, w - 110 - 36));
        }
        ScrollList<ControlPreset.Entry> list = addRenderableWidget(new ScrollList<>(x, contentTop(), w,
                footerTop - 6 - contentTop(), ROW_HEIGHT, title, this::renderRow, this::clickRow));
        list.setItems(profile.preset().entries());

        addRenderableWidget(new TextButton(x, buttonsY, 110, 20, Component.translatable("screen.retrohandhelds.controls.reset"),
                TextButton.Style.QUIET, () -> {
                    profile.reset();
                    waitingFor = null;
                    rebuildWidgets();
                }));
        addRenderableWidget(new TextButton(x + w - 80, buttonsY, 80, 20, Component.translatable("gui.done"),
                TextButton.Style.PRIMARY, this::onClose));
    }

    private void renderRow(GuiGraphics g, ControlPreset.Entry entry, int x, int y, int w, int h, boolean hovered) {
        boolean waiting = waitingFor == entry.action();
        if (hovered || waiting) {
            Ui.round(g, x, y + 1, w, h - 2, Ui.SURFACE);
        }
        g.drawString(font, entry.label(), x + 6, y + (h - font.lineHeight) / 2 + 1, Ui.TEXT, false);
        Binding binding = profile.binding(entry.action());
        Component key;
        int textColor;
        int border;
        if (waiting) {
            key = Component.translatable("screen.retrohandhelds.controls.press_key");
            textColor = Ui.ACCENT_HOVER;
            border = Ui.ACCENT;
        } else if (clash(entry.action()) != null) {
            key = keyName(binding);
            textColor = Ui.DANGER;
            border = Ui.DANGER;
        } else {
            key = keyName(binding);
            textColor = binding.isBound() ? Ui.TEXT : Ui.TEXT_FAINT;
            border = hovered ? Ui.TEXT_FAINT : Ui.PANEL_BORDER;
        }
        int capW = Ui.keyCapWidth(font, key);
        Ui.keyCap(g, font, key, x + w - capW - 6, y + (h - Ui.KEY_CAP_HEIGHT) / 2, textColor, border, 1);
    }

    private void clickRow(ControlPreset.Entry entry, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            profile.set(entry.action(), Binding.NONE);
            waitingFor = null;
        } else {
            waitingFor = entry.action();
        }
    }

    /**
     * Why {@code action}'s key is shown in red: another control of this console or an emulator key
     * (which wins) uses it too. Null when it doesn't clash.
     */
    @Nullable
    private Component clash(ControlAction action) {
        Binding binding = profile.binding(action);
        if (!binding.isBound()) {
            return null;
        }
        for (KeyMapping key : EMULATOR_KEYS) {
            InputConstants.Type type = binding.mouse() ? InputConstants.Type.MOUSE : InputConstants.Type.KEYSYM;
            if (key.getKey().getType() == type && key.getKey().getValue() == binding.code()) {
                return Component.translatable("screen.retrohandhelds.controls.clash", keyName(binding),
                        Component.translatable(key.getName()));
            }
        }
        for (ControlPreset.Entry other : profile.preset().entries()) {
            if (other.action() != action && profile.binding(other.action()).equals(binding)) {
                return Component.translatable("screen.retrohandhelds.controls.clash", keyName(binding), other.label());
            }
        }
        return null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingFor != null) {
            profile.set(waitingFor, keyCode == GLFW.GLFW_KEY_ESCAPE ? Binding.NONE : Binding.key(keyCode));
            waitingFor = null;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (waitingFor != null) {
            profile.set(waitingFor, Binding.mouseButton(button));
            waitingFor = null;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = contentLeft();
        if (analog()) {
            int sliderY = clashY - 48 + 24;
            g.drawString(font, Component.translatable("screen.retrohandhelds.controls.sensitivity"), x + 4, sliderY + 6, Ui.TEXT, false);
            String value = String.format(Locale.ROOT, "%.1fx", profile.sensitivity());
            g.drawString(font, value, x + contentWidth() - font.width(value) - 4, sliderY + 6, Ui.TEXT_DIM, false);
        }
        for (ControlPreset.Entry entry : profile.preset().entries()) {
            Component reason = clash(entry.action());
            if (reason != null) {
                g.drawString(font, font.plainSubstrByWidth(reason.getString(), contentWidth()), x, clashY + 1, Ui.DANGER, false);
                break;
            }
        }
    }

    @Override
    public void onClose() {
        ControlProfiles.save();
        super.onClose();
    }

    private final class SensitivitySlider extends UiSlider {
        SensitivitySlider(int x, int y, int width) {
            super(x, y, width, 20, toSlider(profile.sensitivity()));
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("screen.retrohandhelds.controls.sensitivity"));
        }

        @Override
        protected void applyValue() {
            profile.setSensitivity(fromSlider(value));
        }

        private static double toSlider(float sensitivity) {
            return (sensitivity - ControlProfile.MIN_SENSITIVITY) / (ControlProfile.MAX_SENSITIVITY - ControlProfile.MIN_SENSITIVITY);
        }

        private static float fromSlider(double value) {
            return (float) (ControlProfile.MIN_SENSITIVITY + value * (ControlProfile.MAX_SENSITIVITY - ControlProfile.MIN_SENSITIVITY));
        }
    }
}
