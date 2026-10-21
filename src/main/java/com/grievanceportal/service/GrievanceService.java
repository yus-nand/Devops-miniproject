package com.grievanceportal.service;

import com.grievanceportal.dao.GrievanceDao;
import com.grievanceportal.model.Category;
import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceForm;
import com.grievanceportal.model.Status;
import com.grievanceportal.model.User;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class GrievanceService {

    /** Days an insurer has to resolve a grievance. */
    public static final int RESOLUTION_DAYS = 14;

    private static final Pattern POLICY_NUMBER = Pattern.compile("[A-Za-z0-9-]{5,30}");

    private final GrievanceDao dao = new GrievanceDao();
    private final Clock clock;

    public GrievanceService() {
        this(Clock.systemDefaultZone());
    }

    GrievanceService(Clock clock) {
        this.clock = clock;
    }

    /** Returns one message per invalid field, keyed by field name. Empty means the form is valid. */
    public Map<String, String> validate(GrievanceForm form) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (!POLICY_NUMBER.matcher(form.getPolicyNumber()).matches()) {
            errors.put("policyNumber", "Enter a policy number of 5 to 30 letters, digits or hyphens.");
        }
        if (Category.parse(form.getCategory()).isEmpty()) {
            errors.put("category", "Choose a category.");
        }
        int titleLength = form.getTitle().length();
        if (titleLength == 0 || titleLength > 150) {
            errors.put("title", "Enter a title of up to 150 characters.");
        }
        int descriptionLength = form.getDescription().length();
        if (descriptionLength < 10 || descriptionLength > 2000) {
            errors.put("description", "Describe the problem in 10 to 2000 characters.");
        }
        return errors;
    }

    /** Saves a valid form as a new Submitted grievance due in 14 days. */
    public Grievance submit(User user, GrievanceForm form) throws SQLException {
        Map<String, String> errors = validate(form);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("The form is not valid: " + errors.keySet());
        }
        LocalDate today = LocalDate.now(clock);
        Grievance g = new Grievance();
        g.setPolicyholderId(user.getId());
        g.setPolicyNumber(form.getPolicyNumber());
        g.setCategory(Category.parse(form.getCategory()).orElseThrow());
        g.setTitle(form.getTitle());
        g.setDescription(form.getDescription());
        g.setStatus(Status.SUBMITTED);
        g.setCreatedOn(today);
        g.setDueOn(today.plusDays(RESOLUTION_DAYS));
        return dao.create(g, LocalDateTime.now(clock));
    }

    /** The grievances this user submitted, newest first. */
    public List<Grievance> listOwn(User user) throws SQLException {
        return dao.findByOwner(user.getId());
    }

    /** Finds a grievance by reference number, only if it belongs to this user. */
    public Optional<Grievance> findOwn(String refNo, User user) throws SQLException {
        if (refNo == null || refNo.isBlank()) {
            return Optional.empty();
        }
        return dao.findByRefAndOwner(refNo.trim(), user.getId());
    }
}
