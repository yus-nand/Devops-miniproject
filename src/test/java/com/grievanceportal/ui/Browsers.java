package com.grievanceportal.ui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;

/**
 * Starts the browser for the UI tests.
 *
 * <p>Settings (all optional, passed as -D options on the Maven command line):
 * <ul>
 *   <li>{@code ui.browser.path}: full path to the browser program, for example brave.exe</li>
 *   <li>{@code ui.browser}: brave, chrome or edge, to prefer one of the installed browsers</li>
 *   <li>{@code ui.driver.path}: a chromedriver.exe to use instead of the automatic download</li>
 *   <li>{@code ui.headless}: true (default) runs without a window, false shows the browser</li>
 * </ul>
 * Without settings the first of Brave, Chrome and Edge found in the usual install folders is used.
 */
final class Browsers {

    private static final String[] ORDER = {"brave", "chrome", "edge"};

    private Browsers() {
    }

    static WebDriver start() {
        boolean headless = !"false".equalsIgnoreCase(System.getProperty("ui.headless", "true"));
        Path binary = findBinary();
        List<String> arguments = new ArrayList<>(List.of("--window-size=1280,900", "--disable-gpu"));
        if (headless) {
            arguments.add("--headless=new");
        }

        if (binary != null && binary.getFileName().toString().toLowerCase(Locale.ROOT).contains("msedge")) {
            EdgeOptions options = new EdgeOptions();
            options.setBinary(binary.toString());
            options.addArguments(arguments);
            return new EdgeDriver(options);
        }

        String driverPath = System.getProperty("ui.driver.path");
        if (driverPath != null && !driverPath.isBlank()) {
            System.setProperty("webdriver.chrome.driver", driverPath);
        }
        ChromeOptions options = new ChromeOptions();
        if (binary != null) {
            options.setBinary(binary.toString());
        }
        options.addArguments(arguments);
        return new ChromeDriver(options);
    }

    /** Returns the browser program to use, or null to let Selenium look for Chrome itself. */
    static Path findBinary() {
        String explicit = System.getProperty("ui.browser.path");
        if (explicit != null && !explicit.isBlank()) {
            Path path = Path.of(explicit);
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("ui.browser.path does not point to a file: " + explicit);
            }
            return path;
        }
        String wanted = System.getProperty("ui.browser", "").toLowerCase(Locale.ROOT);
        for (String name : ORDER) {
            if (!wanted.isEmpty() && !wanted.equals(name)) {
                continue;
            }
            for (Path candidate : candidates(name)) {
                if (Files.isRegularFile(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static List<Path> candidates(String name) {
        List<Path> paths = new ArrayList<>();
        String relative;
        switch (name) {
            case "brave":
                relative = "BraveSoftware\\Brave-Browser\\Application\\brave.exe";
                break;
            case "chrome":
                relative = "Google\\Chrome\\Application\\chrome.exe";
                break;
            default:
                relative = "Microsoft\\Edge\\Application\\msedge.exe";
                break;
        }
        for (String variable : new String[] {"ProgramFiles", "ProgramFiles(x86)", "LOCALAPPDATA"}) {
            String base = System.getenv(variable);
            if (base != null && !base.isBlank()) {
                paths.add(Path.of(base, relative));
            }
        }
        return paths;
    }
}
