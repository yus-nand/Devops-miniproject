package com.grievanceportal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SchemaTest {

    @BeforeAll
    static void useInMemoryDatabase() {
        System.setProperty("GP_DB_PATH", "mem:schematest");
    }

    @AfterAll
    static void clearSetting() {
        System.clearProperty("GP_DB_PATH");
    }

    @Test
    void seedingCreatesDemoUsersOnlyOnce() throws Exception {
        Schema.init();
        Seeder.seedUsers();
        Seeder.seedUsers();
        try (Connection c = Database.getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            assertEquals(4, rs.getInt(1));
        }
    }

    @Test
    void demoPasswordIsStoredHashed() throws Exception {
        Schema.init();
        Seeder.seedUsers();
        try (Connection c = Database.getConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT password_hash FROM users WHERE username = ?")) {
            ps.setString(1, "asha");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String stored = rs.getString(1);
                assertNotEquals(Seeder.DEMO_PASSWORD, stored);
                assertTrue(PasswordHasher.verify(Seeder.DEMO_PASSWORD, stored));
            }
        }
    }
}