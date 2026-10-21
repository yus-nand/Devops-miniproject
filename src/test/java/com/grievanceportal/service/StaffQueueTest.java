package com.grievanceportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grievanceportal.model.Category;
import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceForm;
import com.grievanceportal.model.QueueFilter;
import com.grievanceportal.model.QueueRow;
import com.grievanceportal.model.Status;
import com.grievanceportal.model.User;
import com.grievanceportal.util.Database;
import com.grievanceportal.util.Schema;
import com.grievanceportal.util.Seeder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StaffQueueTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneId.of("UTC"));

    private static User asha;
    private static User ravi;

    private static Grievance claimOld;     // POL-AAA11, claim, submitted 2026-10-01, due 2026-10-15 (overdue)
    private static Grievance billingNew;   // POL-BBB22, billing, submitted 2026-10-18, due 2026-11-01
    private static Grievance claimResolved; // POL-AAA11, claim, submitted 2026-09-01, resolved, long past due
    private static Grievance serviceDueToday; // POL-CCC33, servicing, submitted 2026-10-06, due 2026-10-20

    private final StaffQueueService queue = new StaffQueueService(NOW);

    @BeforeAll
    static void prepareDatabase() throws Exception {
        System.setProperty("GP_DB_PATH", "mem:queuetest");
        Schema.init();
        Seeder.seedUsers();
        AuthService auth = new AuthService();
        asha = auth.login("asha", Seeder.DEMO_PASSWORD).orElseThrow();
        ravi = auth.login("ravi", Seeder.DEMO_PASSWORD).orElseThrow();

        claimOld = submit(asha, "2026-10-01", "POL-AAA11", "CLAIM_SETTLEMENT", "Claim stuck");
        billingNew = submit(ravi, "2026-10-18", "POL-BBB22", "PREMIUM_AND_BILLING", "Charged twice");
        claimResolved = submit(asha, "2026-09-01", "POL-AAA11", "CLAIM_SETTLEMENT", "Old claim");
        serviceDueToday = submit(ravi, "2026-10-06", "POL-CCC33", "POLICY_SERVICING", "Address change");
        setStatus(claimResolved, Status.RESOLVED);
    }

    @AfterAll
    static void clearSetting() {
        System.clearProperty("GP_DB_PATH");
    }

    private static Grievance submit(User user, String day, String policy, String category, String title)
            throws Exception {
        Clock clock = Clock.fixed(Instant.parse(day + "T10:00:00Z"), ZoneId.of("UTC"));
        return new GrievanceService(clock).submit(user,
                new GrievanceForm(policy, category, title, "A long enough description."));
    }

    private static void setStatus(Grievance g, Status status) throws Exception {
        try (Connection c = Database.getConnection();
                PreparedStatement ps = c.prepareStatement("UPDATE grievances SET status = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setLong(2, g.getId());
            ps.executeUpdate();
        }
    }

    private List<String> refs(QueueFilter filter) throws Exception {
        return queue.queue(filter).stream()
                .map(row -> row.getGrievance().getRefNo())
                .collect(Collectors.toList());
    }

    @Test
    void listShowsEveryGrievanceFromEveryPolicyholder() throws Exception {
        List<String> refs = refs(QueueFilter.NONE);
        assertEquals(4, refs.size());
        assertTrue(refs.contains(claimOld.getRefNo()));
        assertTrue(refs.contains(billingNew.getRefNo()));
    }

    @Test
    void earliestDueDateComesFirst() throws Exception {
        assertEquals(List.of(claimResolved.getRefNo(), claimOld.getRefNo(), serviceDueToday.getRefNo(),
                billingNew.getRefNo()), refs(QueueFilter.NONE));
    }

    @Test
    void searchFindsByReferenceId() throws Exception {
        assertEquals(List.of(billingNew.getRefNo()), refs(QueueFilter.of(billingNew.getRefNo(), "", "")));
    }

    @Test
    void searchFindsByPolicyNumberIgnoringCaseAndSpaces() throws Exception {
        assertEquals(List.of(claimResolved.getRefNo(), claimOld.getRefNo()), refs(QueueFilter.of("  pol-aaa11 ", "", "")));
    }

    @Test
    void searchMatchesPartOfAPolicyNumber() throws Exception {
        assertEquals(List.of(serviceDueToday.getRefNo()), refs(QueueFilter.of("CCC", "", "")));
    }

    @Test
    void statusFilterKeepsOnlyThatStatus() throws Exception {
        assertEquals(List.of(claimResolved.getRefNo()), refs(QueueFilter.of("", "RESOLVED", "")));
    }

    @Test
    void categoryFilterKeepsOnlyThatCategory() throws Exception {
        assertEquals(List.of(billingNew.getRefNo()), refs(QueueFilter.of("", "", "PREMIUM_AND_BILLING")));
    }

    @Test
    void searchStatusAndCategoryCombine() throws Exception {
        assertEquals(List.of(claimOld.getRefNo()), refs(QueueFilter.of("POL-AAA11", "SUBMITTED", "CLAIM_SETTLEMENT")));
        assertTrue(refs(QueueFilter.of("POL-AAA11", "SUBMITTED", "PREMIUM_AND_BILLING")).isEmpty());
        assertTrue(refs(QueueFilter.of("POL-BBB22", "RESOLVED", "")).isEmpty());
    }

    @Test
    void noMatchGivesAnEmptyList() throws Exception {
        assertTrue(refs(QueueFilter.of("DOES-NOT-EXIST", "", "")).isEmpty());
    }

    @Test
    void percentAndUnderscoreAreSearchedAsPlainCharacters() throws Exception {
        assertTrue(refs(QueueFilter.of("%", "", "")).isEmpty());
        assertTrue(refs(QueueFilter.of("POL_AAA11", "", "")).isEmpty());
    }

    @Test
    void anUnknownStatusOrCategoryIsIgnored() throws Exception {
        assertEquals(4, refs(QueueFilter.of("", "NOT_A_STATUS", "NOT_A_CATEGORY")).size());
    }

    @Test
    void openGrievancePastItsDueDateIsOverdue() throws Exception {
        QueueRow row = rowFor(claimOld);
        assertTrue(row.isOverdue());
    }

    @Test
    void grievanceDueTodayIsNotOverdueYet() throws Exception {
        assertEquals(LocalDate.of(2026, 10, 20), serviceDueToday.getDueOn());
        assertFalse(rowFor(serviceDueToday).isOverdue());
    }

    @Test
    void grievanceWithTimeLeftIsNotOverdue() throws Exception {
        assertFalse(rowFor(billingNew).isOverdue());
    }

    @Test
    void closedStatusesAreNeverOverdue() {
        LocalDate longAfter = LocalDate.of(2027, 1, 1);
        for (Status status : new Status[] {Status.RESOLVED, Status.REJECTED, Status.CLOSED}) {
            Grievance g = new Grievance();
            g.setStatus(status);
            g.setDueOn(LocalDate.of(2026, 1, 1));
            assertFalse(StaffQueueService.isOverdue(g, longAfter), status + " must not be flagged");
        }
        for (Status status : new Status[] {Status.SUBMITTED, Status.UNDER_REVIEW}) {
            Grievance g = new Grievance();
            g.setStatus(status);
            g.setDueOn(LocalDate.of(2026, 1, 1));
            assertTrue(StaffQueueService.isOverdue(g, longAfter), status + " must be flagged");
        }
    }

    @Test
    void resolvedGrievancePastItsDueDateIsListedWithoutAFlag() throws Exception {
        assertFalse(rowFor(claimResolved).isOverdue());
    }

    @Test
    void filterKnowsWhenItIsActive() {
        assertFalse(QueueFilter.NONE.isActive());
        assertFalse(QueueFilter.of(" ", "", "").isActive());
        assertTrue(QueueFilter.of("x", "", "").isActive());
        assertTrue(QueueFilter.of("", "SUBMITTED", "").isActive());
        assertTrue(QueueFilter.of("", "", "OTHER").isActive());
        assertEquals(Category.OTHER, QueueFilter.of("", "", "OTHER").getCategory());
    }

    private QueueRow rowFor(Grievance g) throws Exception {
        return queue.queue(QueueFilter.NONE).stream()
                .filter(row -> row.getGrievance().getId() == g.getId())
                .findFirst()
                .orElseThrow();
    }
}
