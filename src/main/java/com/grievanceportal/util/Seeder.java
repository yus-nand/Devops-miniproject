package com.grievanceportal.util;

import com.grievanceportal.model.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Inserts the demo users. Safe to run more than once. All demo data is synthetic. */
public final class Seeder {

    /** Password shared by all demo users. These are demo accounts, not real credentials. */
    public static final String DEMO_PASSWORD = "demo123";

    private Seeder() {
    }

    public static void seedUsers() throws SQLException {
        try (Connection c = Database.getConnection()) {
            addUser(c, "asha", "Asha Verma", Role.POLICYHOLDER);
            addUser(c, "ravi", "Ravi Kulkarni", Role.POLICYHOLDER);
            addUser(c, "officer1", "Meera Joshi", Role.OFFICER);
            addUser(c, "admin1", "Arjun Rao", Role.ADMIN);
        }
    }

    private static void addUser(Connection c, String username, String fullName, Role role)
            throws SQLException {
        try (PreparedStatement check = c.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE username = ?")) {
            check.setString(1, username);
            try (ResultSet rs = check.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) {
                    return;
                }
            }
        }
        try (PreparedStatement insert = c.prepareStatement(
                "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)")) {
            insert.setString(1, username);
            insert.setString(2, PasswordHasher.hash(DEMO_PASSWORD));
            insert.setString(3, fullName);
            insert.setString(4, role.name());
            insert.executeUpdate();
        }
    }
}