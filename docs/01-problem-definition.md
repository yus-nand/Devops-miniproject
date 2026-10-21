# Task 1: Problem Definition and MVP Scope

Project: Puppet-Provisioned Policyholder Grievance Portal
Author: Yash Nandivadekar (23102C0032, CMPN 'C')
Date: 21 October 2026
Status: Draft for approval

## 1. Problem statement

Insurance policyholders in India who are unhappy with a claim, a premium, a policy
document or a sales practice must complain to their insurer first. The insurer is
expected to acknowledge the complaint immediately and resolve it within 14 days. If
the policyholder is not satisfied, they can escalate to IRDAI through its Bima Bharosa
portal. In the 2025-26 financial year insurers received 2,97,468 complaints through
Bima Bharosa alone: 1,19,027 life, 1,17,979 health and 60,462 non-life. 9,972 were
still pending at year end, and 5,664 of those were health complaints.

At that volume, a complaint that reaches an insurer through a branch, an email, a
helpline or a website needs one tracked record with an owner, a status and a deadline.
Without one, the policyholder cannot see what happened to their complaint, officers
cannot see which cases are close to breaching the 14-day limit, and managers cannot
see the backlog.

The Policyholder Grievance Portal is a small web application that gives each
grievance one record from submission to closure. Policyholders submit and track their
grievances, officers work them through a controlled status workflow, and admins see a
summary dashboard.

This project is a teaching-scale MVP. It does not integrate with IRDAI or any real
insurer system and uses only synthetic data.

## 2. Target users

| User | What they need from the portal |
| --- | --- |
| Policyholder | Submit a grievance in one step, get a reference number and due date immediately, check status at any time, correct a mistake before review starts. |
| Grievance officer | See all open grievances, pick one up, move it through the workflow, record a resolution note. |
| Admin (senior or supervisor role) | See the whole backlog, spot overdue cases, close or reopen resolved grievances, view summary counts. |

## 3. Existing pain points

Items 1 and 2 follow what insurers' published grievance policies describe. Items 3 to 5
are my reasoning from that process and not sourced findings.

1. Complaints arrive through several channels (branch, email, helpline, website, Bima
   Bharosa), so one complaint can exist in several places.
2. The process has fixed steps and deadlines (immediate acknowledgement, a request for
   missing details within 7 days, resolution within 14 days) that are easy to miss
   when tracked by hand.
3. After submitting, the policyholder has no self-service view of progress.
4. Nothing records who changed a status and when, so decisions cannot be audited.
5. Managers have no quick summary of volume, status mix and overdue cases.

## 4. Stakeholders

| Stakeholder | Role in the project | Interest |
| --- | --- | --- |
| Policyholder | Primary user | Fast, transparent handling of their complaint |
| Grievance officer | Primary user | A clear queue and a simple workflow |
| Admin / supervisor | Primary user | Control over the workflow and visibility of the backlog |
| Insurer management | Sponsor (simulated) | Meeting the 14-day requirement and reducing escalations |
| IRDAI | External regulator (simulated) | Compliance with grievance timelines; no integration in the MVP |
| Developer / DevOps engineer (me) | Builds, ships and operates the system | A working, automated delivery pipeline |
| Course instructor | Evaluator | Complete deliverables and evidence for each task |

## 5. Objectives and measurable success criteria

| # | Objective | Success criterion |
| --- | --- | --- |
| O1 | Capture grievances quickly | A policyholder submits a grievance on one form of 5 fields or fewer and receives a reference ID and due date on the confirmation page. |
| O2 | Make progress visible | A policyholder sees their grievance's current status and full history within 2 clicks of logging in. |
| O3 | Support the 14-day rule | Every grievance has a due date of submission + 14 days, and overdue open grievances are flagged on the dashboard. |
| O4 | Control the workflow | 100% of status transitions not allowed for a role are rejected, covered by automated tests. |
| O5 | Give managers a summary | The dashboard shows counts by status, by category and the overdue count, and these match the list view. |
| O6 | Deliver through DevOps practice | A commit to the main branch produces a tested build and a deployed container with no manual steps. A second Puppet run on the provisioned node reports 0 changed resources. |

## 6. Constraints

- One developer and the 14-task course sequence, with each task closed before the next.
- Free tools only: Java 17, Maven, Tomcat 10.1, H2, Jenkins, Docker, Puppet, Selenium.
- Everything runs on a single Windows 11 laptop with Docker Desktop and WSL2 (WSL2 to
  be confirmed).
- The GitHub repository is public, so no secrets, passwords or real personal data are
  committed.
- Only synthetic policyholder data is used.
- Scope is frozen at the 15 items below. Anything else goes on a "later" list.

## 7. Approved MVP scope (15 items)

| # | Item | Covers |
| --- | --- | --- |
| 1 | Login and logout with three roles (seeded demo users) | Role-based access |
| 2 | Submit a grievance; reference ID, acknowledgement and due date shown at once | Create |
| 3 | Policyholder "My grievances" list | View |
| 4 | Grievance detail page with status history | View |
| 5 | Edit a grievance while it is still Submitted | Update |
| 6 | Officer and admin list of all grievances | View |
| 7 | Search and filter by reference ID, policy number, status and category | Search |
| 8 | Officer picks up a grievance (assigns it to themself) | Workflow |
| 9 | Role-based status workflow; transitions not allowed for a role are rejected | Workflow |
| 10 | Resolution note required when resolving or rejecting | Workflow |
| 11 | Admin closes or reopens a resolved grievance | Workflow |
| 12 | 14-day due date and overdue flag | SLA |
| 13 | Summary dashboard: counts by status and category, overdue count | Dashboard |
| 14 | Input validation and friendly error pages | Quality |
| 15 | Health-check endpoint (`/health`) and demo data seeding | Operations |

Draft status workflow: Submitted -> Under Review -> Resolved or Rejected -> Closed
(admin can reopen a Resolved grievance back to Under Review). The exact
transitions per role are finalised in Task 3.

Out of scope: IRDAI or Bima Bharosa integration, email or SMS notifications, file
attachments, self-registration, payments, multiple languages, escalation to the
Insurance Ombudsman.

## Sources

- Grievance timelines and escalation to IRDAI: https://ianslive.in/policyholders-can-approach-insurer-irdai-for-redress-of-insurance-related-grievances--20260814154803
- Insurer grievance handling steps, roles and deadlines (Aviva India policy): https://www.avivaindia.com/sites/default/files/Grievance-Redressal-policy-2025v1.pdf
- FY26 complaint volumes through Bima Bharosa: https://cafemutual.com/news/insurance/38460-insurance-industry-disposed-off-287-lakh-complaints-in-fy26-through-bima-bharosa