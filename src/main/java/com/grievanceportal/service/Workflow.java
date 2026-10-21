package com.grievanceportal.service;

import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.Role;
import com.grievanceportal.model.Status;
import com.grievanceportal.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The fixed status workflow (US-09): which role may move a grievance from which status to which,
 * and whether a note is needed. Every other change is refused.
 */
public final class Workflow {

    /** One allowed change of status. */
    public static final class Move {
        private final Status from;
        private final Status to;
        private final Role role;
        private final String label;
        private final String noteName;

        private Move(Status from, Status to, Role role, String label, String noteName) {
            this.from = from;
            this.to = to;
            this.role = role;
            this.label = label;
            this.noteName = noteName;
        }

        public Status getFrom() {
            return from;
        }

        public Status getTo() {
            return to;
        }

        public Role getRole() {
            return role;
        }

        /** The text on the button for this move. */
        public String getLabel() {
            return label;
        }

        public boolean isNoteRequired() {
            return noteName != null;
        }

        /** What the note is called in messages, for example "resolution note". Null when no note is needed. */
        public String getNoteName() {
            return noteName;
        }
    }

    /** The buttons one viewer gets on one grievance. */
    public static final class Actions {
        private final boolean canPickUp;
        private final List<Move> noteMoves;
        private final List<Move> plainMoves;

        private Actions(boolean canPickUp, List<Move> noteMoves, List<Move> plainMoves) {
            this.canPickUp = canPickUp;
            this.noteMoves = noteMoves;
            this.plainMoves = plainMoves;
        }

        public boolean isCanPickUp() {
            return canPickUp;
        }

        /** Moves that need a note, shown as one form with a text box and one button per move. */
        public List<Move> getNoteMoves() {
            return noteMoves;
        }

        /** Moves that need no note, shown as plain buttons. */
        public List<Move> getPlainMoves() {
            return plainMoves;
        }

        public boolean isAny() {
            return canPickUp || !noteMoves.isEmpty() || !plainMoves.isEmpty();
        }

        /** The label for the note box, for example "Resolution note". */
        public String getNoteLabel() {
            if (noteMoves.isEmpty()) {
                return "";
            }
            String name = noteMoves.get(0).getNoteName();
            return Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }

    private static final List<Move> MOVES = List.of(
            new Move(Status.SUBMITTED, Status.UNDER_REVIEW, Role.OFFICER, "Pick up", null),
            new Move(Status.UNDER_REVIEW, Status.RESOLVED, Role.OFFICER, "Resolve", "resolution note"),
            new Move(Status.UNDER_REVIEW, Status.REJECTED, Role.OFFICER, "Reject", "resolution note"),
            new Move(Status.RESOLVED, Status.CLOSED, Role.ADMIN, "Close", null),
            new Move(Status.REJECTED, Status.CLOSED, Role.ADMIN, "Close", null),
            new Move(Status.RESOLVED, Status.UNDER_REVIEW, Role.ADMIN, "Reopen", "reason for reopening"));

    private Workflow() {
    }

    /** The move that lets this role change a grievance from one status to the other, if there is one. */
    public static Optional<Move> find(Status from, Status to, Role role) {
        for (Move move : MOVES) {
            if (move.from == from && move.to == to && move.role == role) {
                return Optional.of(move);
            }
        }
        return Optional.empty();
    }

    /**
     * The actions to offer this viewer. An officer may only resolve or reject a grievance that
     * is assigned to them, so that every grievance has one clear owner.
     */
    public static Actions actionsFor(Grievance g, User viewer) {
        boolean canPickUp = false;
        List<Move> noteMoves = new ArrayList<>();
        List<Move> plainMoves = new ArrayList<>();
        boolean ownsIt = g.getAssignedOfficerId() != null && g.getAssignedOfficerId() == viewer.getId();
        for (Move move : MOVES) {
            if (move.from != g.getStatus() || move.role != viewer.getRole()) {
                continue;
            }
            if (move.from == Status.SUBMITTED) {
                canPickUp = true;
            } else if (move.role == Role.OFFICER && !ownsIt) {
                continue;
            } else if (move.isNoteRequired()) {
                noteMoves.add(move);
            } else {
                plainMoves.add(move);
            }
        }
        return new Actions(canPickUp, noteMoves, plainMoves);
    }
}
