package com.grievanceportal.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Journey J4: role restrictions. Pages a role may not use answer with Access denied (403). */
class AccessControlTest extends UiTestBase {

    @Test
    void policyholderCannotOpenTheDashboard() {
        login("asha");
        driver.get(url("/dashboard"));
        assertAccessDenied();
    }

    @Test
    void policyholderCannotOpenTheStaffQueue() {
        login("ravi");
        driver.get(url("/grievances"));
        assertAccessDenied();
    }

    @Test
    void officerCannotOpenTheDashboard() {
        login("officer1");
        driver.get(url("/dashboard"));
        assertAccessDenied();
    }

    @Test
    void visitorWithoutLoginIsSentToTheLoginPage() {
        driver.get(url("/grievances/mine"));
        byId("login-form");
        assertTrue(driver.getCurrentUrl().endsWith("/login"), "Expected the login page: " + driver.getCurrentUrl());
    }
}
