package com.grievanceportal.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Build information read from app.properties. */
public final class AppInfo {

    private static final String VERSION = loadVersion();

    private AppInfo() {
    }

    public static String version() {
        return VERSION;
    }

    private static String loadVersion() {
        try (InputStream in = AppInfo.class.getResourceAsStream("/app.properties")) {
            if (in == null) {
                return "unknown";
            }
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("app.version", "unknown");
        } catch (IOException e) {
            return "unknown";
        }
    }
}