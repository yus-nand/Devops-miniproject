package com.grievanceportal.model;

/** One line of a grievance list: the grievance and whether it is past its due date. */
public final class GrievanceRow {

    private final Grievance grievance;
    private final boolean overdue;

    public GrievanceRow(Grievance grievance, boolean overdue) {
        this.grievance = grievance;
        this.overdue = overdue;
    }

    public Grievance getGrievance() {
        return grievance;
    }

    public boolean isOverdue() {
        return overdue;
    }
}
