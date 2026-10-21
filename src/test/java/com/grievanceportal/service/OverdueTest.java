package com.grievanceportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceRow;
import com.grievanceportal.model.Status;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverdueTest {

    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneId.of("UTC"));

    private static Grievance grievance(Status status, String due) {
        Grievance g = new Grievance();
        g.setStatus(status);
        g.setDueOn(LocalDate.parse(due));
        return g;
    }

    @Test
    void flagKeepsTheOrderAndMarksOnlyOpenPastDueGrievances() {
        List<GrievanceRow> rows = Overdue.flag(List.of(
                grievance(Status.UNDER_REVIEW, "2026-10-10"),
                grievance(Status.UNDER_REVIEW, "2026-10-20"),
                grievance(Status.CLOSED, "2026-10-10"),
                grievance(Status.SUBMITTED, "2026-10-19")), NOW);

        assertEquals(4, rows.size());
        assertTrue(rows.get(0).isOverdue());
        assertFalse(rows.get(1).isOverdue());
        assertFalse(rows.get(2).isOverdue());
        assertTrue(rows.get(3).isOverdue());
    }

    @Test
    void anEmptyListStaysEmpty() {
        assertTrue(Overdue.flag(List.of(), NOW).isEmpty());
    }
}
