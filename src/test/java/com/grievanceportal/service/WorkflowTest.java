package com.grievanceportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.Role;
import com.grievanceportal.model.Status;
import com.grievanceportal.model.User;
import org.junit.jupiter.api.Test;

class WorkflowTest {

    private static User user(long id, Role role) {
        return new User(id, "u" + id, "User " + id, role);
    }

    private static Grievance grievance(Status status, Long officerId) {
        Grievance g = new Grievance();
        g.setStatus(status);
        g.setAssignedOfficerId(officerId);
        return g;
    }

    @Test
    void theAllowedMovesAreExactlyTheOnesInTheDesign() {
        int allowed = 0;
        for (Status from : Status.values()) {
            for (Status to : Status.values()) {
                for (Role role : Role.values()) {
                    if (Workflow.find(from, to, role).isPresent()) {
                        allowed++;
                    }
                }
            }
        }
        assertEquals(6, allowed);
        assertTrue(Workflow.find(Status.SUBMITTED, Status.UNDER_REVIEW, Role.OFFICER).isPresent());
        assertTrue(Workflow.find(Status.UNDER_REVIEW, Status.RESOLVED, Role.OFFICER).isPresent());
        assertTrue(Workflow.find(Status.UNDER_REVIEW, Status.REJECTED, Role.OFFICER).isPresent());
        assertTrue(Workflow.find(Status.RESOLVED, Status.CLOSED, Role.ADMIN).isPresent());
        assertTrue(Workflow.find(Status.REJECTED, Status.CLOSED, Role.ADMIN).isPresent());
        assertTrue(Workflow.find(Status.RESOLVED, Status.UNDER_REVIEW, Role.ADMIN).isPresent());
    }

    @Test
    void policyholdersMayNotMoveAnything() {
        for (Status from : Status.values()) {
            for (Status to : Status.values()) {
                assertFalse(Workflow.find(from, to, Role.POLICYHOLDER).isPresent());
            }
        }
    }

    @Test
    void anAdminMayNotDoTheOfficersMoves() {
        assertFalse(Workflow.find(Status.SUBMITTED, Status.UNDER_REVIEW, Role.ADMIN).isPresent());
        assertFalse(Workflow.find(Status.UNDER_REVIEW, Status.RESOLVED, Role.ADMIN).isPresent());
    }

    @Test
    void anOfficerMayNotCloseOrReopen() {
        assertFalse(Workflow.find(Status.RESOLVED, Status.CLOSED, Role.OFFICER).isPresent());
        assertFalse(Workflow.find(Status.RESOLVED, Status.UNDER_REVIEW, Role.OFFICER).isPresent());
    }

    @Test
    void rejectedCannotBeReopenedAndClosedIsFinal() {
        assertFalse(Workflow.find(Status.REJECTED, Status.UNDER_REVIEW, Role.ADMIN).isPresent());
        for (Status to : Status.values()) {
            for (Role role : Role.values()) {
                assertFalse(Workflow.find(Status.CLOSED, to, role).isPresent());
            }
        }
    }

    @Test
    void resolveRejectAndReopenNeedANoteButPickUpAndCloseDoNot() {
        assertTrue(Workflow.find(Status.UNDER_REVIEW, Status.RESOLVED, Role.OFFICER).orElseThrow().isNoteRequired());
        assertTrue(Workflow.find(Status.UNDER_REVIEW, Status.REJECTED, Role.OFFICER).orElseThrow().isNoteRequired());
        assertTrue(Workflow.find(Status.RESOLVED, Status.UNDER_REVIEW, Role.ADMIN).orElseThrow().isNoteRequired());
        assertFalse(Workflow.find(Status.SUBMITTED, Status.UNDER_REVIEW, Role.OFFICER).orElseThrow().isNoteRequired());
        assertFalse(Workflow.find(Status.RESOLVED, Status.CLOSED, Role.ADMIN).orElseThrow().isNoteRequired());
    }

    @Test
    void officerSeesPickUpOnASubmittedGrievance() {
        Workflow.Actions actions = Workflow.actionsFor(grievance(Status.SUBMITTED, null), user(5, Role.OFFICER));
        assertTrue(actions.isCanPickUp());
        assertTrue(actions.getNoteMoves().isEmpty());
        assertTrue(actions.isAny());
    }

    @Test
    void onlyTheAssignedOfficerSeesResolveAndReject() {
        Grievance g = grievance(Status.UNDER_REVIEW, 5L);

        Workflow.Actions owner = Workflow.actionsFor(g, user(5, Role.OFFICER));
        assertEquals(2, owner.getNoteMoves().size());
        assertEquals("Resolution note", owner.getNoteLabel());

        assertFalse(Workflow.actionsFor(g, user(6, Role.OFFICER)).isAny());
    }

    @Test
    void adminSeesCloseAndReopenOnAResolvedGrievance() {
        Workflow.Actions actions = Workflow.actionsFor(grievance(Status.RESOLVED, 5L), user(9, Role.ADMIN));
        assertEquals(1, actions.getPlainMoves().size());
        assertEquals("Close", actions.getPlainMoves().get(0).getLabel());
        assertEquals(1, actions.getNoteMoves().size());
        assertEquals("Reopen", actions.getNoteMoves().get(0).getLabel());
        assertEquals("Reason for reopening", actions.getNoteLabel());
    }

    @Test
    void adminSeesOnlyCloseOnARejectedGrievance() {
        Workflow.Actions actions = Workflow.actionsFor(grievance(Status.REJECTED, 5L), user(9, Role.ADMIN));
        assertEquals(1, actions.getPlainMoves().size());
        assertTrue(actions.getNoteMoves().isEmpty());
    }

    @Test
    void nobodyGetsActionsOnAClosedGrievanceAndPolicyholdersNeverDo() {
        for (Role role : Role.values()) {
            assertFalse(Workflow.actionsFor(grievance(Status.CLOSED, 5L), user(5, role)).isAny());
        }
        for (Status status : Status.values()) {
            assertFalse(Workflow.actionsFor(grievance(status, 5L), user(5, Role.POLICYHOLDER)).isAny());
        }
    }
}
