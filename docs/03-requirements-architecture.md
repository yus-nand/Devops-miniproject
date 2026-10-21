# Task 3: Requirements, Architecture and Technology Setup

Project: Puppet-Provisioned Policyholder Grievance Portal
Date: 21 October 2026
Status: Draft for approval

## 1. SRS summary

### 1.1 Purpose and scope

The portal records insurance grievances from submission to closure, enforces a
role-based status workflow, flags grievances past their 14-day due date, and shows a
summary dashboard. It is a single web application with one database. The 15 MVP items
are in `01-problem-definition.md` and their stories in `02-agile-plan.md`.

### 1.2 Functional requirements

| Group | Requirement | Stories |
| --- | --- | --- |
| Access | Users log in with a username and password and see only what their role allows. | US-01 |
| Capture | A policyholder submits a grievance and receives a reference ID and due date. | US-02 |
| Tracking | Policyholders list their own grievances. Every user can open a grievance they may see and read its status history. | US-03, US-04 |
| Update | A policyholder edits a grievance only while it is Submitted. | US-05 |
| Search | Staff list all grievances and search by reference ID or policy number, and filter by status and category. | US-06, US-07 |
| Workflow | Officers pick up grievances and resolve or reject them with a note. Admins close or reopen. All other changes are refused. | US-08, 09, 10, 11 |
| SLA | Due date is submission + 14 days. Open grievances past it are flagged Overdue. | US-12 |
| Dashboard | Admins see counts by status and category, and the overdue count. | US-13 |
| Quality | Forms validate input, and errors show friendly pages. | US-14 |
| Operations | `/health` reports status and version, and demo data seeds on first start. | US-15 |

### 1.3 Non-functional requirements

| ID | Requirement |
| --- | --- |
| NFR-01 | Security: passwords are stored as salted PBKDF2 hashes. All SQL uses prepared statements. The session cookie is HttpOnly. Every protected URL checks the role on the server. Every POST form carries a CSRF token. |
| NFR-02 | Portability: `mvn clean package` on JDK 17 produces one WAR that runs unchanged on Tomcat 10.1 on Windows, Linux and in Docker. |
| NFR-03 | Configuration: the database path and the environment name come from environment variables, never hard-coded. This is what Tasks 8, 12 and 14 will parameterise. |
| NFR-04 | Performance: list pages respond in under 2 seconds with 1,000 grievances. |
| NFR-05 | Testability: every form field and button has a stable HTML `id` for Selenium. Core flows work without JavaScript. |
| NFR-06 | Operability: version is read from the build, and logs go to the standard Tomcat log. |
| NFR-07 | Data: only synthetic data is used. No secrets are committed. |

### 1.4 Assumptions

- Users and passwords are seeded. There is no self-registration.
- One officer owns a grievance at a time.
- Time is handled in the server's local date. Due dates are dates, not timestamps.

## 2. Use-case diagram

```mermaid
flowchart LR
    PH([Policyholder])
    OF([Grievance officer])
    AD([Admin])
    subgraph Portal["Policyholder Grievance Portal"]
        U1(["Log in and out"])
        U2(["Submit grievance"])
        U3(["View own grievances"])
        U4(["View grievance and history"])
        U5(["Edit grievance while Submitted"])
        U6(["List all grievances"])
        U7(["Search and filter"])
        U8(["Pick up grievance"])
        U9(["Resolve or reject with note"])
        U10(["Close or reopen"])
        U11(["View dashboard"])
    end
    PH --- U1
    PH --- U2
    PH --- U3
    PH --- U4
    PH --- U5
    OF --- U1
    OF --- U4
    OF --- U6
    OF --- U7
    OF --- U8
    OF --- U9
    AD --- U1
    AD --- U4
    AD --- U6
    AD --- U7
    AD --- U10
    AD --- U11
```

## 3. Status workflow

```mermaid
stateDiagram-v2
    state "Under Review" as UR
    [*] --> Submitted : policyholder submits
    Submitted --> UR : officer picks up
    UR --> Resolved : officer, note required
    UR --> Rejected : officer, note required
    Resolved --> Closed : admin
    Rejected --> Closed : admin
    Resolved --> UR : admin reopens
    Closed --> [*]
```

| From | To | Allowed role | Note required |
| --- | --- | --- | --- |
| Submitted | Under Review | Officer | No |
| Under Review | Resolved | Officer | Yes |
| Under Review | Rejected | Officer | Yes |
| Resolved | Closed | Admin | No |
| Rejected | Closed | Admin | No |
| Resolved | Under Review | Admin | Yes (reason for reopening) |

Every other change is refused, and every allowed change adds a row to the status history.

