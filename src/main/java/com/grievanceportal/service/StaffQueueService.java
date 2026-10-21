package com.grievanceportal.service;

import com.grievanceportal.dao.GrievanceDao;
import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.QueueFilter;
import com.grievanceportal.model.QueueRow;
import com.grievanceportal.model.Status;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** The all-grievances list for officers and admins, with search, filters and the Overdue flag. */
public class StaffQueueService {

    private final GrievanceDao dao = new GrievanceDao();
    private final Clock clock;

    public StaffQueueService() {
        this(Clock.systemDefaultZone());
    }

    StaffQueueService(Clock clock) {
        this.clock = clock;
    }

    /** Grievances that match the filter, earliest due date first. */
    public List<QueueRow> queue(QueueFilter filter) throws SQLException {
        LocalDate today = LocalDate.now(clock);
        List<QueueRow> rows = new ArrayList<>();
        for (Grievance g : dao.search(filter.getQuery(), filter.getStatus(), filter.getCategory())) {
            rows.add(new QueueRow(g, isOverdue(g, today)));
        }
        return rows;
    }

    /** A grievance is overdue when it is still open and its due date has passed. */
    public static boolean isOverdue(Grievance g, LocalDate today) {
        boolean open = g.getStatus() == Status.SUBMITTED || g.getStatus() == Status.UNDER_REVIEW;
        return open && today.isAfter(g.getDueOn());
    }
}
