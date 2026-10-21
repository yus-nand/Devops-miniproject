package com.grievanceportal.dao;

import com.grievanceportal.model.Role;
import com.grievanceportal.model.User;
import com.grievanceportal.util.Database;
import com.grievanceportal.util.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDao {

    /** Returns the user when the username exists and the password matches. */
    public Optional<User> authenticate(String username, String password) throws SQLException {
        String sql = "SELECT id, username, password_hash, full_name, role FROM users WHERE username = ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                if (!PasswordHasher.verify(password, rs.getString("password_hash"))) {
                    return Optional.empty();
                }
                return Optional.of(new User(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        Role.valueOf(rs.getString("role"))));
            }
        }
    }
}
