package com.grievanceportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceForm;
import com.grievanceportal.model.Status;
import com.grievanceportal.model.User;
import com.grievanceportal.util.Database;
import com.grievanceportal.util.Schema;
import com.grievanceportal.util.Seeder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GrievanceServiceTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneId.of("UTC"));

    private static User asha;
    private static User ravi;

    private final GrievanceService service = new GrievanceService(FIXED);

    @BeforeAll
    static void prepareDatabase() throws Exception {
        System.setProperty("GP_DB_PATH", "mem:grievancetest");
        Schema.init();
        Seeder.seedUsers();
        AuthService auth = new AuthService();
        asha = auth.login("asha", Seeder.DEMO_PASSWORD).orElseThrow();
        ravi = auth.login("ravi", Seeder.DEMO_PASSWORD).orElseThrow();
    }

    @AfterAll
    static void clearSetting() {
        System.clearProperty("GP_DB_PATH");
    }

    private static GrievanceForm validForm() {
        return new GrievanceForm("POL-12345", "CLAIM_SETTLEMENT", "Claim not settled",
                "My claim has been pending for two months.");
    }

    @Test
    void validFormHasNoErrors() {
        assertTrue(service.validate(validForm()).isEmpty());
    }

    @Test
    void everyFieldIsRequired() {
        Map<String, String> errors = service.validate(new GrievanceForm("", "", "", ""));
        assertEquals(4, errors.size());
        assertTrue(errors.containsKey("policyNumber"));
        assertTrue(errors.containsKey("category"));
        assertTrue(errors.containsKey("title"));
        assertTrue(errors.containsKey("description"));
    }

    @Test
    void policyNumberFormatIsChecked() {
        String text = "A long enough description.";
        assertTrue(service.validate(new GrievanceForm("ab", "OTHER", "Title", text)).containsKey("policyNumber"));
        assertTrue(service.validate(new GrievanceForm("POL 12345", "OTHER", "Title", text)).containsKey("policyNumber"));
        assertFalse(service.validate(new GrievanceForm("POL-12345", "OTHER", "Title", text)).containsKey("policyNumber"));
    }

    @Test
    void unknownCategoryIsRejected() {
        GrievanceForm form = new GrievanceForm("POL-12345", "HACKED", "Title", "A long enough description.");
        assertTrue(service.validate(form).containsKey("category"));
    }

    @Test
    void shortDescriptionIsRejected() {
        GrievanceForm form = new GrievanceForm("POL-12345", "OTHER", "Title", "too short");
        assertTrue(service.validate(form).containsKey("description"));
    }

    @Test
    void submitCreatesSubmittedGrievanceWithDueDate() throws Exception {
        Grievance g = service.submit(asha, validForm());
        assertTrue(g.getId() > 0);
        assertTrue(g.getRefNo().matches("GRV-2026-\\d{4}"));
        assertEquals(Status.SUBMITTED, g.getStatus());
        assertEquals(LocalDate.of(2026, 10, 21), g.getCreatedOn());
        assertEquals(LocalDate.of(2026, 11, 4), g.getDueOn());
        assertEquals(asha.getId(), g.getPolicyholderId());
    }

    @Test
    void submitWritesTheFirstHistoryEntry() throws Exception {
        Grievance g = service.submit(asha, validForm());
        String sql = "SELECT from_status, to_status, changed_by FROM status_history WHERE grievance_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, g.getId());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertNull(rs.getString("from_status"));
                assertEquals("SUBMITTED", rs.getString("to_status"));
                assertEquals(asha.getId(), rs.getLong("changed_by"));
                assertFalse(rs.next());
            }
        }
    }

    @Test
    void ownerCanReadOwnGrievanceButOthersCannot() throws Exception {
        Grievance g = service.submit(asha, validForm());
        assertTrue(service.findOwn(g.getRefNo(), asha).isPresent());
        assertTrue(service.findOwn(g.getRefNo(), ravi).isEmpty());
        assertTrue(service.findOwn("GRV-0000-0000", asha).isEmpty());
        assertTrue(service.findOwn(null, asha).isEmpty());
    }

    @Test
    void submittingAnInvalidFormIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> service.submit(asha, new GrievanceForm("", "", "", "")));
    }
}
