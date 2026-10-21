package com.grievanceportal.dao;

import com.grievanceportal.model.Category;
import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.HistoryEntry;
import com.grievanceportal.model.Role;
import com.grievanceportal.model.Status;
import com.grievanceportal.util.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GrievanceDao {

    /** The statuses that count as open, as an SQL list. */
    private static final String OPEN_STATUSES = "'" + Status.SUBMITTED.name() + "', '" + Status.UNDER_REVIEW.name() + "'";

    private static final String COLUMNS = "id, ref_no, policyholder_id, assigned_officer_id, policy_number, "
            + "category, title, description, status, resolution_note, created_on, due_on";

    /** Saves a new grievance and its first history entry. Fills in the id and reference number. */
    public Grievance create(Grievance g, LocalDateTime now) throws SQLException {
        String insert = "INSERT INTO grievances (ref_no, policyholder_id, policy_number, category, title, "
                + "description, status, created_on, due_on) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String history = "INSERT INTO status_history (grievance_id, from_status, to_status, changed_by, "
                + "note, changed_at) VALUES (?, NULL, ?, ?, NULL, ?)";

        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement ps = c.prepareStatement(insert, new String[] {"ID"})) {
                    ps.setString(1, "TMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
                    ps.setLong(2, g.getPolicyholderId());
                    ps.setString(3, g.getPolicyNumber());
                    ps.setString(4, g.getCategory().name());
                    ps.setString(5, g.getTitle());
                    ps.setString(6, g.getDescription());
                    ps.setString(7, g.getStatus().name());
                    ps.setObject(8, g.getCreatedOn());
                    ps.setObject(9, g.getDueOn());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getLong(1);
                    }
                }

                String refNo = String.format(Locale.ROOT, "GRV-%d-%04d", g.getCreatedOn().getYear(), id);
                try (PreparedStatement ps = c.prepareStatement("UPDATE grievances SET ref_no = ? WHERE id = ?")) {
                    ps.setString(1, refNo);
                    ps.setLong(2, id);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(history)) {
                    ps.setLong(1, id);
                    ps.setString(2, g.getStatus().name());
                    ps.setLong(3, g.getPolicyholderId());
                    ps.setTimestamp(4, Timestamp.valueOf(now));
                    ps.executeUpdate();
                }

                c.commit();
                g.setId(id);
                g.setRefNo(refNo);
                return g;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Finds a grievance by reference number, only if it belongs to the given policyholder. */
    public Optional<Grievance> findByRefAndOwner(String refNo, long ownerId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM grievances WHERE ref_no = ? AND policyholder_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, refNo);
            ps.setLong(2, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Changes the editable fields, but only while the grievance still belongs to this owner and is
     * Submitted. Returns false when nothing was changed. The reference ID, due date and history stay as they are.
     */
    public boolean updateWhileSubmitted(long id, long ownerId, String policyNumber, Category category,
            String title, String description) throws SQLException {
        String sql = "UPDATE grievances SET policy_number = ?, category = ?, title = ?, description = ? "
                + "WHERE id = ? AND policyholder_id = ? AND status = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, policyNumber);
            ps.setString(2, category.name());
            ps.setString(3, title);
            ps.setString(4, description);
            ps.setLong(5, id);
            ps.setLong(6, ownerId);
            ps.setString(7, Status.SUBMITTED.name());
            return ps.executeUpdate() == 1;
        }
    }

    /** All grievances of one policyholder, newest first. */
    public List<Grievance> findByOwner(long ownerId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM grievances WHERE policyholder_id = ? "
                + "ORDER BY created_on DESC, id DESC";
        List<Grievance> result = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    /** Finds a grievance by its numeric id. */
    public Optional<Grievance> findById(long id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM grievances WHERE id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** The status history of one grievance, oldest first, with the name and role of who changed it. */
    public List<HistoryEntry> historyOf(long grievanceId) throws SQLException {
        String sql = "SELECT h.from_status, h.to_status, h.note, h.changed_at, u.full_name, u.role "
                + "FROM status_history h JOIN users u ON u.id = h.changed_by "
                + "WHERE h.grievance_id = ? ORDER BY h.changed_at, h.id";
        List<HistoryEntry> result = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, grievanceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String from = rs.getString("from_status");
                    result.add(new HistoryEntry(
                            from == null ? null : Status.valueOf(from),
                            Status.valueOf(rs.getString("to_status")),
                            rs.getString("full_name"),
                            Role.valueOf(rs.getString("role")),
                            rs.getString("note"),
                            rs.getTimestamp("changed_at").toLocalDateTime()));
                }
            }
        }
        return result;
    }

    /** The full name of a user, if that user exists. */
    public Optional<String> fullNameOf(long userId) throws SQLException {
        try (Connection c = Database.getConnection();
                PreparedStatement ps = c.prepareStatement("SELECT full_name FROM users WHERE id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        }
    }

    private static Grievance map(ResultSet rs) throws SQLException {
        Grievance g = new Grievance();
        g.setId(rs.getLong("id"));
        g.setRefNo(rs.getString("ref_no"));
        g.setPolicyholderId(rs.getLong("policyholder_id"));
        long officerId = rs.getLong("assigned_officer_id");
        g.setAssignedOfficerId(rs.wasNull() ? null : officerId);
        g.setPolicyNumber(rs.getString("policy_number"));
        g.setCategory(Category.valueOf(rs.getString("category")));
        g.setTitle(rs.getString("title"));
        g.setDescription(rs.getString("description"));
        g.setStatus(Status.valueOf(rs.getString("status")));
        g.setResolutionNote(rs.getString("resolution_note"));
        g.setCreatedOn(rs.getObject("created_on", LocalDate.class));
        g.setDueOn(rs.getObject("due_on", LocalDate.class));
        return g;
    }

    /**
     * Grievances for the staff list, earliest due date first. The text matches part of the reference
     * ID or policy number, ignoring case. A null or blank text, status or category does not restrict.
     */
    public List<Grievance> search(String text, Status status, Category category) throws SQLException {
        return search(text, status, category, null);
    }

    /** Like the search above. When overdueOn is given, only open grievances due before that day are returned. */
    public List<Grievance> search(String text, Status status, Category category, LocalDate overdueOn)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM grievances WHERE 1 = 1");
        List<String> values = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String pattern = "%" + escapeLike(text.trim().toUpperCase(Locale.ROOT)) + "%";
            sql.append(" AND (UPPER(ref_no) LIKE ? ESCAPE '\\' OR UPPER(policy_number) LIKE ? ESCAPE '\\')");
            values.add(pattern);
            values.add(pattern);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            values.add(status.name());
        }
        if (category != null) {
            sql.append(" AND category = ?");
            values.add(category.name());
        }
        if (overdueOn != null) {
            sql.append(" AND status IN (" + OPEN_STATUSES + ") AND due_on < ?");
        }
        sql.append(" ORDER BY due_on ASC, id ASC");

        List<Grievance> result = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int next = 1;
            for (String value : values) {
                ps.setString(next++, value);
            }
            if (overdueOn != null) {
                ps.setObject(next, overdueOn);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * Assigns a Submitted, unassigned grievance to an officer and moves it to Under Review, and records
     * the history row, in one transaction. Returns false when someone else got there first.
     */
    public boolean pickUp(long id, long officerId, LocalDateTime now) throws SQLException {
        String update = "UPDATE grievances SET status = ?, assigned_officer_id = ? "
                + "WHERE id = ? AND status = ? AND assigned_officer_id IS NULL";
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                int changed;
                try (PreparedStatement ps = c.prepareStatement(update)) {
                    ps.setString(1, Status.UNDER_REVIEW.name());
                    ps.setLong(2, officerId);
                    ps.setLong(3, id);
                    ps.setString(4, Status.SUBMITTED.name());
                    changed = ps.executeUpdate();
                }
                if (changed != 1) {
                    c.rollback();
                    return false;
                }
                insertHistory(c, id, Status.SUBMITTED, Status.UNDER_REVIEW, officerId, null, now);
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /**
     * Moves a grievance from one status to another and records the history row, in one transaction.
     * It only happens while the grievance is still in the status the caller saw; otherwise it returns false.
     */
    public boolean changeStatus(long id, Status from, Status to, String resolutionNote, long changedBy,
            String historyNote, LocalDateTime now) throws SQLException {
        String update = "UPDATE grievances SET status = ?, resolution_note = ? WHERE id = ? AND status = ?";
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                int changed;
                try (PreparedStatement ps = c.prepareStatement(update)) {
                    ps.setString(1, to.name());
                    ps.setString(2, resolutionNote);
                    ps.setLong(3, id);
                    ps.setString(4, from.name());
                    changed = ps.executeUpdate();
                }
                if (changed != 1) {
                    c.rollback();
                    return false;
                }
                insertHistory(c, id, from, to, changedBy, historyNote, now);
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private static void insertHistory(Connection c, long grievanceId, Status from, Status to, long changedBy,
            String note, LocalDateTime now) throws SQLException {
        String sql = "INSERT INTO status_history (grievance_id, from_status, to_status, changed_by, note, "
                + "changed_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, grievanceId);
            ps.setString(2, from.name());
            ps.setString(3, to.name());
            ps.setLong(4, changedBy);
            ps.setString(5, note);
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.executeUpdate();
        }
    }

    /** How many grievances are in each status. Statuses with none are left out. */
    public Map<Status, Integer> countByStatus() throws SQLException {
        Map<Status, Integer> counts = new LinkedHashMap<>();
        try (Connection c = Database.getConnection();
                PreparedStatement ps = c.prepareStatement("SELECT status, COUNT(*) FROM grievances GROUP BY status");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(Status.valueOf(rs.getString(1)), rs.getInt(2));
            }
        }
        return counts;
    }

    /** How many grievances are in each category. Categories with none are left out. */
    public Map<Category, Integer> countByCategory() throws SQLException {
        Map<Category, Integer> counts = new LinkedHashMap<>();
        try (Connection c = Database.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT category, COUNT(*) FROM grievances GROUP BY category");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(Category.valueOf(rs.getString(1)), rs.getInt(2));
            }
        }
        return counts;
    }

    /** How many open grievances are due before the given day. */
    public int countOverdue(LocalDate today) throws SQLException {
        String sql = "SELECT COUNT(*) FROM grievances WHERE status IN (" + OPEN_STATUSES + ") AND due_on < ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, today);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
