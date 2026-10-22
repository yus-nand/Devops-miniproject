package com.grievanceportal.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Shared set-up for the browser tests. They run against an application that is already deployed
 * (default http://localhost:8080/grievance-portal) and use the demo accounts, whose password is demo123.
 * Change the address with -Dui.baseUrl=...
 */
@Tag("ui")
@ExtendWith(ScreenshotOnFailure.class)
abstract class UiTestBase {

    static final String BASE_URL = System.getProperty("ui.baseUrl", "http://localhost:8080/grievance-portal");
    static final String PASSWORD = "demo123";

    WebDriver driver;
    WebDriverWait wait;

    @BeforeEach
    void startBrowser() {
        driver = Browsers.start();
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    void stopBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    String url(String path) {
        return BASE_URL + path;
    }

    WebElement byId(String id) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));
    }

    /**
     * Clicks a button that submits a form and waits until the old page is gone. Without the wait, the next
     * byId() can find an element on the page that is just about to be replaced and fail with a stale element.
     */
    void clickAndWaitForNewPage(String id) {
        WebElement button = byId(id);
        button.click();
        wait.until(ExpectedConditions.stalenessOf(button));
    }

    void login(String username) {
        driver.get(url("/login"));
        byId("username").sendKeys(username);
        byId("password").sendKeys(PASSWORD);
        byId("login-button").click();
        byId("welcome");
    }

    void logout() {
        byId("logout-button").click();
        byId("login-form");
    }

    /** Text that is unique to this run, so that a test can find its own grievance among the others. */
    static String uniqueTitle(String prefix) {
        return prefix + " " + System.currentTimeMillis();
    }

    /** Submits a valid grievance as the logged-in policyholder and returns its reference ID. */
    String submitGrievance(String title) {
        driver.get(url("/grievances/new"));
        byId("policyNumber").sendKeys("POL-UI-" + (System.currentTimeMillis() % 100000));
        new Select(byId("category")).selectByVisibleText("Claim settlement");
        byId("title").sendKeys(title);
        byId("description").sendKeys("Created by the automated Selenium suite. Safe to ignore.");
        byId("submit-grievance").click();
        return byId("ref-no").getText().trim();
    }

    /** Opens the detail page of a grievance by searching the staff queue for its reference ID. */
    void openFromQueue(String refNo) {
        driver.get(url("/grievances?q=" + refNo));
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a.ref-link"))).click();
        byId("detail-heading");
    }

    void assertAccessDenied() {
        assertTrue(byId("error-heading").getText().contains("Access denied"),
                "Expected the Access denied page");
        assertTrue(byId("error-code").getText().contains("403"), "Expected error code 403");
    }
}
