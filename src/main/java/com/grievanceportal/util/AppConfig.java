    package com.grievanceportal.util;

/** Reads settings from system properties, then environment variables, then defaults. */
public final class AppConfig {

    private AppConfig() {
    }

    public static String get(String name, String defaultValue) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            value = System.getenv(name);
        }
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /** Environment label shown in the page footer. */
    public static String envName() {
        return get("GP_ENV_NAME", "local");
    }

    /** JDBC URL for the H2 database. A path starting with "mem:" gives an in-memory database. */
    public static String dbUrl() {
        String base = System.getProperty("catalina.base");
        String defaultPath = (base != null ? base + "/data" : "./data") + "/grievance-db";
        String path = get("GP_DB_PATH", defaultPath);
        if (path.startsWith("mem:")) {
            return "jdbc:h2:" + path + ";DB_CLOSE_DELAY=-1";
        }
        return "jdbc:h2:file:" + path;
    }
}