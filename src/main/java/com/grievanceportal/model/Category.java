package com.grievanceportal.model;

import java.util.Optional;

public enum Category {
    CLAIM_SETTLEMENT("Claim settlement"),
    PREMIUM_AND_BILLING("Premium and billing"),
    POLICY_SERVICING("Policy servicing"),
    MIS_SELLING("Mis-selling"),
    OTHER("Other");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** The value stored in the database and sent by the form. */
    public String getCode() {
        return name();
    }

    public static Optional<Category> parse(String code) {
        if (code == null) {
            return Optional.empty();
        }
        for (Category category : values()) {
            if (category.name().equals(code)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
