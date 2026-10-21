package com.grievanceportal.model;

/** One line of the staff list: a grievance and whether it is past its due date. */
public final class QueueRow {

    private final Grievance grievance;
    private final boolean overdue;

    public QueueRow(Grievance grievance, boolean overdue) {
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
