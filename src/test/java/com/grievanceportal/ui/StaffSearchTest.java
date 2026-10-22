package com.grievanceportal.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/** Journey J3: staff search the queue by reference ID. */
class StaffSearchTest extends UiTestBase {

    @Test
    void searchingByReferenceFindsExactlyThatGrievanceWhatEverTheCase() {
        String title = uniqueTitle("J3 search");
        login("asha");
        String refNo = submitGrievance(title);
        logout();

        login("officer1");
        for (String query : new String[] {refNo, refNo.toLowerCase()}) {
            driver.get(url("/grievances"));
            byId("q").clear();
            byId("q").sendKeys(query);
            byId("search-button").click();

            assertTrue(byId("result-count").getText().startsWith("1 grievance"),
                    "Expected one result for " + query);
            assertEquals(refNo, driver.findElement(By.cssSelector("td.col-ref")).getText().trim());
            assertEquals(title, driver.findElement(By.cssSelector("td.col-title")).getText().trim());
        }
    }

    @Test
    void unknownReferenceShowsANoResultsMessage() {
        login("officer1");
        driver.get(url("/grievances?q=GRV-000000"));
        assertTrue(byId("empty-message").getText().contains("No grievances match"));
    }
}
