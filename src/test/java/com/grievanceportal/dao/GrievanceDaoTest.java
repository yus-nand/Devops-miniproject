package com.grievanceportal.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grievanceportal.model.Category;
import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceForm;
import com.grievanceportal.model.HistoryEntry;
import com.grievanceportal.model.Role;
import com.grievanceportal.model.Status;
import com.grievanceportal.model.User;
import com.grievanceportal.service.AuthService;
import com.grievanceportal.service.GrievanceService;
import com.grievanceportal.service.SampleData;
import com.grievanceportal.util.Schema;
import com.grievanceportal.util.Seeder;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GrievanceDaoTest {

    private static User asha;
    private static User ravi;
    private static User officer;

    private final GrievanceDao dao = new GrievanceDao();

    @BeforeAll
    static void prepareDatabase() throws Exception {
        System.setProperty("GP_DB_PATH", "mem:daotest");
        Schema.init();
        Seeder.seedUsers();
        SampleData.seed();
        AuthService auth = new AuthService();
        asha = auth.login("asha", Seeder.DEMO_PASSWORD).orElseThrow();
        ravi = auth.login("ravi", Seeder.DEMO_PASSWORD).orElseThrow();
        officer = auth.login("officer1", Seeder.DEMO_PASSWORD).orElseThrow();
    }

    @AfterAll
    static void clearSetting() {
        System.clearProperty("GP_DB_PATH");
    }

    private Grievance sample(User owner, String title) throws Exception {
        return dao.findByOwner(owner.getId()).stream()
                .filter(g -> g.getTitle().equals(title))
                .findFirst().orElseThrow();
    }

    @Test
    void findByIdReturnsTheGrievanceOrNothing() throws Exception {
        Grievance sample = sample(asha, "Premium debited twice this month");
        Grievance found = dao.findById(sample.getId()).orElseThrow();
        assertEquals(sample.getRefNo(), found.getRefNo());
        assertEquals(Status.UNDER_REVIEW, found.getStatus());
        assertTrue(dao.findById(999999).isEmpty());
    }

    @Test
    void newGrievanceHasOneHistoryEntryByTheSubmitter() throws Exception {
        Grievance g = new GrievanceService().submit(asha, new GrievanceForm("POL-55555", "OTHER",
                "History check", "A long enough description."));
        List<HistoryEntry> history = dao.historyOf(g.getId());
        assertEquals(1, history.size());
        HistoryEntry first = history.get(0);
        assertNull(first.getFromStatus());
        assertEquals(Status.SUBMITTED, first.getToStatus());
        assertEquals("Asha Verma", first.getChangedByName());
        assertEquals(Role.POLICYHOLDER, first.getChangedByRole());
    }

    @Test
    void resolvedSampleHistoryIsInOrderWithOfficerAndNote() throws Exception {
        Grievance resolved = sample(ravi, "Address change not reflected on policy");
        List<HistoryEntry> history = dao.historyOf(resolved.getId());
        assertEquals(3, history.size());
        assertEquals(Status.SUBMITTED, history.get(0).getToStatus());
        assertEquals(Status.UNDER_REVIEW, history.get(1).getToStatus());
        assertEquals(Status.UNDER_REVIEW, history.get(2).getFromStatus());
        assertEquals(Status.RESOLVED, history.get(2).getToStatus());
        assertEquals("Meera Joshi", history.get(1).getChangedByName());
        assertEquals(Role.OFFICER, history.get(1).getChangedByRole());
        assertEquals(resolved.getResolutionNote(), history.get(2).getNote());
        assertTrue(history.get(0).getChangedAt().isBefore(history.get(2).getChangedAt()));
    }

    @Test
    void historyTextShowsDateAndMinutes() throws Exception {
        Grievance resolved = sample(ravi, "Address change not reflected on policy");
        String text = dao.historyOf(resolved.getId()).get(0).getChangedAtText();
        assertTrue(text.matches("\\d{4}-\\d{2}-\\d{2} 09:00"));
    }

    @Test
    void fullNameLookupWorks() throws Exception {
        assertEquals("Meera Joshi", dao.fullNameOf(officer.getId()).orElseThrow());
        assertTrue(dao.fullNameOf(999999).isEmpty());
    }

    @Test
    void updateWhileSubmittedChangesTheRowButNotAfterReview() throws Exception {
        Grievance submitted = sample(asha, "Claim settlement delayed beyond 30 days");
        assertTrue(dao.updateWhileSubmitted(submitted.getId(), asha.getId(), "POL-100231",
                Category.OTHER, "Changed title", "Changed description text."));
        Grievance reloaded = dao.findById(submitted.getId()).orElseThrow();
        assertEquals("Changed title", reloaded.getTitle());
        assertEquals(submitted.getRefNo(), reloaded.getRefNo());
        assertEquals(submitted.getDueOn(), reloaded.getDueOn());

        Grievance underReview = sample(asha, "Premium debited twice this month");
        assertFalse(dao.updateWhileSubmitted(underReview.getId(), asha.getId(), "POL-100231",
                Category.OTHER, "Should not apply", "Should not apply either."));
        assertEquals("Premium debited twice this month", dao.findById(underReview.getId()).orElseThrow().getTitle());
        assertFalse(dao.updateWhileSubmitted(submitted.getId(), ravi.getId(), "POL-100231",
                Category.OTHER, "Wrong owner", "Wrong owner description."));
    }
}
