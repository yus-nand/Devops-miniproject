package com.grievanceportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SampleDataTest {

    private static final Clock FIXED = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneId.of("UTC"));
    private static final String SAMPLE_POLICIES = "('POL-100231', 'POL-100487', 'POL-200815')";

    private static User asha;
    private static User ravi;

    private final GrievanceService service = new GrievanceService(FIXED);

    @BeforeAll
    static void prepareDatabase() throws Exception {
        System.setProperty("GP_DB_PATH", "mem:sampledatatest");
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

    private static int count(String sql) throws Exception {
        try (Connection c = Database.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private static int sampleCount() throws Exception {
        return count("SELECT COUNT(*) FROM grievances WHERE policy_number IN " + SAMPLE_POLICIES);
    }

    private static Grievance sampleByTitle(User owner, String title) throws Exception {
        return new GrievanceService(FIXED).listOwn(owner).stream()
                .filter(g -> g.getTitle().equals(title))
                .findFirst().orElseThrow();
    }

    @Test
    void seedingCreatesFiveSampleGrievances() throws Exception {
        SampleData.seed(FIXED);
        assertEquals(5, sampleCount());
    }

    @Test
    void seedingTwiceDoesNotDuplicate() throws Exception {
        SampleData.seed(FIXED);
        SampleData.seed(FIXED);
        assertEquals(5, sampleCount());
    }

    @Test
    void grievancesSubmittedByUsersDoNotBlockSeedingAndSurviveIt() throws Exception {
        Grievance own = service.submit(asha, new GrievanceForm("POL-77777", "OTHER", "My own grievance",
                "Submitted by a real user before seeding."));
        SampleData.seed(FIXED);
        assertEquals(5, sampleCount());
        assertTrue(service.findOwn(own.getRefNo(), asha).isPresent());
    }

    @Test
    void samplesBelongToBothPolicyholdersWithMixedStatuses() throws Exception {
        SampleData.seed(FIXED);
        assertEquals(Status.SUBMITTED, sampleByTitle(asha, "Claim settlement delayed beyond 30 days").getStatus());
        assertEquals(Status.UNDER_REVIEW, sampleByTitle(asha, "Premium debited twice this month").getStatus());
        assertEquals(Status.RESOLVED, sampleByTitle(ravi, "Address change not reflected on policy").getStatus());
        assertEquals(Status.SUBMITTED, sampleByTitle(ravi, "Cashless claim rejected at the hospital").getStatus());
    }

    @Test
    void oneSampleIsAlreadyOverdue() throws Exception {
        SampleData.seed(FIXED);
        Grievance overdue = sampleByTitle(asha, "Policy sold as a guaranteed return plan");
        assertEquals(LocalDate.of(2026, 10, 1), overdue.getCreatedOn());
        assertEquals(LocalDate.of(2026, 10, 15), overdue.getDueOn());
        assertTrue(overdue.getDueOn().isBefore(LocalDate.now(FIXED)));
        assertNotNull(overdue.getAssignedOfficerId());
    }

    @Test
    void resolvedSampleHasNoteAndFullHistory() throws Exception {
        SampleData.seed(FIXED);
        Grievance resolved = sampleByTitle(ravi, "Address change not reflected on policy");
        assertNotNull(resolved.getResolutionNote());
        assertEquals(3, count("SELECT COUNT(*) FROM status_history WHERE grievance_id = " + resolved.getId()));

        Grievance underReview = sampleByTitle(asha, "Premium debited twice this month");
        assertEquals(2, count("SELECT COUNT(*) FROM status_history WHERE grievance_id = " + underReview.getId()));

        Grievance submitted = sampleByTitle(asha, "Claim settlement delayed beyond 30 days");
        assertEquals(1, count("SELECT COUNT(*) FROM status_history WHERE grievance_id = " + submitted.getId()));
    }
}
