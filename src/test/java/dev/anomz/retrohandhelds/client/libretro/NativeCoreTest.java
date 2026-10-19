package dev.anomz.retrohandhelds.client.libretro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import dev.anomz.retrohandhelds.RetroSystem;

/**
 * Checks that the cores bundled for the current OS/architecture extract, load and start.
 * CI runs this on Windows, Linux and macOS, which is the only check those platforms get.
 */
class NativeCoreTest {
    // Not a JUnit @TempDir: Windows cannot delete a DLL while it is loaded. `gradle clean` removes this.
    private static final Path NATIVES_DIR = Path.of("build", "test-natives");

    @Test
    void bundledCoreLoadsAndInitialises() throws Exception {
        LibretroCore core = NativeLoader.loadInto(NATIVES_DIR, "mgba");
        assertEquals(1, core.retro_api_version(), "libretro API version");

        // The environment callback must stay reachable while the core may call it.
        LibretroCore.EnvironmentCallback environment = (cmd, data) -> LibretroConstants.FALSE;
        core.retro_set_environment(environment);
        core.retro_init();
        core.retro_deinit();
    }

    @Test
    void everyBundledCoreLoadsAndInitialises() throws Exception {
        for (RetroSystem system : RetroSystem.values()) {
            LibretroCore core = NativeLoader.loadInto(NATIVES_DIR, system.bundledCore());
            assertEquals(1, core.retro_api_version(), system.bundledCore());
            RetroSystemInfo info = new RetroSystemInfo();
            core.retro_get_system_info(info);
            System.out.println(system.bundledCore() + ": " + info.libraryName + " " + info.libraryVersion
                    + " [" + info.validExtensions + "]");
            for (String extension : system.extensions()) {
                assertTrue(info.validExtensions.contains(extension), system.bundledCore() + " takes ." + extension);
            }
        }
    }

    @Test
    void secondLoadReusesTheExtractedFile() throws Exception {
        NativeLoader.loadInto(NATIVES_DIR, "mgba");
        LibretroCore again = NativeLoader.loadInto(NATIVES_DIR, "mgba");
        assertEquals(1, again.retro_api_version());
    }
}
