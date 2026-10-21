package com.grievanceportal.service;

import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceRow;
import com.grievanceportal.model.Status;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The Overdue rule (US-12), shared by every list that shows grievances. */
public final class Overdue {

    private Overdue() {
    }

    /** A grievance is overdue when it is still open and its due date has passed. */
    public static boolean isOverdue(Grievance g, LocalDate today) {
        boolean open = g.getStatus() == Status.SUBMITTED || g.getStatus() == Status.UNDER_REVIEW;
        return open && today.isAfter(g.getDueOn());
    }

    /** Pairs every grievance with its Overdue flag, as of today by the system clock. */
    public static List<GrievanceRow> flag(List<Grievance> grievances) {
        return flag(grievances, Clock.systemDefaultZone());
    }

    static List<GrievanceRow> flag(List<Grievance> grievances, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        List<GrievanceRow> rows = new ArrayList<>();
        for (Grievance g : grievances) {
            rows.add(new GrievanceRow(g, isOverdue(g, today)));
        }
        return rows;
    }
}
