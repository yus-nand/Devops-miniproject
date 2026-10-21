package com.grievanceportal.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grievanceportal.model.Role;
import org.junit.jupiter.api.Test;

class AccessRulesTest {

    @Test
    void policyholderMayOpenAndSubmitTheForm() {
        assertTrue(AccessRules.isAllowed("GET", "/grievances/new", Role.POLICYHOLDER));
        assertTrue(AccessRules.isAllowed("POST", "/grievances/new", Role.POLICYHOLDER));
    }

    @Test
    void staffMayNotSubmit() {
        assertFalse(AccessRules.isAllowed("GET", "/grievances/new", Role.OFFICER));
        assertFalse(AccessRules.isAllowed("POST", "/grievances/new", Role.ADMIN));
    }

    @Test
    void onlyStaffMayListAllGrievances() {
        assertTrue(AccessRules.isAllowed("GET", "/grievances", Role.OFFICER));
        assertTrue(AccessRules.isAllowed("GET", "/grievances", Role.ADMIN));
        assertFalse(AccessRules.isAllowed("GET", "/grievances", Role.POLICYHOLDER));
    }

    @Test
    void onlyAdminMayOpenTheDashboard() {
        assertTrue(AccessRules.isAllowed("GET", "/dashboard", Role.ADMIN));
        assertFalse(AccessRules.isAllowed("GET", "/dashboard", Role.OFFICER));
        assertFalse(AccessRules.isAllowed("GET", "/dashboard", Role.POLICYHOLDER));
    }

    @Test
    void onlyOfficerMayPickUp() {
        assertTrue(AccessRules.isAllowed("POST", "/grievances/pickup", Role.OFFICER));
        assertFalse(AccessRules.isAllowed("POST", "/grievances/pickup", Role.ADMIN));
        assertFalse(AccessRules.isAllowed("POST", "/grievances/pickup", Role.POLICYHOLDER));
    }

    @Test
    void policyholderMayNotChangeStatus() {
        assertFalse(AccessRules.isAllowed("POST", "/grievances/status", Role.POLICYHOLDER));
        assertTrue(AccessRules.isAllowed("POST", "/grievances/status", Role.OFFICER));
        assertTrue(AccessRules.isAllowed("POST", "/grievances/status", Role.ADMIN));
    }

    @Test
    void onlyPolicyholdersMayEdit() {
        assertTrue(AccessRules.isAllowed("GET", "/grievances/edit", Role.POLICYHOLDER));
        assertTrue(AccessRules.isAllowed("POST", "/grievances/edit", Role.POLICYHOLDER));
        assertFalse(AccessRules.isAllowed("GET", "/grievances/edit", Role.OFFICER));
        assertFalse(AccessRules.isAllowed("POST", "/grievances/edit", Role.ADMIN));
    }

    @Test
    void detailPageIsOpenToEveryRole() {
        for (Role role : Role.values()) {
            assertTrue(AccessRules.isAllowed("GET", "/grievances/view", role));
        }
    }

    @Test
    void unlistedPathsAreOpenToEveryRole() {
        for (Role role : Role.values()) {
            assertTrue(AccessRules.isAllowed("GET", "/home", role));
        }
    }
}
