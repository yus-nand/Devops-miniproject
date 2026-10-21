package com.grievanceportal.service;

import com.grievanceportal.dao.GrievanceDao;
import com.grievanceportal.model.Category;
import com.grievanceportal.model.Status;
import com.grievanceportal.model.Summary;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Counts for the admin dashboard (US-13). They come from the same data and the same Overdue rule as the lists. */
public class DashboardService {

    private final GrievanceDao dao = new GrievanceDao();
    private final Clock clock;

    public DashboardService() {
        this(Clock.systemDefaultZone());
    }

    DashboardService(Clock clock) {
        this.clock = clock;
    }

    public Summary summarize() throws SQLException {
        Map<Status, Integer> byStatus = new LinkedHashMap<>();
        for (Status status : Status.values()) {
            byStatus.put(status, 0);
        }
        byStatus.putAll(dao.countByStatus());

        Map<Category, Integer> byCategory = new LinkedHashMap<>();
        for (Category category : Category.values()) {
            byCategory.put(category, 0);
        }
        byCategory.putAll(dao.countByCategory());

        int total = byStatus.values().stream().mapToInt(Integer::intValue).sum();
        int overdue = dao.countOverdue(LocalDate.now(clock));
        return new Summary(total, byStatus, byCategory, overdue);
    }
}
