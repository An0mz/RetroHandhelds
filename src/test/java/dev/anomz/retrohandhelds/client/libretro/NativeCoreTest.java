package dev.anomz.retrohandhelds.client.libretro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Checks that the core bundled for the current OS/architecture extracts, loads and starts.
 * CI runs this on Windows, Linux and macOS, which is the only check those platforms get.
 */
class NativeCoreTest {
    // Not a JUnit @TempDir: Windows cannot delete a DLL while it is loaded. `gradle clean` removes this.
    private static final Path NATIVES_DIR = Path.of("build", "test-natives");

    @Test
    void bundledCoreLoadsAndInitialises() throws Exception {
        MgbaCore core = NativeLoader.loadInto(NATIVES_DIR);
        assertEquals(1, core.retro_api_version(), "libretro API version");

        // The environment callback must stay reachable while the core may call it.
        MgbaCore.EnvironmentCallback environment = (cmd, data) -> LibretroConstants.FALSE;
        core.retro_set_environment(environment);
        core.retro_init();
        core.retro_deinit();
    }

    @Test
    void secondLoadReusesTheExtractedFile() throws Exception {
        NativeLoader.loadInto(NATIVES_DIR);
        MgbaCore again = NativeLoader.loadInto(NATIVES_DIR);
        assertEquals(1, again.retro_api_version());
    }
}
