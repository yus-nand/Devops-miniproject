package com.grievanceportal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grievanceportal.model.Grievance;
import com.grievanceportal.model.GrievanceForm;
import com.grievanceportal.model.User;
import com.grievanceportal.util.Schema;
import com.grievanceportal.util.Seeder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GrievanceListTest {

    private static User asha;
    private static User ravi;
    private static User officer;

    private final GrievanceService service = new GrievanceService();

    @BeforeAll
    static void prepareDatabase() throws Exception {
        System.setProperty("GP_DB_PATH", "mem:listtest");
        Schema.init();
        Seeder.seedUsers();
        AuthService auth = new AuthService();
        asha = auth.login("asha", Seeder.DEMO_PASSWORD).orElseThrow();
        ravi = auth.login("ravi", Seeder.DEMO_PASSWORD).orElseThrow();
        officer = auth.login("officer1", Seeder.DEMO_PASSWORD).orElseThrow();
    }

    @AfterAll
    static void clearSetting() {
        System.clearProperty("GP_DB_PATH");
    }

    private static GrievanceForm form(String title) {
        return new GrievanceForm("POL-12345", "OTHER", title, "A long enough description.");
    }

    private static GrievanceService on(String day) {
        return new GrievanceService(Clock.fixed(Instant.parse(day + "T10:00:00Z"), ZoneId.of("UTC")));
    }

    private static List<String> refs(List<Grievance> list) {
        return list.stream().map(Grievance::getRefNo).collect(Collectors.toList());
    }

    @Test
    void listContainsOnlyTheUsersOwnGrievances() throws Exception {
        Grievance mine = service.submit(asha, form("Asha's grievance"));
        Grievance theirs = service.submit(ravi, form("Ravi's grievance"));

        List<Grievance> list = service.listOwn(asha);
        assertTrue(refs(list).contains(mine.getRefNo()));
        assertFalse(refs(list).contains(theirs.getRefNo()));
        for (Grievance g : list) {
            assertEquals(asha.getId(), g.getPolicyholderId());
        }
    }

    @Test
    void newestGrievanceComesFirst() throws Exception {
        Grievance older = on("2026-09-01").submit(asha, form("Older"));
        Grievance newer = on("2026-10-15").submit(asha, form("Newer"));

        List<String> refs = refs(service.listOwn(asha));
        assertTrue(refs.indexOf(newer.getRefNo()) < refs.indexOf(older.getRefNo()));
    }

    @Test
    void listHoldsEveryFieldTheTableShows() throws Exception {
        Grievance saved = service.submit(ravi, form("Fields check"));
        Grievance loaded = service.listOwn(ravi).stream()
                .filter(g -> g.getRefNo().equals(saved.getRefNo()))
                .findFirst().orElseThrow();
        assertEquals("Fields check", loaded.getTitle());
        assertEquals("Other", loaded.getCategory().getLabel());
        assertEquals("Submitted", loaded.getStatus().getLabel());
        assertEquals(saved.getDueOn(), loaded.getDueOn());
    }

    @Test
    void userWithNoGrievancesGetsAnEmptyList() throws Exception {
        assertTrue(service.listOwn(officer).isEmpty());
    }
}
