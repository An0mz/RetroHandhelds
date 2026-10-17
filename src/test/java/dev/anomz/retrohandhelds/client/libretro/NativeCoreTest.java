package dev.anomz.retrohandhelds.client.libretro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Checks that every core bundled for the current OS/architecture extracts, loads and starts.
 * CI runs this on Windows, Linux and macOS, which is the only check those platforms get.
 */
class NativeCoreTest {
    // Not a JUnit @TempDir: Windows cannot delete a DLL while it is loaded. `gradle clean` removes this.
    private static final Path NATIVES_DIR = Path.of("build", "test-natives");

    @ParameterizedTest
    @EnumSource(CoreKind.class)
    void bundledCoreLoadsAndInitialises(CoreKind kind) throws Exception {
        LibretroCore core = NativeLoader.loadInto(NATIVES_DIR, kind);
        assertEquals(1, core.retro_api_version(), "libretro API version");

        // The environment callback must stay reachable while the core may call it.
        LibretroCore.EnvironmentCallback environment = (cmd, data) -> LibretroConstants.FALSE;
        core.retro_set_environment(environment);
        core.retro_init();
        core.retro_deinit();
    }

    @ParameterizedTest
    @EnumSource(CoreKind.class)
    void secondLoadReusesTheExtractedFile(CoreKind kind) throws Exception {
        NativeLoader.loadInto(NATIVES_DIR, kind);
        LibretroCore again = NativeLoader.loadInto(NATIVES_DIR, kind);
        assertEquals(1, again.retro_api_version());
    }
}
