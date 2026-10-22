# Task 9: Test Plan for the Browser Tests

Project: Puppet-Provisioned Policyholder Grievance Portal
Date: 22 October 2026
Status: Draft for approval

## 1. Purpose and scope

The 130 unit tests check the code in pieces. The browser tests check the running application the way a user meets it: they open a real browser, log in, fill in forms and read what the pages say. They cover the five journeys that matter most for the MVP stories in `02-agile-plan.md`.

Out of scope for now: editing a grievance, closing and reopening, and layout checks.

## 2. How the tests run

| Item | Choice |
| --- | --- |
| Tools | Selenium WebDriver 4 with JUnit 5, Maven |
| Browser | Brave, found automatically (Chrome or Edge are used if Brave is missing), headless by default |
| Target | an application that is already deployed, by default `http://localhost:8080/grievance-portal` |
| Command | `mvn test -Pui` |
| Normal build | `mvn test` and the Jenkins jobs skip these tests, because they carry the tag `ui` |
| Failure evidence | a screenshot is saved to `target/screenshots/<class>.<test>.png` when a test fails |
| Report | `target/surefire-reports` (text and XML), and an HTML page from `mvn -Pui test surefire-report:report-only` in `target/site/surefire-report.html` |

## 3. Test data

- The synthetic demo accounts, all with the password `demo123`: `asha` and `ravi` (policyholders), `officer1` (Meera Joshi), `admin1` (Arjun Rao).
- Every test that needs a grievance creates its own, with a title made of a short label and the current time in milliseconds, so it can find that grievance among the others.
- The database keeps its data between runs, so no test expects a fixed total. Dashboard tests compare the numbers with each other, for example "total after = total before + 1".

## 4. Journeys and test cases

| ID | Journey | Story | Test | Expected result |
| --- | --- | --- | --- | --- |
| J1 | A policyholder submits a grievance | US-02, US-03, US-14 | `SubmitGrievanceTest.submittedGrievanceGetsAReferenceAndAppearsInMyGrievances` | A reference ID like GRV-0009 and status Submitted are shown, and the title appears once in My grievances |
| J1 | Validation | US-14 | `SubmitGrievanceTest.emptyFormShowsAMessageNextToEveryField` | All four fields show an error message |
| J2 | An officer picks up and resolves | US-08, US-09, US-10 | `OfficerWorkflowTest.officerPicksUpAndResolvesAndPolicyholderSeesTheOutcome` | Under Review with Meera Joshi assigned, then Resolved with the note, and the policyholder sees Resolved |
| J2 | A note is required | US-10 | `OfficerWorkflowTest.resolvingWithoutANoteIsRefused` | An error message is shown and the status stays Under Review |
| J3 | Staff search by reference ID | US-07 | `StaffSearchTest.searchingByReferenceFindsExactlyThatGrievanceWhatEverTheCase` | Exactly one result, in upper and lower case |
| J3 | Nothing found | US-07 | `StaffSearchTest.unknownReferenceShowsANoResultsMessage` | A "No grievances match" message |
| J4 | Role restrictions | US-06, US-13 | `AccessControlTest` (4 tests) | Policyholders and officers get Access denied (403) where they should, and a visitor is sent to the login page |
| J5 | Dashboard counts | US-13 | `DashboardUiTest` (3 tests) | The total grows by one after a submission, and the count links open lists with the same number of grievances |

`FailureDemoTest` is skipped normally. Run with `-Dui.forceFail=true` it fails on purpose, which produces the failure screenshot used as evidence.

## 5. Exit criteria

- All tests pass against a freshly deployed `develop` build.
- A failed test leaves a screenshot in `target/screenshots`.
- The suite is kept out of the normal `mvn test`, until Task 10 adds it to the pipeline as a stage of its own.
