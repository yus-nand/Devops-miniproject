package com.grievanceportal.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void correctPasswordVerifies() {
        String stored = PasswordHasher.hash("secret");
        assertTrue(PasswordHasher.verify("secret", stored));
    }

    @Test
    void wrongPasswordFails() {
        String stored = PasswordHasher.hash("secret");
        assertFalse(PasswordHasher.verify("other", stored));
    }

    @Test
    void samePasswordGivesDifferentHashesBecauseOfSalt() {
        assertNotEquals(PasswordHasher.hash("secret"), PasswordHasher.hash("secret"));
    }

    @Test
    void malformedStoredValueFails() {
        assertFalse(PasswordHasher.verify("secret", "not-a-valid-hash"));
    }
}