package com.grievanceportal.model;

import java.util.List;

/** Everything the detail page shows about one grievance, as seen by one viewer. */
public class GrievanceDetail {

    private final Grievance grievance;
    private final List<HistoryEntry> history;
    private final String policyholderName;
    private final String officerName;
    private final boolean editable;

    public GrievanceDetail(Grievance grievance, List<HistoryEntry> history, String policyholderName,
            String officerName, boolean editable) {
        this.grievance = grievance;
        this.history = history;
        this.policyholderName = policyholderName;
        this.officerName = officerName;
        this.editable = editable;
    }

    public Grievance getGrievance() {
        return grievance;
    }

    public List<HistoryEntry> getHistory() {
        return history;
    }

    public String getPolicyholderName() {
        return policyholderName;
    }

    /** The assigned officer's name, or null when nobody has picked the grievance up. */
    public String getOfficerName() {
        return officerName;
    }

    /** True when this viewer may still edit the grievance. */
    public boolean isEditable() {
        return editable;
    }
}