## 4. Architecture

```mermaid
flowchart TB
    B["Browser"] -->|"HTTP"| T
    subgraph T["Apache Tomcat 10.1 - grievance-portal.war"]
        F["AuthFilter<br/>session, role and CSRF check"]
        C["Servlet controllers"]
        V["JSP views with JSTL"]
        S["Service layer<br/>workflow rules, due date, validation"]
        D["DAO layer<br/>JDBC, prepared statements"]
        F --> C
        C --> S
        S --> D
        C --> V
    end
    D --> DB[("H2 database file")]
```

| Layer | Package or location | Responsibility |
| --- | --- | --- |
| Filter | `com.grievanceportal.filter` | Login check, role check, CSRF token |
| Controllers | `com.grievanceportal.controller` | Read the request, call a service, pick a view |
| Services | `com.grievanceportal.service` | Business rules: workflow, due date, overdue |
| DAO | `com.grievanceportal.dao` | All SQL |
| Model | `com.grievanceportal.model` | Plain Java classes: User, Grievance, StatusChange |
| Views | `src/main/webapp/WEB-INF/views` | JSP pages, not reachable by direct URL |

Configuration (environment variables, with defaults):

| Variable | Default | Purpose |
| --- | --- | --- |
| `GP_DB_PATH` | `${catalina.base}/data/grievance-db` under Tomcat, otherwise `./data/grievance-db` | Location of the H2 database file |
| `GP_ENV_NAME` | `local` | Environment label shown in the page footer |

## 5. Technology stack (final)

| Layer | Choice |
| --- | --- |
| Language | Java 17 (Eclipse Temurin) |
| Web | Jakarta Servlet 6.0 and JSP 3.1 on Apache Tomcat 10.1 |
| View helpers | JSTL 3.0 |
| Build | Maven 3.9, packaging a WAR |
| Database | H2 2.2 in file mode |
| Unit tests | JUnit 5 |
| UI tests | Selenium WebDriver 4 with headless Chrome |
| CI and CD | Jenkins LTS, Jenkinsfile |
| Containers | Docker, Docker Hub |
| Configuration management | Puppet |

## 6. Data model

```mermaid
erDiagram
    USERS ||--o{ GRIEVANCES : submits
    USERS ||--o{ GRIEVANCES : "is assigned"
    GRIEVANCES ||--o{ STATUS_HISTORY : has
    USERS ||--o{ STATUS_HISTORY : changes
    USERS {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar full_name
        varchar role
    }
    GRIEVANCES {
        bigint id PK
        varchar ref_no UK
        bigint policyholder_id FK
        bigint assigned_officer_id FK
        varchar policy_number
        varchar category
        varchar title
        varchar description
        varchar status
        varchar resolution_note
        date created_on
        date due_on
    }
    STATUS_HISTORY {
        bigint id PK
        bigint grievance_id FK
        varchar from_status
        varchar to_status
        bigint changed_by FK
        varchar note
        timestamp changed_at
    }
```

- `role` is one of POLICYHOLDER, OFFICER, ADMIN.
- `status` is one of SUBMITTED, UNDER_REVIEW, RESOLVED, REJECTED, CLOSED.
- `category` is one of CLAIM_SETTLEMENT, PREMIUM_AND_BILLING, POLICY_SERVICING, MIS_SELLING, OTHER.
- `ref_no` looks like `GRV-2026-0001`.
- Overdue is calculated (open status and today after `due_on`), not stored.

## 7. URL list

| Method | URL | Role | Purpose | Story |
| --- | --- | --- | --- | --- |
| GET | `/login` | Anyone | Login form | US-01 |
| POST | `/login` | Anyone | Log in | US-01 |
| POST | `/logout` | Logged in | Log out | US-01 |
| GET | `/grievances/new` | Policyholder | Submission form | US-02 |
| POST | `/grievances` | Policyholder | Create a grievance | US-02 |
| GET | `/grievances/mine` | Policyholder | Own grievances | US-03 |
| GET | `/grievances/view?id=` | Owner or staff | Detail and history | US-04 |
| GET | `/grievances/edit?id=` | Policyholder | Edit form | US-05 |
| POST | `/grievances/edit` | Policyholder | Save edit | US-05 |
| GET | `/grievances` | Officer, Admin | All grievances, with `q`, `status`, `category` | US-06, US-07 |
| POST | `/grievances/pickup` | Officer | Pick up | US-08 |
| POST | `/grievances/status` | Officer, Admin | Change status per the table in section 3 | US-09, 10, 11 |
| GET | `/dashboard` | Admin | Summary counts | US-13 |
| GET | `/health` | Anyone | Status and version | US-15 |