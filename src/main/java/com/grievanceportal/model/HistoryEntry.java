package com.grievanceportal.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** One row of a grievance's status history. The first entry has no previous status. */
public class HistoryEntry {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Status fromStatus;
    private final Status toStatus;
    private final String changedByName;
    private final Role changedByRole;
    private final String note;
    private final LocalDateTime changedAt;

    public HistoryEntry(Status fromStatus, Status toStatus, String changedByName, Role changedByRole,
            String note, LocalDateTime changedAt) {
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByName = changedByName;
        this.changedByRole = changedByRole;
        this.note = note;
        this.changedAt = changedAt;
    }

    /** The previous status, or null for the first entry. */
    public Status getFromStatus() {
        return fromStatus;
    }

    public Status getToStatus() {
        return toStatus;
    }

    public String getChangedByName() {
        return changedByName;
    }

    public Role getChangedByRole() {
        return changedByRole;
    }

    /** The note recorded with the change, or null. */
    public String getNote() {
        return note;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public String getChangedAtText() {
        return changedAt.format(DISPLAY);
    }
}
