package com.grievanceportal.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/**
 * Journey J5: the admin dashboard. The database keeps its data between runs, so these tests compare
 * numbers with each other (before and after, dashboard and list) and never expect fixed totals.
 */
class DashboardUiTest extends UiTestBase {

    private int number(String elementId) {
        return Integer.parseInt(byId(elementId).getText().trim());
    }

    @Test
    void totalGrowsByOneWhenAPolicyholderSubmitsAGrievance() {
        login("admin1");
        driver.get(url("/dashboard"));
        int before = number("total-count");
        logout();

        login("asha");
        submitGrievance(uniqueTitle("J5 count"));
        logout();

        login("admin1");
        driver.get(url("/dashboard"));
        assertEquals(before + 1, number("total-count"));
    }

    @Test
    void totalLinkOpensAListWithTheSameNumberOfGrievances() {
        login("admin1");
        driver.get(url("/dashboard"));
        int total = number("total-count");
        assertTrue(total > 0, "The demo data should contain grievances");

        byId("total-count").click();
        assertTrue(byId("result-count").getText().startsWith(total + " grievance"),
                "List should show " + total + " grievances but shows: " + byId("result-count").getText());
    }

    @Test
    void submittedCountLinkOpensTheFilteredList() {
        login("admin1");
        driver.get(url("/dashboard"));
        int submitted = Integer.parseInt(driver.findElement(By.cssSelector("#status-SUBMITTED .col-count a"))
                .getText().trim());
        driver.findElement(By.cssSelector("#status-SUBMITTED .col-count a")).click();

        if (submitted == 0) {
            assertTrue(byId("empty-message").isDisplayed());
        } else {
            assertTrue(byId("result-count").getText().startsWith(submitted + " grievance"));
        }
    }
}
