package com.grievanceportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceDetail;
import com.grievanceportal.model.Status;
import com.grievanceportal.model.User;
import com.grievanceportal.util.Schema;
import com.grievanceportal.util.Seeder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GrievanceDetailTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneId.of("UTC"));

    private static User asha;
    private static User ravi;
    private static User officer;
    private static User admin;

    private final GrievanceService service = new GrievanceService(FIXED);

    @BeforeAll
    static void prepareDatabase() throws Exception {
        System.setProperty("GP_DB_PATH", "mem:detailtest");
        Schema.init();
        Seeder.seedUsers();
        SampleData.seed(FIXED);
        AuthService auth = new AuthService();
        asha = auth.login("asha", Seeder.DEMO_PASSWORD).orElseThrow();
        ravi = auth.login("ravi", Seeder.DEMO_PASSWORD).orElseThrow();
        officer = auth.login("officer1", Seeder.DEMO_PASSWORD).orElseThrow();
        admin = auth.login("admin1", Seeder.DEMO_PASSWORD).orElseThrow();
    }

    @AfterAll
    static void clearSetting() {
        System.clearProperty("GP_DB_PATH");
    }

    private Grievance sample(User owner, String title) throws Exception {
        return service.listOwn(owner).stream()
                .filter(g -> g.getTitle().equals(title))
                .findFirst().orElseThrow();
    }

    @Test
    void ownerSeesAllFieldsAndTheFullHistory() throws Exception {
        Grievance resolved = sample(ravi, "Address change not reflected on policy");
        GrievanceDetail detail = service.findDetail(resolved.getId(), ravi).orElseThrow();
        assertEquals(resolved.getRefNo(), detail.getGrievance().getRefNo());
        assertEquals(Status.RESOLVED, detail.getGrievance().getStatus());
        assertEquals("Ravi Kulkarni", detail.getPolicyholderName());
        assertEquals("Meera Joshi", detail.getOfficerName());
        assertEquals(3, detail.getHistory().size());
        assertTrue(detail.getGrievance().getResolutionNote().startsWith("Address updated"));
    }

    @Test
    void anotherPolicyholderCannotOpenIt() throws Exception {
        Grievance ravis = sample(ravi, "Address change not reflected on policy");
        assertTrue(service.findDetail(ravis.getId(), asha).isEmpty());
    }

    @Test
    void officerAndAdminCanOpenAnyGrievance() throws Exception {
        Grievance ravis = sample(ravi, "Cashless claim rejected at the hospital");
        assertTrue(service.findDetail(ravis.getId(), officer).isPresent());
        assertTrue(service.findDetail(ravis.getId(), admin).isPresent());
    }

    @Test
    void unknownIdIsEmpty() throws Exception {
        assertTrue(service.findDetail(999999, asha).isEmpty());
    }

    @Test
    void unassignedGrievanceHasNoOfficerName() throws Exception {
        Grievance submitted = sample(asha, "Claim settlement delayed beyond 30 days");
        GrievanceDetail detail = service.findDetail(submitted.getId(), asha).orElseThrow();
        assertNull(detail.getOfficerName());
        assertEquals(1, detail.getHistory().size());
    }

    @Test
    void onlyTheOwnerOfASubmittedGrievanceMayEdit() throws Exception {
        Grievance submitted = sample(asha, "Claim settlement delayed beyond 30 days");
        Grievance underReview = sample(asha, "Premium debited twice this month");
        assertTrue(service.findDetail(submitted.getId(), asha).orElseThrow().isEditable());
        assertFalse(service.findDetail(underReview.getId(), asha).orElseThrow().isEditable());
        assertFalse(service.findDetail(submitted.getId(), officer).orElseThrow().isEditable());
    }
}
