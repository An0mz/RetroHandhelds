package dev.anomz.retrohandhelds.client.controls;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.anomz.retrohandhelds.RetroHandhelds;
import dev.anomz.retrohandhelds.RetroSystem;
import dev.anomz.retrohandhelds.client.RetroPaths;
import dev.anomz.retrohandhelds.client.emu.AtomicFiles;

/**
 * Every console's controls, kept in {@code retro_handhelds/controls.json}: per console, only the
 * bindings the player changed, plus mouse look, whether it is vertical too, and its sensitivity. Read once, on first use; anything
 * missing or unreadable falls back to the preset. Render thread only.
 */
public final class ControlProfiles {
    private static Map<RetroSystem, ControlProfile> profiles;

    private ControlProfiles() {
    }

    public static ControlProfile get(RetroSystem system) {
        if (profiles == null) {
            profiles = load(RetroPaths.controls());
        }
        return profiles.computeIfAbsent(system, ControlProfile::new);
    }

    /** Writes every console's changes. */
    public static void save() {
        if (profiles == null) {
            return;
        }
        Path file = RetroPaths.controls();
        try {
            byte[] bytes = write(profiles.values()).getBytes(StandardCharsets.UTF_8);
            AtomicFiles.write(file, bytes, bytes.length);
        } catch (IOException e) {
            RetroHandhelds.LOGGER.error("Retro Handhelds: could not save controls to {}", file, e);
        }
    }

    private static Map<RetroSystem, ControlProfile> load(Path file) {
        if (!Files.isRegularFile(file)) {
            return new EnumMap<>(RetroSystem.class);
        }
        try {
            return read(Files.readString(file));
        } catch (IOException | RuntimeException e) {
            RetroHandhelds.LOGGER.warn("Retro Handhelds: could not read {}, using the default controls", file, e);
            return new EnumMap<>(RetroSystem.class);
        }
    }

    /** Parses saved controls, skipping whatever it does not understand. */
    static Map<RetroSystem, ControlProfile> read(String json) {
        Map<RetroSystem, ControlProfile> result = new EnumMap<>(RetroSystem.class);
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            return result;
        }
        for (Map.Entry<String, JsonElement> console : root.getAsJsonObject().entrySet()) {
            RetroSystem system = systemNamed(console.getKey());
            if (system == null || !console.getValue().isJsonObject()) {
                continue;
            }
            JsonObject saved = console.getValue().getAsJsonObject();
            ControlProfile profile = new ControlProfile(system);
            if (saved.get("bindings") instanceof JsonObject bindings) {
                for (Map.Entry<String, JsonElement> binding : bindings.entrySet()) {
                    ControlAction action = actionNamed(binding.getKey());
                    if (action != null && binding.getValue().isJsonPrimitive()) {
                        Binding.parse(binding.getValue().getAsString()).ifPresent(b -> profile.set(action, b));
                    }
                }
            }
            if (saved.get("mouseLook") instanceof JsonElement look && look.isJsonPrimitive() && look.getAsJsonPrimitive().isBoolean()) {
                profile.setMouseLook(look.getAsBoolean());
            }
            if (saved.get("mouseVertical") instanceof JsonElement vertical && vertical.isJsonPrimitive()
                    && vertical.getAsJsonPrimitive().isBoolean()) {
                profile.setMouseVertical(vertical.getAsBoolean());
            }
            if (saved.get("mouseSensitivity") instanceof JsonElement sens && sens.isJsonPrimitive() && sens.getAsJsonPrimitive().isNumber()) {
                profile.setSensitivity(sens.getAsFloat());
            }
            result.put(system, profile);
        }
        return result;
    }

    /** Serializes the changed parts of {@code profiles}. */
    static String write(Collection<ControlProfile> profiles) {
        JsonObject root = new JsonObject();
        for (ControlProfile profile : profiles) {
            if (!profile.isChanged()) {
                continue;
            }
            JsonObject saved = new JsonObject();
            JsonObject bindings = new JsonObject();
            profile.changedBindings().forEach((action, binding) -> bindings.addProperty(action.name(), binding.serialize()));
            saved.add("bindings", bindings);
            if (profile.mouseLookOverride() != null) {
                saved.addProperty("mouseLook", profile.mouseLookOverride());
            }
            if (profile.mouseVerticalOverride() != null) {
                saved.addProperty("mouseVertical", profile.mouseVerticalOverride());
            }
            saved.addProperty("mouseSensitivity", profile.sensitivity());
            root.add(profile.system().name().toLowerCase(Locale.ROOT), saved);
        }
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    private static RetroSystem systemNamed(String name) {
        try {
            return RetroSystem.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static ControlAction actionNamed(String name) {
        try {
            return ControlAction.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
