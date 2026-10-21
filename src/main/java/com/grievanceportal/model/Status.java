package com.grievanceportal.model;

public enum Status {
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under Review"),
    RESOLVED("Resolved"),
    REJECTED("Rejected"),
    CLOSED("Closed");

    private final String label;

    Status(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public String getCode() {
        return name();
    }
}
