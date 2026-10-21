package com.grievanceportal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HealthServletTest {

    @Test
    void upResponseContainsStatusVersionAndEnvironment() {
        String json = HealthServlet.buildJson(true, "1.2.3", "local");
        assertEquals("{\"status\":\"UP\",\"version\":\"1.2.3\",\"environment\":\"local\"}", json);
    }

    @Test
    void downResponseReportsDown() {
        assertTrue(HealthServlet.buildJson(false, "1.0", "local").contains("\"status\":\"DOWN\""));
    }

    @Test
    void quotesInValuesAreEscaped() {
        String json = HealthServlet.buildJson(true, "1.0", "a\"b");
        assertTrue(json.contains("a\\\"b"));
    }
}