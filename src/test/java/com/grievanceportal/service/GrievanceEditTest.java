package com.grievanceportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grievanceportal.model.Category;
import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceDetail;
import com.grievanceportal.model.GrievanceForm;
import com.grievanceportal.model.Status;
import com.grievanceportal.model.User;
import com.grievanceportal.util.Schema;
import com.grievanceportal.util.Seeder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GrievanceEditTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneId.of("UTC"));

    private static User asha;
    private static User ravi;
    private static User officer;

    private final GrievanceService service = new GrievanceService(FIXED);

    @BeforeAll
    static void prepareDatabase() throws Exception {
        System.setProperty("GP_DB_PATH", "mem:edittest");
        Schema.init();
        Seeder.seedUsers();
        SampleData.seed(FIXED);
        AuthService auth = new AuthService();
        asha = auth.login("asha", Seeder.DEMO_PASSWORD).orElseThrow();
        ravi = auth.login("ravi", Seeder.DEMO_PASSWORD).orElseThrow();
        officer = auth.login("officer1", Seeder.DEMO_PASSWORD).orElseThrow();
    }

    @AfterAll
    static void clearSetting() {
        System.clearProperty("GP_DB_PATH");
    }

    private static GrievanceForm newValues() {
        return new GrievanceForm("POL-999999", "PREMIUM_AND_BILLING", "Corrected title",
                "Corrected description with more detail.");
    }

    /** A fresh Submitted grievance for Asha, so tests do not depend on each other. */
    private Grievance freshSubmitted() throws Exception {
        return service.submit(asha, new GrievanceForm("POL-12345", "OTHER", "Original title",
                "Original description text."));
    }

    @Test
    void editChangesTheFieldsAndKeepsReferenceDueDateAndStatus() throws Exception {
        Grievance original = freshSubmitted();
        Grievance saved = service.update(asha, original.getId(), newValues());
        assertEquals("Corrected title", saved.getTitle());
        assertEquals("POL-999999", saved.getPolicyNumber());
        assertEquals(Category.PREMIUM_AND_BILLING, saved.getCategory());
        assertEquals("Corrected description with more detail.", saved.getDescription());
        assertEquals(original.getRefNo(), saved.getRefNo());
        assertEquals(original.getDueOn(), saved.getDueOn());
        assertEquals(Status.SUBMITTED, saved.getStatus());
    }

    @Test
    void editDoesNotAddHistory() throws Exception {
        Grievance original = freshSubmitted();
        service.update(asha, original.getId(), newValues());
        GrievanceDetail detail = service.findDetail(original.getId(), asha).orElseThrow();
        assertEquals(1, detail.getHistory().size());
    }

    @Test
    void editIsClosedOnceReviewHasStarted() throws Exception {
        Grievance underReview = service.listOwn(asha).stream()
                .filter(g -> g.getTitle().equals("Premium debited twice this month"))
                .findFirst().orElseThrow();
        assertThrows(IllegalStateException.class, () -> service.update(asha, underReview.getId(), newValues()));
    }

    @Test
    void anotherPolicyholderCannotEdit() throws Exception {
        Grievance original = freshSubmitted();
        assertThrows(NoSuchElementException.class, () -> service.update(ravi, original.getId(), newValues()));
        assertTrue(service.findOwnById(original.getId(), ravi).isEmpty());
    }

    @Test
    void staffCannotEdit() throws Exception {
        Grievance original = freshSubmitted();
        assertThrows(NoSuchElementException.class, () -> service.update(officer, original.getId(), newValues()));
    }

    @Test
    void anInvalidFormIsRefusedAndChangesNothing() throws Exception {
        Grievance original = freshSubmitted();
        assertThrows(IllegalArgumentException.class,
                () -> service.update(asha, original.getId(), new GrievanceForm("", "", "", "")));
        Grievance unchanged = service.findOwnById(original.getId(), asha).orElseThrow();
        assertEquals("Original title", unchanged.getTitle());
    }
}
