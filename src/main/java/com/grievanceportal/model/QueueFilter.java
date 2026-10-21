package com.grievanceportal.model;

import java.util.Optional;

/** What staff typed into the search and filter form on the all-grievances list. */
public final class QueueFilter {

    /** A filter that matches every grievance. */
    public static final QueueFilter NONE = new QueueFilter("", null, null, false);

    private final String query;
    private final Status status;
    private final Category category;
    private final boolean overdueOnly;

    private QueueFilter(String query, Status status, Category category, boolean overdueOnly) {
        this.query = query;
        this.status = status;
        this.category = category;
        this.overdueOnly = overdueOnly;
    }

    /**
     * Builds a filter from raw request parameters. Missing or blank values mean "no restriction",
     * and a status or category that does not exist is ignored instead of failing the page.
     */
    public static QueueFilter of(String query, String statusCode, String categoryCode) {
        return of(query, statusCode, categoryCode, false);
    }

    /** Same as above, and when overdueOnly is true only open grievances past their due date match. */
    public static QueueFilter of(String query, String statusCode, String categoryCode, boolean overdueOnly) {
        String text = query == null ? "" : query.trim();
        if (text.length() > 60) {
            text = text.substring(0, 60);
        }
        return new QueueFilter(text, parseStatus(statusCode).orElse(null), Category.parse(categoryCode).orElse(null),
                overdueOnly);
    }

    private static Optional<Status> parseStatus(String code) {
        if (code == null) {
            return Optional.empty();
        }
        for (Status status : Status.values()) {
            if (status.name().equals(code)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }

    public String getQuery() {
        return query;
    }

    public Status getStatus() {
        return status;
    }

    public Category getCategory() {
        return category;
    }

    /** The status code for the form, empty when no status is chosen. */
    public String getStatusCode() {
        return status == null ? "" : status.getCode();
    }

    /** The category code for the form, empty when no category is chosen. */
    public String getCategoryCode() {
        return category == null ? "" : category.getCode();
    }

    public boolean isOverdueOnly() {
        return overdueOnly;
    }

    /** True when at least one restriction is set. */
    public boolean isActive() {
        return !query.isEmpty() || status != null || category != null || overdueOnly;
    }
}
