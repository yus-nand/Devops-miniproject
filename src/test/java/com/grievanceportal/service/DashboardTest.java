package com.grievanceportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grievanceportal.dao.GrievanceDao;
import com.grievanceportal.model.Category;
import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceForm;
import com.grievanceportal.model.QueueFilter;
import com.grievanceportal.model.Status;
import com.grievanceportal.model.Summary;
import com.grievanceportal.model.User;
import com.grievanceportal.util.Schema;
import com.grievanceportal.util.Seeder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneId.of("UTC"));

    private static final AtomicInteger DATABASES = new AtomicInteger();

    private static User asha;
    private static User officer;
    private static User admin;

    private final DashboardService dashboard = new DashboardService(NOW);
    private final StaffQueueService queue = new StaffQueueService(NOW);

    /** Every test gets its own empty database, so the exact counts do not depend on the test order. */
    @BeforeEach
    void prepareDatabase() throws Exception {
        System.setProperty("GP_DB_PATH", "mem:dashboardtest" + DATABASES.incrementAndGet());
        Schema.init();
        Seeder.seedUsers();
        AuthService auth = new AuthService();
        asha = auth.login("asha", Seeder.DEMO_PASSWORD).orElseThrow();
        officer = auth.login("officer1", Seeder.DEMO_PASSWORD).orElseThrow();
        admin = auth.login("admin1", Seeder.DEMO_PASSWORD).orElseThrow();
    }

    @AfterAll
    static void clearSetting() {
        System.clearProperty("GP_DB_PATH");
    }

    private static Grievance submit(String day, String category) throws Exception {
        Clock clock = Clock.fixed(Instant.parse(day + "T10:00:00Z"), ZoneId.of("UTC"));
        return new GrievanceService(clock).submit(asha,
                new GrievanceForm("POL-12345", category, "A title", "A long enough description."));
    }

    private static void setStatus(Grievance g, Status status) throws Exception {
        GrievanceService service = new GrievanceService(NOW);
        if (status == Status.SUBMITTED) {
            return;
        }
        service.pickUp(officer, g.getId());
        if (status == Status.RESOLVED || status == Status.CLOSED) {
            service.changeStatus(officer, g.getId(), Status.RESOLVED, "Done.");
        }
        if (status == Status.REJECTED) {
            service.changeStatus(officer, g.getId(), Status.REJECTED, "No.");
        }
        if (status == Status.CLOSED) {
            service.changeStatus(admin, g.getId(), Status.CLOSED, null);
        }
    }

    @Test
    void anEmptyDatabaseShowsZeroForEveryStatusAndCategory() throws Exception {
        Summary summary = dashboard.summarize();
        assertEquals(0, summary.getTotal());
        assertEquals(0, summary.getOverdue());
        assertEquals(Status.values().length, summary.getByStatus().size());
        assertEquals(Category.values().length, summary.getByCategory().size());
        assertTrue(summary.getByStatus().values().stream().allMatch(n -> n == 0));
        assertTrue(summary.getByCategory().values().stream().allMatch(n -> n == 0));
    }

    @Test
    void countsByStatusCategoryAndOverdueMatchTheData() throws Exception {
        // Overdue: open and due before 2026-10-20 (submitted 2026-10-05 is due 2026-10-19).
        Grievance overdueSubmitted = submit("2026-10-05", "CLAIM_SETTLEMENT");
        Grievance overdueReview = submit("2026-10-01", "CLAIM_SETTLEMENT");
        setStatus(overdueReview, Status.UNDER_REVIEW);
        // Due today is not overdue yet.
        submit("2026-10-06", "MIS_SELLING");
        // Old but finished: never overdue.
        Grievance oldResolved = submit("2026-09-01", "POLICY_SERVICING");
        setStatus(oldResolved, Status.RESOLVED);
        Grievance oldRejected = submit("2026-09-01", "OTHER");
        setStatus(oldRejected, Status.REJECTED);
        Grievance oldClosed = submit("2026-09-01", "OTHER");
        setStatus(oldClosed, Status.CLOSED);

        Summary summary = dashboard.summarize();
        assertEquals(6, summary.getTotal());
        assertEquals(2, summary.getOverdue());
        assertEquals(2, summary.getByStatus().get(Status.SUBMITTED).intValue());
        assertEquals(1, summary.getByStatus().get(Status.UNDER_REVIEW).intValue());
        assertEquals(1, summary.getByStatus().get(Status.RESOLVED).intValue());
        assertEquals(1, summary.getByStatus().get(Status.REJECTED).intValue());
        assertEquals(1, summary.getByStatus().get(Status.CLOSED).intValue());
        assertEquals(2, summary.getByCategory().get(Category.CLAIM_SETTLEMENT).intValue());
        assertEquals(1, summary.getByCategory().get(Category.MIS_SELLING).intValue());
        assertEquals(1, summary.getByCategory().get(Category.POLICY_SERVICING).intValue());
        assertEquals(2, summary.getByCategory().get(Category.OTHER).intValue());
        assertEquals(0, summary.getByCategory().get(Category.PREMIUM_AND_BILLING).intValue());
        assertEquals(overdueSubmitted.getStatus(), Status.SUBMITTED);
    }

    @Test
    void everyCountMatchesTheListThatItLinksTo() throws Exception {
        submit("2026-10-02", "PREMIUM_AND_BILLING");
        Grievance g = submit("2026-10-18", "PREMIUM_AND_BILLING");
        setStatus(g, Status.UNDER_REVIEW);

        Summary summary = dashboard.summarize();
        assertEquals(summary.getTotal(), queue.queue(QueueFilter.NONE).size());
        for (Map.Entry<Status, Integer> e : summary.getByStatus().entrySet()) {
            assertEquals(e.getValue().intValue(),
                    queue.queue(QueueFilter.of("", e.getKey().getCode(), "")).size());
        }
        for (Map.Entry<Category, Integer> e : summary.getByCategory().entrySet()) {
            assertEquals(e.getValue().intValue(),
                    queue.queue(QueueFilter.of("", "", e.getKey().getCode())).size());
        }
        assertEquals(summary.getOverdue(), queue.queue(QueueFilter.of("", "", "", true)).size());
    }

    @Test
    void overdueFilterListsOnlyOpenGrievancesPastTheirDueDate() throws Exception {
        submit("2026-10-01", "OTHER");
        Grievance finished = submit("2026-09-01", "OTHER");
        setStatus(finished, Status.RESOLVED);

        for (var row : queue.queue(QueueFilter.of("", "", "", true))) {
            assertTrue(row.isOverdue());
            assertTrue(row.getGrievance().getStatus() == Status.SUBMITTED
                    || row.getGrievance().getStatus() == Status.UNDER_REVIEW);
        }
        assertTrue(queue.queue(QueueFilter.of("", "RESOLVED", "", true)).isEmpty());
    }

    @Test
    void overdueFilterMakesTheFilterActive() {
        assertTrue(QueueFilter.of("", "", "", true).isActive());
        assertTrue(QueueFilter.of("", "", "", true).isOverdueOnly());
        assertTrue(!QueueFilter.NONE.isOverdueOnly());
    }

    @Test
    void daoCountsUseTheSameRuleAsTheOverdueClass() throws Exception {
        GrievanceDao dao = new GrievanceDao();
        int fromDao = dao.countOverdue(java.time.LocalDate.now(NOW));
        long fromRule = dao.search(null, null, null).stream()
                .filter(g -> Overdue.isOverdue(g, java.time.LocalDate.now(NOW))).count();
        assertEquals(fromRule, fromDao);
    }
}
