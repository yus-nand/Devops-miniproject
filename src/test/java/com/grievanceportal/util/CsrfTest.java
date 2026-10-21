package com.grievanceportal.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CsrfTest {

    @Test
    void identicalTokensMatch() {
        assertTrue(Csrf.matches("abc", "abc"));
    }

    @Test
    void differentTokensDoNotMatch() {
        assertFalse(Csrf.matches("abc", "abd"));
    }

    @Test
    void missingTokensDoNotMatch() {
        assertFalse(Csrf.matches(null, "abc"));
        assertFalse(Csrf.matches("abc", null));
        assertFalse(Csrf.matches(null, null));
    }

    @Test
    void newTokensAreUniqueAndNotBlank() {
        String first = Csrf.newToken();
        assertFalse(first.isBlank());
        assertNotEquals(first, Csrf.newToken());
    }
}
