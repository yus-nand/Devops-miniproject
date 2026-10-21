package com.grievanceportal.model;

/** The values typed into the submission form, trimmed. Missing values become empty strings. */
public class GrievanceForm {

    private final String policyNumber;
    private final String category;
    private final String title;
    private final String description;

    public GrievanceForm(String policyNumber, String category, String title, String description) {
        this.policyNumber = clean(policyNumber);
        this.category = clean(category);
        this.title = clean(title);
        this.description = clean(description);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
