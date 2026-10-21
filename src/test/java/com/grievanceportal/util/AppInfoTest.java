package com.grievanceportal.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class AppInfoTest {

    @Test
    void versionComesFromTheBuild() {
        String version = AppInfo.version();
        assertFalse(version.isBlank());
        assertNotEquals("unknown", version);
        assertFalse(version.contains("${"), "Maven filtering did not replace the placeholder");
    }
}