package com.grievanceportal.dao;

import com.grievanceportal.model.Category;
import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.Status;
import com.grievanceportal.util.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class GrievanceDao {

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
}
