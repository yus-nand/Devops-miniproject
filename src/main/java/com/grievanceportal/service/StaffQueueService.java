package com.grievanceportal.service;

import com.grievanceportal.dao.GrievanceDao;
import com.grievanceportal.model.GrievanceRow;
import com.grievanceportal.model.QueueFilter;
import java.sql.SQLException;
import java.time.Clock;
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
    public List<GrievanceRow> queue(QueueFilter filter) throws SQLException {
        return Overdue.flag(dao.search(filter.getQuery(), filter.getStatus(), filter.getCategory()), clock);
    }
}
