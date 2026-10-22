package com.grievanceportal.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** Journey J2: an officer picks up a grievance and resolves it, and the policyholder sees the outcome. */
class OfficerWorkflowTest extends UiTestBase {

    @Test
    void officerPicksUpAndResolvesAndPolicyholderSeesTheOutcome() {
        String title = uniqueTitle("J2 resolve");
        login("asha");
        String refNo = submitGrievance(title);
        logout();

        login("officer1");
        openFromQueue(refNo);
        assertEquals("Submitted", byId("status").getText());

        byId("pickup-button").click();
        assertEquals("Under Review", byId("status").getText());
        assertEquals("Meera Joshi", byId("assigned-officer").getText());

        String note = "Claim paid on 22 Oct, cheque sent.";
        byId("note").sendKeys(note);
        byId("move-RESOLVED").click();
        assertEquals("Resolved", byId("status").getText());
        assertEquals(note, byId("resolution-note").getText());
        logout();

        login("asha");
        driver.get(url("/grievances/mine"));
        byId("grievance-table");
        List<WebElement> status = driver.findElements(By.xpath(
                "//tr[.//td[@class='col-title'][normalize-space()='" + title + "']]/td[@class='col-status']"));
        assertEquals(1, status.size());
        assertEquals("Resolved", status.get(0).getText());
    }

    @Test
    void resolvingWithoutANoteIsRefused() {
        login("asha");
        String refNo = submitGrievance(uniqueTitle("J2 no note"));
        logout();

        login("officer1");
        openFromQueue(refNo);
        byId("pickup-button").click();
        byId("move-RESOLVED").click();

        assertTrue(byId("action-error").isDisplayed(), "An error message should explain the refusal");
        assertEquals("Under Review", byId("status").getText());
    }
}
