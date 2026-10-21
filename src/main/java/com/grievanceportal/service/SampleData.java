package com.grievanceportal.service;

import com.grievanceportal.dao.GrievanceDao;
import com.grievanceportal.model.Category;
import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.Status;
import com.grievanceportal.util.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Inserts a few synthetic grievances so a fresh deployment has something to show.
 * Each sample is identified by its policy number and title, so running this again adds nothing,
 * and grievances that users submitted themselves are left alone.
 */
public final class SampleData {

    private static final class Sample {
        final String owner;
        final String policyNumber;
        final Category category;
        final String title;
        final String description;
        final int daysAgo;
        final Status finalStatus;
        final String resolutionNote;

        Sample(String owner, String policyNumber, Category category, String title, String description,
                int daysAgo, Status finalStatus, String resolutionNote) {
            this.owner = owner;
            this.policyNumber = policyNumber;
            this.category = category;
            this.title = title;
            this.description = description;
            this.daysAgo = daysAgo;
            this.finalStatus = finalStatus;
            this.resolutionNote = resolutionNote;
        }
    }

    private static final List<Sample> SAMPLES = List.of(
            new Sample("asha", "POL-100231", Category.CLAIM_SETTLEMENT,
                    "Claim settlement delayed beyond 30 days",
                    "I filed a hospitalisation claim five weeks ago and have not received a decision or any update.",
                    3, Status.SUBMITTED, null),
            new Sample("asha", "POL-100231", Category.PREMIUM_AND_BILLING,
                    "Premium debited twice this month",
                    "The annual premium was deducted from my account two times on the same day. "
                            + "Please refund the extra amount.",
                    9, Status.UNDER_REVIEW, null),
            new Sample("asha", "POL-100487", Category.MIS_SELLING,
                    "Policy sold as a guaranteed return plan",
                    "The agent told me this policy guarantees fixed returns, but the policy document "
                            + "says returns are market linked.",
                    20, Status.UNDER_REVIEW, null),
            new Sample("ravi", "POL-200815", Category.POLICY_SERVICING,
                    "Address change not reflected on policy",
                    "I requested an address change two weeks ago. The latest policy statement still "
                            + "shows my old address.",
                    12, Status.RESOLVED,
                    "Address updated on the policy record. A corrected statement has been emailed to the policyholder."),
            new Sample("ravi", "POL-200815", Category.CLAIM_SETTLEMENT,
                    "Cashless claim rejected at the hospital",
                    "The hospital said my cashless request was rejected without a reason. "
                            + "I need the reason in writing.",
                    1, Status.SUBMITTED, null));

    private SampleData() {
    }

    public static void seed() throws SQLException {
        seed(Clock.systemDefaultZone());
    }

    static void seed(Clock clock) throws SQLException {
        GrievanceDao dao = new GrievanceDao();
        LocalDate today = LocalDate.now(clock);
        long officerId;
        try (Connection c = Database.getConnection()) {
            officerId = userId(c, "officer1");
        }
        for (Sample s : SAMPLES) {
            long ownerId;
            try (Connection c = Database.getConnection()) {
                if (exists(c, s.policyNumber, s.title)) {
                    continue;
                }
                ownerId = userId(c, s.owner);
            }
            LocalDate createdOn = today.minusDays(s.daysAgo);
            Grievance g = new Grievance();
            g.setPolicyholderId(ownerId);
            g.setPolicyNumber(s.policyNumber);
            g.setCategory(s.category);
            g.setTitle(s.title);
            g.setDescription(s.description);
            g.setStatus(Status.SUBMITTED);
            g.setCreatedOn(createdOn);
            g.setDueOn(createdOn.plusDays(GrievanceService.RESOLUTION_DAYS));
            dao.create(g, createdOn.atTime(9, 0));

            if (s.finalStatus == Status.UNDER_REVIEW || s.finalStatus == Status.RESOLVED) {
                move(g.getId(), Status.SUBMITTED, Status.UNDER_REVIEW, officerId, officerId, null,
                        createdOn.plusDays(1).atTime(10, 0));
            }
            if (s.finalStatus == Status.RESOLVED) {
                move(g.getId(), Status.UNDER_REVIEW, Status.RESOLVED, officerId, null, s.resolutionNote,
                        createdOn.plusDays(3).atTime(15, 0));
            }
        }
    }

    private static long userId(Connection c, String username) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Demo user missing, seed users first: " + username);
                }
                return rs.getLong(1);
            }
        }
    }

    private static boolean exists(Connection c, String policyNumber, String title) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM grievances WHERE policy_number = ? AND title = ?")) {
            ps.setString(1, policyNumber);
            ps.setString(2, title);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** Applies one status change and records it in the history, in a single transaction. */
    private static void move(long grievanceId, Status from, Status to, long changedBy, Long assignOfficerId,
            String resolutionNote, LocalDateTime at) throws SQLException {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE grievances SET status = ?, "
                                + "assigned_officer_id = COALESCE(?, assigned_officer_id), "
                                + "resolution_note = COALESCE(?, resolution_note) WHERE id = ?")) {
                    ps.setString(1, to.name());
                    ps.setObject(2, assignOfficerId);
                    ps.setString(3, resolutionNote);
                    ps.setLong(4, grievanceId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO status_history (grievance_id, from_status, to_status, changed_by, "
                                + "note, changed_at) VALUES (?, ?, ?, ?, ?, ?)")) {
                    ps.setLong(1, grievanceId);
                    ps.setString(2, from.name());
                    ps.setString(3, to.name());
                    ps.setLong(4, changedBy);
                    ps.setString(5, resolutionNote);
                    ps.setTimestamp(6, Timestamp.valueOf(at));
                    ps.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
