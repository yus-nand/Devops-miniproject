package com.grievanceportal.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

/**
 * Saves a screenshot of the browser to target/screenshots whenever a test method fails.
 * It runs before the browser is closed, then lets the failure carry on as normal.
 */
public class ScreenshotOnFailure implements TestExecutionExceptionHandler {

    static final Path FOLDER = Path.of("target", "screenshots");

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable failure) throws Throwable {
        Object test = context.getRequiredTestInstance();
        if (test instanceof UiTestBase) {
            UiTestBase base = (UiTestBase) test;
            if (base.driver instanceof TakesScreenshot) {
                String name = context.getRequiredTestClass().getSimpleName() + "."
                        + context.getRequiredTestMethod().getName() + ".png";
                try {
                    byte[] png = ((TakesScreenshot) base.driver).getScreenshotAs(OutputType.BYTES);
                    Files.createDirectories(FOLDER);
                    Path file = FOLDER.resolve(name);
                    Files.write(file, png);
                    System.out.println("Screenshot saved: " + file.toAbsolutePath());
                } catch (IOException | RuntimeException e) {
                    System.out.println("Could not save a screenshot: " + e.getMessage());
                }
            }
        }
        throw failure;
    }
}
