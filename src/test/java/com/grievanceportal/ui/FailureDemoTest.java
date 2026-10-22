package com.grievanceportal.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/**
 * Evidence for "a screenshot is saved when a test fails". It is skipped normally and fails on purpose
 * only when you run it with -Dui.forceFail=true.
 */
class FailureDemoTest extends UiTestBase {

    @Test
    void deliberateFailureToShowTheScreenshot() {
        assumeTrue(Boolean.getBoolean("ui.forceFail"), "Run with -Dui.forceFail=true to see it fail");
        driver.get(url("/login"));
        assertEquals("This heading does not exist", driver.findElement(By.tagName("h1")).getText());
    }
}
