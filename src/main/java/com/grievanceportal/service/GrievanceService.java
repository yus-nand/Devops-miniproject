package com.grievanceportal.service;

import com.grievanceportal.dao.GrievanceDao;
import com.grievanceportal.model.Category;
import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceDetail;
import com.grievanceportal.model.GrievanceForm;
import com.grievanceportal.model.Role;
import com.grievanceportal.model.Status;
import com.grievanceportal.model.User;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;

public class GrievanceService {

    /** Days an insurer has to resolve a grievance. */
    public static final int RESOLUTION_DAYS = 14;

    /** Longest note a user may enter. */
    public static final int MAX_NOTE_LENGTH = 1000;

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

    /**
     * The detail view of one grievance. A policyholder gets it only for their own grievances,
     * staff get it for any grievance. Anything else looks the same as a missing grievance.
     */
    public Optional<GrievanceDetail> findDetail(long id, User viewer) throws SQLException {
        Optional<Grievance> found = dao.findById(id);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Grievance g = found.get();
        boolean own = g.getPolicyholderId() == viewer.getId();
        if (viewer.isPolicyholder() && !own) {
            return Optional.empty();
        }
        String policyholderName = dao.fullNameOf(g.getPolicyholderId()).orElse("Unknown");
        String officerName = g.getAssignedOfficerId() == null
                ? null
                : dao.fullNameOf(g.getAssignedOfficerId()).orElse(null);
        boolean editable = viewer.isPolicyholder() && own && g.getStatus() == Status.SUBMITTED;
        return Optional.of(new GrievanceDetail(g, dao.historyOf(id), policyholderName, officerName, editable));
    }

    /** Finds a grievance by id, only if it belongs to this user. */
    public Optional<Grievance> findOwnById(long id, User user) throws SQLException {
        return dao.findById(id).filter(g -> g.getPolicyholderId() == user.getId());
    }

    /**
     * Saves new values for a grievance the user owns, while it is still Submitted.
     * The reference ID, due date and status stay as they are.
     *
     * @throws NoSuchElementException if the grievance does not exist or is not the user's
     * @throws IllegalStateException if review has started, so editing is closed
     * @throws IllegalArgumentException if the form is not valid
     */
    public Grievance update(User user, long id, GrievanceForm form) throws SQLException {
        Grievance current = findOwnById(id, user)
                .orElseThrow(() -> new NoSuchElementException("Grievance not found"));
        if (current.getStatus() != Status.SUBMITTED) {
            throw new IllegalStateException("The grievance can no longer be edited");
        }
        Map<String, String> errors = validate(form);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("The form is not valid: " + errors.keySet());
        }
        boolean saved = dao.updateWhileSubmitted(id, user.getId(), form.getPolicyNumber(),
                Category.parse(form.getCategory()).orElseThrow(), form.getTitle(), form.getDescription());
        if (!saved) {
            throw new IllegalStateException("The grievance can no longer be edited");
        }
        return dao.findById(id).orElseThrow();
    }

    /**
     * An officer picks up a Submitted grievance: it is assigned to them and moves to Under Review.
     *
     * @throws NoSuchElementException if the grievance does not exist
     * @throws IllegalStateException if the user is not an officer or the grievance cannot be picked up;
     *         the message says why and is safe to show
     */
    public Grievance pickUp(User officer, long id) throws SQLException {
        Grievance g = dao.findById(id).orElseThrow(() -> new NoSuchElementException("Grievance not found"));
        if (Workflow.find(Status.SUBMITTED, Status.UNDER_REVIEW, officer.getRole()).isEmpty()) {
            throw new IllegalStateException("Only officers can pick up a grievance.");
        }
        if (g.getStatus() != Status.SUBMITTED) {
            throw new IllegalStateException(alreadyTakenMessage(g, officer));
        }
        if (!dao.pickUp(id, officer.getId(), LocalDateTime.now(clock))) {
            Grievance latest = dao.findById(id).orElse(g);
            throw new IllegalStateException(alreadyTakenMessage(latest, officer));
        }
        return dao.findById(id).orElseThrow();
    }

    private String alreadyTakenMessage(Grievance g, User officer) throws SQLException {
        Long assigned = g.getAssignedOfficerId();
        if (assigned == null) {
            return "Only a Submitted grievance can be picked up.";
        }
        if (assigned == officer.getId()) {
            return "You have already picked up this grievance.";
        }
        String name = dao.fullNameOf(assigned).orElse("another officer");
        return "This grievance has already been picked up by " + name + ".";
    }

    /**
     * Changes the status of a grievance by the rules in {@link Workflow}. Resolving or rejecting needs a
     * resolution note, reopening needs a reason, and both are kept in the history. A resolution note is
     * also kept on the grievance; reopening clears it, because the grievance is open again.
     *
     * @throws NoSuchElementException if the grievance does not exist
     * @throws IllegalStateException if the user may not make this change; the message is safe to show
     * @throws IllegalArgumentException if a required note is missing or too long; the message is safe to show
     */
    public Grievance changeStatus(User actor, long id, Status to, String note) throws SQLException {
        Grievance g = dao.findById(id).orElseThrow(() -> new NoSuchElementException("Grievance not found"));
        Status from = g.getStatus();
        Workflow.Move move = Workflow.find(from, to, actor.getRole()).orElseThrow(() ->
                new IllegalStateException("You cannot move a grievance from " + from.getLabel()
                        + " to " + to.getLabel() + "."));
        if (from == Status.SUBMITTED) {
            throw new IllegalStateException("Use Pick up to start the review of a Submitted grievance.");
        }
        if (move.getRole() == Role.OFFICER
                && (g.getAssignedOfficerId() == null || g.getAssignedOfficerId() != actor.getId())) {
            throw new IllegalStateException("Only the officer who picked this grievance up can change its status.");
        }
        String text = note == null ? "" : note.trim();
        if (move.isNoteRequired() && text.isEmpty()) {
            throw new IllegalArgumentException("Enter a " + move.getNoteName() + ".");
        }
        if (text.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("The note can be at most " + MAX_NOTE_LENGTH + " characters.");
        }
        String resolutionNote;
        if (to == Status.RESOLVED || to == Status.REJECTED) {
            resolutionNote = text;
        } else if (to == Status.UNDER_REVIEW) {
            resolutionNote = null;
        } else {
            resolutionNote = g.getResolutionNote();
        }
        boolean changed = dao.changeStatus(id, from, to, resolutionNote, actor.getId(),
                text.isEmpty() ? null : text, LocalDateTime.now(clock));
        if (!changed) {
            throw new IllegalStateException("This grievance was changed by someone else. Reload the page and try again.");
        }
        return dao.findById(id).orElseThrow();
    }

    /** Finds a grievance by reference number, only if it belongs to this user. */
    public Optional<Grievance> findOwn(String refNo, User user) throws SQLException {
        if (refNo == null || refNo.isBlank()) {
            return Optional.empty();
        }
        return dao.findByRefAndOwner(refNo.trim(), user.getId());
    }
}
