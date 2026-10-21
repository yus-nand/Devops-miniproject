package com.grievanceportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grievanceportal.model.Role;
import com.grievanceportal.model.User;
import com.grievanceportal.util.Schema;
import com.grievanceportal.util.Seeder;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AuthServiceTest {

    private final AuthService service = new AuthService();

    @BeforeAll
    static void prepareDatabase() throws Exception {
        System.setProperty("GP_DB_PATH", "mem:authtest");
        Schema.init();
        Seeder.seedUsers();
    }

    @AfterAll
    static void clearSetting() {
        System.clearProperty("GP_DB_PATH");
    }

    @Test
    void validPolicyholderLogin() throws Exception {
        Optional<User> user = service.login("asha", Seeder.DEMO_PASSWORD);
        assertTrue(user.isPresent());
        assertEquals(Role.POLICYHOLDER, user.get().getRole());
        assertEquals("Asha Verma", user.get().getFullName());
    }

    @Test
    void officerAndAdminRolesAreLoaded() throws Exception {
        assertEquals(Role.OFFICER, service.login("officer1", Seeder.DEMO_PASSWORD).orElseThrow().getRole());
        assertEquals(Role.ADMIN, service.login("admin1", Seeder.DEMO_PASSWORD).orElseThrow().getRole());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        assertTrue(service.login("asha", "wrong-password").isEmpty());
    }

    @Test
    void unknownUserIsRejected() throws Exception {
        assertTrue(service.login("nobody", Seeder.DEMO_PASSWORD).isEmpty());
    }

    @Test
    void blankInputIsRejected() throws Exception {
        assertTrue(service.login("", "x").isEmpty());
        assertTrue(service.login("asha", "").isEmpty());
        assertTrue(service.login(null, null).isEmpty());
    }
}
