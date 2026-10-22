package com.grievanceportal.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** Journey J1: a policyholder submits a grievance and finds it in My grievances. */
class SubmitGrievanceTest extends UiTestBase {

    @Test
    void submittedGrievanceGetsAReferenceAndAppearsInMyGrievances() {
        String title = uniqueTitle("J1 submit");
        login("asha");

        String refNo = submitGrievance(title);

        assertTrue(refNo.matches("GRV-\\d{4,}"), "Reference ID looks wrong: " + refNo);
        assertEquals("Submitted", byId("status").getText());

        driver.get(url("/grievances/mine"));
        byId("grievance-table");
        List<WebElement> rows = driver.findElements(
                By.xpath("//td[@class='col-title'][normalize-space()='" + title + "']"));
        assertEquals(1, rows.size(), "The new grievance should be listed exactly once");
    }

    @Test
    void emptyFormShowsAMessageNextToEveryField() {
        login("asha");
        driver.get(url("/grievances/new"));
        byId("submit-grievance").click();

        assertTrue(byId("policyNumber-error").isDisplayed());
        assertTrue(byId("category-error").isDisplayed());
        assertTrue(byId("title-error").isDisplayed());
        assertTrue(byId("description-error").isDisplayed());
    }
}
