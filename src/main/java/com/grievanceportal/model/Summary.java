package com.grievanceportal.model;

import java.util.Map;

/** The numbers on the admin dashboard. Every status and category is present, with 0 when it has none. */
public final class Summary {

    private final int total;
    private final Map<Status, Integer> byStatus;
    private final Map<Category, Integer> byCategory;
    private final int overdue;

    public Summary(int total, Map<Status, Integer> byStatus, Map<Category, Integer> byCategory, int overdue) {
        this.total = total;
        this.byStatus = byStatus;
        this.byCategory = byCategory;
        this.overdue = overdue;
    }

    public int getTotal() {
        return total;
    }

    public Map<Status, Integer> getByStatus() {
        return byStatus;
    }

    public Map<Category, Integer> getByCategory() {
        return byCategory;
    }

    public int getOverdue() {
        return overdue;
    }
}
