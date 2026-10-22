package dev.anomz.retrohandhelds.client.controls;

import java.util.Optional;

/**
 * A keyboard key (GLFW key code) or mouse button (GLFW button number) bound to a control, or
 * {@link #NONE}. Stored as {@code key:<code>} / {@code mouse:<button>}.
 */
public record Binding(boolean mouse, int code) {
    public static final Binding NONE = new Binding(false, -1);

    public static Binding key(int glfwKey) {
        return glfwKey < 0 ? NONE : new Binding(false, glfwKey);
    }

    public static Binding mouseButton(int glfwButton) {
        return glfwButton < 0 ? NONE : new Binding(true, glfwButton);
    }

    public boolean isBound() {
        return code >= 0;
    }

    public String serialize() {
        return isBound() ? (mouse ? "mouse:" : "key:") + code : "none";
    }

    /** The binding {@code text} names, or empty if it names none this mod understands. */
    public static Optional<Binding> parse(String text) {
        if ("none".equals(text)) {
            return Optional.of(NONE);
        }
        int colon = text == null ? -1 : text.indexOf(':');
        if (colon < 0) {
            return Optional.empty();
        }
        String kind = text.substring(0, colon);
        int code;
        try {
            code = Integer.parseInt(text.substring(colon + 1));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        if (code < 0 || code > 512) {
            return Optional.empty();
        }
        return switch (kind) {
            case "key" -> Optional.of(key(code));
            case "mouse" -> Optional.of(mouseButton(code));
            default -> Optional.empty();
        };
    }
}
