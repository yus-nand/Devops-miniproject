package com.grievanceportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grievanceportal.dao.GrievanceDao;
import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceDetail;
import com.grievanceportal.model.GrievanceForm;
import com.grievanceportal.model.HistoryEntry;
import com.grievanceportal.model.Role;
import com.grievanceportal.model.Status;
import com.grievanceportal.model.User;
import com.grievanceportal.util.Schema;
import com.grievanceportal.util.Seeder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StatusChangeTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneId.of("UTC"));

    private static User asha;
    private static User officer1;
    private static User officer2;
    private static User admin;

    private final GrievanceService service = new GrievanceService(FIXED);
    private final GrievanceDao dao = new GrievanceDao();

    @BeforeAll
    static void prepareDatabase() throws Exception {
        System.setProperty("GP_DB_PATH", "mem:statustest");
        Schema.init();
        Seeder.seedUsers();
        AuthService auth = new AuthService();
        asha = auth.login("asha", Seeder.DEMO_PASSWORD).orElseThrow();
        officer1 = auth.login("officer1", Seeder.DEMO_PASSWORD).orElseThrow();
        officer2 = auth.login("officer2", Seeder.DEMO_PASSWORD).orElseThrow();
        admin = auth.login("admin1", Seeder.DEMO_PASSWORD).orElseThrow();
    }

    @AfterAll
    static void clearSetting() {
        System.clearProperty("GP_DB_PATH");
    }

    private Grievance submitted() throws Exception {
        return service.submit(asha,
                new GrievanceForm("POL-12345", "OTHER", "Something is wrong", "A long enough description."));
    }

    private Grievance underReview(User officer) throws Exception {
        Grievance g = submitted();
        service.pickUp(officer, g.getId());
        return g;
    }

    private Grievance resolved() throws Exception {
        Grievance g = underReview(officer1);
        service.changeStatus(officer1, g.getId(), Status.RESOLVED, "Fixed it.");
        return g;
    }

    private Grievance fresh(Grievance g) throws Exception {
        return dao.findById(g.getId()).orElseThrow();
    }

    private HistoryEntry lastHistory(Grievance g) throws Exception {
        List<HistoryEntry> history = dao.historyOf(g.getId());
        return history.get(history.size() - 1);
    }

    // US-08

    @Test
    void pickUpAssignsTheOfficerAndStartsTheReview() throws Exception {
        Grievance g = submitted();
        Grievance after = service.pickUp(officer1, g.getId());
        assertEquals(Status.UNDER_REVIEW, after.getStatus());
        assertEquals(Long.valueOf(officer1.getId()), after.getAssignedOfficerId());
    }

    @Test
    void pickUpIsRecordedInTheHistory() throws Exception {
        Grievance g = submitted();
        service.pickUp(officer1, g.getId());
        HistoryEntry entry = lastHistory(g);
        assertEquals(Status.SUBMITTED, entry.getFromStatus());
        assertEquals(Status.UNDER_REVIEW, entry.getToStatus());
        assertEquals("Meera Joshi", entry.getChangedByName());
        assertEquals(2, dao.historyOf(g.getId()).size());
    }

    @Test
    void assignedOfficerIsShownOnTheDetailPage() throws Exception {
        Grievance g = underReview(officer1);
        GrievanceDetail detail = service.findDetail(g.getId(), officer1).orElseThrow();
        assertEquals("Meera Joshi", detail.getOfficerName());
    }

    @Test
    void anotherOfficerCannotPickUpAnAssignedGrievance() throws Exception {
        Grievance g = underReview(officer1);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.pickUp(officer2, g.getId()));
        assertTrue(e.getMessage().contains("Meera Joshi"), e.getMessage());
        assertEquals(Long.valueOf(officer1.getId()), fresh(g).getAssignedOfficerId());
        assertEquals(2, dao.historyOf(g.getId()).size());
    }

    @Test
    void pickingUpTwiceIsRefusedWithAMessage() throws Exception {
        Grievance g = underReview(officer1);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.pickUp(officer1, g.getId()));
        assertTrue(e.getMessage().contains("already picked up"), e.getMessage());
    }

    @Test
    void onlyOfficersCanPickUp() throws Exception {
        Grievance g = submitted();
        assertThrows(IllegalStateException.class, () -> service.pickUp(admin, g.getId()));
        assertThrows(IllegalStateException.class, () -> service.pickUp(asha, g.getId()));
        assertEquals(Status.SUBMITTED, fresh(g).getStatus());
    }

    @Test
    void pickingUpAMissingGrievanceFails() {
        assertThrows(NoSuchElementException.class, () -> service.pickUp(officer1, 999999));
    }

    @Test
    void statusEndpointCannotBeUsedToSkipTheAssignment() throws Exception {
        Grievance g = submitted();
        assertThrows(IllegalStateException.class,
                () -> service.changeStatus(officer1, g.getId(), Status.UNDER_REVIEW, null));
        assertNull(fresh(g).getAssignedOfficerId());
    }

    // US-09 and US-10

    @Test
    void officerResolvesWithANoteAndTheNoteIsStoredAndInTheHistory() throws Exception {
        Grievance g = underReview(officer1);
        Grievance after = service.changeStatus(officer1, g.getId(), Status.RESOLVED, "  Refund issued.  ");
        assertEquals(Status.RESOLVED, after.getStatus());
        assertEquals("Refund issued.", after.getResolutionNote());
        assertEquals("Refund issued.", lastHistory(g).getNote());
        assertEquals(Status.UNDER_REVIEW, lastHistory(g).getFromStatus());
        assertEquals(Status.RESOLVED, lastHistory(g).getToStatus());
    }

    @Test
    void officerRejectsWithANote() throws Exception {
        Grievance g = underReview(officer1);
        Grievance after = service.changeStatus(officer1, g.getId(), Status.REJECTED, "Not covered by the policy.");
        assertEquals(Status.REJECTED, after.getStatus());
        assertEquals("Not covered by the policy.", after.getResolutionNote());
    }

    @Test
    void anEmptyOrBlankNoteIsRefusedAndNothingChanges() throws Exception {
        Grievance g = underReview(officer1);
        for (String note : new String[] {null, "", "   "}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> service.changeStatus(officer1, g.getId(), Status.RESOLVED, note));
            assertEquals("Enter a resolution note.", e.getMessage());
        }
        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus(officer1, g.getId(), Status.REJECTED, " "));
        assertEquals(Status.UNDER_REVIEW, fresh(g).getStatus());
        assertEquals(2, dao.historyOf(g.getId()).size());
    }

    @Test
    void aTooLongNoteIsRefused() throws Exception {
        Grievance g = underReview(officer1);
        String tooLong = "x".repeat(GrievanceService.MAX_NOTE_LENGTH + 1);
        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus(officer1, g.getId(), Status.RESOLVED, tooLong));
        service.changeStatus(officer1, g.getId(), Status.RESOLVED, "x".repeat(GrievanceService.MAX_NOTE_LENGTH));
        assertEquals(Status.RESOLVED, fresh(g).getStatus());
    }

    @Test
    void onlyTheAssignedOfficerMayResolve() throws Exception {
        Grievance g = underReview(officer1);
        assertThrows(IllegalStateException.class,
                () -> service.changeStatus(officer2, g.getId(), Status.RESOLVED, "Done."));
        assertEquals(Status.UNDER_REVIEW, fresh(g).getStatus());
    }

    @Test
    void policyholderCannotChangeStatus() throws Exception {
        Grievance g = underReview(officer1);
        for (Status to : Status.values()) {
            assertThrows(IllegalStateException.class, () -> service.changeStatus(asha, g.getId(), to, "Please."));
        }
        assertEquals(Status.UNDER_REVIEW, fresh(g).getStatus());
    }

    @Test
    void changesOutsideTheWorkflowAreRefusedWithAMessage() throws Exception {
        Grievance g = underReview(officer1);
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.changeStatus(officer1, g.getId(), Status.CLOSED, null));
        assertEquals("You cannot move a grievance from Under Review to Closed.", e.getMessage());
        assertThrows(IllegalStateException.class,
                () -> service.changeStatus(officer1, g.getId(), Status.SUBMITTED, null));
        assertThrows(IllegalStateException.class,
                () -> service.changeStatus(admin, g.getId(), Status.RESOLVED, "Admin shortcut."));
        assertEquals(Status.UNDER_REVIEW, fresh(g).getStatus());
    }

    @Test
    void anOfficerCannotCloseOrReopen() throws Exception {
        Grievance g = resolved();
        assertThrows(IllegalStateException.class, () -> service.changeStatus(officer1, g.getId(), Status.CLOSED, null));
        assertThrows(IllegalStateException.class,
                () -> service.changeStatus(officer1, g.getId(), Status.UNDER_REVIEW, "Again."));
        assertEquals(Status.RESOLVED, fresh(g).getStatus());
    }

    @Test
    void changingAMissingGrievanceFails() {
        assertThrows(NoSuchElementException.class,
                () -> service.changeStatus(officer1, 999999, Status.RESOLVED, "Note."));
    }

    // US-11

    @Test
    void adminClosesAResolvedGrievanceAndKeepsTheNote() throws Exception {
        Grievance g = resolved();
        Grievance after = service.changeStatus(admin, g.getId(), Status.CLOSED, null);
        assertEquals(Status.CLOSED, after.getStatus());
        assertEquals("Fixed it.", after.getResolutionNote());
        assertEquals(Status.RESOLVED, lastHistory(g).getFromStatus());
        assertEquals(Status.CLOSED, lastHistory(g).getToStatus());
        assertEquals("Arjun Rao", lastHistory(g).getChangedByName());
        assertNull(lastHistory(g).getNote());
    }

    @Test
    void adminClosesARejectedGrievance() throws Exception {
        Grievance g = underReview(officer1);
        service.changeStatus(officer1, g.getId(), Status.REJECTED, "Out of scope.");
        assertEquals(Status.CLOSED, service.changeStatus(admin, g.getId(), Status.CLOSED, null).getStatus());
    }

    @Test
    void adminReopensAResolvedGrievanceWithAReasonAndTheOfficerKeepsIt() throws Exception {
        Grievance g = resolved();
        Grievance after = service.changeStatus(admin, g.getId(), Status.UNDER_REVIEW, "Policyholder disputes the fix.");
        assertEquals(Status.UNDER_REVIEW, after.getStatus());
        assertNull(after.getResolutionNote());
        assertEquals(Long.valueOf(officer1.getId()), after.getAssignedOfficerId());
        assertEquals("Policyholder disputes the fix.", lastHistory(g).getNote());
        assertEquals(Status.RESOLVED, lastHistory(g).getFromStatus());
        assertEquals(Status.UNDER_REVIEW, lastHistory(g).getToStatus());
    }

    @Test
    void reopeningNeedsAReason() throws Exception {
        Grievance g = resolved();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus(admin, g.getId(), Status.UNDER_REVIEW, " "));
        assertEquals("Enter a reason for reopening.", e.getMessage());
        assertEquals(Status.RESOLVED, fresh(g).getStatus());
    }

    @Test
    void aRejectedGrievanceCannotBeReopenedAndAClosedOneIsFinal() throws Exception {
        Grievance rejected = underReview(officer1);
        service.changeStatus(officer1, rejected.getId(), Status.REJECTED, "No.");
        assertThrows(IllegalStateException.class,
                () -> service.changeStatus(admin, rejected.getId(), Status.UNDER_REVIEW, "Try again."));

        Grievance closed = resolved();
        service.changeStatus(admin, closed.getId(), Status.CLOSED, null);
        for (Status to : Status.values()) {
            assertThrows(IllegalStateException.class, () -> service.changeStatus(admin, closed.getId(), to, "Note."));
        }
        assertEquals(Status.CLOSED, fresh(closed).getStatus());
    }

    @Test
    void theWholeLifeOfAGrievanceIsInTheHistoryInOrder() throws Exception {
        Grievance g = resolved();
        service.changeStatus(admin, g.getId(), Status.UNDER_REVIEW, "Reopened.");
        service.changeStatus(officer1, g.getId(), Status.RESOLVED, "Fixed properly.");
        service.changeStatus(admin, g.getId(), Status.CLOSED, null);

        List<Status> path = dao.historyOf(g.getId()).stream().map(HistoryEntry::getToStatus)
                .collect(java.util.stream.Collectors.toList());
        assertEquals(List.of(Status.SUBMITTED, Status.UNDER_REVIEW, Status.RESOLVED, Status.UNDER_REVIEW,
                Status.RESOLVED, Status.CLOSED), path);
    }

    @Test
    void aStaleChangeIsRefusedInsteadOfOverwritingNewerWork() throws Exception {
        Grievance g = underReview(officer1);
        // The grievance moves on between the check and the write.
        assertTrue(dao.changeStatus(g.getId(), Status.UNDER_REVIEW, Status.RESOLVED, "First.", officer1.getId(),
                "First.", java.time.LocalDateTime.now(FIXED)));
        assertFalse(dao.changeStatus(g.getId(), Status.UNDER_REVIEW, Status.REJECTED, "Second.", officer1.getId(),
                "Second.", java.time.LocalDateTime.now(FIXED)));
        assertEquals(Status.RESOLVED, fresh(g).getStatus());
        assertEquals("First.", fresh(g).getResolutionNote());
    }

    @Test
    void roleOfTheSeededOfficersIsOfficer() {
        assertEquals(Role.OFFICER, officer1.getRole());
        assertEquals(Role.OFFICER, officer2.getRole());
    }
}
